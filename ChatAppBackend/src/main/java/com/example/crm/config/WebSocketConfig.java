package com.example.crm.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
	@Override 
	public void configureMessageBroker(MessageBrokerRegistry config) {
		config.enableSimpleBroker("/topic");
		// /topic/message
		config.setApplicationDestinationPrefixes("/app");
	}
	@Override 
	public void registerStompEndpoints(StompEndpointRegistry registry) {
		registry.addEndpoint("/chat") // /chat Endpoint Par Hamara Connection Establish Hoga 
				.setAllowedOriginPatterns("http://localhost:3000")
				.withSockJS(); // for Fall Back
	}
}
