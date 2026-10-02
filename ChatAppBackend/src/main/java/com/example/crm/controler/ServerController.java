package com.example.crm.controler;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.crm.dto.PageResponse;
import com.example.crm.dto.Response;
import com.example.crm.dto.request.CreateChannelRequest;
import com.example.crm.dto.request.CreateInviteRequest;
import com.example.crm.dto.request.CreateServerRequest;
import com.example.crm.dto.request.UpdateRoleRequest;
import com.example.crm.dto.response.AuditLogResponse;
import com.example.crm.dto.response.InviteResponse;
import com.example.crm.dto.response.RoomResponse;
import com.example.crm.dto.response.ServerDetailResponse;
import com.example.crm.dto.response.ServerMemberResponse;
import com.example.crm.dto.response.ServerResponse;
import com.example.crm.security.AuthUser;
import com.example.crm.service.AuditLogService;
import com.example.crm.service.InviteService;
import com.example.crm.service.RoomService;
import com.example.crm.service.ServerService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

// Discord Style Servers (Communities With Channels, Roles, Invites And An Audit Log)
@RestController
@RequestMapping("/api/v1/servers")
@RequiredArgsConstructor
public class ServerController {
	private final ServerService serverService;
	private final RoomService roomService;
	private final InviteService inviteService;
	private final AuditLogService auditLogService;

	@GetMapping
	public ResponseEntity<Response<List<ServerResponse>>> myServers(@AuthenticationPrincipal AuthUser me) {
		return ResponseEntity.ok(Response.ok(serverService.mine(me.id()), "Your Servers"));
	}

	@PostMapping
	public ResponseEntity<Response<ServerResponse>> create(@AuthenticationPrincipal AuthUser me,
			@Valid @RequestBody CreateServerRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(Response.ok(serverService.create(request, me.id()), "Server Created"));
	}

	@GetMapping("/discover")
	public ResponseEntity<Response<List<ServerResponse>>> discover(@AuthenticationPrincipal AuthUser me) {
		return ResponseEntity.ok(Response.ok(serverService.discover(me.id()), "Public Servers"));
	}

	@GetMapping("/{serverId}")
	public ResponseEntity<Response<ServerDetailResponse>> detail(@AuthenticationPrincipal AuthUser me, @PathVariable String serverId) {
		return ResponseEntity.ok(Response.ok(serverService.detail(serverId, me.id()), "Server Details"));
	}

	@PutMapping("/{serverId}")
	public ResponseEntity<Response<ServerResponse>> update(@AuthenticationPrincipal AuthUser me, @PathVariable String serverId,
			@Valid @RequestBody CreateServerRequest request) {
		return ResponseEntity.ok(Response.ok(serverService.update(serverId, request, me.id()), "Server Updated"));
	}

	@DeleteMapping("/{serverId}")
	public ResponseEntity<Response<Void>> delete(@AuthenticationPrincipal AuthUser me, @PathVariable String serverId) {
		serverService.delete(serverId, me.id());
		return ResponseEntity.ok(Response.ok(null, "Server Deleted"));
	}

	@PostMapping("/{serverId}/join")
	public ResponseEntity<Response<ServerResponse>> join(@AuthenticationPrincipal AuthUser me, @PathVariable String serverId) {
		return ResponseEntity.ok(Response.ok(serverService.join(serverId, me.id()), "Joined Server"));
	}

	@PostMapping("/{serverId}/leave")
	public ResponseEntity<Response<Void>> leave(@AuthenticationPrincipal AuthUser me, @PathVariable String serverId) {
		serverService.leave(serverId, me.id());
		return ResponseEntity.ok(Response.ok(null, "Left Server"));
	}

	@DeleteMapping("/{serverId}/members/{userId}")
	public ResponseEntity<Response<Void>> kick(@AuthenticationPrincipal AuthUser me, @PathVariable String serverId,
			@PathVariable String userId) {
		serverService.kick(serverId, userId, me.id());
		return ResponseEntity.ok(Response.ok(null, "Member Kicked"));
	}

	@PutMapping("/{serverId}/members/{userId}/role")
	public ResponseEntity<Response<ServerMemberResponse>> updateRole(@AuthenticationPrincipal AuthUser me,
			@PathVariable String serverId, @PathVariable String userId, @Valid @RequestBody UpdateRoleRequest request) {
		return ResponseEntity.ok(Response.ok(serverService.updateRole(serverId, userId, request.getRole(), me.id()), "Role Updated"));
	}

	@PostMapping("/{serverId}/channels")
	public ResponseEntity<Response<RoomResponse>> createChannel(@AuthenticationPrincipal AuthUser me, @PathVariable String serverId,
			@Valid @RequestBody CreateChannelRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(Response.ok(roomService.createChannel(serverId, request, me.id()), "Channel Created"));
	}

	@PostMapping("/{serverId}/invites")
	public ResponseEntity<Response<InviteResponse>> createInvite(@AuthenticationPrincipal AuthUser me, @PathVariable String serverId,
			@Valid @RequestBody CreateInviteRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(Response.ok(inviteService.create(serverId, request, me.id()), "Invite Created"));
	}

	@GetMapping("/{serverId}/invites")
	public ResponseEntity<Response<List<InviteResponse>>> invites(@AuthenticationPrincipal AuthUser me, @PathVariable String serverId) {
		return ResponseEntity.ok(Response.ok(inviteService.list(serverId, me.id()), "Server Invites"));
	}

	@GetMapping("/{serverId}/audit-logs")
	public ResponseEntity<Response<PageResponse<AuditLogResponse>>> auditLogs(@AuthenticationPrincipal AuthUser me,
			@PathVariable String serverId,
			@RequestParam(value = "page", defaultValue = "0") int page,
			@RequestParam(value = "size", defaultValue = "30") int size) {
		return ResponseEntity.ok(Response.ok(auditLogService.list(serverId, page, size, me.id()), "Audit Log"));
	}
}
