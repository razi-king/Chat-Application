package com.example.crm.dto.request;

import jakarta.validation.constraints.NotBlank;

import lombok.Data;

@Data
public class FriendRequestDto {
	@NotBlank(message = "Username is required")
	private String username;
}
