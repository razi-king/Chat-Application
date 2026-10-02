package com.example.crm.dto.response;

import java.time.Instant;
import com.example.crm.enums.AuditAction;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogResponse {
	private String id;
	private UserResponse actor;
	private AuditAction action;
	private String targetId;
	private String details;
	private Instant createdAt;
}
