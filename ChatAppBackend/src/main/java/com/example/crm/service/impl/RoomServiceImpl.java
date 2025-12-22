package com.example.crm.service.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.crm.dto.RoomRequestDto;
import com.example.crm.dto.RoomResponseDto;
import com.example.crm.entity.Message;
import com.example.crm.entity.Room;
import com.example.crm.enums.ErrorCode;
import com.example.crm.exception.DuplicateResourceException;
import com.example.crm.exception.ResourceNotFoundException;
import com.example.crm.repository.RoomRepository;
import com.example.crm.service.RoomService;

import jakarta.validation.Valid;

@Service
public class RoomServiceImpl implements RoomService {
	@Autowired
	private RoomRepository roomRepository;
	@Override
	public RoomResponseDto createRoom(@Valid RoomRequestDto roomRequestDto) {
		Room savedRoom = new Room();
		if (roomRepository.findByRoomId(roomRequestDto.getRoomId()) != null) {
			throw new DuplicateResourceException(
					"Room Is Already Exists",
					ErrorCode.ROOM_IS_ALREADY_CREATED);
		}
		else {
			Room room = new Room();
			room.setRoomId(roomRequestDto.getRoomId());
			if (roomRequestDto.getMessage() == null || !roomRequestDto.getMessage().isEmpty()) {
				room.setMessage(roomRequestDto.getMessage());
			}
			savedRoom = roomRepository.save(room);
		}
		return convertToDto(savedRoom);
	}

	private RoomResponseDto convertToDto(Room savedRoom) {
		RoomResponseDto roomResponse = new RoomResponseDto();
		roomResponse.setId(savedRoom.getId());
		roomResponse.setMessage(savedRoom.getMessage());
		roomResponse.setRoomId(savedRoom.getRoomId());
		return roomResponse;
	}

	@Override
	public List<Message> getMessageByRoomId(String roomId) {
		if(!roomRepository.existsByRoomId(roomId)) {
			throw new ResourceNotFoundException(
					"Room Not Found For This Room Id "+roomId,
					ErrorCode.ROOM_NOT_FOUND);
		}
		Room room = roomRepository.findByRoomId(roomId);
		List<Message> messageList = room.getMessage();
		return messageList;
	}

	@Override
	public RoomResponseDto getRoomByRoomId(String roomId) {
		if(!roomRepository.existsByRoomId(roomId)) {
			throw new ResourceNotFoundException(
					"Room Not Found For This Room Id "+roomId,
					ErrorCode.ROOM_NOT_FOUND);
		}
		Room room = roomRepository.findByRoomId(roomId);
		return convertToDto(room);
	}

	@Override
	public List<Message> getMessageByRoomId2(String roomId, int page, int size) {
		Room room = roomRepository.findByRoomId(roomId);
	    if (room == null) {
	        throw new ResourceNotFoundException(
	                "Room Not Found For This Room Id " + roomId,
	                ErrorCode.ROOM_NOT_FOUND);
	    }

	    List<Message> messageList = room.getMessage();
	    int listSize = messageList.size();

	    // 2. Calculate indices for Reverse Pagination (Chat History)
	    
	    // START: Calculates the starting index of the page, counting backward from the list end.
	    // Page 0 retrieves the last 'size' elements.
	    int start = Math.max(0, listSize - (page + 1) * size);
	    
	    // END: Calculates the exclusive end index (start + size), ensuring it doesn't exceed the list size.
	    // Note: The previous page (page-1) starts where the current page ends (listSize - page * size)
	    // A simpler way is to use start + size, which is guaranteed to work because start is correctly clamped.
	    int end = Math.min(listSize, start + size); 
	    
	    // 3. Return the Paginated Sublist
	    // subList(start, end) is inclusive of 'start' and exclusive of 'end'.
	    List<Message> paginatedMessage = messageList.subList(start, end);
	    return paginatedMessage;
	}

}
