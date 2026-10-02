package com.example.crm.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;

import com.example.crm.dto.event.ChatEvent;
import com.example.crm.dto.event.PresenceEvent;
import com.example.crm.repository.UserRepository;
import com.example.crm.service.RealtimeService;
import com.example.crm.util.RedisSafe;

/**
 * One user opens many tabs at the same instant, then closes them all at the same instant.
 * The presence counter must be race-free: exactly ONE "online" and ONE "offline" event,
 * never a flicker of online/offline/online. Redis is deliberately "down" here, so this also
 * proves the in-memory fallback (AtomicLong per user) is thread-safe.
 */
class PresenceServiceConcurrencyTest {
	private static final int TABS = 64;

	@Test
	void manyConcurrentConnectionsOfOneUser_exactlyOneOnlineAndOneOfflineEvent() throws Exception {
		StringRedisTemplate redis = mock(StringRedisTemplate.class);
		when(redis.opsForHash()).thenThrow(new RedisConnectionFailureException("redis down"));
		UserRepository users = mock(UserRepository.class);
		when(users.findById(any())).thenReturn(java.util.Optional.empty());

		AtomicInteger online = new AtomicInteger();
		AtomicInteger offline = new AtomicInteger();
		RealtimeService realtime = new RealtimeService() {
			@Override
			public void toRoom(String roomId, ChatEvent event) {
			}

			@Override
			public void toUser(String userId, ChatEvent event) {
			}

			@Override
			public void toUsers(java.util.Collection<String> userIds, ChatEvent event) {
			}

			@Override
			public void presence(PresenceEvent event) {
				(event.isOnline() ? online : offline).incrementAndGet();
			}
		};
		PresenceServiceImpl presence = new PresenceServiceImpl(redis, new RedisSafe(), users, realtime);

		runConcurrently(() -> presence.connected("razi"));
		assertThat(online.get()).isEqualTo(1);
		assertThat(presence.isOnline("razi")).isTrue();

		runConcurrently(() -> presence.disconnected("razi"));
		assertThat(offline.get()).isEqualTo(1);
		assertThat(presence.isOnline("razi")).isFalse();
	}

	private static void runConcurrently(Runnable task) throws InterruptedException {
		ExecutorService pool = Executors.newFixedThreadPool(16);
		CountDownLatch start = new CountDownLatch(1);
		List<Runnable> jobs = new ArrayList<>();
		for (int i = 0; i < TABS; i++) {
			jobs.add(() -> {
				try {
					start.await();
					task.run();
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			});
		}
		jobs.forEach(pool::submit);
		start.countDown();
		pool.shutdown();
		assertThat(pool.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
	}
}
