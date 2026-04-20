package com.approval.backend.service.impl;

import com.approval.backend.common.BusinessException;
import com.approval.backend.common.PageResult;
import com.approval.backend.dto.response.AvailableProcessResponse;
import com.approval.backend.entity.ProcessDefinition;
import com.approval.backend.entity.User;
import com.approval.backend.mapper.ProcessDefinitionMapper;
import com.approval.backend.mapper.RoleMapper;
import com.approval.backend.mapper.UserMapper;
import com.approval.backend.service.ProcessInstanceService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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

    public ProcessInstanceServiceImpl(UserMapper userMapper, RoleMapper roleMapper, ProcessDefinitionMapper processDefinitionMapper) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.processDefinitionMapper = processDefinitionMapper;
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

}
