package com.example.crm.dto.response;



import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReplyPreviewResponse {
	private String id;
	private String senderId;
	private String senderName;
	private String content;
	private boolean deleted;
}
