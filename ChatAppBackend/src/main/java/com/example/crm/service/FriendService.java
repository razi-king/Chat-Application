package com.example.crm.service;

import java.util.List;

import com.example.crm.dto.response.FriendshipResponse;

public interface FriendService {

	List<FriendshipResponse> list(String userId);

	FriendshipResponse sendRequest(String username, String userId);

	FriendshipResponse accept(String friendshipId, String userId);

	void remove(String friendshipId, String userId);
}
