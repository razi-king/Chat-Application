package com.example.crm.entity;

import java.time.Instant;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// Discord Style Community That Holds Many Channels
@Document(collection = "servers")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Server {
	@Id
	private String id;
	private String name;
	private String description;
	private String iconColor;
	private String ownerId;
	private boolean discoverable;
	@CreatedDate
	private Instant createdAt;
}
