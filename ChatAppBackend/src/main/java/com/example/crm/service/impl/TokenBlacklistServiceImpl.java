package com.example.crm.service.impl;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.example.crm.service.TokenBlacklistService;
import com.example.crm.util.RedisSafe;

import lombok.RequiredArgsConstructor;

// Logout = Put The Token Id In Redis Until The Token Would Have Expired Anyway
@Service
@RequiredArgsConstructor
public class TokenBlacklistServiceImpl implements TokenBlacklistService {
	private static final String PREFIX = "auth:blacklist:";

	private final StringRedisTemplate redis;
	private final RedisSafe redisSafe;
	private final Map<String, Instant> memory = new ConcurrentHashMap<>();

	@Override
	public void blacklist(String tokenId, Instant expiresAt) {
		Duration ttl = Duration.between(Instant.now(), expiresAt);
		if (ttl.isNegative() || ttl.isZero()) {
			return;
		}
		redisSafe.run(() -> redis.opsForValue().set(PREFIX + tokenId, "1", ttl),
				() -> memory.put(tokenId, expiresAt));
	}

	@Override
	public boolean isBlacklisted(String tokenId) {
		if (tokenId == null) {
			return false;
		}
		Instant local = memory.get(tokenId);
		if (local != null) {
			if (local.isAfter(Instant.now())) {
				return true;
			}
			memory.remove(tokenId);
		}
		return redisSafe.call(() -> Boolean.TRUE.equals(redis.hasKey(PREFIX + tokenId)), () -> false);
	}
}
