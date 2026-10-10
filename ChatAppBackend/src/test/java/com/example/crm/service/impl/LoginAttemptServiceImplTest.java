package com.example.crm.service.impl;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;

import com.example.crm.enums.ErrorCode;
import com.example.crm.exception.ChatAppException;
import com.example.crm.exception.RateLimitException;
import com.example.crm.util.RedisSafe;

// Runs With Redis DOWN: Proves The Lockout Still Works Through The In-Memory Fallback
@ExtendWith(MockitoExtension.class)
class LoginAttemptServiceImplTest {
	@Mock
	private StringRedisTemplate redis;
	private LoginAttemptServiceImpl service;

	@BeforeEach
	void setUp() {
		// Every Redis Call Fails Like A Real Outage; RedisSafe Then Switches To The Memory Fallback
		lenient().when(redis.getExpire(anyString())).thenThrow(new RedisConnectionFailureException("Redis is down"));
		lenient().when(redis.opsForValue()).thenThrow(new RedisConnectionFailureException("Redis is down"));
		service = new LoginAttemptServiceImpl(redis, new RedisSafe());
	}

	// Protects: 4 Wrong Passwords Are Allowed, The 5th Locks The Account
	@Test
	void fiveFailures_lockTheAccount() {
		for (int i = 1; i < LoginAttemptServiceImpl.MAX_FAILURES; i++) {
			service.recordFailure("u1");
		}
		assertThatCode(() -> service.ensureNotLocked("u1")).doesNotThrowAnyException();

		service.recordFailure("u1");

		assertThatThrownBy(() -> service.ensureNotLocked("u1"))
				.isInstanceOf(RateLimitException.class)
				.extracting(ex -> ((ChatAppException) ex).getErrorCode()).isEqualTo(ErrorCode.ACCOUNT_LOCKED);
		// Other Accounts Are Not Affected
		assertThatCode(() -> service.ensureNotLocked("u2")).doesNotThrowAnyException();
	}

	// Protects: A Successful Login Resets The Counter, So Old Typos Do Not Add Up To A Lock
	@Test
	void success_resetsTheCounter() {
		for (int i = 1; i < LoginAttemptServiceImpl.MAX_FAILURES; i++) {
			service.recordFailure("u1");
		}
		service.recordSuccess("u1");
		service.recordFailure("u1");

		assertThatCode(() -> service.ensureNotLocked("u1")).doesNotThrowAnyException();
	}
}
