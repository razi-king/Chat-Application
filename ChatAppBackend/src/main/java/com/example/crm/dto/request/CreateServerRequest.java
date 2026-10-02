package com.example.crm.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import lombok.Data;

@Data
public class CreateServerRequest {
	@NotBlank(message = "Server name is required")
	@Size(min = 2, max = 40, message = "Server name must be 2-40 characters")
	private String name;

	@Size(max = 200, message = "Description must not exceed 200 characters")
	private String description;

	@Pattern(regexp = "^#[0-9a-fA-F]{6}$", message = "Color must be a hex like #22d3ee")
	private String iconColor;

	private boolean discoverable;
}
