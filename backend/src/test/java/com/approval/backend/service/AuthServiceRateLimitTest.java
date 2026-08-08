package com.approval.backend.service;

import com.approval.backend.common.BusinessException;
import com.approval.backend.dto.request.LoginRequest;
import com.approval.backend.dto.response.LoginResponse;
import cn.dev33.satoken.stp.StpUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AuthServiceRateLimitTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private static final String TEST_USERNAME = "ratelimit_test_user";
    private static final String FAIL_KEY = "login:fail:" + TEST_USERNAME;
    private static final String ADMIN_FAIL_KEY = "login:fail:admin";

    @AfterEach
    void cleanup() {
        redisTemplate.delete(FAIL_KEY);
        redisTemplate.delete(ADMIN_FAIL_KEY);
        if (StpUtil.isLogin()) {
            StpUtil.logout();
        }
    }

    @Test
    @Order(1)
    void shouldLockAfterFiveFailures() {
        redisTemplate.delete(FAIL_KEY);

        LoginRequest request = new LoginRequest();
        request.setUsername(TEST_USERNAME);
        request.setPassword("wrong_password");

        // 第1-4次失败：提示剩余次数
        for (int i = 1; i <= 4; i++) {
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> authService.login(request));
            assertTrue(ex.getMessage().contains("尝试机会"),
                    "第" + i + "次失败应提示剩余次数，实际: " + ex.getMessage());
        }

        // 第5次失败：直接提示锁定
        BusinessException lockEx = assertThrows(BusinessException.class,
                () -> authService.login(request));
        assertTrue(lockEx.getMessage().contains("锁定"),
                "第5次失败应提示账号已锁定，实际: " + lockEx.getMessage());

        // 第6次尝试：被入口检查拦截
        BusinessException blockedEx = assertThrows(BusinessException.class,
                () -> authService.login(request));
        assertTrue(blockedEx.getMessage().contains("锁定"),
                "锁定后再次登录应被拦截，实际: " + blockedEx.getMessage());

        String count = redisTemplate.opsForValue().get(FAIL_KEY);
        assertNotNull(count);
        assertEquals("5", count);
    }

    @Test
    @Order(2)
    void shouldClearFailCountAfterSuccess() {
        redisTemplate.delete(ADMIN_FAIL_KEY);

        // 用 admin 账号失败两次
        LoginRequest failRequest = new LoginRequest();
        failRequest.setUsername("admin");
        failRequest.setPassword("wrong_password");
        assertThrows(BusinessException.class, () -> authService.login(failRequest));
        assertThrows(BusinessException.class, () -> authService.login(failRequest));

        String count = redisTemplate.opsForValue().get(ADMIN_FAIL_KEY);
        assertEquals("2", count);

        // 用 admin 账号正确密码登录成功
        LoginRequest successRequest = new LoginRequest();
        successRequest.setUsername("admin");
        successRequest.setPassword("Admin@123");
        assertDoesNotThrow(() -> {
            LoginResponse response = authService.login(successRequest);
            assertNotNull(response.getToken());
        });

        // 同一用户的失败计数应被清除
        count = redisTemplate.opsForValue().get(ADMIN_FAIL_KEY);
        assertNull(count, "登录成功后应清除失败计数");
    }

    @Test
    @Order(3)
    void shouldRemainingChanceDecrease() {
        redisTemplate.delete(FAIL_KEY);

        LoginRequest request = new LoginRequest();
        request.setUsername(TEST_USERNAME);
        request.setPassword("wrong_password");

        for (int i = 1; i <= 5; i++) {
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> authService.login(request));
            if (i < 5) {
                int expectedRemaining = 5 - i;
                assertTrue(ex.getMessage().contains(expectedRemaining + "次尝试机会"),
                        "第" + i + "次失败应提示剩余" + expectedRemaining + "次，实际: " + ex.getMessage());
            } else {
                assertTrue(ex.getMessage().contains("锁定"),
                        "第5次失败应提示锁定，实际: " + ex.getMessage());
            }
        }
    }
}
