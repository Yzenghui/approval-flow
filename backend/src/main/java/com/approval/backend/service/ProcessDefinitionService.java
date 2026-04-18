package com.approval.backend.service;

import com.approval.backend.common.PageResult;
import com.approval.backend.dto.request.CreateProcessRequest;
import com.approval.backend.dto.response.ProcessDefinitionResponse;
import com.approval.backend.dto.response.ProcessDefinitionDetailResponse;

/**
 * 流程定义服务接口
 */
public interface ProcessDefinitionService {
    /**
     * 分页查询流程定义列表
     *
     * @param pageNum  页码
     * @param pageSize 每页条数
     * @param keyword  搜索关键词（流程名称）
     * @param status   状态
     * @return 分页结果
     */
    PageResult<ProcessDefinitionResponse> listDefinitions(Integer pageNum, Integer pageSize, String keyword, Integer status);

    /**
     * 获取流程详情
     *
     * @param id 流程定义ID
     * @return 流程详情
     */
    ProcessDefinitionDetailResponse getProcessDefinitionDetail(Long id);

    /**
     * 新增流程
     *
     * @param createProcessRequest 新增流程请求数据传输对象
     * @return 流程定义响应数据传输对象
     */
    ProcessDefinitionResponse createProcess(CreateProcessRequest createProcessRequest);

    /**
     * 编辑流程
     *
     * @param id                   流程定义ID
     * @param createProcessRequest 编辑流程请求数据传输对象
     * @return 流程定义响应数据传输对象
     */
    ProcessDefinitionResponse updateProcess(Long id, CreateProcessRequest createProcessRequest);

    /**
     * 启用流程
     *
     * @param id 流程定义ID
     */
    void enableProcess(Long id);

    /**
     * 禁用流程
     *
     * @param id 流程定义ID
     */
    void disableProcess(Long id);
}
