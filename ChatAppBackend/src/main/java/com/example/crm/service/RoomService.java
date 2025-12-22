package com.example.crm.service;

import java.util.List;

import com.example.crm.dto.RoomRequestDto;
import com.example.crm.dto.RoomResponseDto;
import com.example.crm.entity.Message;

import jakarta.validation.Valid;

public interface RoomService {

	RoomResponseDto createRoom(@Valid RoomRequestDto roomRequestDto);

	List<Message> getMessageByRoomId(String roomId);

	RoomResponseDto getRoomByRoomId(String roomId);

	List<Message> getMessageByRoomId2(String roomId, int page, int size);

}
