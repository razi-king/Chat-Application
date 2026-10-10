package com.example.crm.service.impl;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.example.crm.enums.ErrorCode;
import com.example.crm.exception.RateLimitException;
import com.example.crm.service.LoginAttemptService;
import com.example.crm.util.RedisSafe;

import lombok.RequiredArgsConstructor;

/**
 * Redis keys:
 *   auth:fail:{account} -> number of wrong passwords in a row (expires after LOCK_TIME)
 *   auth:lock:{account} -> "1" while the account is locked (expires after LOCK_TIME)
 * If Redis is down the same logic runs in memory, so the lock still works on a single server.
 */
@Service
@RequiredArgsConstructor
public class LoginAttemptServiceImpl implements LoginAttemptService {
	static final int MAX_FAILURES = 5;
	static final Duration LOCK_TIME = Duration.ofMinutes(15);
	private static final String FAIL_PREFIX = "auth:fail:";
	private static final String LOCK_PREFIX = "auth:lock:";

	private final StringRedisTemplate redis;
	private final RedisSafe redisSafe;
	private final Map<String, Counter> memoryFailures = new ConcurrentHashMap<>();
	private final Map<String, Instant> memoryLocks = new ConcurrentHashMap<>();

	private record Counter(int count, Instant expiresAt) {
	}

	@Override
	public void ensureNotLocked(String accountKey) {
		long secondsLeft = redisSafe.call(() -> {
			Long ttl = redis.getExpire(LOCK_PREFIX + accountKey);
			return ttl == null || ttl < 0 ? 0L : ttl;
		}, () -> memorySecondsLeft(accountKey));
		if (secondsLeft > 0) {
			long minutes = Math.max(1, (secondsLeft + 59) / 60);
			throw new RateLimitException("Too many failed login attempts. Account locked, try again in "
					+ minutes + (minutes == 1 ? " minute" : " minutes"), ErrorCode.ACCOUNT_LOCKED);
		}
	}

	@Override
	public void recordFailure(String accountKey) {
		long failures = redisSafe.call(() -> {
			Long value = redis.opsForValue().increment(FAIL_PREFIX + accountKey);
			if (value != null && value == 1) {
				redis.expire(FAIL_PREFIX + accountKey, LOCK_TIME);
			}
			return value == null ? 0L : value;
		}, () -> memoryIncrement(accountKey));
		if (failures >= MAX_FAILURES) {
			redisSafe.run(() -> {
				redis.opsForValue().set(LOCK_PREFIX + accountKey, "1", LOCK_TIME);
				redis.delete(FAIL_PREFIX + accountKey);
			}, () -> {
				memoryLocks.put(accountKey, Instant.now().plus(LOCK_TIME));
				memoryFailures.remove(accountKey);
			});
		}
	}

	@Override
	public void recordSuccess(String accountKey) {
		memoryFailures.remove(accountKey);
		redisSafe.run(() -> redis.delete(FAIL_PREFIX + accountKey), () -> {
		});
	}

	private long memorySecondsLeft(String accountKey) {
		Instant until = memoryLocks.get(accountKey);
		if (until == null) {
			return 0;
		}
		long left = Duration.between(Instant.now(), until).getSeconds();
		if (left <= 0) {
			memoryLocks.remove(accountKey);
			return 0;
		}
		return left;
	}

	private long memoryIncrement(String accountKey) {
		Instant now = Instant.now();
		return memoryFailures.compute(accountKey, (k, c) -> c == null || c.expiresAt().isBefore(now)
				? new Counter(1, now.plus(LOCK_TIME))
				: new Counter(c.count() + 1, c.expiresAt())).count();
	}
}
