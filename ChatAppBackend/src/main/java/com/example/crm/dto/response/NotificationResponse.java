package com.example.crm.dto.response;

import java.time.Instant;
import com.example.crm.enums.NotificationType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {
	private String id;
	private NotificationType type;
	private String title;
	private String body;
	private String link;
	private UserResponse actor;
	private boolean read;
	private Instant createdAt;
}
