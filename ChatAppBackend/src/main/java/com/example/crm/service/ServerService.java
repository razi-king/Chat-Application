package com.example.crm.service;

import java.util.List;

import com.example.crm.dto.request.CreateServerRequest;
import com.example.crm.dto.response.ServerDetailResponse;
import com.example.crm.dto.response.ServerMemberResponse;
import com.example.crm.dto.response.ServerResponse;
import com.example.crm.entity.Server;
import com.example.crm.enums.MemberRole;

public interface ServerService {

	ServerResponse create(CreateServerRequest request, String userId);

	List<ServerResponse> mine(String userId);

	List<ServerResponse> discover(String userId);

	ServerDetailResponse detail(String serverId, String userId);

	ServerResponse update(String serverId, CreateServerRequest request, String userId);

	void delete(String serverId, String userId);

	ServerResponse join(String serverId, String userId);

	/** Adds the user to the server and every channel (used by public join and invites). */
	ServerResponse addMember(Server server, String userId);

	void leave(String serverId, String userId);

	void kick(String serverId, String targetUserId, String userId);

	ServerMemberResponse updateRole(String serverId, String targetUserId, MemberRole role, String userId);
}
