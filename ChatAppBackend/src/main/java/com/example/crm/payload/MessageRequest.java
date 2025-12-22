package com.example.crm.payload;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class MessageRequest {
	private String content;
	private String sender;
	private String roomId;
}
