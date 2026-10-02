package com.example.crm.dto.request;

import java.util.List;
import jakarta.validation.constraints.NotEmpty;

import lombok.Data;

@Data
public class AddMembersRequest {
	@NotEmpty(message = "Pick at least one member")
	private List<String> userIds;
}
