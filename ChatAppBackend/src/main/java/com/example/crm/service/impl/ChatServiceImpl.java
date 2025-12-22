package com.example.crm.service.impl;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.crm.entity.Message;
import com.example.crm.entity.Room;
import com.example.crm.enums.ErrorCode;
import com.example.crm.exception.ResourceNotFoundException;
import com.example.crm.payload.MessageRequest;
import com.example.crm.repository.RoomRepository;
import com.example.crm.service.ChatService;

@Service
public class ChatServiceImpl implements ChatService{
	@Autowired
	private RoomRepository roomRepository;
	
	@Override
	public Message sendMessage(String roomId, MessageRequest request) {
		if(roomRepository.existsByRoomId(roomId)) {
			Room room = roomRepository.findByRoomId(roomId);
			Message message = new Message();
			message.setContent(request.getContent());
			message.setSender(request.getSender());
			message.setTimeStamp(LocalDateTime.now());
			room.getMessage().add(message);
			roomRepository.save(room);
			return message;
		}
		else {
			throw new ResourceNotFoundException("Room Not Found For This RoomId "+roomId, ErrorCode.ROOM_NOT_FOUND);
		}
	}

}
