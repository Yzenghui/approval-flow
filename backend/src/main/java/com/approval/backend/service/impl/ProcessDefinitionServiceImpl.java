package com.approval.backend.service.impl;

import com.approval.backend.common.BusinessException;
import com.approval.backend.common.PageResult;
import com.approval.backend.dto.request.CreateProcessNodeRequest;
import com.approval.backend.dto.request.CreateProcessRequest;
import com.approval.backend.dto.response.ProcessDefinitionResponse;
import com.approval.backend.entity.ProcessDefinition;
import com.approval.backend.entity.ProcessNode;
import com.approval.backend.entity.Role;
import com.approval.backend.mapper.ProcessDefinitionMapper;
import com.approval.backend.mapper.ProcessNodeMapper;
import com.approval.backend.mapper.RoleMapper;
import com.approval.backend.service.ProcessDefinitionService;
import com.approval.backend.dto.response.ProcessNodeResponse;
import com.approval.backend.dto.response.ProcessDefinitionDetailResponse;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 流程定义服务实现类
 */
@Service
public class ProcessDefinitionServiceImpl implements ProcessDefinitionService {
    private final ProcessDefinitionMapper processDefinitionMapper;
    private final ProcessNodeMapper processNodeMapper;
    private final RoleMapper roleMapper;

    public ProcessDefinitionServiceImpl(ProcessDefinitionMapper processDefinitionMapper, ProcessNodeMapper processNodeMapper, RoleMapper roleMapper) {
        this.processDefinitionMapper = processDefinitionMapper;
        this.processNodeMapper = processNodeMapper;
        this.roleMapper = roleMapper;
    }

    /**
     * 获取流程定义列表
     *
     * @param pageNum  页码
     * @param pageSize 每页条数
     * @param keyword  搜索关键词（流程名称）
     * @param status   状态
     * @return 流程定义列表
     */
    @Override
    public PageResult<ProcessDefinitionResponse> listDefinitions(Integer pageNum, Integer pageSize, String keyword, Integer status) {
        LambdaQueryWrapper<ProcessDefinition> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.like(ProcessDefinition::getName, keyword);
        }

        if (status != null) {
            wrapper.eq(ProcessDefinition::getStatus, status);
        }

        wrapper.orderByDesc(ProcessDefinition::getCreateTime);

        Page<ProcessDefinition> page = new Page<>(pageNum, pageSize);
        Page<ProcessDefinition> processDefinitionPage = processDefinitionMapper.selectPage(page, wrapper);

        PageResult<ProcessDefinitionResponse> pageResult = new PageResult<>();
        pageResult.setTotal(processDefinitionPage.getTotal());
        pageResult.setRecords(processDefinitionPage.getRecords()
                .stream()
                .map(this::convertToDefinitionResponse)
                .collect(Collectors.toList())
        );

