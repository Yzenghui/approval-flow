package com.approval.backend.service.impl;

import com.approval.backend.common.BusinessException;
import com.approval.backend.common.PageResult;
import com.approval.backend.service.cache.ProcessCacheHelper;
import com.approval.backend.dto.request.CreateDraftRequest;
import com.approval.backend.dto.request.SubmitApplicationRequest;
import com.approval.backend.dto.response.*;
import com.approval.backend.entity.*;
import com.approval.backend.mapper.*;
import com.approval.backend.service.event.NotificationEventPublisher;
import com.approval.backend.service.ProcessInstanceService;
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
import java.util.Objects;

/**
 * 流程实例服务具体实现类
 */
@Service
public class ProcessInstanceServiceImpl implements ProcessInstanceService {
    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final ProcessDefinitionMapper processDefinitionMapper;
    private final ProcessInstanceMapper processInstanceMapper;
    private final TaskMapper taskMapper;
    private final ApprovalRecordMapper approvalRecordMapper;
    private final ProcessCacheHelper processCacheHelper;
    private final NotificationEventPublisher notificationEventPublisher;

    /**
     * 复用 Spring 容器里已配置好的 ObjectMapper（JavaTimeModule、未知字段容错等）。
     * 相比在方法里 new 一个裸 mapper，既省掉每次调用的构造开销，也保证全局序列化行为一致。
     */
    private final ObjectMapper objectMapper;

