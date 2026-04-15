package com.approval.backend.service.impl;

import com.approval.backend.common.BusinessException;
import com.approval.backend.common.PageResult;
import com.approval.backend.dto.response.ProcessDefinitionResponse;
import com.approval.backend.entity.ProcessDefinition;
import com.approval.backend.entity.ProcessNode;
import com.approval.backend.mapper.ProcessDefinitionMapper;
import com.approval.backend.mapper.ProcessNodeMapper;
import com.approval.backend.service.ProcessDefinitionService;
import com.approval.backend.dto.response.ProcessNodeResponse;
import com.approval.backend.dto.response.ProcessDefinitionDetailResponse;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 流程定义服务实现类
 */
@Service
public class ProcessDefinitionServiceImpl implements ProcessDefinitionService {
    private final ProcessDefinitionMapper processDefinitionMapper;
    private final ProcessNodeMapper processNodeMapper;

    public ProcessDefinitionServiceImpl(ProcessDefinitionMapper processDefinitionMapper, ProcessNodeMapper processNodeMapper) {
        this.processDefinitionMapper = processDefinitionMapper;
        this.processNodeMapper = processNodeMapper;
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
