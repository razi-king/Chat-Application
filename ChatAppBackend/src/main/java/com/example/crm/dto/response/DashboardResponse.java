package com.example.crm.dto.response;



import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardResponse {
	private long friends;
	private long pendingRequests;
	private long servers;
	private long conversations;
	private long unreadMessages;
	private long unreadNotifications;
	private long onlineFriends;
}
