package com.approval.backend.service.impl;

import com.approval.backend.common.BusinessException;
import com.approval.backend.common.PageResult;
import com.approval.backend.service.cache.ProcessCacheHelper;
import com.approval.backend.common.RedisLockUtil;
import com.approval.backend.dto.response.CompletedTaskResponse;
import com.approval.backend.dto.response.PendingTaskResponse;
import com.approval.backend.entity.*;
import com.approval.backend.mapper.*;
import com.approval.backend.service.event.NotificationEventPublisher;
import com.approval.backend.service.TaskService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.googlecode.aviator.AviatorEvaluator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 任务服务实现类
 */
@Service
public class TaskServiceImpl implements TaskService {
    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final ProcessNodeMapper processNodeMapper;
    private final ProcessInstanceMapper processInstanceMapper;
    private final TaskMapper taskMapper;
    private final ApprovalRecordMapper approvalRecordMapper;
    private final RedisLockUtil redisLockUtil;
    private final ProcessCacheHelper processCacheHelper;
    private final NotificationEventPublisher notificationEventPublisher;

    /**
     * 复用 Spring 容器里已配置好的 ObjectMapper（JavaTimeModule、未知字段容错等）。
     * 相比在方法里 new 一个裸 mapper，既省掉每次调用的构造开销，也保证全局序列化行为一致。
     */
    private final ObjectMapper objectMapper;

