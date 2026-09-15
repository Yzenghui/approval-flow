package com.approval.backend.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 事务后置动作工具
 *
 * 把「必须等事务真正结束才能做的副作用」移出事务体，避免在事务未提交时就让外界观察到变更。
 * 无事务上下文时立即执行，而非跳过，保证传入的动作在任何情况下都必然会执行一次。
 */
public final class TransactionSyncHelper {

    private static final Logger log = LoggerFactory.getLogger(TransactionSyncHelper.class);

    private TransactionSyncHelper() {
    }

    /**
     * 事务提交后执行，回滚时不执行。
     * 适用于「只有数据确实落库成功才需要做」的动作，例如删除缓存。
     */
    public static void afterCommit(Runnable action) {
        // 判断当前有没有事务
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            safeRun(action); // 没事务，直接执行
            return;
        }
        // 如果有事务，注册一个“钩子”
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                safeRun(action); // 只有事务真正提交成功后，才会执行这里的代码
            }
        });
    }

    /**
     * 事务完成后执行，提交或回滚都会执行。
     * 适用于「无论事务结果如何都必须做」的动作，例如释放分布式锁。
     */
    public static void afterCompletion(Runnable action) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            safeRun(action);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                safeRun(action);
            }
        });
    }

    /**
     * 后置动作失败不应影响已经结束的主流程，这里统一兜底并记录日志。
     * afterCommit 阶段抛出的异常会传播给调用方（事务已提交），因此必须在此吞掉。
     */
    private static void safeRun(Runnable action) {
        try {
            action.run();
        } catch (Exception e) {
            log.warn("事务后置动作执行失败，已忽略（不影响主流程）", e);
        }
    }
}
