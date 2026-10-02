package com.example.crm.mapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.example.crm.dto.response.MessageResponse;
import com.example.crm.dto.response.ReactionResponse;
import com.example.crm.dto.response.ReplyPreviewResponse;
import com.example.crm.dto.response.UserResponse;
import com.example.crm.entity.Message;
import com.example.crm.repository.MessageRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class MessageMapper {
	private static final int REPLY_PREVIEW_LENGTH = 120;

	private final UserMapper userMapper;
	private final MessageRepository messageRepository;

	public MessageResponse toResponse(Message message) {
		return toResponses(List.of(message)).get(0);
	}

	public List<MessageResponse> toResponses(List<Message> messages) {
		// 1. Load Every Replied-To Message In One Query
		Set<String> replyIds = new HashSet<>();
		messages.forEach(m -> {
			if (m.getReplyToId() != null) {
				replyIds.add(m.getReplyToId());
			}
		});
		Map<String, Message> replies = new HashMap<>();
		messageRepository.findAllById(replyIds).forEach(r -> replies.put(r.getId(), r));

		// 2. Load Every Sender (Including Reply Senders) In One Query
		Set<String> userIds = new HashSet<>();
		messages.forEach(m -> userIds.add(m.getSenderId()));
		replies.values().forEach(r -> userIds.add(r.getSenderId()));
		Map<String, UserResponse> users = userMapper.mapByIds(userIds);

		List<MessageResponse> result = new ArrayList<>(messages.size());
		for (Message m : messages) {
			result.add(MessageResponse.builder()
					.id(m.getId())
					.roomId(m.getRoomId())
					.clientMessageId(m.getClientMessageId())
					.sender(m.getSenderId() == null ? null : users.getOrDefault(m.getSenderId(), userMapper.unknown(m.getSenderId())))
					.content(m.isDeleted() ? "" : m.getContent())
					.type(m.getType())
					.replyTo(toReplyPreview(replies.get(m.getReplyToId()), users))
					.reactions(toReactions(m))
					.mentions(m.getMentions())
					.edited(m.isEdited())
					.deleted(m.isDeleted())
					.pinned(m.isPinned())
					.createdAt(m.getCreatedAt())
					.editedAt(m.getEditedAt())
					.build());
		}
		return result;
	}

	private ReplyPreviewResponse toReplyPreview(Message reply, Map<String, UserResponse> users) {
		if (reply == null) {
			return null;
		}
		UserResponse sender = users.get(reply.getSenderId());
		String content = reply.getContent() == null ? "" : reply.getContent();
		return ReplyPreviewResponse.builder()
				.id(reply.getId())
				.senderId(reply.getSenderId())
				.senderName(sender != null ? sender.getDisplayName() : "Unknown")
				.content(reply.isDeleted() ? "" : content.substring(0, Math.min(content.length(), REPLY_PREVIEW_LENGTH)))
				.deleted(reply.isDeleted())
				.build();
	}

	private List<ReactionResponse> toReactions(Message m) {
		if (m.getReactions() == null) {
			return List.of();
		}
		return m.getReactions().entrySet().stream()
				.filter(e -> e.getValue() != null && !e.getValue().isEmpty())
				.map(e -> new ReactionResponse(e.getKey(), e.getValue().size(), e.getValue()))
				.filter(Objects::nonNull)
				.toList();
	}
}
