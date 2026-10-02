package com.example.crm.dto.event;



import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UnreadEvent {
	private String roomId;
	private String serverId;
	private long unreadCount;
}
