package com.example.crm.dto.response;

import java.time.Instant;
import com.example.crm.enums.FriendshipStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FriendshipResponse {
	private String id;
	// The Other Person In The Relation
	private UserResponse user;
	private FriendshipStatus status;
	// true = They Sent It To Me
	private boolean incoming;
	private Instant createdAt;
}
