package com.approval.backend.common;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 事务后置动作工具的集成测试
 * <p>
 * 固定「副作用只在事务真正结束后才执行」这一契约：
 * 事务内不执行、提交后才执行；
 * 回滚时 afterCommit 跳过、afterCompletion 仍执行。
 * </p>
 */
@SpringBootTest
@DisplayName("事务后置动作")
class TransactionSyncHelperTest {

    @Autowired
    private PlatformTransactionManager transactionManager;

    private TransactionTemplate transactionTemplate;

    @BeforeEach
    void setUp() {
        transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Test
    @DisplayName("afterCommit：事务提交后才执行")
    void afterCommitShouldRunAfterCommit() {
        List<String> stages = new ArrayList<>();

        // executeWithoutResult 自动管理事务生命周期：Lambda 正常结束自动 commit()，抛出异常自动 rollback()
        // 入参 status（TransactionStatus）用于在 Lambda 内干预事务，如 status.setRollbackOnly() 强制回滚
        transactionTemplate.executeWithoutResult(status -> {
            TransactionSyncHelper.afterCommit(() -> stages.add("afterCommit"));
            stages.add("inTransaction");
        });

        assertEquals(List.of("inTransaction", "afterCommit"), stages, "回滚前不应执行，提交后才轮到它");
    }

    @Test
    @DisplayName("afterCommit：事务回滚时不执行")
    void afterCommitShouldBeSkippedOnRollback() {
        List<String> stages = new ArrayList<>();

        assertThrows(IllegalStateException.class, () -> transactionTemplate.executeWithoutResult(status -> {
            TransactionSyncHelper.afterCommit(() -> stages.add("afterCommit"));
            throw new IllegalStateException("模拟业务异常触发回滚");
        }));

        assertTrue(stages.isEmpty(), "回滚意味着数据未变更，缓存仍然正确，不应删除");
    }

    @Test
    @DisplayName("afterCompletion：事务回滚时仍执行")
    void afterCompletionShouldRunOnRollback() {
        List<String> stages = new ArrayList<>();

        assertThrows(IllegalStateException.class, () -> transactionTemplate.executeWithoutResult(status -> {
            TransactionSyncHelper.afterCompletion(() -> stages.add("afterCompletion"));
            throw new IllegalStateException("模拟业务异常触发回滚");
        }));

        assertEquals(List.of("afterCompletion"), stages, "回滚后锁也必须释放，否则只能干等 TTL 过期");
    }

    @Test
    @DisplayName("无事务上下文：立即执行，不会静默丢弃")
    void shouldRunImmediatelyWithoutTransaction() {
        List<String> stages = new ArrayList<>();

        TransactionSyncHelper.afterCommit(() -> stages.add("afterCommit"));
        TransactionSyncHelper.afterCompletion(() -> stages.add("afterCompletion"));

        assertEquals(List.of("afterCommit", "afterCompletion"), stages);
    }

    @Test
    @DisplayName("后置动作抛异常：不影响已经完成的主流程")
    void postActionFailureShouldNotPropagate() {
        // afterCommit 阶段抛出的异常会传播给调用方（此时事务已提交），因此必须在此吞掉
        assertDoesNotThrow(
                () -> transactionTemplate.executeWithoutResult(status -> TransactionSyncHelper.afterCommit(() -> {
                    throw new IllegalStateException("后置动作失败");
                })));
    }
}
