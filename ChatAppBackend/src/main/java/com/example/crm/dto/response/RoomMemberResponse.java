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
public class RoomMemberResponse {
	private UserResponse user;
	private MemberRole role;
	private Instant lastReadAt;
	private Instant joinedAt;
}
