package com.example.crm.service.impl;

import java.util.Collection;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.example.crm.dto.event.ChatEvent;
import com.example.crm.dto.event.PresenceEvent;
import com.example.crm.enums.ChatEventType;
import com.example.crm.service.RealtimeService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RealtimeServiceImpl implements RealtimeService {
	private final SimpMessagingTemplate messagingTemplate;

	@Override
	public void toRoom(String roomId, ChatEvent event) {
		messagingTemplate.convertAndSend("/topic/rooms/" + roomId, event);
	}

	@Override
	public void toUser(String userId, ChatEvent event) {
		messagingTemplate.convertAndSendToUser(userId, "/queue/events", event);
	}

	@Override
	public void toUsers(Collection<String> userIds, ChatEvent event) {
		userIds.forEach(id -> toUser(id, event));
	}

	@Override
	public void presence(PresenceEvent event) {
		messagingTemplate.convertAndSend("/topic/presence", ChatEvent.of(ChatEventType.PRESENCE, null, event));
	}
}
