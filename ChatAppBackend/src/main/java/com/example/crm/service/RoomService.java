package com.example.crm.service;

import java.util.List;

import com.example.crm.dto.request.CreateChannelRequest;
import com.example.crm.dto.request.CreateGroupRequest;
import com.example.crm.dto.response.RoomResponse;

public interface RoomService {

	List<RoomResponse> myConversations(String userId);

	RoomResponse getRoom(String roomId, String userId);

	RoomResponse openDirect(String otherUserId, String userId);

	RoomResponse createGroup(CreateGroupRequest request, String userId);

	RoomResponse createChannel(String serverId, CreateChannelRequest request, String userId);

	void deleteRoom(String roomId, String userId);

	RoomResponse addMembers(String roomId, List<String> userIds, String userId);

	void removeMember(String roomId, String targetUserId, String userId);

	void markRead(String roomId, String userId);
}
