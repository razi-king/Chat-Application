package com.example.crm.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import lombok.Data;

@Data
public class RegisterRequest {
	@NotBlank(message = "Username is required")
	@Size(min = 3, max = 20, message = "Username must be 3-20 characters")
	@Pattern(regexp = "^[a-zA-Z0-9_.]+$", message = "Only letters, numbers, dot and underscore")
	private String username;

	@NotBlank(message = "Email is required")
	@Email(message = "Enter a valid email")
	private String email;

	@NotBlank(message = "Password is required")
	@Size(min = 6, max = 64, message = "Password must be at least 6 characters")
	private String password;

	@NotBlank(message = "Display name is required")
	@Size(max = 40, message = "Display name must not exceed 40 characters")
	private String displayName;
}
