package com.example.crm.dto.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import lombok.Data;

@Data
public class UpdateProfileRequest {
	@Size(min = 1, max = 40, message = "Display name must be 1-40 characters")
	private String displayName;

	@Size(max = 140, message = "About must not exceed 140 characters")
	private String about;

	@Size(max = 60, message = "Status must not exceed 60 characters")
	private String customStatus;

	@Pattern(regexp = "^#[0-9a-fA-F]{6}$", message = "Color must be a hex like #22d3ee")
	private String avatarColor;
}
