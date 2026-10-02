package com.example.crm.service.impl;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;

import com.example.crm.dto.PageResponse;
import com.example.crm.dto.event.ChatEvent;
import com.example.crm.dto.event.TypingEvent;
import com.example.crm.dto.event.UnreadEvent;
import com.example.crm.dto.request.SendMessageRequest;
import com.example.crm.dto.response.MessageResponse;
import com.example.crm.entity.Message;
import com.example.crm.entity.Room;
import com.example.crm.entity.RoomMember;
import com.example.crm.entity.User;
import com.example.crm.enums.AuditAction;
import com.example.crm.enums.ChatEventType;
import com.example.crm.enums.ErrorCode;
import com.example.crm.enums.MessageType;
import com.example.crm.enums.NotificationType;
import com.example.crm.enums.RoomType;
import com.example.crm.exception.BadRequestException;
import com.example.crm.exception.ForbiddenException;
import com.example.crm.exception.ResourceNotFoundException;
import com.example.crm.mapper.MessageMapper;
import com.example.crm.repository.MessageRepository;
import com.example.crm.repository.RoomMemberRepository;
import com.example.crm.repository.RoomRepository;
import com.example.crm.repository.UserRepository;
import com.example.crm.security.AccessGuard;
import com.example.crm.service.AuditLogService;
import com.example.crm.service.ChatService;
import com.example.crm.service.NotificationService;
import com.example.crm.service.RateLimitService;
import com.example.crm.service.RealtimeService;
import com.example.crm.service.UnreadService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {
	private static final Pattern MENTION = Pattern.compile("@([a-zA-Z0-9_.]{3,20})");
	private static final int PREVIEW_LENGTH = 80;
	private static final String MESSAGE_RATE_ACTION = "message";
	private static final int MESSAGE_RATE_LIMIT = 8;
	private static final int MESSAGE_RATE_WINDOW_SECONDS = 5;

	private final MessageRepository messageRepository;
	private final RoomRepository roomRepository;
	private final RoomMemberRepository roomMemberRepository;
	private final UserRepository userRepository;
	private final AccessGuard accessGuard;
	private final MessageMapper messageMapper;
	private final RealtimeService realtimeService;
	private final UnreadService unreadService;
	private final RateLimitService rateLimitService;
	private final NotificationService notificationService;
	private final AuditLogService auditLogService;

	@Override
	public MessageResponse sendMessage(String roomId, SendMessageRequest request, String userId) {
		Room room = accessGuard.requireRoom(roomId);
		RoomMember senderMembership = accessGuard.requireRoomMember(roomId, userId);

		// Idempotency: A Network Retry Of A Message We Already Stored Returns The Stored One.
		// Checked Before Rate Limiting So Retries Do Not Burn The User's Quota.
		Message alreadySaved = findByIdempotencyKey(roomId, userId, request.getClientMessageId());
		if (alreadySaved != null) {
			return redeliver(alreadySaved);
		}
		// Max 8 Messages In 5 Seconds (Redis Counter) -> RateLimitException (429).
		// Soft Limit By Design: We Count Only AFTER A Successful Save, So Concurrent Retries Of One Message
		// Never Burn Quota. The Price: A Burst Of Distinct Concurrent Messages May Overshoot Slightly.
		rateLimitService.ensureBelowLimit(userId, MESSAGE_RATE_ACTION, MESSAGE_RATE_LIMIT, MESSAGE_RATE_WINDOW_SECONDS);
		String content = request.getContent().trim();
		if (content.isEmpty()) {
			throw new BadRequestException("Message cannot be empty", ErrorCode.VALIDATION_FAILED);
		}
		if (request.getReplyToId() != null) {
			messageRepository.findById(request.getReplyToId())
					.filter(m -> m.getRoomId().equals(roomId))
					.orElseThrow(() -> new BadRequestException("You can only reply to a message in this room", ErrorCode.BAD_REQUEST));
		}
		List<RoomMember> members = roomMemberRepository.findByRoomId(roomId);
		Set<String> mentions = resolveMentions(content, members);
		Instant now = Instant.now();

		Message message;
		try {
			message = messageRepository.save(Message.builder()
					.roomId(roomId).senderId(userId).clientMessageId(request.getClientMessageId())
					.content(content).type(MessageType.TEXT)
					.replyToId(request.getReplyToId()).mentions(mentions).createdAt(now).build());
		} catch (DuplicateKeyException e) {
			// Two Copies Of The Same Message Raced Past The Check Above (e.g. WebSocket Send + REST Retry).
			// The Unique Index Let Exactly One Win; The Loser Answers With The Winner's Message.
			return redeliver(findByIdempotencyKey(roomId, userId, request.getClientMessageId()));
		}
		rateLimitService.record(userId, MESSAGE_RATE_ACTION, MESSAGE_RATE_WINDOW_SECONDS);

		User sender = userRepository.findById(userId).orElseThrow();
		room.setLastMessage(new Room.LastMessage(message.getId(), userId, sender.getDisplayName(), preview(content), now));
		room.setLastActivityAt(now);
		roomRepository.save(room);

		// Sender Has Obviously Read Everything Up To Now
		senderMembership.setLastReadAt(now);
		roomMemberRepository.save(senderMembership);

		MessageResponse response = messageMapper.toResponse(message);
		realtimeService.toRoom(roomId, ChatEvent.of(ChatEventType.MESSAGE_CREATED, roomId, response));

		// Unread Badges For Everyone Else (Redis HINCRBY) Pushed To Their Private Queue
		for (RoomMember member : members) {
			if (!member.getUserId().equals(userId)) {
				long count = unreadService.increment(member);
				realtimeService.toUser(member.getUserId(), ChatEvent.of(ChatEventType.UNREAD, roomId,
						new UnreadEvent(roomId, room.getServerId(), count)));
			}
		}
		for (String mentioned : mentions) {
			if (!mentioned.equals(userId)) {
				notificationService.notify(mentioned, NotificationType.MENTION,
						sender.getDisplayName() + " mentioned you" + (room.getName() != null ? " in #" + room.getName() : ""),
						preview(content), link(room), userId);
			}
		}
		return response;
	}

	@Override
	public PageResponse<MessageResponse> getMessages(String roomId, Instant before, int size, String userId) {
		accessGuard.requireRoomMember(roomId, userId);
		PageRequest page = PageRequest.of(0, Math.min(Math.max(size, 1), 100));
		Slice<Message> slice = before == null
				? messageRepository.findByRoomIdOrderByCreatedAtDesc(roomId, page)
				: messageRepository.findByRoomIdAndCreatedAtBeforeOrderByCreatedAtDesc(roomId, before, page);
		// DB Gives Newest First, The Chat Shows Oldest First
		List<Message> ordered = new ArrayList<>(slice.getContent());
		Collections.reverse(ordered);
		return new PageResponse<>(messageMapper.toResponses(ordered), 0, page.getPageSize(), slice.hasNext());
	}

	@Override
	public MessageResponse edit(String messageId, String content, String userId) {
		Message message = requireMessage(messageId);
		if (!userId.equals(message.getSenderId()) || message.isDeleted()) {
			throw new ForbiddenException(ErrorCode.MESSAGE_NOT_OWNED);
		}
		message.setContent(content.trim());
		message.setEdited(true);
		message.setEditedAt(Instant.now());
		messageRepository.save(message);
		updateLastMessagePreview(message);
		return broadcast(ChatEventType.MESSAGE_UPDATED, message);
	}

	@Override
	public MessageResponse delete(String messageId, String userId) {
		Message message = requireMessage(messageId);
		Room room = accessGuard.requireRoom(message.getRoomId());
		if (!userId.equals(message.getSenderId()) && !accessGuard.canModerate(room, userId)) {
			throw new ForbiddenException(ErrorCode.MESSAGE_NOT_OWNED);
		}
		// Soft Delete: Keep The Row So Replies Still Point Somewhere ("This message was deleted")
		message.setDeleted(true);
		message.setContent(null);
		message.setPinned(false);
		message.getReactions().clear();
		messageRepository.save(message);
		updateLastMessagePreview(message);
		return broadcast(ChatEventType.MESSAGE_DELETED, message);
	}

	@Override
	public MessageResponse react(String messageId, String emoji, String userId) {
		Message message = requireMessage(messageId);
		accessGuard.requireRoomMember(message.getRoomId(), userId);
		if (message.isDeleted()) {
			throw new BadRequestException("Cannot react to a deleted message", ErrorCode.BAD_REQUEST);
		}
		Set<String> users = message.getReactions().computeIfAbsent(emoji, k -> new LinkedHashSet<>());
		// Toggle: Click Again To Remove Your Reaction
		if (!users.remove(userId)) {
			users.add(userId);
		}
		if (users.isEmpty()) {
			message.getReactions().remove(emoji);
		}
		messageRepository.save(message);
		return broadcast(ChatEventType.MESSAGE_UPDATED, message);
	}

	@Override
	public MessageResponse togglePin(String messageId, String userId) {
		Message message = requireMessage(messageId);
		Room room = accessGuard.requireRoom(message.getRoomId());
		accessGuard.requireRoomMember(room.getId(), userId);
		// In Server Channels Only Admins Pin (Like Discord), In DMs / Groups Anyone Can
		if (room.getType() == RoomType.CHANNEL && !accessGuard.canModerate(room, userId)) {
			throw new ForbiddenException("Only admins can pin messages in channels", ErrorCode.ACCESS_DENIED);
		}
		message.setPinned(!message.isPinned());
		messageRepository.save(message);
		if (room.getServerId() != null && message.isPinned()) {
			auditLogService.log(room.getServerId(), userId, AuditAction.MESSAGE_PINNED, message.getId(), "#" + room.getName());
		}
		return broadcast(ChatEventType.MESSAGE_UPDATED, message);
	}

	@Override
	public List<MessageResponse> pinned(String roomId, String userId) {
		accessGuard.requireRoomMember(roomId, userId);
		return messageMapper.toResponses(messageRepository.findByRoomIdAndPinnedTrueOrderByCreatedAtDesc(roomId));
	}

	@Override
	public List<MessageResponse> search(String roomId, String query, String userId) {
		accessGuard.requireRoomMember(roomId, userId);
		if (query == null || query.isBlank()) {
			return List.of();
		}
		return messageMapper.toResponses(messageRepository
				.findTop50ByRoomIdAndDeletedFalseAndContentContainingIgnoreCaseOrderByCreatedAtDesc(roomId, query.trim()));
	}

	@Override
	public void typing(String roomId, boolean typing, String userId) {
		accessGuard.requireRoomMember(roomId, userId);
		String name = userRepository.findById(userId).map(User::getDisplayName).orElse("Someone");
		realtimeService.toRoom(roomId, ChatEvent.of(ChatEventType.TYPING, roomId, new TypingEvent(userId, name, typing)));
	}

	@Override
	public void systemMessage(Room room, String content) {
		Instant now = Instant.now();
		Message message = messageRepository.save(Message.builder()
				.roomId(room.getId()).content(content).type(MessageType.SYSTEM).createdAt(now).build());
		room.setLastMessage(new Room.LastMessage(message.getId(), null, null, preview(content), now));
		room.setLastActivityAt(now);
		roomRepository.save(room);
		realtimeService.toRoom(room.getId(), ChatEvent.of(ChatEventType.MESSAGE_CREATED, room.getId(), messageMapper.toResponse(message)));
	}

	private Message findByIdempotencyKey(String roomId, String userId, String clientMessageId) {
		if (clientMessageId == null || clientMessageId.isBlank()) {
			return null;
		}
		return messageRepository.findByRoomIdAndSenderIdAndClientMessageId(roomId, userId, clientMessageId).orElse(null);
	}

	// Re-Broadcasting Is Safe Because Clients De-Duplicate By Message Id; It Helps A Sender
	// Whose Socket Dropped Before The Original Echo Arrived. Unread Counters Are NOT Touched Again.
	private MessageResponse redeliver(Message message) {
		MessageResponse response = messageMapper.toResponse(message);
		realtimeService.toRoom(message.getRoomId(), ChatEvent.of(ChatEventType.MESSAGE_CREATED, message.getRoomId(), response));
		return response;
	}

	private MessageResponse broadcast(ChatEventType type, Message message) {
		MessageResponse response = messageMapper.toResponse(message);
		realtimeService.toRoom(message.getRoomId(), ChatEvent.of(type, message.getRoomId(), response));
		return response;
	}

	private void updateLastMessagePreview(Message message) {
		roomRepository.findById(message.getRoomId()).ifPresent(room -> {
			Room.LastMessage last = room.getLastMessage();
			if (last != null && message.getId().equals(last.getMessageId())) {
				last.setPreview(message.isDeleted() ? "This message was deleted" : preview(message.getContent()));
				roomRepository.save(room);
			}
		});
	}

	private Message requireMessage(String messageId) {
		return messageRepository.findById(messageId)
				.orElseThrow(() -> new ResourceNotFoundException(ErrorCode.MESSAGE_NOT_FOUND));
	}

	// "@razi hello" -> Razi's userId, But Only If Razi Is In This Room
	private Set<String> resolveMentions(String content, List<RoomMember> members) {
		Set<String> usernames = new LinkedHashSet<>();
		Matcher matcher = MENTION.matcher(content);
		while (matcher.find() && usernames.size() < 10) {
			usernames.add(matcher.group(1).toLowerCase(Locale.ROOT));
		}
		Set<String> result = new LinkedHashSet<>();
		if (usernames.isEmpty()) {
			return result;
		}
		Set<String> memberIds = new LinkedHashSet<>();
		members.forEach(m -> memberIds.add(m.getUserId()));
		for (String username : usernames) {
			userRepository.findByUsername(username)
					.filter(u -> memberIds.contains(u.getId()))
					.ifPresent(u -> result.add(u.getId()));
		}
		return result;
	}

	private static String preview(String content) {
		if (content == null) {
			return "";
		}
		return content.length() > PREVIEW_LENGTH ? content.substring(0, PREVIEW_LENGTH) + "..." : content;
	}

	private static String link(Room room) {
		return room.getServerId() != null
				? "/app/servers/" + room.getServerId() + "/" + room.getId()
				: "/app/chats/" + room.getId();
	}
}
