package com.example.crm.controler;

import java.time.Instant;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.crm.dto.PageResponse;
import com.example.crm.dto.Response;
import com.example.crm.dto.request.AddMembersRequest;
import com.example.crm.dto.request.CreateGroupRequest;
import com.example.crm.dto.request.OpenDirectRequest;
import com.example.crm.dto.request.SendMessageRequest;
import com.example.crm.dto.response.MessageResponse;
import com.example.crm.dto.response.RoomResponse;
import com.example.crm.security.AuthUser;
import com.example.crm.service.ChatService;
import com.example.crm.service.RoomService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

// Rooms = WhatsApp Chats (DIRECT / GROUP) And Discord Channels (CHANNEL)
@RestController
@RequestMapping("/api/v1/rooms")
@RequiredArgsConstructor
public class RoomControler {
	private final RoomService roomService;
	private final ChatService chatService;

	// WhatsApp Style Chat List (DMs + Groups)
	@GetMapping
	public ResponseEntity<Response<List<RoomResponse>>> myConversations(@AuthenticationPrincipal AuthUser me) {
		return ResponseEntity.ok(Response.ok(roomService.myConversations(me.id()), "Your Conversations"));
	}

	@GetMapping("/{roomId}")
	public ResponseEntity<Response<RoomResponse>> getRoom(@AuthenticationPrincipal AuthUser me, @PathVariable String roomId) {
		return ResponseEntity.ok(Response.ok(roomService.getRoom(roomId, me.id()), "Get The Room By Room Id"));
	}

	@PostMapping("/direct")
	public ResponseEntity<Response<RoomResponse>> openDirect(@AuthenticationPrincipal AuthUser me,
			@Valid @RequestBody OpenDirectRequest request) {
		return ResponseEntity.ok(Response.ok(roomService.openDirect(request.getUserId(), me.id()), "Direct Chat Ready"));
	}

	@PostMapping("/group")
	public ResponseEntity<Response<RoomResponse>> createGroup(@AuthenticationPrincipal AuthUser me,
			@Valid @RequestBody CreateGroupRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(Response.ok(roomService.createGroup(request, me.id()), "Group Created"));
	}

	@DeleteMapping("/{roomId}")
	public ResponseEntity<Response<Void>> deleteRoom(@AuthenticationPrincipal AuthUser me, @PathVariable String roomId) {
		roomService.deleteRoom(roomId, me.id());
		return ResponseEntity.ok(Response.ok(null, "Room Deleted"));
	}

	@PostMapping("/{roomId}/members")
	public ResponseEntity<Response<RoomResponse>> addMembers(@AuthenticationPrincipal AuthUser me, @PathVariable String roomId,
			@Valid @RequestBody AddMembersRequest request) {
		return ResponseEntity.ok(Response.ok(roomService.addMembers(roomId, request.getUserIds(), me.id()), "Members Added"));
	}

	// Remove Someone, Or Pass Your Own Id To Leave The Group
	@DeleteMapping("/{roomId}/members/{userId}")
	public ResponseEntity<Response<Void>> removeMember(@AuthenticationPrincipal AuthUser me, @PathVariable String roomId,
			@PathVariable String userId) {
		roomService.removeMember(roomId, userId, me.id());
		return ResponseEntity.ok(Response.ok(null, userId.equals(me.id()) ? "Left Group" : "Member Removed"));
	}

	@PostMapping("/{roomId}/read")
	public ResponseEntity<Response<Void>> markRead(@AuthenticationPrincipal AuthUser me, @PathVariable String roomId) {
		roomService.markRead(roomId, me.id());
		return ResponseEntity.ok(Response.ok(null, "Marked As Read"));
	}

	// Cursor Pagination: Pass The createdAt Of The Oldest Loaded Message As "before" To Load Older Ones
	@GetMapping("/{roomId}/messages")
	public ResponseEntity<Response<PageResponse<MessageResponse>>> getMessages(@AuthenticationPrincipal AuthUser me,
			@PathVariable String roomId,
			@RequestParam(value = "before", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant before,
			@RequestParam(value = "size", defaultValue = "30") int size) {
		return ResponseEntity.ok(Response.ok(chatService.getMessages(roomId, before, size, me.id()), "Messages"));
	}

	// REST Fallback For Sending (The Client Normally Sends Over WebSocket)
	@PostMapping("/{roomId}/messages")
	public ResponseEntity<Response<MessageResponse>> sendMessage(@AuthenticationPrincipal AuthUser me, @PathVariable String roomId,
			@Valid @RequestBody SendMessageRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(Response.ok(chatService.sendMessage(roomId, request, me.id()), "Message Sent"));
	}

	@GetMapping("/{roomId}/pinned")
	public ResponseEntity<Response<List<MessageResponse>>> pinned(@AuthenticationPrincipal AuthUser me, @PathVariable String roomId) {
		return ResponseEntity.ok(Response.ok(chatService.pinned(roomId, me.id()), "Pinned Messages"));
	}

	@GetMapping("/{roomId}/search")
	public ResponseEntity<Response<List<MessageResponse>>> search(@AuthenticationPrincipal AuthUser me, @PathVariable String roomId,
			@RequestParam("q") String q) {
		return ResponseEntity.ok(Response.ok(chatService.search(roomId, q, me.id()), "Search Results"));
	}
}
