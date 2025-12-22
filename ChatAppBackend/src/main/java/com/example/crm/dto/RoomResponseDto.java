package com.example.crm.dto;

import java.util.List;

import com.example.crm.entity.Message;

import lombok.Data;

@Data
public class RoomResponseDto {
	private String id;
	private String roomId;
	private List<Message> message;
}
