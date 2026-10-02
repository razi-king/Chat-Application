package com.example.crm.dto.request;

import jakarta.validation.constraints.NotBlank;

import lombok.Data;

@Data
public class OpenDirectRequest {
	@NotBlank(message = "User is required")
	private String userId;
}
