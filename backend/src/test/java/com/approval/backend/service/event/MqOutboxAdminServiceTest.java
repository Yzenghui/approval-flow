package com.approval.backend.service.event;

import com.approval.backend.common.BusinessException;
import com.approval.backend.common.PageResult;
import com.approval.backend.config.rocketmq.RocketMQConstant;
import com.approval.backend.dto.response.MqOutboxOverviewResponse;
import com.approval.backend.dto.response.MqOutboxRecordResponse;
import com.approval.backend.entity.MqOutbox;
import com.approval.backend.common.constant.MqOutboxStatus;
import com.approval.backend.mapper.MqOutboxMapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.AbstractWrapper;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 本地消息表运维查询单元测试（纯单测，不启动 Spring、不连数据库）
 */
@DisplayName("本地消息表运维查询服务")
class MqOutboxAdminServiceTest {

    private static final LocalDateTime REGISTERED_AT = LocalDateTime.of(2026, 9, 18, 9, 30, 0);

    private MqOutboxMapper mqOutboxMapper;
    private MqOutboxAdminService adminService;

    /**
     * Lambda 条件在 {@code getSqlSegment()} 时才解析列名，而列名来自实体元数据缓存。
     * 纯单测没有 Spring 上下文，需要手动把 MqOutbox 的 TableInfo 注册进去。
     */
    @BeforeAll
    static void initTableInfo() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), MqOutbox.class);
    }

    @BeforeEach
    void setUp() {
        mqOutboxMapper = mock(MqOutboxMapper.class);
        adminService = new MqOutboxAdminService(mqOutboxMapper);
    }

    @Test
    @DisplayName("概览：四个计数各按自己的状态口径统计，不能串号")
    void overviewShouldCountEachStatusSeparately() {
        stubCounts(7L, 2L, 30L, 1L);

        MqOutboxOverviewResponse overview = adminService.overview();

        assertEquals(7L, overview.getPending());
        assertEquals(2L, overview.getWaitingBackoff(), "退避中应是待发送里 next_retry_time 还没到点的那些");
        assertEquals(30L, overview.getSent());
        assertEquals(1L, overview.getDead());
    }

    @Test
    @DisplayName("概览：积压时间取待发送里最早登记的一条，且只读时间列")
    void overviewShouldReportOldestPendingTime() {
        stubCounts(1L, 0L, 0L, 0L);
        when(mqOutboxMapper.selectList(any())).thenReturn(List.of(pendingRow(REGISTERED_AT)));

        assertEquals(REGISTERED_AT, adminService.overview().getOldestPendingTime());

        Wrapper<MqOutbox> wrapper = capturedListWrapper();
        String sql = wrapper.getSqlSegment();
        assertTrue(sql.contains("status"), "积压时间是待发送口径，必须按状态过滤: " + sql);
        assertTrue(sql.contains("ORDER BY create_time ASC"), "要的是最早的一条: " + sql);
        assertTrue(sql.contains("LIMIT 1"), "只取一条，不该把待发送的记录整表捞出来: " + sql);
        assertTrue(wrapper.getSqlSelect().contains("create_time"), "只要时间列: " + wrapper.getSqlSelect());
        assertFalse(wrapper.getSqlSelect().contains("body"), "body 没必要读上来: " + wrapper.getSqlSelect());
    }

    @Test
    @DisplayName("概览：没有待发送记录时积压时间为空，由前端展示为无积压")
    void overviewShouldLeaveOldestPendingTimeEmptyWhenNothingPending() {
        stubCounts(0L, 0L, 12L, 0L);
        when(mqOutboxMapper.selectList(any())).thenReturn(List.of());

        MqOutboxOverviewResponse overview = adminService.overview();

        assertNull(overview.getOldestPendingTime());
        assertEquals(0L, overview.getPending());
        assertEquals(12L, overview.getSent());
    }

    @Test
    @DisplayName("列表：状态取值非法直接报错，不静默返回空列表把问题藏起来")
    void listShouldRejectIllegalStatus() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> adminService.listRecords(1, 10, 9, null));

        assertTrue(ex.getMessage().contains("状态"), "错误信息要说清是哪个参数不合法: " + ex.getMessage());
        verifyNoInteractions(mqOutboxMapper);
    }

    @Test
    @DisplayName("列表：状态与业务键都落到 SQL 上，业务键去空白后精确匹配")
    void listShouldFilterByStatusAndMsgKey() {
        stubPage(pageOf(deadRow(9L)));

        adminService.listRecords(1, 10, MqOutboxStatus.DEAD, "  7  ");

        Wrapper<MqOutbox> wrapper = capturedPageWrapper();
        String sql = wrapper.getSqlSegment();
        assertTrue(sql.contains("status"), "状态筛选必须生效: " + sql);
        assertTrue(sql.contains("msg_key"), "按业务主键反查要落在 msg_key 列上: " + sql);
        assertTrue(sql.contains("ORDER BY update_time DESC"), "刚发生状态变化的记录要排在前面: " + sql);
        assertTrue(sql.contains("id DESC"), "update_time 是秒精度，同秒内要靠 id 兜底保证顺序稳定: " + sql);
        assertTrue(paramsOf(wrapper).contains("7"), "业务键应去掉首尾空白后再匹配: " + paramsOf(wrapper));
    }

    @Test
    @DisplayName("列表：不传筛选条件时不加任何条件，只保留排序")
    void listShouldSkipBlankFilters() {
        stubPage(pageOf());

        adminService.listRecords(1, 10, null, "   ");

        String sql = capturedPageWrapper().getSqlSegment();
        assertFalse(sql.contains("msg_key"), "空白业务键不该生成查询条件: " + sql);
        assertFalse(sql.contains("status"), "不传状态表示不限，不能偷偷加上默认状态: " + sql);
    }

    @Test
    @DisplayName("列表：死信带出消息原文与失败原因，够判断能不能重放")
    void listShouldExposeBodyAndErrorForDiagnosis() {
        MqOutbox dead = deadRow(9L);
        dead.setMsgKey("7");
        dead.setRetryCount(10);
        dead.setErrorMsg("connect to broker failed");
        stubPage(pageOf(dead));

        PageResult<MqOutboxRecordResponse> result = adminService.listRecords(1, 10, MqOutboxStatus.DEAD, null);

        assertEquals(1L, result.getTotal());
        MqOutboxRecordResponse only = result.getRecords().getFirst();
        assertEquals(9L, only.getId());
        assertEquals(RocketMQConstant.APPROVAL_EVENT_TOPIC, only.getTopic());
        assertEquals("7", only.getMsgKey());
        assertEquals(MqOutboxStatus.DEAD, only.getStatus());
        assertEquals(10, only.getRetryCount());
        assertEquals("connect to broker failed", only.getErrorMsg());
        assertEquals("{\"taskId\":7}", only.getBody());
        assertEquals(REGISTERED_AT, only.getCreateTime());
        assertEquals(REGISTERED_AT, only.getUpdateTime());
    }

    @Test
    @DisplayName("列表：没有匹配记录时返回空页，而不是 null")
    void listShouldReturnEmptyPageWhenNothingMatches() {
        stubPage(pageOf());

        PageResult<MqOutboxRecordResponse> result = adminService.listRecords(1, 10, MqOutboxStatus.DEAD, "404");

        assertEquals(0L, result.getTotal());
        assertTrue(result.getRecords().isEmpty());
    }

    /**
     * 根据 SQL 条件动态路由返回值，不依赖调用顺序。
     * <p>
     * 条件不匹配时直接抛异常，避免顺序巧合掩盖 bug。
     * </p>
     */
    private void stubCounts(long pending, long waitingBackoff, long sent, long dead) {
        when(mqOutboxMapper.selectCount(any())).thenAnswer(invocation -> {
            Wrapper<MqOutbox> wrapper = invocation.getArgument(0);
            // 条件是懒解析的：先渲染一次 SQL 片段，参数才会被写进参数表
            String sql = wrapper.getSqlSegment();
            Collection<Object> params = paramsOf(wrapper);
            boolean waitingCondition = params.stream().anyMatch(LocalDateTime.class::isInstance);
            Integer status = params.stream()
                    .filter(Integer.class::isInstance)
                    .map(Integer.class::cast)
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("概览计数没有按状态过滤: " + sql));

            return switch (status) {
                case MqOutboxStatus.PENDING -> waitingCondition ? waitingBackoff : pending;
                case MqOutboxStatus.SENT -> sent;
                case MqOutboxStatus.DEAD -> dead;
                default -> throw new AssertionError("概览计数用了未定义的状态值: " + status);
            };
        });
    }

    private void stubPage(Page<MqOutbox> page) {
        when(mqOutboxMapper.selectPage(any(), any())).thenReturn(page);
    }

    private Page<MqOutbox> pageOf(MqOutbox... records) {
        Page<MqOutbox> page = new Page<>(1, 10);
        page.setRecords(List.of(records));
        page.setTotal(records.length);
        return page;
    }

    private Wrapper<MqOutbox> capturedListWrapper() {
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Wrapper<MqOutbox>> captor = ArgumentCaptor.forClass(Wrapper.class);
        verify(mqOutboxMapper).selectList(captor.capture());
        return captor.getValue();
    }

    private Wrapper<MqOutbox> capturedPageWrapper() {
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Wrapper<MqOutbox>> captor = ArgumentCaptor.forClass(Wrapper.class);
        verify(mqOutboxMapper).selectPage(any(), captor.capture());
        return captor.getValue();
    }

    /**
     * 从 Wrapper 中提取 WHERE 条件的参数值集合
     */
    private Collection<Object> paramsOf(Wrapper<MqOutbox> wrapper) {
        return ((AbstractWrapper<MqOutbox, ?, ?>) wrapper).getParamNameValuePairs().values();
    }

    private MqOutbox pendingRow(LocalDateTime createTime) {
        MqOutbox record = new MqOutbox();
        record.setId(1L);
        record.setStatus(MqOutboxStatus.PENDING);
        record.setCreateTime(createTime);
        return record;
    }

    private MqOutbox deadRow(Long id) {
        MqOutbox record = new MqOutbox();
        record.setId(id);
        record.setTopic(RocketMQConstant.APPROVAL_EVENT_TOPIC);
        record.setStatus(MqOutboxStatus.DEAD);
        record.setRetryCount(0);
        record.setBody("{\"taskId\":7}");
        record.setCreateTime(REGISTERED_AT);
        record.setUpdateTime(REGISTERED_AT);
        return record;
    }
}
