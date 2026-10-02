package com.example.crm.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
	private final WebSocketAuthInterceptor webSocketAuthInterceptor;

	@Value("${app.cors.allowed-origins}")
	private List<String> allowedOrigins;

	@Override
	public void configureMessageBroker(MessageBrokerRegistry config) {
		ThreadPoolTaskScheduler heartbeatScheduler = new ThreadPoolTaskScheduler();
		heartbeatScheduler.setPoolSize(1);
		heartbeatScheduler.setThreadNamePrefix("ws-heartbeat-");
		heartbeatScheduler.initialize();

		// /topic/rooms/{roomId} -> Everyone In A Room, /topic/presence -> Online/Offline
		// /user/queue/...       -> Private To One User (Notifications, Unread Counts, Errors)
		config.enableSimpleBroker("/topic", "/queue")
				.setHeartbeatValue(new long[] { 10000, 10000 })
				.setTaskScheduler(heartbeatScheduler);
		config.setApplicationDestinationPrefixes("/app");
		config.setUserDestinationPrefix("/user");
	}

	@Override
	public void registerStompEndpoints(StompEndpointRegistry registry) {
		String[] origins = allowedOrigins.toArray(String[]::new);
		// Plain WebSocket (Used By The Next.js Client)
		registry.addEndpoint("/ws").setAllowedOriginPatterns(origins);
		// SockJS Fall Back For Old Browsers
		registry.addEndpoint("/chat").setAllowedOriginPatterns(origins).withSockJS();
	}

	@Override
	public void configureClientInboundChannel(ChannelRegistration registration) {
		registration.interceptors(webSocketAuthInterceptor);
	}
}
