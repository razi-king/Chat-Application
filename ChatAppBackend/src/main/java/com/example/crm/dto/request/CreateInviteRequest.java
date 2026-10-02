package com.example.crm.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import lombok.Data;

@Data
public class CreateInviteRequest {
	// 0 = Unlimited
	@Min(value = 0, message = "Max uses cannot be negative")
	@Max(value = 1000, message = "Max uses must be at most 1000")
	private int maxUses;

	// 0 = Never Expires
	@Min(value = 0, message = "Expiry cannot be negative")
	@Max(value = 720, message = "Expiry must be at most 30 days")
	private int expiresInHours = 24;
}
