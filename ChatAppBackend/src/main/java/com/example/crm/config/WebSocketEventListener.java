package com.example.crm.config;

import java.security.Principal;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import com.example.crm.service.PresenceService;

import lombok.RequiredArgsConstructor;

// Socket Connect / Disconnect -> Online / Offline (Stored In Redis)
@Component
@RequiredArgsConstructor
public class WebSocketEventListener {
	private final PresenceService presenceService;

	@EventListener
	public void onConnected(SessionConnectedEvent event) {
		Principal user = event.getUser();
		if (user != null) {
			presenceService.connected(user.getName());
		}
	}

	@EventListener
	public void onDisconnected(SessionDisconnectEvent event) {
		Principal user = event.getUser();
		if (user != null) {
			presenceService.disconnected(user.getName());
		}
	}
}
