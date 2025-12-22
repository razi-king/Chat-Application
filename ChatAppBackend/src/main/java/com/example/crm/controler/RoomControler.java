package com.example.crm.controler;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.crm.dto.Response;
import com.example.crm.dto.RoomRequestDto;
import com.example.crm.dto.RoomResponseDto;
import com.example.crm.entity.Message;
import com.example.crm.service.RoomService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/room")
public class RoomControler {
	@Autowired
	RoomService roomService;
	// task for also remembering who created the room and how many people join the room hw 
	// Create Room
	@PostMapping
	public ResponseEntity<Response> createRoom(@Valid @RequestBody RoomRequestDto roomRequestDto) {
		RoomResponseDto roomResponse = roomService.createRoom(roomRequestDto);
		Response response = new Response();
		response.success();
		response.setData(roomResponse);
		response.setMessage("Room Created Successfully");
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}
	// Get Room
	@GetMapping("/{roomId}")
	public ResponseEntity<Response> getRoomByRoomId(@PathVariable String roomId) {
		RoomResponseDto roomResponse = roomService.getRoomByRoomId(roomId);
		Response response = new Response();
		response.success();
		response.setData(roomResponse);
		response.setMessage("Get The Room By Room Id ");
		return ResponseEntity.ok(response);
	}
	// Get Messages Of Room
	@GetMapping("/message/{roomId}")
	public ResponseEntity<Response> getMessageByRoomId(@PathVariable String roomId) {
		List<Message> msgList = roomService.getMessageByRoomId(roomId);
		Response response = new Response();
		response.success();
		response.setData(msgList);
		response.setMessage("Get All The Messages Of A Room By The Room Id");
		return ResponseEntity.ok(response);
	}
	@GetMapping("/message2/{roomId}")
	public ResponseEntity<Response> getMessageByRoomId2(
			@PathVariable String roomId,
			@RequestParam(value = "page", defaultValue = "0", required = false) int page,
			@RequestParam(value = "size", defaultValue = "20", required = false) int size
			){
		List<Message> msgList = roomService.getMessageByRoomId2(roomId, page, size);
		Response response = new Response();
		response.success();
		System.out.println(msgList);
		response.setData(msgList);
		response.setMessage("Get Message Based On The Size, Page And RoomId");
		return ResponseEntity.ok(response);
		
	}
	
}
