package com.approval.backend.service.event;

import com.approval.backend.common.PageResult;
import com.approval.backend.config.rocketmq.RocketMQConstant;
import com.approval.backend.dto.response.MqOutboxOverviewResponse;
import com.approval.backend.dto.response.MqOutboxRecordResponse;
import com.approval.backend.entity.MqOutbox;
import com.approval.backend.common.constant.MqOutboxStatus;
import com.approval.backend.mapper.MqOutboxMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 本地消息表运维查询集成测试（真实 SQL）
 * <p>
 * 关掉扫表任务，否则后台线程会把测试数据真的发给 Broker，状态和计数当场就变，
 * 断言随即失效。
 * </p>
 */
@SpringBootTest(properties = "mq.outbox.enabled=false")
@DisplayName("本地消息表运维查询（真实 SQL）")
class MqOutboxAdminServiceIntegrationTest {

    private static final String BODY = "{\"instanceId\":10,\"taskId\":7,\"type\":2,\"recipientId\":3,\"content\":\"审批通过\"}";

    /** 每个用例一个业务键：JUnit 每个用例新建实例，天然不重复 */
    private final String msgKey = "it-outbox-" + System.nanoTime();

    private final List<Long> insertedIds = new ArrayList<>();

    @Autowired
    private MqOutboxAdminService adminService;

    @Autowired
    private MqOutboxMapper mqOutboxMapper;

    @AfterEach
    void tearDown() {
        insertedIds.forEach(mqOutboxMapper::deleteById);
    }

    @Test
    @DisplayName("概览：四个计数按状态各自变化，退避中的待发送被单独识别")
    void overviewShouldCountByStatus() {
        MqOutboxOverviewResponse before = adminService.overview();
        LocalDateTime now = LocalDateTime.now();

        // 两条都是待发送，但只有一条在退避等待，区别就在 next_retry_time
        insert(MqOutboxStatus.PENDING, 0, now.minusSeconds(1), now.minusMinutes(10), now.minusMinutes(10), null);
        insert(MqOutboxStatus.PENDING, 1, now.plusHours(1), now, now, "broker down");
        insert(MqOutboxStatus.SENT, 0, now, now, now, null);
        insert(MqOutboxStatus.DEAD, 10, now.minusHours(2), now.minusHours(3), now, "broker down");

        MqOutboxOverviewResponse after = adminService.overview();

        // 用增量而不是绝对值，因为库里可能还留着别的记录
        assertEquals(2, after.getPending() - before.getPending(), "两条待发送都应计入 pending");
        assertEquals(1, after.getWaitingBackoff() - before.getWaitingBackoff(), "只有 next_retry_time 未到的一条算退避中");
        assertEquals(1, after.getSent() - before.getSent());
        assertEquals(1, after.getDead() - before.getDead());
        assertNotNull(after.getOldestPendingTime(), "库里有待发送记录时积压时间不应为空");
    }

    @Test
    @DisplayName("概览：积压时间取待发送里最早的一条，已发送的不参与")
    void overviewShouldReportOldestPendingTime() {
        // 截断到整秒：create_time 为 DATETIME 秒精度，MySQL 会对小数秒四舍五入，
        // 带毫秒的值可能被进位 1 秒，导致落库值比 Java 对照值大，断言失败。
        LocalDateTime oldest = LocalDateTime.now().minusHours(5).truncatedTo(ChronoUnit.SECONDS);
        LocalDateTime later = LocalDateTime.now().minusMinutes(1).truncatedTo(ChronoUnit.SECONDS);
        // PENDING 行的 next_retry_time 推到未来：防止扫表线程在测试期间将到期行发出去改为 SENT，
        // 导致 MIN(create_time) 漂移。
        LocalDateTime notDue = LocalDateTime.now().plusHours(1);
        insert(MqOutboxStatus.SENT, 0, oldest, oldest, oldest, null);
        insert(MqOutboxStatus.PENDING, 0, notDue, later, later, null);
        insert(MqOutboxStatus.PENDING, 0, notDue, oldest, oldest, null);

        LocalDateTime oldestPendingTime = adminService.overview().getOldestPendingTime();

        // 库里已有更早的待发送记录时只会更早，所以断言"不晚于本用例造的最早一条"
        assertFalse(oldestPendingTime.isAfter(oldest), "积压时间应取最早的一条待发送记录");
    }

