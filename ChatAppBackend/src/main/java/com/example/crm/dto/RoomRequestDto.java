package com.example.crm.dto;

import java.util.List;

import com.example.crm.entity.Message;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RoomRequestDto {
	@NotBlank
	private String roomId;
	private List<Message> message;
}
