package com.example.crm.controler;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.crm.dto.PageResponse;
import com.example.crm.dto.Response;
import com.example.crm.dto.request.FriendRequestDto;
import com.example.crm.dto.response.FriendshipResponse;
import com.example.crm.dto.response.InviteResponse;
import com.example.crm.dto.response.NotificationResponse;
import com.example.crm.dto.response.ServerResponse;
import com.example.crm.security.AuthUser;
import com.example.crm.service.FriendService;
import com.example.crm.service.InviteService;
import com.example.crm.service.NotificationService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

// Friends, Notifications And Invite Links
@RestController
@RequiredArgsConstructor
public class SocialController {
	private final FriendService friendService;
	private final NotificationService notificationService;
	private final InviteService inviteService;

	// ---------------- Friends ----------------

	@GetMapping("/api/v1/friends")
	public ResponseEntity<Response<List<FriendshipResponse>>> friends(@AuthenticationPrincipal AuthUser me) {
		return ResponseEntity.ok(Response.ok(friendService.list(me.id()), "Friends And Requests"));
	}

	@PostMapping("/api/v1/friends/requests")
	public ResponseEntity<Response<FriendshipResponse>> sendRequest(@AuthenticationPrincipal AuthUser me,
			@Valid @RequestBody FriendRequestDto request) {
		return ResponseEntity.ok(Response.ok(friendService.sendRequest(request.getUsername(), me.id()), "Friend Request Sent"));
	}

	@PostMapping("/api/v1/friends/{friendshipId}/accept")
	public ResponseEntity<Response<FriendshipResponse>> accept(@AuthenticationPrincipal AuthUser me, @PathVariable String friendshipId) {
		return ResponseEntity.ok(Response.ok(friendService.accept(friendshipId, me.id()), "Friend Request Accepted"));
	}

	@DeleteMapping("/api/v1/friends/{friendshipId}")
	public ResponseEntity<Response<Void>> remove(@AuthenticationPrincipal AuthUser me, @PathVariable String friendshipId) {
		friendService.remove(friendshipId, me.id());
		return ResponseEntity.ok(Response.ok(null, "Removed"));
	}

	// ---------------- Notifications ----------------

	@GetMapping("/api/v1/notifications")
	public ResponseEntity<Response<PageResponse<NotificationResponse>>> notifications(@AuthenticationPrincipal AuthUser me,
			@RequestParam(value = "page", defaultValue = "0") int page,
			@RequestParam(value = "size", defaultValue = "20") int size) {
		return ResponseEntity.ok(Response.ok(notificationService.list(me.id(), page, size), "Notifications"));
	}

	@GetMapping("/api/v1/notifications/unread-count")
	public ResponseEntity<Response<Map<String, Long>>> unreadCount(@AuthenticationPrincipal AuthUser me) {
		return ResponseEntity.ok(Response.ok(Map.of("count", notificationService.unreadCount(me.id())), "Unread Notifications"));
	}

	@PostMapping("/api/v1/notifications/{notificationId}/read")
	public ResponseEntity<Response<Void>> markRead(@AuthenticationPrincipal AuthUser me, @PathVariable String notificationId) {
		notificationService.markRead(notificationId, me.id());
		return ResponseEntity.ok(Response.ok(null, "Notification Read"));
	}

	@PostMapping("/api/v1/notifications/read-all")
	public ResponseEntity<Response<Void>> markAllRead(@AuthenticationPrincipal AuthUser me) {
		notificationService.markAllRead(me.id());
		return ResponseEntity.ok(Response.ok(null, "All Notifications Read"));
	}

	// ---------------- Invites ----------------

	// Public: Shows Server Name / Members Before You Join
	@GetMapping("/api/v1/invites/{code}")
	public ResponseEntity<Response<InviteResponse>> previewInvite(@PathVariable String code) {
		return ResponseEntity.ok(Response.ok(inviteService.preview(code), "Invite Details"));
	}

	@PostMapping("/api/v1/invites/{code}/accept")
	public ResponseEntity<Response<ServerResponse>> acceptInvite(@AuthenticationPrincipal AuthUser me, @PathVariable String code) {
		return ResponseEntity.ok(Response.ok(inviteService.accept(code, me.id()), "Joined Server"));
	}

	@GetMapping("/api/v1/health")
	public ResponseEntity<Response<Map<String, String>>> health() {
		return ResponseEntity.ok(Response.ok(Map.of("status", "UP"), "Server Is Running"));
	}
}
