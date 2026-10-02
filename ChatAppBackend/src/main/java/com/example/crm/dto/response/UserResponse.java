package com.example.crm.dto.response;

import java.time.Instant;
import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
	private String id;
	private String username;
	private String displayName;
	// Only Filled For The Logged In User Himself
	@JsonInclude(JsonInclude.Include.NON_NULL)
	private String email;
	private String avatarColor;
	private String about;
	private String customStatus;
	private boolean online;
	private Instant lastSeenAt;
	private Instant createdAt;
}
