package com.example.crm.service;

import java.time.Instant;
import java.util.List;

import com.example.crm.dto.PageResponse;
import com.example.crm.dto.request.SendMessageRequest;
import com.example.crm.dto.response.MessageResponse;
import com.example.crm.entity.Room;

public interface ChatService {

	MessageResponse sendMessage(String roomId, SendMessageRequest request, String userId);

	PageResponse<MessageResponse> getMessages(String roomId, Instant before, int size, String userId);

	MessageResponse edit(String messageId, String content, String userId);

	MessageResponse delete(String messageId, String userId);

	MessageResponse react(String messageId, String emoji, String userId);

	MessageResponse togglePin(String messageId, String userId);

	List<MessageResponse> pinned(String roomId, String userId);

	List<MessageResponse> search(String roomId, String query, String userId);

	void typing(String roomId, boolean typing, String userId);

	/** "Razi joined the server" style message, broadcast like a normal one. */
	void systemMessage(Room room, String content);
}
