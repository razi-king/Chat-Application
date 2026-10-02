package com.example.crm.entity;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import com.example.crm.enums.NotificationType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "notifications")
// Newest-First Page Of One User's Notifications: Equality On userId Then Sort On createdAt
// Is Served Directly By This Index (No In-Memory Sort)
@CompoundIndex(name = "user_created", def = "{'userId': 1, 'createdAt': -1}")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notification {
	@Id
	private String id;
	private String userId;
	private NotificationType type;
	private String title;
	private String body;
	// Frontend Route To Open When Clicked, e.g. /app/rooms/{id}
	private String link;
	private String actorId;
	private boolean read;
	private Instant createdAt;
}
