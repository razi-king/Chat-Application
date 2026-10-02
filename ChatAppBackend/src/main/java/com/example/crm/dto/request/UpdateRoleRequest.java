package com.example.crm.dto.request;

import com.example.crm.enums.MemberRole;
import jakarta.validation.constraints.NotNull;

import lombok.Data;

@Data
public class UpdateRoleRequest {
	@NotNull(message = "Role is required")
	private MemberRole role;
}
