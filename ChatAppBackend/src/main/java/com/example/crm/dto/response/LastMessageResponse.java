package com.example.crm.dto.response;

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LastMessageResponse {
	private String messageId;
	private String senderId;
	private String senderName;
	private String preview;
	private Instant sentAt;
}
