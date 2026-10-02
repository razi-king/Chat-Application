package com.example.crm.service.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.example.crm.entity.RoomMember;
import com.example.crm.repository.MessageRepository;
import com.example.crm.service.UnreadService;
import com.example.crm.util.RedisSafe;

import lombok.RequiredArgsConstructor;

/**
 * Unread badges. Redis hash "unread:{userId}" holds roomId -> count, so the sidebar
 * never has to count messages in MongoDB. If Redis is down we count from MongoDB using lastReadAt.
 */
@Service
@RequiredArgsConstructor
public class UnreadServiceImpl implements UnreadService {
	private static final String PREFIX = "unread:";

	private final StringRedisTemplate redis;
	private final RedisSafe redisSafe;
	private final MessageRepository messageRepository;

	@Override
	public long increment(RoomMember member) {
		return redisSafe.call(
				() -> redis.opsForHash().increment(PREFIX + member.getUserId(), member.getRoomId(), 1),
				() -> countFromMongo(member));
	}

	@Override
	public void reset(String userId, String roomId) {
		redisSafe.run(() -> redis.opsForHash().delete(PREFIX + userId, roomId), () -> {});
	}

	@Override
	public Map<String, Long> counts(String userId, Collection<RoomMember> memberships) {
		Map<String, Long> result = new HashMap<>();
		if (memberships.isEmpty()) {
			return result;
		}
		List<Object> roomIds = new ArrayList<>();
		memberships.forEach(m -> roomIds.add(m.getRoomId()));
		return redisSafe.call(() -> {
			List<Object> values = redis.opsForHash().multiGet(PREFIX + userId, roomIds);
			for (int i = 0; i < roomIds.size(); i++) {
				Object v = values.get(i);
				result.put(roomIds.get(i).toString(), v == null ? 0L : Long.parseLong(v.toString()));
			}
			return result;
		}, () -> {
			memberships.forEach(m -> result.put(m.getRoomId(), countFromMongo(m)));
			return result;
		});
	}

	private long countFromMongo(RoomMember member) {
		if (member.getLastReadAt() == null) {
			return messageRepository.countByRoomIdAndSenderIdNot(member.getRoomId(), member.getUserId());
		}
		return messageRepository.countByRoomIdAndCreatedAtAfterAndSenderIdNot(
				member.getRoomId(), member.getLastReadAt(), member.getUserId());
	}
}
