package com.approval.backend.service.impl;

import com.approval.backend.common.BusinessException;
import com.approval.backend.common.PageResult;
import com.approval.backend.dto.response.PendingTaskResponse;
import com.approval.backend.entity.*;
import com.approval.backend.mapper.*;
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

    public TaskServiceImpl(UserMapper userMapper, RoleMapper roleMapper, ProcessNodeMapper processNodeMapper, ProcessInstanceMapper processInstanceMapper, TaskMapper taskMapper, ApprovalRecordMapper approvalRecordMapper) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.processNodeMapper = processNodeMapper;
        this.processInstanceMapper = processInstanceMapper;
        this.taskMapper = taskMapper;
        this.approvalRecordMapper = approvalRecordMapper;
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

        List<Long> taskIds = taskPage.getRecords().stream()
                .map(Task::getInstanceId)
                .toList();
        Map<Long, ProcessInstance> processInstanceMap = new HashMap<>();
        if (!taskIds.isEmpty()) {
            List<ProcessInstance> processInstances = processInstanceMapper.selectBatchIds(taskIds);
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
        LambdaQueryWrapper<Task> queryWrapper = new LambdaQueryWrapper<Task>()
                .eq(Task::getId, taskId)
                .eq(Task::getStatus, 0L);
        Task task = taskMapper.selectOne(queryWrapper);
        if (task == null) {
            throw new BusinessException("任务不存在或已处理");
        }

        if (!task.getAssigneeId().equals(userId)) {
            throw new BusinessException("无权处理此任务");
        }

        taskMapper.update(null, new LambdaUpdateWrapper<Task>()
                .eq(Task::getId, taskId)
                .set(Task::getStatus, 1L));

        boolean hasNext = createNextTask(task);
        if (!hasNext) {
            processInstanceMapper.update(null, new LambdaUpdateWrapper<ProcessInstance>()
                    .eq(ProcessInstance::getId, task.getInstanceId())
                    .set(ProcessInstance::getStatus, 2));
        }

        ApprovalRecord approvalRecord = new ApprovalRecord();
        approvalRecord.setInstanceId(task.getInstanceId());
        approvalRecord.setOperatorId(userId);
        approvalRecord.setTaskId(task.getId());
        approvalRecord.setAction(1);
        approvalRecord.setRemark(remark);
        approvalRecordMapper.insert(approvalRecord);

    }

    /**
     * 创建下一个任务
     *
     * @param currentTask 当前任务
     * @return 是否创建成功
     */
    private boolean createNextTask(Task currentTask) {
        ProcessNode currentProcessNode = processNodeMapper.selectById(currentTask.getNodeId());
        List<ProcessNode> processNodes = processNodeMapper.selectList(new LambdaQueryWrapper<ProcessNode>()
                .eq(ProcessNode::getDefinitionId, currentProcessNode.getDefinitionId())
                .gt(ProcessNode::getNodeOrder, currentProcessNode.getNodeOrder())
                .orderByAsc(ProcessNode::getNodeOrder)
        );

        if (processNodes.isEmpty()) {
            return false;
        }

        ProcessInstance processInstance = processInstanceMapper.selectById(currentTask.getInstanceId());
        ObjectMapper objectMapper = new ObjectMapper();
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

            return true;
        }

        return false;
    }
}
