package com.example.crm.service.impl;

import java.time.Instant;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import com.example.crm.dto.PageResponse;
import com.example.crm.dto.response.AuditLogResponse;
import com.example.crm.dto.response.UserResponse;
import com.example.crm.entity.AuditLog;
import com.example.crm.enums.AuditAction;
import com.example.crm.enums.MemberRole;
import com.example.crm.mapper.UserMapper;
import com.example.crm.repository.AuditLogRepository;
import com.example.crm.security.AccessGuard;
import com.example.crm.service.AuditLogService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuditLogServiceImpl implements AuditLogService {
	private final AuditLogRepository auditLogRepository;
	private final AccessGuard accessGuard;
	private final UserMapper userMapper;

	@Override
	public void log(String serverId, String actorId, AuditAction action, String targetId, String details) {
		auditLogRepository.save(AuditLog.builder()
				.serverId(serverId).actorId(actorId).action(action).targetId(targetId).details(details)
				.createdAt(Instant.now()).build());
	}

	@Override
	public PageResponse<AuditLogResponse> list(String serverId, int page, int size, String userId) {
		accessGuard.requireServerRole(serverId, userId, MemberRole.ADMIN);
		Page<AuditLog> result = auditLogRepository.findByServerIdOrderByCreatedAtDesc(serverId,
				PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100)));
		Map<String, UserResponse> actors = userMapper.mapByIds(result.getContent().stream().map(AuditLog::getActorId).toList());
		return new PageResponse<>(result.getContent().stream().map(a -> AuditLogResponse.builder()
				.id(a.getId()).actor(actors.get(a.getActorId())).action(a.getAction()).targetId(a.getTargetId())
				.details(a.getDetails()).createdAt(a.getCreatedAt()).build()).toList(),
				result.getNumber(), result.getSize(), result.hasNext());
	}
}