        return pageResult;
    }

    /**
     * 获取流程定义详情
     *
     * @param id 流程定义ID
     * @return 流程定义详情
     */
    @Override
    public ProcessDefinitionDetailResponse getDefinitionDetail(Long id) {
        ProcessDefinition processDefinition = processDefinitionMapper.selectById(id);
        if (processDefinition == null) {
            throw new BusinessException(400, "流程不存在");
        }

        LambdaQueryWrapper<ProcessNode> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ProcessNode::getDefinitionId, id);
        wrapper.orderByAsc(ProcessNode::getNodeOrder);

        List<ProcessNode> processNodes = processNodeMapper.selectList(wrapper);

        List<ProcessNodeResponse> processNodeResponses = processNodes.stream()
                .map(this::convertToNodeResponse)
                .collect(Collectors.toList());

        ProcessDefinitionDetailResponse processDefinitionDetailResponse = new ProcessDefinitionDetailResponse();
        BeanUtils.copyProperties(convertToDefinitionResponse(processDefinition), processDefinitionDetailResponse);
        processDefinitionDetailResponse.setNodes(processNodeResponses);

        return processDefinitionDetailResponse;

    }

    /**
     * 新增流程
     * @param createProcessRequest 新增流程请求数据传输对象
     * @return 流程定义响应数据传输对象
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProcessDefinitionResponse createProcess(CreateProcessRequest createProcessRequest) {
        ProcessDefinition existingProcessName = processDefinitionMapper.selectOne(
                new LambdaQueryWrapper<ProcessDefinition>()
                        .eq(ProcessDefinition::getName, createProcessRequest.getName())
        );
        if (existingProcessName != null) {
            throw new BusinessException(400, "流程名称已存在");
        }

        for (CreateProcessNodeRequest node : createProcessRequest.getNodes()) {
            String assigneeType = node.getAssigneeType();
            String assigneeExpr = node.getAssigneeExpr();

            if ("RELATIVE".equals(assigneeType)) {
                if (!assigneeExpr.matches("^manager_id(\\.manager_id){0,2}$")) {
                    throw new BusinessException(400, "分配目标值格式错误，必须是 manager_id 或上级链");
                }
            } else if ("USER".equals(assigneeType)) {
                if (!assigneeExpr.matches("^\\d+$")) {
                    throw new BusinessException(400, "分配目标值格式错误，必须是数字用户ID");
                }
            } else if ("ROLE".equals(assigneeType)) {
                Role role = roleMapper.selectOne(new LambdaQueryWrapper<Role>().eq(Role::getCode, assigneeExpr));
                if (role == null) {
                    throw new BusinessException(400, "分配目标值格式错误，角色编码不存在");
                }
            } else {
                throw new BusinessException(400, "分配策略无效");
            }
        }

        ProcessDefinition processDefinition = new ProcessDefinition();
        processDefinition.setName(createProcessRequest.getName());
        processDefinition.setVersion(1);
        processDefinition.setStatus(1);
        processDefinitionMapper.insert(processDefinition);

        List<ProcessNode> processNodes = createProcessRequest.getNodes().stream()
                .map(node -> {
                            ProcessNode processNode = new ProcessNode();
                            processNode.setDefinitionId(processDefinition.getId());
                            processNode.setNodeName(node.getNodeName());
                            processNode.setNodeOrder(node.getNodeOrder());
                            processNode.setAssigneeType(node.getAssigneeType());
                            processNode.setAssigneeExpr(node.getAssigneeExpr());
                            processNode.setConditionExpr(node.getConditionExpr());

                            return processNode;
                        }
                )
                .toList();

        Set<Integer> existingOrder = new HashSet<>();
        boolean hasDuplicateOrder = processNodes.stream()
                .anyMatch(node -> !existingOrder.add(node.getNodeOrder()));
        if (hasDuplicateOrder) {
            throw new BusinessException(400, "节点顺序不能重复");
        }

        for (ProcessNode processNode : processNodes) {
            processNodeMapper.insert(processNode);
        }

        return convertToDefinitionResponse(processDefinition);

    }

    /**
     * 转换为流程定义响应对象
     *
     * @param processDefinition 流程定义
     * @return 流程定义响应对象
     */
    private ProcessDefinitionResponse convertToDefinitionResponse(ProcessDefinition processDefinition) {
        ProcessDefinitionResponse processDefinitionResponse = new ProcessDefinitionResponse();
        BeanUtils.copyProperties(processDefinition, processDefinitionResponse);
        return processDefinitionResponse;
    }

    /**
     * 转换为流程节点响应对象
     *
     * @param processNode 流程节点
     * @return 流程节点响应对象
     */
    private ProcessNodeResponse convertToNodeResponse(ProcessNode processNode) {
        ProcessNodeResponse processNodeResponse = new ProcessNodeResponse();
        BeanUtils.copyProperties(processNode, processNodeResponse);
        return processNodeResponse;
    }
}
