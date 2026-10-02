package com.example.crm.entity;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import com.example.crm.enums.MemberRole;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// Membership Of A Room + Read Pointer (lastReadAt) Used For Blue Ticks And Unread Counts
@Document(collection = "room_members")
@CompoundIndex(name = "room_user_unique", def = "{'roomId': 1, 'userId': 1}", unique = true)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoomMember {
	@Id
	private String id;
	private String roomId;
	// "All Rooms Of User X" (Chat List, Dashboard) Cannot Use The {roomId, userId} Index Because
	// userId Is Not Its Prefix -> Needs Its Own Index Or Every Chat List Load Scans The Collection
	@Indexed
	private String userId;
	private MemberRole role;
	private boolean muted;
	private Instant lastReadAt;
	private Instant joinedAt;
}
