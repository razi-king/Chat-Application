package com.example.crm.util;

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * Runs a Redis call and falls back when Redis is down, so the chat keeps working
 * (with in-memory / MongoDB fallbacks) during a demo even if Redis is not started.
 * After a failure Redis is skipped for 30 seconds instead of waiting for a timeout on every call.
 */
@Slf4j
@Component
public class RedisSafe {
	private static final long RETRY_AFTER_MS = 30_000;
	private final AtomicLong downSince = new AtomicLong(0);

	public <T> T call(Supplier<T> redisCall, Supplier<T> fallback) {
		long down = downSince.get();
		if (down != 0 && System.currentTimeMillis() - down < RETRY_AFTER_MS) {
			return fallback.get();
		}
		try {
			T result = redisCall.get();
			if (down != 0) {
				downSince.set(0);
				log.info("Redis is reachable again");
			}
			return result;
		} catch (RuntimeException e) {
			if (downSince.getAndSet(System.currentTimeMillis()) == 0) {
				log.warn("Redis unavailable, using fallback: {}", e.getMessage());
			}
			return fallback.get();
		}
	}

	public void run(Runnable redisCall, Runnable fallback) {
		call(() -> {
			redisCall.run();
			return null;
		}, () -> {
			fallback.run();
			return null;
		});
	}

	public boolean isUp() {
		return downSince.get() == 0;
	}
}
