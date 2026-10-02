package com.example.crm.payload;

import lombok.Data;

// Sent By The Client To /app/rooms/{roomId}/typing
@Data
public class TypingPayload {
	private boolean typing;
}
