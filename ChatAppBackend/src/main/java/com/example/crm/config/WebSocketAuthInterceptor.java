package com.example.crm.config;

import java.util.List;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;

import com.example.crm.exception.ChatAppException;
import com.example.crm.repository.RoomMemberRepository;
import com.example.crm.security.AuthUser;
import com.example.crm.security.JwtAuthenticationFilter;
import com.example.crm.security.JwtService;
import com.example.crm.service.TokenBlacklistService;

import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;

/**
 * CONNECT   -> must carry "Authorization: Bearer <jwt>", the user becomes the socket Principal.
 * SUBSCRIBE -> /topic/rooms/{id} only allowed for members of that room.
 */
@Component
@RequiredArgsConstructor
public class WebSocketAuthInterceptor implements ChannelInterceptor {
	private static final String ROOM_TOPIC = "/topic/rooms/";

	private final JwtService jwtService;
	private final TokenBlacklistService tokenBlacklistService;
	private final RoomMemberRepository roomMemberRepository;

	@Override
	public Message<?> preSend(Message<?> message, MessageChannel channel) {
		StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
		if (accessor == null || accessor.getCommand() == null) {
			return message;
		}
		if (StompCommand.CONNECT.equals(accessor.getCommand())) {
			String token = JwtAuthenticationFilter.extractToken(accessor.getFirstNativeHeader("Authorization"));
			if (token == null) {
				throw new MessageDeliveryException("AUTH_401: Missing token");
			}
			try {
				Claims claims = jwtService.parse(token);
				if (tokenBlacklistService.isBlacklisted(claims.getId())) {
					throw new MessageDeliveryException("AUTH_403: Session expired");
				}
				AuthUser user = jwtService.toAuthUser(claims);
				accessor.setUser(new UsernamePasswordAuthenticationToken(user, null, List.of()));
			} catch (ChatAppException e) {
				throw new MessageDeliveryException(e.getErrorCode().getCode() + ": " + e.getMessage());
			}
		} else if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
			String destination = accessor.getDestination();
			if (destination != null && destination.startsWith(ROOM_TOPIC)) {
				String roomId = destination.substring(ROOM_TOPIC.length());
				String userId = accessor.getUser() != null ? accessor.getUser().getName() : null;
				if (userId == null || !roomMemberRepository.existsByRoomIdAndUserId(roomId, userId)) {
					throw new MessageDeliveryException("ROOM_403: You are not a member of this room");
				}
			}
		}
		return message;
	}
}
