package com.example.crm.service.impl;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.example.crm.dto.request.CreateInviteRequest;
import com.example.crm.dto.response.InviteResponse;
import com.example.crm.dto.response.ServerResponse;
import com.example.crm.dto.response.UserResponse;
import com.example.crm.entity.Invite;
import com.example.crm.entity.Server;
import com.example.crm.enums.AuditAction;
import com.example.crm.enums.ErrorCode;
import com.example.crm.enums.MemberRole;
import com.example.crm.exception.ChatAppException;
import com.example.crm.exception.ResourceNotFoundException;
import com.example.crm.mapper.UserMapper;
import com.example.crm.repository.InviteRepository;
import com.example.crm.repository.ServerMemberRepository;
import com.example.crm.security.AccessGuard;
import com.example.crm.service.AuditLogService;
import com.example.crm.service.InviteService;
import com.example.crm.service.ServerService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class InviteServiceImpl implements InviteService {
	private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789";
	private static final SecureRandom RANDOM = new SecureRandom();

	private final InviteRepository inviteRepository;
	private final ServerMemberRepository serverMemberRepository;
	private final AccessGuard accessGuard;
	private final ServerService serverService;
	private final AuditLogService auditLogService;
	private final UserMapper userMapper;

	@Override
	public InviteResponse create(String serverId, CreateInviteRequest request, String userId) {
		Server server = accessGuard.requireServer(serverId);
		accessGuard.requireServerMember(serverId, userId);
		Instant now = Instant.now();
		Invite invite = inviteRepository.save(Invite.builder()
				.code(newCode()).serverId(serverId).createdBy(userId).maxUses(request.getMaxUses()).uses(0)
				.expiresAt(request.getExpiresInHours() > 0 ? now.plus(Duration.ofHours(request.getExpiresInHours())) : null)
				.createdAt(now).build());
		auditLogService.log(serverId, userId, AuditAction.INVITE_CREATED, invite.getCode(), null);
		return toResponse(invite, server, userMapper.mapByIds(List.of(userId)));
	}

	@Override
	public InviteResponse preview(String code) {
		Invite invite = requireValid(code);
		Server server = accessGuard.requireServer(invite.getServerId());
		return toResponse(invite, server, userMapper.mapByIds(List.of(invite.getCreatedBy())));
	}

	@Override
	public ServerResponse accept(String code, String userId) {
		Invite invite = requireValid(code);
		Server server = accessGuard.requireServer(invite.getServerId());
		if (!serverMemberRepository.existsByServerIdAndUserId(server.getId(), userId)) {
			invite.setUses(invite.getUses() + 1);
			inviteRepository.save(invite);
		}
		return serverService.addMember(server, userId);
	}

	@Override
	public List<InviteResponse> list(String serverId, String userId) {
		Server server = accessGuard.requireServer(serverId);
		accessGuard.requireServerRole(serverId, userId, MemberRole.ADMIN);
		List<Invite> invites = inviteRepository.findByServerIdOrderByCreatedAtDesc(serverId);
		Map<String, UserResponse> creators = userMapper.mapByIds(invites.stream().map(Invite::getCreatedBy).toList());
		return invites.stream().map(i -> toResponse(i, server, creators)).toList();
	}

	private Invite requireValid(String code) {
		Invite invite = inviteRepository.findByCode(code)
				.orElseThrow(() -> new ResourceNotFoundException(ErrorCode.INVITE_NOT_FOUND));
		boolean expired = invite.getExpiresAt() != null && invite.getExpiresAt().isBefore(Instant.now());
		boolean usedUp = invite.getMaxUses() > 0 && invite.getUses() >= invite.getMaxUses();
		if (expired || usedUp) {
			throw new ChatAppException(ErrorCode.INVITE_EXPIRED);
		}
		return invite;
	}

	private InviteResponse toResponse(Invite i, Server s, Map<String, UserResponse> users) {
		return InviteResponse.builder()
				.code(i.getCode()).serverId(s.getId()).serverName(s.getName()).serverDescription(s.getDescription())
				.serverIconColor(s.getIconColor()).memberCount(serverMemberRepository.countByServerId(s.getId()))
				.createdBy(users.get(i.getCreatedBy())).maxUses(i.getMaxUses()).uses(i.getUses())
				.expiresAt(i.getExpiresAt()).createdAt(i.getCreatedAt()).build();
	}

	private static String newCode() {
		StringBuilder sb = new StringBuilder(8);
		for (int i = 0; i < 8; i++) {
			sb.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
		}
		return sb.toString();
	}
}
