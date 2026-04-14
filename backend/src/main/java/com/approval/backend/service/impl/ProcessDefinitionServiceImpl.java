package com.approval.backend.service.impl;

import com.approval.backend.common.PageResult;
import com.approval.backend.entity.ProcessDefinition;
import com.approval.backend.mapper.ProcessDefinitionMapper;
import com.approval.backend.service.ProcessDefinitionService;
import com.approval.backend.vo.ProcessDefinitionVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.stream.Collectors;

/**
 * 流程定义服务实现类
 */
@Service
public class ProcessDefinitionServiceImpl implements ProcessDefinitionService {
    private ProcessDefinitionMapper processDefinitionMapper;

    public ProcessDefinitionServiceImpl(ProcessDefinitionMapper processDefinitionMapper) {
        this.processDefinitionMapper = processDefinitionMapper;
    }

    /**
     * 获取流程定义列表
     * @param pageNum  页码
     * @param pageSize 每页条数
     * @param keyword  搜索关键词（流程名称）
     * @param status   状态
     * @return 流程定义列表
     */
    @Override
    public PageResult<ProcessDefinitionVO> listDefinitions(Integer pageNum, Integer pageSize, String keyword, Integer status) {
        LambdaQueryWrapper<ProcessDefinition> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.like(ProcessDefinition::getName, keyword);
        }

        if (status != null){
            wrapper.eq(ProcessDefinition::getStatus, status);
        }

        Page<ProcessDefinition> page = new Page<>(pageNum, pageSize);
        Page<ProcessDefinition> processDefinitionPage = processDefinitionMapper.selectPage(page, wrapper);

        PageResult<ProcessDefinitionVO> pageResult = new PageResult<>();
        pageResult.setTotal(processDefinitionPage.getTotal());
        pageResult.setRecords(processDefinitionPage.getRecords()
                .stream()
                .map(this::convertToDefinitionVO)
                .collect(Collectors.toList())
        );

        return pageResult;
    }

    /**
     * 转换为流程定义视图对象
     * @param processDefinition 流程定义
     * @return 流程定义视图对象
     */
    private ProcessDefinitionVO convertToDefinitionVO(ProcessDefinition processDefinition){
        ProcessDefinitionVO processDefinitionVO = new ProcessDefinitionVO();
        processDefinitionVO.setId(processDefinition.getId());
        processDefinitionVO.setName(processDefinition.getName());
        processDefinitionVO.setVersion(processDefinition.getVersion());
        processDefinitionVO.setStatus(processDefinition.getStatus());
        processDefinitionVO.setCreateTime(processDefinition.getCreateTime());
        processDefinitionVO.setUpdateTime(processDefinition.getUpdateTime());

        return processDefinitionVO;
    }
}
