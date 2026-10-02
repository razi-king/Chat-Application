package com.example.crm.security;

import java.security.Principal;

/**
 * The logged in user, taken from the JWT. getName() returns the userId, so
 * SimpMessagingTemplate.convertAndSendToUser(userId, ...) reaches the right socket.
 */
public record AuthUser(String id, String username) implements Principal {
	@Override
	public String getName() {
		return id;
	}
}
