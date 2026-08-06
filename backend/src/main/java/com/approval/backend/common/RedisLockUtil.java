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

    public void unlock(String key, String value) {
        redisTemplate.execute(UNLOCK_SCRIPT, Collections.singletonList(key), value);
    }
}