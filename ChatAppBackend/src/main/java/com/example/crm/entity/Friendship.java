package com.example.crm.entity;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import com.example.crm.enums.FriendshipStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "friendships")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Friendship {
	@Id
	private String id;
	// findByRequesterIdOrAddresseeId Is An $or Query: MongoDB Can Use One Index PER Branch,
	// So Both Sides Need Their Own Index
	@Indexed
	private String requesterId;
	@Indexed
	private String addresseeId;
	// Sorted Pair, Unique -> Only One Relation Between Two Users
	@Indexed(unique = true)
	private String pairKey;
	private FriendshipStatus status;
	private Instant createdAt;
	private Instant respondedAt;
}
