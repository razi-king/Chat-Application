package com.example.crm.dto.response;

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InviteResponse {
	private String code;
	private String serverId;
	private String serverName;
	private String serverDescription;
	private String serverIconColor;
	private long memberCount;
	private UserResponse createdBy;
	private int maxUses;
	private int uses;
	private Instant expiresAt;
	private Instant createdAt;
}
