package com.approval.backend.common;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
public class RedisLockUtil {

    private final StringRedisTemplate redisTemplate;

    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT;

    static {
        UNLOCK_SCRIPT = new DefaultRedisScript<>();
        UNLOCK_SCRIPT.setScriptText(
                "if redis.call('get', KEYS[1]) == ARGV[1] then " +
                "    return redis.call('del', KEYS[1]) " +
                "else " +
                "    return 0 " +
                "end"
        );
        UNLOCK_SCRIPT.setResultType(Long.class);
    }

    public RedisLockUtil(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public String tryLock(String key, long timeoutSeconds) {
        String value = UUID.randomUUID().toString();
        Boolean acquired = redisTemplate.opsForValue()
                .setIfAbsent(key, value, timeoutSeconds, TimeUnit.SECONDS);
        return Boolean.TRUE.equals(acquired) ? value : null;
    }

    /**
     * 释放锁的底层实现：校验 value 匹配后删除 key。
     * 不对外暴露，调用方一律用 unlockAfterCompletion，无需自行判断是否处在事务中。
     */
    private void unlock(String key, String value) {
        redisTemplate.execute(UNLOCK_SCRIPT, Collections.singletonList(key), value);
    }

    /**
     * 事务完成后释放锁（提交或回滚都会释放）
     *
     * 在 @Transactional 方法的 finally 里调用 unlock，方法体执行完毕时事务尚未提交，
     * 此刻放锁会留下窗口，并发请求拿到锁后读到的是还未提交的旧数据，加锁本身就失去了意义，
     * 放到 afterCompletion 才能让锁真正覆盖整个事务区间。
     *
     * 无事务上下文时退化为立即释放。
     */
    public void unlockAfterCompletion(String key, String value) {
        TransactionSyncHelper.afterCompletion(() -> unlock(key, value));
    }
}
