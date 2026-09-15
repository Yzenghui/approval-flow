package com.approval.backend.service.impl;

import com.approval.backend.common.BusinessException;
import com.approval.backend.common.PageResult;
import com.approval.backend.service.cache.ProcessCacheHelper;
import com.approval.backend.dto.request.CreateProcessNodeRequest;
import com.approval.backend.dto.request.CreateProcessRequest;
import com.approval.backend.dto.response.ProcessDefinitionResponse;
import com.approval.backend.entity.ProcessDefinition;
import com.approval.backend.entity.ProcessInstance;
import com.approval.backend.entity.ProcessNode;
import com.approval.backend.entity.Role;
import com.approval.backend.mapper.ProcessDefinitionMapper;
import com.approval.backend.mapper.ProcessInstanceMapper;
import com.approval.backend.mapper.ProcessNodeMapper;
import com.approval.backend.mapper.RoleMapper;
import com.approval.backend.service.ProcessDefinitionService;
import com.approval.backend.dto.response.ProcessNodeResponse;
import com.approval.backend.dto.response.ProcessDefinitionDetailResponse;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
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
    private final ProcessInstanceMapper processInstanceMapper;
    private final ProcessCacheHelper processCacheHelper;

    public ProcessDefinitionServiceImpl(ProcessDefinitionMapper processDefinitionMapper, ProcessNodeMapper processNodeMapper, RoleMapper roleMapper, ProcessInstanceMapper processInstanceMapper, ProcessCacheHelper processCacheHelper) {
        this.processDefinitionMapper = processDefinitionMapper;
        this.processNodeMapper = processNodeMapper;
        this.roleMapper = roleMapper;
        this.processInstanceMapper = processInstanceMapper;
        this.processCacheHelper = processCacheHelper;
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
    public PageResult<ProcessDefinitionResponse> listProcessDefinitions(Integer pageNum, Integer pageSize, String keyword, Integer status) {
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
                .map(this::convertToProcessDefinitionResponse)
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
    public ProcessDefinitionDetailResponse getProcessDefinitionDetail(Long id) {
        ProcessDefinition processDefinition = processCacheHelper.getProcessDefinition(id);
        if (processDefinition == null) {
            throw new BusinessException(400, "流程不存在");
        }

        List<ProcessNode> processNodes = processCacheHelper.getNodesByDefinitionId(id);

        List<ProcessNodeResponse> processNodeResponses = processNodes.stream()
                .map(this::convertToProcessNodeResponse)
                .collect(Collectors.toList());

        ProcessDefinitionDetailResponse processDefinitionDetailResponse = new ProcessDefinitionDetailResponse();
        BeanUtils.copyProperties(convertToProcessDefinitionResponse(processDefinition), processDefinitionDetailResponse);
        processDefinitionDetailResponse.setNodes(processNodeResponses);

        return processDefinitionDetailResponse;

    }

    /**
     * 新增流程
     *
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

        Role existingRoleCode = roleMapper.selectOne(new LambdaQueryWrapper<Role>().eq(Role::getCode, createProcessRequest.getRoleCode()));
        if (existingRoleCode == null) {
            throw new BusinessException(400, "适用角色编码无效");
        }

        for (CreateProcessNodeRequest node : createProcessRequest.getNodes()) {
            validateNode(node);
        }

        ProcessDefinition processDefinition = new ProcessDefinition();
        processDefinition.setName(createProcessRequest.getName());
        processDefinition.setVersion(1);
        processDefinition.setRoleCode(createProcessRequest.getRoleCode());
        processDefinition.setStatus(1);
        processDefinition.setFormSchema(createProcessRequest.getFormSchema());
        processDefinitionMapper.insert(processDefinition);

        List<ProcessNode> processNodes = createProcessRequest.getNodes().stream()
                .map(node -> convertToNodeEntity(node, processDefinition.getId()))
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

        return convertToProcessDefinitionResponse(processDefinition);

    }

    /**
     * 编辑流程
     *
     * @param id                   流程定义ID
     * @param createProcessRequest 编辑流程请求数据传输对象
     * @return 流程定义响应数据传输对象
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProcessDefinitionResponse updateProcess(Long id, CreateProcessRequest createProcessRequest) {
        ProcessDefinition existingDefinition = processDefinitionMapper.selectById(id);
        if (existingDefinition == null) {
            throw new BusinessException(400, "流程不存在");
        }

        long count = processInstanceMapper.selectCount(new LambdaQueryWrapper<ProcessInstance>()
                .eq(ProcessInstance::getDefinitionId, id)
                .eq(ProcessInstance::getStatus, 1));
        if (count > 0) {
            throw new BusinessException(400, "当前流程有进行中的审批，无法编辑");
        }

        ProcessDefinition existingProcessName = processDefinitionMapper.selectOne(
                new LambdaQueryWrapper<ProcessDefinition>()
                        .eq(ProcessDefinition::getName, createProcessRequest.getName())
                        .ne(ProcessDefinition::getId, id)
        );
        if (existingProcessName != null) {
            throw new BusinessException(400, "流程名称已存在");
        }

        Role existingRoleCode = roleMapper.selectOne(
                new LambdaQueryWrapper<Role>()
                        .eq(Role::getCode, createProcessRequest.getRoleCode())
        );
        if (existingRoleCode == null) {
            throw new BusinessException(400, "适用角色编码无效");
        }

        for (CreateProcessNodeRequest node : createProcessRequest.getNodes()) {
            validateNode(node);
        }

        List<ProcessNode> processNodes = createProcessRequest.getNodes().stream()
                .map(node -> convertToNodeEntity(node, id))
                .toList();

        Set<Integer> existingOrder = new HashSet<>();
        boolean hasDuplicateOrder = processNodes.stream()
                .anyMatch(node -> !existingOrder.add(node.getNodeOrder()));
        if (hasDuplicateOrder) {
            throw new BusinessException(400, "节点顺序不能重复");
        }

        processDefinitionMapper.update(null, new LambdaUpdateWrapper<ProcessDefinition>()
                .eq(ProcessDefinition::getId, id)
                .set(ProcessDefinition::getName, createProcessRequest.getName())
                .set(ProcessDefinition::getRoleCode, createProcessRequest.getRoleCode())
                .set(ProcessDefinition::getVersion, existingDefinition.getVersion() + 1)
                .set(ProcessDefinition::getFormSchema, createProcessRequest.getFormSchema())
                .set(ProcessDefinition::getUpdateTime, LocalDateTime.now())
        );

        processNodeMapper.delete(new LambdaQueryWrapper<ProcessNode>()
                .eq(ProcessNode::getDefinitionId, id));

        for (ProcessNode processNode : processNodes) {
            processNodeMapper.insert(processNode);
        }

        // 流程定义与节点均被修改，失效相关缓存
        // 延迟到事务提交后删除，避免「缓存已删、事务未提交、并发请求回源写回旧值」的窗口
        processCacheHelper.evictDefinitionAfterCommit(id);

        ProcessDefinition updatedProcessDefinition = processDefinitionMapper.selectById(id);

        return convertToProcessDefinitionResponse(updatedProcessDefinition);

    }

    /**
     * 启用流程
     *
     * @param id 流程定义ID
     */
    @Override
    public void enableProcess(Long id) {
        ProcessDefinition processDefinition = processDefinitionMapper.selectById(id);
        if (processDefinition == null) {
            throw new BusinessException(400, "流程不存在");
        }

        processDefinitionMapper.update(null, new LambdaUpdateWrapper<ProcessDefinition>()
                .eq(ProcessDefinition::getId, id)
                .set(ProcessDefinition::getStatus, 1)
                .set(ProcessDefinition::getUpdateTime, LocalDateTime.now())
        );

        // 状态变更后失效缓存，避免读到旧的 status 值，删除异常由 ProcessCacheHelper 内部兜底
        processCacheHelper.evictDefinition(id);
    }

    /**
     * 禁用流程
     *
     * @param id 流程定义ID
     */
    @Override
    public void disableProcess(Long id) {
        ProcessDefinition processDefinition = processDefinitionMapper.selectById(id);
        if (processDefinition == null) {
            throw new BusinessException(400, "流程不存在");
        }

        processDefinitionMapper.update(null, new LambdaUpdateWrapper<ProcessDefinition>()
                .eq(ProcessDefinition::getId, id)
                .set(ProcessDefinition::getStatus, 0)
                .set(ProcessDefinition::getUpdateTime, LocalDateTime.now())
        );

        // 状态变更后失效缓存，避免读到旧的 status 值，删除异常由 ProcessCacheHelper 内部兜底
        processCacheHelper.evictDefinition(id);
    }

    /**
     * 转换为流程定义响应对象
     *
     * @param processDefinition 流程定义
     * @return 流程定义响应对象
     */
    private ProcessDefinitionResponse convertToProcessDefinitionResponse(ProcessDefinition processDefinition) {
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
    private ProcessNodeResponse convertToProcessNodeResponse(ProcessNode processNode) {
        ProcessNodeResponse processNodeResponse = new ProcessNodeResponse();
        BeanUtils.copyProperties(processNode, processNodeResponse);
        return processNodeResponse;
    }

    /**
     * 转换为流程节点实体对象
     *
     * @param node         流程节点
     * @param definitionId 流程定义ID
     * @return 流程节点实体对象
     */
    private ProcessNode convertToNodeEntity(CreateProcessNodeRequest node, Long definitionId) {
        ProcessNode processNode = new ProcessNode();
        processNode.setDefinitionId(definitionId);
        processNode.setNodeName(node.getNodeName());
        processNode.setNodeOrder(node.getNodeOrder());
        processNode.setAssigneeType(node.getAssigneeType());
        processNode.setAssigneeExpr(node.getAssigneeExpr());
        processNode.setConditionExpr(node.getConditionExpr());
        return processNode;
    }

    /**
     * 校验节点分配策略与目标值
     */
    private void validateNode(CreateProcessNodeRequest node) {
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

}
