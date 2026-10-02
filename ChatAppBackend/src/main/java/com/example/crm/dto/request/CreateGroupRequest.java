package com.example.crm.dto.request;

import java.util.List;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import lombok.Data;

@Data
public class CreateGroupRequest {
	@NotBlank(message = "Group name is required")
	@Size(min = 2, max = 40, message = "Group name must be 2-40 characters")
	private String name;

	@Size(max = 120, message = "Description must not exceed 120 characters")
	private String description;

	@NotEmpty(message = "Pick at least one member")
	private List<String> memberIds;
}
