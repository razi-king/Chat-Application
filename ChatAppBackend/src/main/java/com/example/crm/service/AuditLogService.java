package com.example.crm.service;

import com.example.crm.dto.PageResponse;
import com.example.crm.dto.response.AuditLogResponse;
import com.example.crm.enums.AuditAction;

public interface AuditLogService {

	void log(String serverId, String actorId, AuditAction action, String targetId, String details);

	PageResponse<AuditLogResponse> list(String serverId, int page, int size, String userId);
}
