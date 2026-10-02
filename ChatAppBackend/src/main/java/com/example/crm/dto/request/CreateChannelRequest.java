package com.example.crm.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import lombok.Data;

@Data
public class CreateChannelRequest {
	@NotBlank(message = "Channel name is required")
	@Size(max = 30, message = "Channel name must not exceed 30 characters")
	@Pattern(regexp = "^[a-z0-9-]+$", message = "Use lowercase letters, numbers and dashes")
	private String name;

	@Size(max = 120, message = "Topic must not exceed 120 characters")
	private String description;
}
