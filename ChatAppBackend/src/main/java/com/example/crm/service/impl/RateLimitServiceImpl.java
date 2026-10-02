package com.example.crm.service.impl;

import java.time.Duration;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.example.crm.enums.ErrorCode;
import com.example.crm.exception.RateLimitException;
import com.example.crm.service.RateLimitService;
import com.example.crm.util.RedisSafe;

import lombok.RequiredArgsConstructor;

// Fixed Window Counter: INCR rate:{action}:{userId}:{window} + EXPIRE
@Service
@RequiredArgsConstructor
public class RateLimitServiceImpl implements RateLimitService {
	private final StringRedisTemplate redis;
	private final RedisSafe redisSafe;

	// Atomic INCR: Even Under Concurrency Exactly "limit" Calls Per Window Get Through (Used For Login)
	@Override
	public void check(String userId, String action, int limit, int windowSeconds) {
		failIfOver(increment(key(userId, action, windowSeconds), windowSeconds), limit, windowSeconds);
	}

	@Override
	public void ensureBelowLimit(String userId, String action, int limit, int windowSeconds) {
		String key = key(userId, action, windowSeconds);
		// "Count >= limit" Because This Call Itself Is Not Counted Yet
		long count = redisSafe.call(() -> {
			String value = redis.opsForValue().get(key);
			return value == null ? 0L : Long.parseLong(value);
		}, () -> 0L);
		failIfOver(count + 1, limit, windowSeconds);
	}

	@Override
	public void record(String userId, String action, int windowSeconds) {
		increment(key(userId, action, windowSeconds), windowSeconds);
	}

	private long increment(String key, int windowSeconds) {
		// If Redis Is Down We Let The Request Through (Fail Open): Losing Spam Protection For A While
		// Is Better Than Blocking Every User's Chat
		return redisSafe.call(() -> {
			Long value = redis.opsForValue().increment(key);
			if (value != null && value == 1) {
				redis.expire(key, Duration.ofSeconds(windowSeconds));
			}
			return value == null ? 0L : value;
		}, () -> 0L);
	}

	private static String key(String userId, String action, int windowSeconds) {
		long window = System.currentTimeMillis() / 1000 / windowSeconds;
		return "rate:" + action + ":" + userId + ":" + window;
	}

	private static void failIfOver(long count, int limit, int windowSeconds) {
		if (count > limit) {
			throw new RateLimitException("Slow down! Max " + limit + " per " + windowSeconds + "s", ErrorCode.RATE_LIMITED);
		}
	}
}
