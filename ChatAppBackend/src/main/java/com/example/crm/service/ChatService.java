package com.example.crm.service;

import com.example.crm.entity.Message;
import com.example.crm.payload.MessageRequest;

public interface ChatService {

	Message sendMessage(String roomId, MessageRequest request);

}
