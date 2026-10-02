package com.example.crm.service.impl;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import com.example.crm.dto.PageResponse;
import com.example.crm.dto.event.ChatEvent;
import com.example.crm.dto.response.NotificationResponse;
import com.example.crm.dto.response.UserResponse;
import com.example.crm.entity.Notification;
import com.example.crm.enums.ChatEventType;
import com.example.crm.enums.ErrorCode;
import com.example.crm.enums.NotificationType;
import com.example.crm.exception.ResourceNotFoundException;
import com.example.crm.mapper.UserMapper;
import com.example.crm.repository.NotificationRepository;
import com.example.crm.service.NotificationService;
import com.example.crm.service.RealtimeService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {
	private final NotificationRepository notificationRepository;
	private final RealtimeService realtimeService;
	private final UserMapper userMapper;

	@Override
	public void notify(String userId, NotificationType type, String title, String body, String link, String actorId) {
		Notification saved = notificationRepository.save(Notification.builder()
				.userId(userId).type(type).title(title).body(body).link(link).actorId(actorId)
				.read(false).createdAt(Instant.now()).build());
		// Push It Live -> Toast + Bell Badge On The Frontend
		realtimeService.toUser(userId, ChatEvent.of(ChatEventType.NOTIFICATION, null, toResponses(List.of(saved)).get(0)));
	}

	@Override
	public PageResponse<NotificationResponse> list(String userId, int page, int size) {
		Page<Notification> result = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId,
				PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 50)));
		return new PageResponse<>(toResponses(result.getContent()), result.getNumber(), result.getSize(), result.hasNext());
	}

	@Override
	public long unreadCount(String userId) {
		return notificationRepository.countByUserIdAndReadFalse(userId);
	}

	@Override
	public void markRead(String notificationId, String userId) {
		Notification n = notificationRepository.findById(notificationId)
				.filter(x -> x.getUserId().equals(userId))
				.orElseThrow(() -> new ResourceNotFoundException(ErrorCode.NOTIFICATION_NOT_FOUND));
		n.setRead(true);
		notificationRepository.save(n);
	}

	@Override
	public void markAllRead(String userId) {
		List<Notification> unread = notificationRepository.findByUserIdAndReadFalse(userId);
		unread.forEach(n -> n.setRead(true));
		notificationRepository.saveAll(unread);
	}

	private List<NotificationResponse> toResponses(List<Notification> list) {
		Map<String, UserResponse> actors = userMapper.mapByIds(list.stream().map(Notification::getActorId).toList());
		return list.stream().map(n -> NotificationResponse.builder()
				.id(n.getId()).type(n.getType()).title(n.getTitle()).body(n.getBody()).link(n.getLink())
				.actor(n.getActorId() == null ? null : actors.get(n.getActorId()))
				.read(n.isRead()).createdAt(n.getCreatedAt()).build()).toList();
	}
}