    public TaskServiceImpl(UserMapper userMapper, RoleMapper roleMapper, ProcessNodeMapper processNodeMapper, ProcessInstanceMapper processInstanceMapper, TaskMapper taskMapper, ApprovalRecordMapper approvalRecordMapper, RedisLockUtil redisLockUtil, ProcessCacheHelper processCacheHelper, NotificationEventPublisher notificationEventPublisher, ObjectMapper objectMapper) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.processNodeMapper = processNodeMapper;
        this.processInstanceMapper = processInstanceMapper;
        this.taskMapper = taskMapper;
        this.approvalRecordMapper = approvalRecordMapper;
        this.redisLockUtil = redisLockUtil;
        this.processCacheHelper = processCacheHelper;
        this.notificationEventPublisher = notificationEventPublisher;
        this.objectMapper = objectMapper;
    }

    /**
     * 获取待办任务
     *
     * @param pageNum  页码
     * @param pageSize 每页数量
     * @param userId   用户ID
     * @return 待办任务
     */
    @Override
    public PageResult<PendingTaskResponse> getPendingTasks(Integer pageNum, Integer pageSize, Long userId) {
        Page<Task> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Task> queryWrapper = new LambdaQueryWrapper<Task>()
                .eq(Task::getAssigneeId, userId)
                .eq(Task::getStatus, 0)
                .orderByDesc(Task::getCreateTime);
        Page<Task> taskPage = taskMapper.selectPage(page, queryWrapper);

        List<Long> instanceIds = taskPage.getRecords().stream()
                .map(Task::getInstanceId)
                .toList();
        Map<Long, ProcessInstance> processInstanceMap = new HashMap<>();
        if (!instanceIds.isEmpty()) {
            List<ProcessInstance> processInstances = processInstanceMapper.selectBatchIds(instanceIds);
            processInstances.forEach(processInstance -> processInstanceMap.put(processInstance.getId(), processInstance));
        }

        List<Long> initiatorIds = processInstanceMap.values().stream()
                .map(ProcessInstance::getInitiatorId)
                .distinct()
                .toList();
        Map<Long, String> initiatorNameMap = new HashMap<>();
        if (!initiatorIds.isEmpty()) {
            List<User> initiators = userMapper.selectBatchIds(initiatorIds);
            initiators.forEach(initiator -> initiatorNameMap.put(initiator.getId(), initiator.getName()));
        }

        PageResult<PendingTaskResponse> pageResult = new PageResult<>();
        pageResult.setTotal(taskPage.getTotal());
        pageResult.setRecords(taskPage.getRecords().stream()
                .map(task -> {
                    PendingTaskResponse pendingTaskResponse = new PendingTaskResponse();
                    pendingTaskResponse.setTaskId(task.getId());
                    pendingTaskResponse.setInstanceId(task.getInstanceId());

                    ProcessInstance instance = processInstanceMap.get(task.getInstanceId());
                    pendingTaskResponse.setDefinitionName(instance.getDefinitionName());
                    pendingTaskResponse.setInitiatorName(initiatorNameMap.get(instance.getInitiatorId()));
                    pendingTaskResponse.setFormData(instance.getFormData());

                    pendingTaskResponse.setNodeName(task.getNodeName());
                    pendingTaskResponse.setCreateTime(task.getCreateTime());

                    return pendingTaskResponse;
                })
                .toList()
        );

        return pageResult;

    }

    /**
     * 通过任务
     *
     * @param taskId 任务ID
     * @param remark 审批意见
     * @param userId 用户ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approveTask(Long taskId, String remark, Long userId) {
        String lockKey = "task:lock:" + taskId;
        String lockValue = redisLockUtil.tryLock(lockKey, 15);
        if (lockValue == null) {
            throw new BusinessException("任务正在处理中，请勿重复提交");
        }

        try {
            LambdaQueryWrapper<Task> queryWrapper = new LambdaQueryWrapper<Task>()
                    .eq(Task::getId, taskId)
                    .eq(Task::getStatus, 0);
            Task task = taskMapper.selectOne(queryWrapper);
            if (task == null) {
                throw new BusinessException("任务不存在或已处理");
            }

            if (!task.getAssigneeId().equals(userId)) {
                throw new BusinessException("无权处理此任务");
            }

            int updated = taskMapper.update(null, new LambdaUpdateWrapper<Task>()
                    .eq(Task::getId, taskId)
                    .eq(Task::getStatus, 0)
                    .set(Task::getStatus, 1)
                    .set(Task::getUpdateTime, LocalDateTime.now()));
            if (updated == 0) {
                throw new BusinessException("任务正在处理中，请勿重复提交");
            }

            boolean hasNext = createNextTask(task);
            if (!hasNext) {
                processInstanceMapper.update(null, new LambdaUpdateWrapper<ProcessInstance>()
                        .eq(ProcessInstance::getId, task.getInstanceId())
                        .set(ProcessInstance::getStatus, 2)
                        .set(ProcessInstance::getUpdateTime, LocalDateTime.now()));

                // 没有下一个节点即流程结束，通知发起人审批通过
                ProcessInstance processInstance = processInstanceMapper.selectById(task.getInstanceId());
                notificationEventPublisher.publishApproved(processInstance, task);
            }

            ApprovalRecord approvalRecord = new ApprovalRecord();
            approvalRecord.setInstanceId(task.getInstanceId());
            approvalRecord.setOperatorId(userId);
            approvalRecord.setTaskId(task.getId());
            approvalRecord.setAction(1);
            approvalRecord.setRemark(remark);
            approvalRecordMapper.insert(approvalRecord);
        } finally {
            redisLockUtil.unlock(lockKey, lockValue);
        }
    }

    /**
     * 驳回任务
     *
     * @param taskId 任务ID
     * @param remark 审批意见
     * @param userId 用户ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rejectTask(Long taskId, String remark, Long userId) {
        String lockKey = "task:lock:" + taskId;
        String lockValue = redisLockUtil.tryLock(lockKey, 15);
        if (lockValue == null) {
            throw new BusinessException("任务正在处理中，请勿重复提交");
        }

        try {
            LambdaQueryWrapper<Task> queryWrapper = new LambdaQueryWrapper<Task>()
                    .eq(Task::getId, taskId)
                    .eq(Task::getStatus, 0);
            Task task = taskMapper.selectOne(queryWrapper);
            if (task == null) {
                throw new BusinessException("任务不存在或已处理");
            }

            if (!task.getAssigneeId().equals(userId)) {
                throw new BusinessException("无权处理此任务");
            }

            int updated = taskMapper.update(null, new LambdaUpdateWrapper<Task>()
                    .eq(Task::getId, taskId)
                    .eq(Task::getStatus, 0)
                    .set(Task::getStatus, 1)
                    .set(Task::getUpdateTime, LocalDateTime.now()));
            if (updated == 0) {
                throw new BusinessException("任务正在处理中，请勿重复提交");
            }

            processInstanceMapper.update(null, new LambdaUpdateWrapper<ProcessInstance>()
                    .eq(ProcessInstance::getId, task.getInstanceId())
                    .set(ProcessInstance::getStatus, 3)
                    .set(ProcessInstance::getUpdateTime, LocalDateTime.now()));

            ApprovalRecord approvalRecord = new ApprovalRecord();
            approvalRecord.setInstanceId(task.getInstanceId());
            approvalRecord.setOperatorId(userId);
            approvalRecord.setTaskId(task.getId());
            approvalRecord.setAction(2);
            approvalRecord.setRemark(remark);
            approvalRecordMapper.insert(approvalRecord);

            ProcessInstance processInstance = processInstanceMapper.selectById(task.getInstanceId());
            notificationEventPublisher.publishReject(processInstance, task, remark);
        } finally {
            redisLockUtil.unlock(lockKey, lockValue);
        }
    }

    /**
     * 获取已办任务
     *
     * @param pageNum  页码
     * @param pageSize 每页数量
     * @param userId   用户ID
     * @return 已办任务
     */
    @Override
    public PageResult<CompletedTaskResponse> getCompletedTasks(Integer pageNum, Integer pageSize, Long userId) {
        Page<Task> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Task> queryWrapper = new LambdaQueryWrapper<Task>()
                .eq(Task::getAssigneeId, userId)
                .eq(Task::getStatus, 1)
                .orderByDesc(Task::getUpdateTime);
        Page<Task> taskPage = taskMapper.selectPage(page, queryWrapper);

        List<Long> instanceIds = taskPage.getRecords().stream()
                .map(Task::getInstanceId)
                .toList();
        Map<Long, ProcessInstance> processInstanceMap = new HashMap<>();
        if (!instanceIds.isEmpty()) {
            List<ProcessInstance> processInstances = processInstanceMapper.selectBatchIds(instanceIds);
            processInstances.forEach(processInstance -> processInstanceMap.put(processInstance.getId(), processInstance));
        }

        List<Long> initiatorIds = processInstanceMap.values().stream()
                .map(ProcessInstance::getInitiatorId)
                .distinct()
                .toList();
        Map<Long, String> initiatorNameMap = new HashMap<>();
        if (!initiatorIds.isEmpty()) {
            List<User> initiators = userMapper.selectBatchIds(initiatorIds);
            initiators.forEach(initiator -> initiatorNameMap.put(initiator.getId(), initiator.getName()));
        }

        List<Long> taskIds = taskPage.getRecords().stream()
                .map(Task::getId)
                .toList();
        Map<Long, ApprovalRecord> approvalRecordMap = new HashMap<>();
        if (!taskIds.isEmpty()) {
            List<ApprovalRecord> approvalRecords = approvalRecordMapper.selectList(new LambdaQueryWrapper<ApprovalRecord>()
                    .in(ApprovalRecord::getTaskId, taskIds)
            );
            approvalRecords.forEach(approvalRecord -> approvalRecordMap.put(approvalRecord.getTaskId(), approvalRecord));
        }

        PageResult<CompletedTaskResponse> pageResult = new PageResult<>();
        pageResult.setTotal(taskPage.getTotal());
        pageResult.setRecords(taskPage.getRecords().stream()
                .map(task -> {
                    CompletedTaskResponse completedTaskResponse = new CompletedTaskResponse();
                    completedTaskResponse.setTaskId(task.getId());
                    completedTaskResponse.setInstanceId(task.getInstanceId());

                    ProcessInstance instance = processInstanceMap.get(task.getInstanceId());
                    completedTaskResponse.setDefinitionName(instance.getDefinitionName());
                    completedTaskResponse.setInitiatorName(initiatorNameMap.get(instance.getInitiatorId()));
                    completedTaskResponse.setFormData(instance.getFormData());

                    completedTaskResponse.setAction(approvalRecordMap.get(task.getId()).getAction());

                    completedTaskResponse.setNodeName(task.getNodeName());
                    completedTaskResponse.setCreateTime(task.getCreateTime());
                    completedTaskResponse.setCompleteTime(task.getUpdateTime());

                    return completedTaskResponse;
                })
                .toList()
        );

        return pageResult;
    }

    /**
     * 创建下一个任务
     *
     * @param currentTask 当前任务
     * @return 是否创建成功
     */
    private boolean createNextTask(Task currentTask) {
        ProcessInstance processInstance = processInstanceMapper.selectById(currentTask.getInstanceId());

        // 节点列表为读多写少数据，走 cache-aside；缓存按 node_order 升序返回
        List<ProcessNode> allNodes = processCacheHelper.getNodesByDefinitionId(processInstance.getDefinitionId());

        // 从缓存列表中定位当前节点，避免额外 DB 查询
        ProcessNode currentProcessNode = allNodes.stream()
                .filter(n -> n.getId().equals(currentTask.getNodeId()))
                .findFirst()
                .orElseGet(() -> processNodeMapper.selectById(currentTask.getNodeId()));

        List<ProcessNode> processNodes = allNodes.stream()
                .filter(n -> n.getNodeOrder() > currentProcessNode.getNodeOrder())
                .toList();

        if (processNodes.isEmpty()) {
            return false;
        }

        Map<String, Object> env;
        try {
            env = objectMapper.readValue(processInstance.getFormData(), new TypeReference<>() {
            });
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        for (ProcessNode processNode : processNodes) {
            User currentUser = userMapper.selectById(processInstance.getInitiatorId());

            if (processNode.getAssigneeType().equals("RELATIVE")) {
                String conditionExpr = processNode.getConditionExpr();
                if (StringUtils.hasText(conditionExpr)) {
                    Boolean satisfiled = (Boolean) AviatorEvaluator.execute(conditionExpr, env);
                    if (!satisfiled) {
                        continue;
                    }
                }

                String[] levels = processNode.getAssigneeExpr().split("\\.");
                for (int i = 0; i < levels.length; i++) {
                    Long managerId = currentUser.getManagerId();
                    if (managerId == null) {
                        throw new BusinessException(400, "节点【" + processNode.getNodeName() + "】审批人无法确定");
                    }
                    currentUser = userMapper.selectById(managerId);
                }
            } else if (processNode.getAssigneeType().equals("USER")) {
                currentUser = userMapper.selectById(processNode.getAssigneeExpr());
            } else if (processNode.getAssigneeType().equals("ROLE")) {
                Role role = roleMapper.selectOne(new LambdaQueryWrapper<Role>()
                        .eq(Role::getCode, processNode.getAssigneeExpr()));
                Long roleId = role.getId();
                currentUser = userMapper.selectOne(new LambdaQueryWrapper<User>()
                        .eq(User::getRoleId, roleId)
                        .eq(User::getStatus, 1)
                        .ne(User::getId, processInstance.getInitiatorId())
                        .orderByAsc(User::getId)
                        .last("LIMIT 1"));
            }

            if (currentUser == null || currentUser.getStatus() != 1) {
                throw new BusinessException(400, "节点【" + processNode.getNodeName() + "】审批人无法确定");
            }

            Task task = new Task();
            task.setInstanceId(processInstance.getId());
            task.setNodeId(processNode.getId());
            task.setNodeName(processNode.getNodeName());
            task.setAssigneeId(currentUser.getId());
            task.setSubmitRound(processInstance.getSubmitCount());
            task.setStatus(0);
            taskMapper.insert(task);

            // 任务落库后主键才回填，通知该节点的审批人
            notificationEventPublisher.publishTodo(processInstance, task);

            return true;
        }

        return false;
    }
}
