package com.example.crm.entity;

import java.time.Instant;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import com.example.crm.enums.RoomType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One entity for every conversation:
 *  DIRECT  -> WhatsApp 1-to-1 chat (exactly two RoomMembers, unique directKey)
 *  GROUP   -> WhatsApp group (RoomMembers with roles)
 *  CHANNEL -> Discord text channel inside a Server (access comes from ServerMember)
 */
@Document(collection = "rooms")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Room {
	@Id
	private String id;
	private String name;
	private String description;
	private RoomType type;
	@Indexed
	private String serverId;
	// Sorted "userA:userB" So The Same Two People Never Get Two DM Rooms
	@Indexed(unique = true, sparse = true)
	private String directKey;
	private String iconColor;
	private String createdBy;
	private int position;
	private LastMessage lastMessage;
	@CreatedDate
	private Instant createdAt;
	private Instant lastActivityAt;

	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	public static class LastMessage {
		private String messageId;
		private String senderId;
		private String senderName;
		private String preview;
		private Instant sentAt;
	}
}
