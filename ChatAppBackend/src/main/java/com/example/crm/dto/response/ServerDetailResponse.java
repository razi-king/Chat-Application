package com.example.crm.dto.response;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServerDetailResponse {
	private ServerResponse server;
	private List<RoomResponse> channels;
	private List<ServerMemberResponse> members;
}
