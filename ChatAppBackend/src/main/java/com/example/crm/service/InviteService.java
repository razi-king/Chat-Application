package com.example.crm.service;

import java.util.List;

import com.example.crm.dto.request.CreateInviteRequest;
import com.example.crm.dto.response.InviteResponse;
import com.example.crm.dto.response.ServerResponse;

public interface InviteService {

	InviteResponse create(String serverId, CreateInviteRequest request, String userId);

	InviteResponse preview(String code);

	ServerResponse accept(String code, String userId);

	List<InviteResponse> list(String serverId, String userId);
}