    @Test
    @DisplayName("列表：按业务键反查，状态筛选只返回该状态的记录，字段完整带出")
    void listShouldFilterByStatusAndMsgKey() {
        LocalDateTime now = LocalDateTime.now();
        Long pendingId = insert(MqOutboxStatus.PENDING, 0, now.plusHours(1), now, now, null);
        Long deadId = insert(MqOutboxStatus.DEAD, 10, now, now, now, "connect to broker failed");

        PageResult<MqOutboxRecordResponse> deadOnly = adminService.listRecords(1, 10, MqOutboxStatus.DEAD, msgKey);

        assertEquals(1L, deadOnly.getTotal());
        MqOutboxRecordResponse record = deadOnly.getRecords().getFirst();
        assertEquals(deadId, record.getId());
        assertEquals(RocketMQConstant.APPROVAL_EVENT_TOPIC, record.getTopic());
        assertEquals(msgKey, record.getMsgKey());
        assertEquals(10, record.getRetryCount());
        assertEquals("connect to broker failed", record.getErrorMsg());
        assertEquals(BODY, record.getBody(), "死信要能直接看到消息原文");

        // 换成待发送能看到另一条，说明筛选确实生效，而不是把结果整批返回
        List<Long> pendingIds = adminService.listRecords(1, 10, MqOutboxStatus.PENDING, msgKey)
                .getRecords().stream().map(MqOutboxRecordResponse::getId).toList();
        assertEquals(List.of(pendingId), pendingIds);
    }

    @Test
    @DisplayName("列表：不传状态表示不限，待发送与死信都能查到")
    void listShouldReturnAllStatusesWhenStatusAbsent() {
        LocalDateTime now = LocalDateTime.now();
        insert(MqOutboxStatus.PENDING, 0, now.plusHours(1), now, now, null);
        insert(MqOutboxStatus.DEAD, 10, now, now, now, "broker down");

        assertEquals(2L, adminService.listRecords(1, 10, null, msgKey).getTotal(),
                "不传状态时不能偷偷带上默认筛选");
    }

    @Test
    @DisplayName("列表：最近发生状态变化的排在前面，同一秒用 id 降序兜底")
    void listShouldOrderByLatestChange() {
        LocalDateTime base = LocalDateTime.of(2026, 9, 18, 10, 0, 0);
        Long earlier = insert(MqOutboxStatus.SENT, 0, base, base, base, null);
        Long sameSecondFirst = insert(MqOutboxStatus.SENT, 0, base, base, base, null);
        Long latest = insert(MqOutboxStatus.SENT, 0, base, base, base.plusSeconds(1), null);

        List<Long> ids = adminService.listRecords(1, 10, null, msgKey)
                .getRecords().stream().map(MqOutboxRecordResponse::getId).toList();

        assertEquals(List.of(latest, sameSecondFirst, earlier), ids,
                "update_time 相同时必须按 id 降序，否则顺序不稳定");
    }

    private Long insert(int status, int retryCount, LocalDateTime nextRetryTime,
            LocalDateTime createTime, LocalDateTime updateTime, String errorMsg) {
        MqOutbox record = new MqOutbox();
        record.setTopic(RocketMQConstant.APPROVAL_EVENT_TOPIC);
        record.setMsgKey(msgKey);
        record.setBody(BODY);
        record.setStatus(status);
        record.setRetryCount(retryCount);
        record.setNextRetryTime(nextRetryTime);
        record.setErrorMsg(errorMsg);
        record.setCreateTime(createTime);
        record.setUpdateTime(updateTime);

        mqOutboxMapper.insert(record);
        insertedIds.add(record.getId());
        return record.getId();
    }
}