    public ProcessInstanceServiceImpl(UserMapper userMapper, RoleMapper roleMapper, ProcessDefinitionMapper processDefinitionMapper, ProcessNodeMapper processNodeMapper, ProcessInstanceMapper processInstanceMapper, TaskMapper taskMapper, ApprovalRecordMapper approvalRecordMapper, ProcessCacheHelper processCacheHelper, NotificationEventPublisher notificationEventPublisher, ObjectMapper objectMapper) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.processDefinitionMapper = processDefinitionMapper;
        this.processInstanceMapper = processInstanceMapper;
        this.taskMapper = taskMapper;
        this.approvalRecordMapper = approvalRecordMapper;
        this.processCacheHelper = processCacheHelper;
        this.notificationEventPublisher = notificationEventPublisher;
        this.objectMapper = objectMapper;
    }

    /**
     * 获取可用流程列表
     *
     * @param pageNum  页码
     * @param pageSize 每页数量
     * @param id       用户ID
     * @return 可用流程列表
     */
    @Override
    public PageResult<AvailableProcessResponse> listAvailableProcesses(Integer pageNum, Integer pageSize, Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(400, "用户不存在");
        }

        String roleCode = roleMapper.selectRoleCodeByUserId(id);

        Page<ProcessDefinition> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<ProcessDefinition> wrapper = new LambdaQueryWrapper<ProcessDefinition>()
                .eq(ProcessDefinition::getStatus, 1)
                .and(w -> w
                        .eq(ProcessDefinition::getRoleCode, roleCode)
                        .or()
                        .isNull(ProcessDefinition::getRoleCode)
                )
                .orderByDesc(ProcessDefinition::getCreateTime);
        Page<ProcessDefinition> processDefinitionPage = processDefinitionMapper.selectPage(page, wrapper);

        PageResult<AvailableProcessResponse> pageResult = new PageResult<>();
        pageResult.setTotal(processDefinitionPage.getTotal());
        pageResult.setRecords(processDefinitionPage.getRecords().stream()
                .map(processDefinition -> {
                    AvailableProcessResponse availableProcessResponse = new AvailableProcessResponse();
                    availableProcessResponse.setId(processDefinition.getId());
                    availableProcessResponse.setName(processDefinition.getName());
                    availableProcessResponse.setRoleCode(processDefinition.getRoleCode());
                    availableProcessResponse.setFormSchema(processDefinition.getFormSchema());
                    return availableProcessResponse;
                })
                .toList());

        return pageResult;
    }

    /**
     * 获取单个可用流程详情
     *
     * @param definitionId 流程定义ID
     * @param userId       用户ID
     * @return 流程详情
     */
    @Override
    public AvailableProcessResponse getAvailableProcessDetail(Long definitionId, Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(400, "用户不存在");
        }

        ProcessDefinition processDefinition = processDefinitionMapper.selectById(definitionId);
        if (processDefinition == null
                || !processDefinition.getStatus().equals(1)
                || (processDefinition.getRoleCode() != null
                && !processDefinition.getRoleCode().equals(roleMapper.selectRoleCodeByUserId(userId)))) {
            throw new BusinessException(400, "流程不存在或已禁用");
        }

        AvailableProcessResponse response = new AvailableProcessResponse();
        response.setId(processDefinition.getId());
        response.setName(processDefinition.getName());
        response.setRoleCode(processDefinition.getRoleCode());
        response.setFormSchema(processDefinition.getFormSchema());
        return response;
    }

    /**
     * 新建并提交申请
     *
     * @param submitApplicationRequest 新建并提交申请请求数据传输对象
     * @param userId                   用户ID
     * @return 新建并提交申请响应数据传输对象
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public SubmitApplicationResponse submitApplication(SubmitApplicationRequest submitApplicationRequest, Long userId) {
        ProcessDefinition existingProcess = processDefinitionMapper.selectOne(new LambdaQueryWrapper<ProcessDefinition>()
                .eq(ProcessDefinition::getId, submitApplicationRequest.getDefinitionId())
                .eq(ProcessDefinition::getStatus, 1)
        );
        if (existingProcess == null) {
            throw new BusinessException(400, "流程不存在或已禁用");
        }

        ProcessInstance processInstance = new ProcessInstance();
        processInstance.setDefinitionId(existingProcess.getId());
        processInstance.setDefinitionName(existingProcess.getName());
        processInstance.setInitiatorId(userId);
        processInstance.setFormData(submitApplicationRequest.getFormData());
        processInstance.setSubmitCount(1);
        processInstance.setStatus(1);
        processInstanceMapper.insert(processInstance);

        startApprovalProcess(processInstance, existingProcess);

        ApprovalRecord record = new ApprovalRecord();
        record.setInstanceId(processInstance.getId());
        record.setOperatorId(processInstance.getInitiatorId());
        record.setTaskId(null);
        record.setAction(0);
        record.setRemark(null);
        approvalRecordMapper.insert(record);

        SubmitApplicationResponse submitApplicationResponse = new SubmitApplicationResponse();
        submitApplicationResponse.setInstanceId(processInstance.getId());

        return submitApplicationResponse;
    }

    /**
     * 新建草稿
     *
     * @param createDraftRequest 新建草稿请求数据传输对象
     * @param userId             用户ID
     * @return 新建草稿响应数据传输对象
     */
    @Override
    public CreateDraftResponse createDraft(CreateDraftRequest createDraftRequest, Long userId) {
        ProcessDefinition existingProcess = processDefinitionMapper.selectOne(new LambdaQueryWrapper<ProcessDefinition>()
                .eq(ProcessDefinition::getId, createDraftRequest.getDefinitionId())
                .eq(ProcessDefinition::getStatus, 1)
        );
        if (existingProcess == null) {
            throw new BusinessException(400, "流程不存在或已禁用");
        }

        ProcessInstance processInstance = new ProcessInstance();
        processInstance.setDefinitionId(existingProcess.getId());
        processInstance.setDefinitionName(existingProcess.getName());
        processInstance.setInitiatorId(userId);
        processInstance.setFormData(createDraftRequest.getFormData());
        processInstance.setSubmitCount(1);
        processInstance.setStatus(0);
        processInstanceMapper.insert(processInstance);

        CreateDraftResponse createDraftResponse = new CreateDraftResponse();
        createDraftResponse.setInstanceId(processInstance.getId());

        return createDraftResponse;
    }

    /**
     * 修改草稿
     *
     * @param id       草稿ID
     * @param formData 表单数据
     * @param userId   用户ID
     */
    @Override
    public void updateDraft(Long id, String formData, Long userId) {
        ProcessInstance existingDraft = processInstanceMapper.selectById(id);
        if (existingDraft == null) {
            throw new BusinessException(400, "草稿不存在");
        }

        if (existingDraft.getStatus() != 0) {
            throw new BusinessException(400, "只有草稿状态可修改");
        }

        if (!existingDraft.getInitiatorId().equals(userId)) {
            throw new BusinessException(400, "无权修改此草稿");
        }

        processInstanceMapper.update(null, new LambdaUpdateWrapper<ProcessInstance>()
                .eq(ProcessInstance::getId, id)
                .set(ProcessInstance::getFormData, formData)
                .set(ProcessInstance::getUpdateTime, LocalDateTime.now())
        );
    }

    /**
     * 提交草稿
     *
     * @param id     草稿ID
     * @param userId 用户ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitDraft(Long id, Long userId) {
        ProcessInstance existingDraft = processInstanceMapper.selectById(id);
        if (existingDraft == null) {
            throw new BusinessException(400, "申请不存在");
        }

        if (existingDraft.getStatus() != 0) {
            throw new BusinessException(400, "只有草稿状态可提交");
        }

        if (!existingDraft.getInitiatorId().equals(userId)) {
            throw new BusinessException(400, "无权提交此申请");
        }

        ProcessDefinition existingProcess = processDefinitionMapper.selectOne(new LambdaQueryWrapper<ProcessDefinition>()
                .eq(ProcessDefinition::getId, existingDraft.getDefinitionId())
                .eq(ProcessDefinition::getStatus, 1)
        );
        if (existingProcess == null) {
            throw new BusinessException(400, "流程不存在或已禁用");
        }

        processInstanceMapper.update(null, new LambdaUpdateWrapper<ProcessInstance>()
                .eq(ProcessInstance::getId, id)
                .set(ProcessInstance::getStatus, 1)
                .set(ProcessInstance::getUpdateTime, LocalDateTime.now())
        );

        startApprovalProcess(existingDraft, existingProcess);

        ApprovalRecord record = new ApprovalRecord();
        record.setInstanceId(existingDraft.getId());
        record.setOperatorId(existingDraft.getInitiatorId());
        record.setTaskId(null);
        record.setAction(0);
        record.setRemark(null);
        approvalRecordMapper.insert(record);

    }

    /**
     * 撤回申请
     *
     * @param id     申请ID
     * @param userId 用户ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void withdrawApplication(Long id, Long userId) {
        ProcessInstance existingDraft = processInstanceMapper.selectById(id);
        if (existingDraft == null) {
            throw new BusinessException(400, "申请不存在");
        }

        if (existingDraft.getStatus() != 1) {
            throw new BusinessException(400, "只有审批中的申请可撤回");
        }

        if (!existingDraft.getInitiatorId().equals(userId)) {
            throw new BusinessException(400, "无权撤回此申请");
        }

        processInstanceMapper.update(null, new LambdaUpdateWrapper<ProcessInstance>()
                .eq(ProcessInstance::getId, id)
                .set(ProcessInstance::getStatus, 4)
                .set(ProcessInstance::getUpdateTime, LocalDateTime.now())
        );

        taskMapper.update(null, new LambdaUpdateWrapper<Task>()
                .eq(Task::getInstanceId, id)
                .eq(Task::getStatus, 0)
                .set(Task::getStatus, 2)
                .set(Task::getUpdateTime, LocalDateTime.now())
        );

        ApprovalRecord record = new ApprovalRecord();
        record.setInstanceId(existingDraft.getId());
        record.setOperatorId(existingDraft.getInitiatorId());
        record.setTaskId(null);
        record.setAction(3);
        record.setRemark(null);
        approvalRecordMapper.insert(record);

    }

    /**
     * 修改重提
     *
     * @param id       申请ID
     * @param formData 表单数据
     * @param userId   用户ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resubmitApplication(Long id, String formData, Long userId) {
        ProcessInstance existingInstance = processInstanceMapper.selectById(id);
        if (existingInstance == null) {
            throw new BusinessException(400, "申请不存在");
        }

        if (existingInstance.getStatus() != 3 && existingInstance.getStatus() != 4) {
            throw new BusinessException(400, "只有驳回或撤回的申请可修改重提");
        }

        if (!existingInstance.getInitiatorId().equals(userId)) {
            throw new BusinessException(400, "无权修改此申请");
        }

        ProcessDefinition existingProcess = processDefinitionMapper.selectOne(new LambdaQueryWrapper<ProcessDefinition>()
                .eq(ProcessDefinition::getId, existingInstance.getDefinitionId())
                .eq(ProcessDefinition::getStatus, 1)
        );
        if (existingProcess == null) {
            throw new BusinessException(400, "流程不存在或已禁用");
        }

        processInstanceMapper.update(null, new LambdaUpdateWrapper<ProcessInstance>()
                .eq(ProcessInstance::getId, id)
                .set(ProcessInstance::getFormData, formData)
                .set(ProcessInstance::getSubmitCount, existingInstance.getSubmitCount() + 1)
                .set(ProcessInstance::getStatus, 1)
                .set(ProcessInstance::getUpdateTime, LocalDateTime.now())
        );

        ProcessInstance updatedInstance = processInstanceMapper.selectById(id);
        startApprovalProcess(updatedInstance, existingProcess);

        ApprovalRecord record = new ApprovalRecord();
        record.setInstanceId(existingInstance.getId());
        record.setOperatorId(existingInstance.getInitiatorId());
        record.setTaskId(null);
        record.setAction(4);
        record.setRemark(null);
        approvalRecordMapper.insert(record);

    }

    /**
     * 获取我的申请列表
     *
     * @param pageNum  页码
     * @param pageSize 每页数量
     * @param status   申请状态
     * @param userId   用户ID
     * @return 我的申请列表
     */
    @Override
    public PageResult<MyInstanceResponse> getMyInstances(Integer pageNum, Integer pageSize, Integer status, Long userId) {
        LambdaQueryWrapper<ProcessInstance> queryWrapper = new LambdaQueryWrapper<ProcessInstance>()
                .eq(ProcessInstance::getInitiatorId, userId)
                .orderByDesc(ProcessInstance::getCreateTime);
        if (status != null) {
            queryWrapper.eq(ProcessInstance::getStatus, status);
        }

        Page<ProcessInstance> page = new Page<>(pageNum, pageSize);
        Page<ProcessInstance> processInstancePage = processInstanceMapper.selectPage(page, queryWrapper);

        PageResult<MyInstanceResponse> pageResult = new PageResult<>();
        pageResult.setTotal(processInstancePage.getTotal());
        pageResult.setRecords(processInstancePage.getRecords().stream()
                .map(processInstance -> {
                    MyInstanceResponse myInstanceResponse = new MyInstanceResponse();
                    myInstanceResponse.setId(processInstance.getId());
                    myInstanceResponse.setDefinitionId(processInstance.getDefinitionId());
                    myInstanceResponse.setDefinitionName(processInstance.getDefinitionName());
                    myInstanceResponse.setInitiatorId(processInstance.getInitiatorId());
                    myInstanceResponse.setFormData(processInstance.getFormData());
                    myInstanceResponse.setSubmitCount(processInstance.getSubmitCount());
                    myInstanceResponse.setStatus(processInstance.getStatus());
                    myInstanceResponse.setCreateTime(processInstance.getCreateTime());
                    myInstanceResponse.setUpdateTime(processInstance.getUpdateTime());

                    return myInstanceResponse;
                })
                .toList()
        );

        return pageResult;
    }

    /**
     * 获取流程实例详情
     *
     * @param instanceId 流程实例ID
     * @param userId     用户ID
     * @return 流程实例详情
     */
    @Override
    public ProcessInstanceDetailResponse getInstanceDetail(Long instanceId, Long userId) {
        ProcessInstance processInstance = processInstanceMapper.selectById(instanceId);
        if (processInstance == null) {
            throw new BusinessException(400, "申请不存在");
        }

        boolean isInitiator = processInstance.getInitiatorId().equals(userId);
        boolean isAssignee = taskMapper.exists(new LambdaQueryWrapper<Task>()
                .eq(Task::getInstanceId, instanceId)
                .eq(Task::getAssigneeId, userId));
        if (!isInitiator && !isAssignee) {
            throw new BusinessException(403, "无权查看此申请");
        }

        ProcessInstanceDetailResponse processInstanceDetailResponse = new ProcessInstanceDetailResponse();
        processInstanceDetailResponse.setId(processInstance.getId());
        processInstanceDetailResponse.setDefinitionId(processInstance.getDefinitionId());
        processInstanceDetailResponse.setDefinitionName(processInstance.getDefinitionName());
        processInstanceDetailResponse.setInitiatorId(processInstance.getInitiatorId());
        processInstanceDetailResponse.setFormData(processInstance.getFormData());
        processInstanceDetailResponse.setSubmitCount(processInstance.getSubmitCount());
        processInstanceDetailResponse.setStatus(processInstance.getStatus());
        processInstanceDetailResponse.setCreateTime(processInstance.getCreateTime());
        processInstanceDetailResponse.setUpdateTime(processInstance.getUpdateTime());

        List<ApprovalRecord> approvalRecords = approvalRecordMapper.selectList(new LambdaQueryWrapper<ApprovalRecord>()
                .eq(ApprovalRecord::getInstanceId, instanceId)
                .orderByAsc(ApprovalRecord::getCreateTime)
        );

        List<Long> operatorIds = approvalRecords.stream()
                .map(ApprovalRecord::getOperatorId)
                .distinct()
                .toList();
        Map<Long, String> operatorNameMap = new HashMap<>();
        if (!operatorIds.isEmpty()) {
            List<User> users = userMapper.selectBatchIds(operatorIds);
            users.forEach(user -> operatorNameMap.put(user.getId(), user.getName()));
        }

        List<Long> taskIds = approvalRecords.stream()
                .map(ApprovalRecord::getTaskId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, Task> taskMap = new HashMap<>();
        if (!taskIds.isEmpty()) {
            List<Task> tasks = taskMapper.selectBatchIds(taskIds);
            tasks.forEach(task -> taskMap.put(task.getId(), task));
        }

        processInstanceDetailResponse.setRecords(approvalRecords.stream()
                .map(approvalRecord -> {
                    ApprovalTrajectoryResponse approvalTrajectoryResponse = new ApprovalTrajectoryResponse();
                    approvalTrajectoryResponse.setOperatorName(operatorNameMap.get(approvalRecord.getOperatorId()));
                    approvalTrajectoryResponse.setAction(approvalRecord.getAction());

                    Task task = taskMap.get(approvalRecord.getTaskId());
                    if (task != null) {
                        approvalTrajectoryResponse.setSubmitRound(task.getSubmitRound());
                        approvalTrajectoryResponse.setNodeName(task.getNodeName());
                    } else {
                        approvalTrajectoryResponse.setSubmitRound(null);
                        approvalTrajectoryResponse.setNodeName(null);
                    }

                    approvalTrajectoryResponse.setRemark(approvalRecord.getRemark());
                    approvalTrajectoryResponse.setCreateTime(approvalRecord.getCreateTime());

                    return approvalTrajectoryResponse;
                })
                .toList()
        );

        return processInstanceDetailResponse;

    }

    /**
     * 启动审批流程：查找第一个满足条件的节点并生成任务
     *
     * @param processInstance   流程实例
     * @param processDefinition 流程定义
     */
    private void startApprovalProcess(ProcessInstance processInstance, ProcessDefinition processDefinition) {
        // 节点列表为读多写少数据，走 cache-aside；缓存按 node_order 升序返回
        List<ProcessNode> processNodes = processCacheHelper.getNodesByDefinitionId(processDefinition.getId());

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

            break;
        }
    }

}
