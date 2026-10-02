package com.example.crm.dto.response;

import java.time.Instant;
import java.util.List;
import com.example.crm.enums.MemberRole;
import com.example.crm.enums.RoomType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoomResponse {
	private String id;
	private String name;
	private String description;
	private RoomType type;
	private String serverId;
	private String iconColor;
	private String createdBy;
	private int position;
	private LastMessageResponse lastMessage;
	private long unreadCount;
	private MemberRole myRole;
	private boolean muted;
	// For DIRECT Rooms: The Person You Are Talking To
	private UserResponse otherUser;
	// For DIRECT And GROUP Rooms (Channels Use Server Members)
	private List<RoomMemberResponse> members;
	private Instant createdAt;
	private Instant lastActivityAt;
}
