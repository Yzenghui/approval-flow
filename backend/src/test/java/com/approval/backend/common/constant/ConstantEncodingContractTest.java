package com.approval.backend.common.constant;

import com.approval.backend.dto.message.ApprovalEventMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 常量编码契约测试：全项目唯一允许裸数字的测试
 * <p>
 * 这些数字是对 DB schema 与存量数据承诺的编码，不是本地实现细节。
 * 主代码与其余所有测试均通过常量名引用取值，常量值一旦被改动，除本文件外不会有任何测试变红。
 * </p>
 */
@DisplayName("常量编码契约")
class ConstantEncodingContractTest {

    @Test
    @DisplayName("流程实例状态：DRAFT(0) APPROVING(1) APPROVED(2) REJECTED(3) WITHDRAWN(4)")
    void processInstanceStatus() {
        assertEquals(0, ProcessInstanceStatus.DRAFT);
        assertEquals(1, ProcessInstanceStatus.APPROVING);
        assertEquals(2, ProcessInstanceStatus.APPROVED);
        assertEquals(3, ProcessInstanceStatus.REJECTED);
        assertEquals(4, ProcessInstanceStatus.WITHDRAWN);
    }

    @Test
    @DisplayName("审批任务状态：PENDING(0) COMPLETED(1) CANCELLED(2)")
    void taskStatus() {
        assertEquals(0, TaskStatus.PENDING);
        assertEquals(1, TaskStatus.COMPLETED);
        assertEquals(2, TaskStatus.CANCELLED);
    }

    @Test
    @DisplayName("启用禁用状态：DISABLED(0) ENABLED(1)")
    void enableStatus() {
        assertEquals(0, EnableStatus.DISABLED);
        assertEquals(1, EnableStatus.ENABLED);
    }

    @Test
    @DisplayName("审批操作：SUBMIT(0) APPROVE(1) REJECT(2) WITHDRAW(3) RESUBMIT(4)")
    void approvalAction() {
        assertEquals(0, ApprovalAction.SUBMIT);
        assertEquals(1, ApprovalAction.APPROVE);
        assertEquals(2, ApprovalAction.REJECT);
        assertEquals(3, ApprovalAction.WITHDRAW);
        assertEquals(4, ApprovalAction.RESUBMIT);
    }

    @Test
    @DisplayName("通知已读状态：UNREAD(0) READ(1)")
    void notificationReadStatus() {
        assertEquals(0, NotificationReadStatus.UNREAD);
        assertEquals(1, NotificationReadStatus.READ);
    }

    @Test
    @DisplayName("发件箱状态：PENDING(0) SENT(1) DEAD(2)")
    void mqOutboxStatus() {
        assertEquals(0, MqOutboxStatus.PENDING);
        assertEquals(1, MqOutboxStatus.SENT);
        assertEquals(2, MqOutboxStatus.DEAD);
    }

    @Test
    @DisplayName("通知类型：TYPE_REJECT(0) TYPE_TODO(1) TYPE_APPROVED(2)")
    void notificationType() {
        assertEquals(0, ApprovalEventMessage.TYPE_REJECT);
        assertEquals(1, ApprovalEventMessage.TYPE_TODO);
        assertEquals(2, ApprovalEventMessage.TYPE_APPROVED);
    }
}
