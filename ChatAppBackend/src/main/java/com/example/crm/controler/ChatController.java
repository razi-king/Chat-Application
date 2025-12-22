package com.example.crm.controler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.crm.dto.Response;
import com.example.crm.entity.Message;
import com.example.crm.payload.MessageRequest;
import com.example.crm.repository.RoomRepository;
import com.example.crm.service.ChatService;

@RestController
@RequestMapping("/api/v1/chat")
public class ChatController {
	@Autowired
	private ChatService chatService;
	// We Use Messagemapping For Sending And Recieving Data
	@MessageMapping("/sendMessage/{roomId}")
	public ResponseEntity<Response> sendMessage(
		@DestinationVariable String roomId,
		@RequestBody MessageRequest request
	){
		Message message = chatService.sendMessage(roomId, request); 
		Response response = new Response();
		response.success();
		response.setData(message);
		response.setMessage("Message Save SuccessFully");
		return ResponseEntity.ok(response);
	}
}	
