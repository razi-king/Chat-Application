package com.example.crm.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.Data;

@Data
public class EditMessageRequest {
	@NotBlank(message = "Message cannot be empty")
	@Size(max = 2000, message = "Message must not exceed 2000 characters")
	private String content;
}
