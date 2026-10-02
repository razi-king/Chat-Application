package com.example.crm.service;

import java.util.Collection;

import com.example.crm.dto.event.ChatEvent;
import com.example.crm.dto.event.PresenceEvent;

public interface RealtimeService {

	/** Everyone subscribed to /topic/rooms/{roomId} */
	void toRoom(String roomId, ChatEvent event);

	/** One user on /user/queue/events */
	void toUser(String userId, ChatEvent event);

	void toUsers(Collection<String> userIds, ChatEvent event);

	/** Everyone on /topic/presence */
	void presence(PresenceEvent event);
}
