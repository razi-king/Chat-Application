package com.example.crm.service.impl;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.example.crm.dto.event.PresenceEvent;
import com.example.crm.repository.UserRepository;
import com.example.crm.service.PresenceService;
import com.example.crm.service.RealtimeService;
import com.example.crm.util.RedisSafe;

import lombok.RequiredArgsConstructor;

/**
 * Online status. Redis hash "presence:connections" holds userId -> number of open sockets,
 * so a user with two tabs open only goes offline when the last tab closes.
 */
@Service
@RequiredArgsConstructor
public class PresenceServiceImpl implements PresenceService {
	private static final String KEY = "presence:connections";

	private final StringRedisTemplate redis;
	private final RedisSafe redisSafe;
	private final UserRepository userRepository;
	private final RealtimeService realtimeService;
	private final Map<String, AtomicLong> memory = new ConcurrentHashMap<>();

	// Sockets Do Not Survive A Restart, So Old Counters Are Wrong -> Clear Them
	@EventListener(ApplicationReadyEvent.class)
	public void resetOnStartup() {
		redisSafe.run(() -> redis.delete(KEY), () -> {});
	}

	@Override
	public void connected(String userId) {
		long count = redisSafe.call(
				() -> redis.opsForHash().increment(KEY, userId, 1),
				() -> memory.computeIfAbsent(userId, k -> new AtomicLong()).incrementAndGet());
		if (count == 1) {
			realtimeService.presence(new PresenceEvent(userId, true, null));
		}
	}

	@Override
	public void disconnected(String userId) {
		long count = redisSafe.call(
				() -> redis.opsForHash().increment(KEY, userId, -1),
				() -> memory.computeIfAbsent(userId, k -> new AtomicLong()).decrementAndGet());
		if (count <= 0) {
			// Race: Another Tab May Reconnect Between Our Decrement And This Cleanup. Removing The Entry
			// Unconditionally Would Erase That New Connection, So Remove Only If It Is STILL <= 0 (Atomic
			// In The Map). In Redis The Zero Entry Is Simply Left In Place: onlineOf() Treats <= 0 As Offline.
			redisSafe.run(() -> {}, () -> memory.computeIfPresent(userId, (k, v) -> v.get() <= 0 ? null : v));
			Instant now = Instant.now();
			userRepository.findById(userId).ifPresent(user -> {
				user.setLastSeenAt(now);
				userRepository.save(user);
			});
			realtimeService.presence(new PresenceEvent(userId, false, now));
		}
	}

	@Override
	public boolean isOnline(String userId) {
		return !onlineOf(List.of(userId)).isEmpty();
	}

	@Override
	public Set<String> onlineOf(Collection<String> userIds) {
		if (userIds.isEmpty()) {
			return Set.of();
		}
		List<Object> keys = new ArrayList<>(userIds);
		return redisSafe.call(() -> {
			List<Object> values = redis.opsForHash().multiGet(KEY, keys);
			Set<String> online = new HashSet<>();
			for (int i = 0; i < keys.size(); i++) {
				Object v = values.get(i);
				if (v != null && Long.parseLong(v.toString()) > 0) {
					online.add(keys.get(i).toString());
				}
			}
			return online;
		}, () -> {
			Set<String> online = new HashSet<>();
			for (String id : userIds) {
				AtomicLong c = memory.get(id);
				if (c != null && c.get() > 0) {
					online.add(id);
				}
			}
			return online;
		});
	}
}
