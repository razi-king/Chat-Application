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

@Document(collection = "server_members")
@CompoundIndex(name = "server_user_unique", def = "{'serverId': 1, 'userId': 1}", unique = true)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServerMember {
	@Id
	private String id;
	private String serverId;
	// "My Servers" Query (findByUserId) -> Same Prefix Reasoning As RoomMember.userId
	@Indexed
	private String userId;
	private MemberRole role;
	private String nickname;
	private Instant joinedAt;
}
