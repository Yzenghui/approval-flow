package com.approval.backend.service;

import com.approval.backend.common.PageResult;
import com.approval.backend.dto.request.CreateDraftRequest;
import com.approval.backend.dto.response.AvailableProcessResponse;
import com.approval.backend.dto.response.CreateDraftResponse;

/**
 * 流程实例服务接口
 */
public interface ProcessInstanceService {
    /**
     * 获取可用流程列表
     *
     * @param pageNum  页码
     * @param pageSize 每页数量
     * @param id       用户ID
     * @return 可用流程列表
     */
    PageResult<AvailableProcessResponse> listAvailableProcesses(Integer pageNum, Integer pageSize, Long id);

    /**
     * 新建草稿
     *
     * @param createDraftRequest 新建草稿请求数据传输对象
     * @return 新建草稿响应数据传输对象
     */
    CreateDraftResponse createDraft(CreateDraftRequest createDraftRequest);

    /**
     * 修改草稿
     *
     * @param id       草稿ID
     * @param formData 表单数据
     */
    void updateDraft(Long id, String formData);
}
