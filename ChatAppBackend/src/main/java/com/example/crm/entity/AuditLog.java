package com.example.crm.entity;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import com.example.crm.enums.AuditAction;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// Discord Style Audit Log: Who Did What Inside A Server
@Document(collection = "audit_logs")
@CompoundIndex(name = "server_created", def = "{'serverId': 1, 'createdAt': -1}")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog {
	@Id
	private String id;
	private String serverId;
	private String actorId;
	private AuditAction action;
	private String targetId;
	private String details;
	private Instant createdAt;
}
