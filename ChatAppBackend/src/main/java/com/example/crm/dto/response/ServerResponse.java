package com.example.crm.dto.response;

import java.time.Instant;
import com.example.crm.enums.MemberRole;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServerResponse {
	private String id;
	private String name;
	private String description;
	private String iconColor;
	private String ownerId;
	private boolean discoverable;
	private long memberCount;
	private MemberRole myRole;
	private long unreadCount;
	private Instant createdAt;
}
