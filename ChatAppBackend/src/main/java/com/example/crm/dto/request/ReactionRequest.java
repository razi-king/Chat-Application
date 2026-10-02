package com.example.crm.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.Data;

@Data
public class ReactionRequest {
	@NotBlank(message = "Emoji is required")
	@Size(max = 16, message = "Invalid emoji")
	private String emoji;
}
