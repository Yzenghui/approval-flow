package com.approval.backend.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.approval.backend.common.BusinessException;
import com.approval.backend.common.PageResult;
import com.approval.backend.dto.request.CreateDraftRequest;
import com.approval.backend.dto.response.AvailableProcessResponse;
import com.approval.backend.dto.response.CreateDraftResponse;
import com.approval.backend.entity.ProcessDefinition;
import com.approval.backend.entity.ProcessInstance;
import com.approval.backend.entity.User;
import com.approval.backend.mapper.ProcessDefinitionMapper;
import com.approval.backend.mapper.ProcessInstanceMapper;
import com.approval.backend.mapper.RoleMapper;
import com.approval.backend.mapper.UserMapper;
import com.approval.backend.service.ProcessInstanceService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.stereotype.Service;

/**
 * 流程实例服务具体实现类
 */
@Service
public class ProcessInstanceServiceImpl implements ProcessInstanceService {
    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final ProcessDefinitionMapper processDefinitionMapper;
    private final ProcessInstanceMapper processInstanceMapper;

    public ProcessInstanceServiceImpl(UserMapper userMapper, RoleMapper roleMapper, ProcessDefinitionMapper processDefinitionMapper, ProcessInstanceMapper processInstanceMapper) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.processDefinitionMapper = processDefinitionMapper;
        this.processInstanceMapper = processInstanceMapper;
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
     * 新建草稿
     *
     * @param createDraftRequest 新建草稿请求数据传输对象
     * @return 新建草稿响应数据传输对象
     */
    @Override
    public CreateDraftResponse createDraft(CreateDraftRequest createDraftRequest) {
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
        processInstance.setInitiatorId(StpUtil.getLoginIdAsLong());
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
     */
    @Override
    public void updateDraft(Long id, String formData) {
        ProcessInstance existingDraft = processInstanceMapper.selectById(id);
        if (existingDraft == null) {
            throw new BusinessException(400, "草稿不存在");
        }

        if (existingDraft.getStatus() != 0) {
            throw new BusinessException(400, "只有草稿状态可修改");
        }

        if (!existingDraft.getInitiatorId().equals(StpUtil.getLoginIdAsLong())) {
            throw new BusinessException(400, "无权修改此草稿");
        }

        processInstanceMapper.update(null, new LambdaUpdateWrapper<ProcessInstance>()
                .eq(ProcessInstance::getId, id)
                .set(ProcessInstance::getFormData, formData)
        );
    }

}
