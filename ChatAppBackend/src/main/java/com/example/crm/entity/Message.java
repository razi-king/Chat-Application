package com.example.crm.entity;

import java.time.LocalDateTime;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class Message {
	private String sender;
	private String content;
	private LocalDateTime timeStamp;
	public Message(String sender, String content) {
		this.sender = sender;
		this.content = content;
		this.timeStamp = LocalDateTime.now();
	}
}
