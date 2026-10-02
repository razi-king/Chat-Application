package com.example.crm.dto.event;

import java.time.Instant;
import com.example.crm.enums.ChatEventType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatEvent {
	private ChatEventType type;
	private String roomId;
	private Object payload;
	@lombok.Builder.Default
	private Instant timestamp = Instant.now();

	public static ChatEvent of(ChatEventType type, String roomId, Object payload) {
		return ChatEvent.builder().type(type).roomId(roomId).payload(payload).build();
	}
}
