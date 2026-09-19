package com.approval.backend.service.event;

import com.approval.backend.common.BusinessException;
import com.approval.backend.common.PageResult;
import com.approval.backend.dto.response.MqOutboxOverviewResponse;
import com.approval.backend.dto.response.MqOutboxRecordResponse;
import com.approval.backend.entity.MqOutbox;
import com.approval.backend.common.constant.MqOutboxStatus;
import com.approval.backend.mapper.MqOutboxMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 本地消息表运维查询服务
 * <p>
 * 只解决：投递有没有积压、有没有死信、死信里是什么内容。
 * </p>
 */
@Service
public class MqOutboxAdminService {

    private final MqOutboxMapper mqOutboxMapper;

    public MqOutboxAdminService(MqOutboxMapper mqOutboxMapper) {
        this.mqOutboxMapper = mqOutboxMapper;
    }

    /**
     * 投递概览：各状态条数 + 最早一条待发送记录的登记时间。
     *
     * @return 概览数据
     */
    public MqOutboxOverviewResponse overview() {
        // 所有统计共用同一个基准时间，避免几条 count 之间因为时间推进而自相矛盾
        LocalDateTime now = LocalDateTime.now();

        MqOutboxOverviewResponse response = new MqOutboxOverviewResponse();
        response.setPending(countByStatus(MqOutboxStatus.PENDING));
        response.setWaitingBackoff(mqOutboxMapper.selectCount(new LambdaQueryWrapper<MqOutbox>()
                .eq(MqOutbox::getStatus, MqOutboxStatus.PENDING)
                .gt(MqOutbox::getNextRetryTime, now)));
        response.setSent(countByStatus(MqOutboxStatus.SENT));
        response.setDead(countByStatus(MqOutboxStatus.DEAD));
        response.setOldestPendingTime(oldestPendingTime());
        return response;
    }

    /**
     * 分页查询消息记录
     *
     * @param pageNum  页码
     * @param pageSize 每页条数
     * @param status   状态筛选：0待发送 1已发送 2死信，null 表示不限
     * @param msgKey   消息业务键精确匹配（当前即 taskId），null 或空白表示不限
     * @return 分页记录
     */
    public PageResult<MqOutboxRecordResponse> listRecords(Integer pageNum, Integer pageSize, Integer status, String msgKey) {
        if (status != null && !MqOutboxStatus.isValid(status)) {
            throw new BusinessException("状态取值非法，只支持 0待发送 / 1已发送 / 2死信");
        }

        String key = msgKey == null ? null : msgKey.trim();
        boolean filterByKey = key != null && !key.isEmpty();

        Page<MqOutbox> page = mqOutboxMapper.selectPage(new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<MqOutbox>()
                        .eq(status != null, MqOutbox::getStatus, status)
                        .eq(filterByKey, MqOutbox::getMsgKey, key)
                        // update_time 是秒精度，同秒内顺序不稳定，用 id 降序兜底
                        .orderByDesc(MqOutbox::getUpdateTime)
                        .orderByDesc(MqOutbox::getId));

        PageResult<MqOutboxRecordResponse> result = new PageResult<>();
        result.setTotal(page.getTotal());
        result.setRecords(page.getRecords().stream()
                .map(MqOutboxAdminService::toResponse)
                .toList());
        return result;
    }

    private Long countByStatus(int status) {
        return mqOutboxMapper.selectCount(new LambdaQueryWrapper<MqOutbox>()
                .eq(MqOutbox::getStatus, status));
    }

    /**
     * 积压最久的一条：只读时间列
     */
    private LocalDateTime oldestPendingTime() {
        List<MqOutbox> rows = mqOutboxMapper.selectList(new LambdaQueryWrapper<MqOutbox>()
                .select(MqOutbox::getCreateTime)
                .eq(MqOutbox::getStatus, MqOutboxStatus.PENDING)
                .orderByAsc(MqOutbox::getCreateTime)
                .last("LIMIT 1"));
        return rows.isEmpty() ? null : rows.getFirst().getCreateTime();
    }

    /**
     * 实体转响应对象
     */
    private static MqOutboxRecordResponse toResponse(MqOutbox record) {
        MqOutboxRecordResponse response = new MqOutboxRecordResponse();
        response.setId(record.getId());
        response.setTopic(record.getTopic());
        response.setMsgKey(record.getMsgKey());
        response.setStatus(record.getStatus());
        response.setRetryCount(record.getRetryCount());
        response.setNextRetryTime(record.getNextRetryTime());
        response.setErrorMsg(record.getErrorMsg());
        response.setBody(record.getBody());
        response.setCreateTime(record.getCreateTime());
        response.setUpdateTime(record.getUpdateTime());
        return response;
    }
}
