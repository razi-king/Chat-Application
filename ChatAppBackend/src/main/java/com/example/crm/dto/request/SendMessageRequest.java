package com.example.crm.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import lombok.Data;

@Data
public class SendMessageRequest {
	@NotBlank(message = "Message cannot be empty")
	@Size(max = 2000, message = "Message must not exceed 2000 characters")
	private String content;

	private String replyToId;

	// Optional Idempotency Key (The Client's Temporary Id). Same Key Twice = Same Message.
	@Size(max = 64, message = "clientMessageId must not exceed 64 characters")
	@Pattern(regexp = "^[a-zA-Z0-9_-]*$", message = "clientMessageId may only contain letters, digits, - and _")
	private String clientMessageId;
}
