package com.example.crm.controler;

import java.security.Principal;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.support.MethodArgumentNotValidException;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.crm.dto.Response;
import com.example.crm.dto.request.EditMessageRequest;
import com.example.crm.dto.request.ReactionRequest;
import com.example.crm.dto.request.SendMessageRequest;
import com.example.crm.dto.response.MessageResponse;
import com.example.crm.enums.ErrorCode;
import com.example.crm.exception.ChatAppException;
import com.example.crm.payload.TypingPayload;
import com.example.crm.security.AuthUser;
import com.example.crm.service.ChatService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/v1/messages")
@RequiredArgsConstructor
public class ChatController {
	private final ChatService chatService;

	// ---------------- WebSocket (STOMP) ----------------

	// Client Sends To /app/rooms/{roomId}/send -> Saved -> Broadcast On /topic/rooms/{roomId}
	@MessageMapping("/rooms/{roomId}/send")
	public void sendMessage(@DestinationVariable String roomId, @Valid @Payload SendMessageRequest request, Principal principal) {
		chatService.sendMessage(roomId, request, principal.getName());
	}

	// Client Sends To /app/rooms/{roomId}/typing -> "Razi is typing..." On /topic/rooms/{roomId}
	@MessageMapping("/rooms/{roomId}/typing")
	public void typing(@DestinationVariable String roomId, @Payload TypingPayload payload, Principal principal) {
		chatService.typing(roomId, payload.isTyping(), principal.getName());
	}

	// WebSocket Errors Use The Same Response Envelope, Delivered Privately On /user/queue/errors
	@MessageExceptionHandler(ChatAppException.class)
	@SendToUser(destinations = "/queue/errors", broadcast = false)
	public Response<Void> handleChatAppException(ChatAppException ex) {
		return Response.error(ex.getErrorCode(), ex.getMessage(), ex.getMeta());
	}

	@MessageExceptionHandler(MethodArgumentNotValidException.class)
	@SendToUser(destinations = "/queue/errors", broadcast = false)
	public Response<Void> handleValidation(MethodArgumentNotValidException ex) {
		Map<String, String> fieldErrors = new LinkedHashMap<>();
		if (ex.getBindingResult() != null) {
			ex.getBindingResult().getFieldErrors().forEach(fe -> fieldErrors.putIfAbsent(fe.getField(), fe.getDefaultMessage()));
		}
		return Response.error(ErrorCode.VALIDATION_FAILED, "Message is not valid", fieldErrors);
	}

	@MessageExceptionHandler(Exception.class)
	@SendToUser(destinations = "/queue/errors", broadcast = false)
	public Response<Void> handleUnexpected(Exception ex) {
		log.error("WebSocket error", ex);
		return Response.error(ErrorCode.INTERNAL_ERROR, ErrorCode.INTERNAL_ERROR.getDefaultMessage(), null);
	}

	// ---------------- REST (Edit / Delete / React / Pin) ----------------

	@PutMapping("/{messageId}")
	public ResponseEntity<Response<MessageResponse>> edit(@AuthenticationPrincipal AuthUser me, @PathVariable String messageId,
			@Valid @RequestBody EditMessageRequest request) {
		return ResponseEntity.ok(Response.ok(chatService.edit(messageId, request.getContent(), me.id()), "Message Edited"));
	}

	@DeleteMapping("/{messageId}")
	public ResponseEntity<Response<MessageResponse>> delete(@AuthenticationPrincipal AuthUser me, @PathVariable String messageId) {
		return ResponseEntity.ok(Response.ok(chatService.delete(messageId, me.id()), "Message Deleted"));
	}

	@PostMapping("/{messageId}/reactions")
	public ResponseEntity<Response<MessageResponse>> react(@AuthenticationPrincipal AuthUser me, @PathVariable String messageId,
			@Valid @RequestBody ReactionRequest request) {
		return ResponseEntity.ok(Response.ok(chatService.react(messageId, request.getEmoji(), me.id()), "Reaction Updated"));
	}

	@PostMapping("/{messageId}/pin")
	public ResponseEntity<Response<MessageResponse>> pin(@AuthenticationPrincipal AuthUser me, @PathVariable String messageId) {
		return ResponseEntity.ok(Response.ok(chatService.togglePin(messageId, me.id()), "Pin Updated"));
	}
}
