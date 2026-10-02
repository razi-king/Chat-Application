package com.example.crm.entity;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "invites")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Invite {
	@Id
	private String id;
	@Indexed(unique = true)
	private String code;
	private String serverId;
	private String createdBy;
	// 0 = Unlimited
	private int maxUses;
	private int uses;
	private Instant expiresAt;
	private Instant createdAt;
}
