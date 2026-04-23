package com.approval.backend.service;

import com.approval.backend.common.PageResult;
import com.approval.backend.dto.request.CreateDraftRequest;
import com.approval.backend.dto.request.SubmitApplicationRequest;
import com.approval.backend.dto.response.AvailableProcessResponse;
import com.approval.backend.dto.response.CreateDraftResponse;
import com.approval.backend.dto.response.MyInstanceResponse;
import com.approval.backend.dto.response.SubmitApplicationResponse;

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
     * 新建并提交申请
     *
     * @param submitApplicationRequest 新建并提交申请请求数据传输对象
     * @param userId                   用户ID
     * @return 新建并提交申请响应数据传输对象
     */
    SubmitApplicationResponse submitApplication(SubmitApplicationRequest submitApplicationRequest, Long userId);

    /**
     * 新建草稿
     *
     * @param createDraftRequest 新建草稿请求数据传输对象
     * @param userId             用户ID
     * @return 新建草稿响应数据传输对象
     */
    CreateDraftResponse createDraft(CreateDraftRequest createDraftRequest, Long userId);

    /**
     * 修改草稿
     *
     * @param id       草稿ID
     * @param formData 表单数据
     * @param userId   用户ID
     */
    void updateDraft(Long id, String formData, Long userId);

    /**
     * 提交草稿
     *
     * @param id     草稿ID
     * @param userId 用户ID
     */
    void submitDraft(Long id, Long userId);

    /**
     * 撤回申请
     *
     * @param id     申请ID
     * @param userId 用户ID
     */
    void withdrawApplication(Long id, Long userId);

    /**
     * 修改重提
     *
     * @param id       申请ID
     * @param formData 表单数据
     * @param userId   用户ID
     */
    void resubmitApplication(Long id, String formData, Long userId);

    /**
     * 获取我的申请列表
     *
     * @param pageNum  页码
     * @param pageSize 每页数量
     * @param status   申请状态
     * @param id       用户ID
     * @return 我的申请列表
     */
    PageResult<MyInstanceResponse> getMyInstances(Integer pageNum, Integer pageSize, Integer status, Long id);
}
