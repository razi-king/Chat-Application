package com.example.crm.dto.response;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import com.example.crm.enums.MessageType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageResponse {
	private String id;
	private String roomId;
	// Echoed Back So The Sender Can Swap Its Optimistic Copy For The Saved One
	private String clientMessageId;
	private UserResponse sender;
	private String content;
	private MessageType type;
	private ReplyPreviewResponse replyTo;
	private List<ReactionResponse> reactions;
	private Set<String> mentions;
	private boolean edited;
	private boolean deleted;
	private boolean pinned;
	private Instant createdAt;
	private Instant editedAt;
}
