package com.approval.backend.service.impl;

import com.approval.backend.common.BusinessException;
import com.approval.backend.common.PageResult;
import com.approval.backend.dto.request.CreateDraftRequest;
import com.approval.backend.dto.request.SubmitApplicationRequest;
import com.approval.backend.dto.response.AvailableProcessResponse;
import com.approval.backend.dto.response.CreateDraftResponse;
import com.approval.backend.dto.response.MyInstanceResponse;
import com.approval.backend.dto.response.SubmitApplicationResponse;
import com.approval.backend.entity.*;
import com.approval.backend.mapper.*;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 流程实例服务具体实现类
 */
@Service
public class ProcessInstanceServiceImpl implements ProcessInstanceService {
    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final ProcessDefinitionMapper processDefinitionMapper;
    private final ProcessNodeMapper processNodeMapper;
    private final ProcessInstanceMapper processInstanceMapper;
    private final TaskMapper taskMapper;
    private final ApprovalRecordMapper approvalRecordMapper;

    public ProcessInstanceServiceImpl(UserMapper userMapper, RoleMapper roleMapper, ProcessDefinitionMapper processDefinitionMapper, ProcessNodeMapper processNodeMapper, ProcessInstanceMapper processInstanceMapper, TaskMapper taskMapper, ApprovalRecordMapper approvalRecordMapper) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.processDefinitionMapper = processDefinitionMapper;
        this.processNodeMapper = processNodeMapper;
        this.processInstanceMapper = processInstanceMapper;
        this.taskMapper = taskMapper;
        this.approvalRecordMapper = approvalRecordMapper;
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
                    return availableProcessResponse;
                })
                .toList());

        return pageResult;
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
        );

        taskMapper.update(null, new LambdaUpdateWrapper<Task>()
                .eq(Task::getInstanceId, id)
                .eq(Task::getStatus, 0)
                .set(Task::getStatus, 2)
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
                    myInstanceResponse.setDefinitionName(processInstance.getDefinitionName());
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
     * 启动审批流程：查找第一个满足条件的节点并生成任务
     *
     * @param processInstance   流程实例
     * @param processDefinition 流程定义
     */
    private void startApprovalProcess(ProcessInstance processInstance, ProcessDefinition processDefinition) {
        List<ProcessNode> processNodes = processNodeMapper.selectList(
                new LambdaQueryWrapper<ProcessNode>()
                        .eq(ProcessNode::getDefinitionId, processDefinition.getId())
                        .orderByAsc(ProcessNode::getNodeOrder)
        );

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
            task.setSubmitCount(processInstance.getSubmitCount());
            task.setStatus(0);
            taskMapper.insert(task);

            break;
        }
    }

}
