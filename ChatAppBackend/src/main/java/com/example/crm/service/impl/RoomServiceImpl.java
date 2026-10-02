package com.example.crm.service.impl;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import com.example.crm.dto.event.ChatEvent;
import com.example.crm.dto.event.ReadEvent;
import com.example.crm.dto.event.UnreadEvent;
import com.example.crm.dto.request.CreateChannelRequest;
import com.example.crm.dto.request.CreateGroupRequest;
import com.example.crm.dto.response.RoomResponse;
import com.example.crm.dto.response.UserResponse;
import com.example.crm.entity.Room;
import com.example.crm.entity.RoomMember;
import com.example.crm.entity.ServerMember;
import com.example.crm.entity.User;
import com.example.crm.enums.AuditAction;
import com.example.crm.enums.ChatEventType;
import com.example.crm.enums.ErrorCode;
import com.example.crm.enums.MemberRole;
import com.example.crm.enums.NotificationType;
import com.example.crm.enums.RoomType;
import com.example.crm.exception.BadRequestException;
import com.example.crm.exception.DuplicateResourceException;
import com.example.crm.exception.ForbiddenException;
import com.example.crm.mapper.RoomMapper;
import com.example.crm.mapper.UserMapper;
import com.example.crm.repository.MessageRepository;
import com.example.crm.repository.RoomMemberRepository;
import com.example.crm.repository.RoomRepository;
import com.example.crm.repository.ServerMemberRepository;
import com.example.crm.repository.UserRepository;
import com.example.crm.security.AccessGuard;
import com.example.crm.service.AuditLogService;
import com.example.crm.service.ChatService;
import com.example.crm.service.NotificationService;
import com.example.crm.service.RealtimeService;
import com.example.crm.service.RoomService;
import com.example.crm.service.UnreadService;
import com.example.crm.service.UserService;
import com.example.crm.util.Colors;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RoomServiceImpl implements RoomService {
	private final RoomRepository roomRepository;
	private final RoomMemberRepository roomMemberRepository;
	private final ServerMemberRepository serverMemberRepository;
	private final MessageRepository messageRepository;
	private final UserRepository userRepository;
	private final AccessGuard accessGuard;
	private final UserService userService;
	private final ChatService chatService;
	private final NotificationService notificationService;
	private final AuditLogService auditLogService;
	private final UnreadService unreadService;
	private final RealtimeService realtimeService;
	private final RoomMapper roomMapper;
	private final UserMapper userMapper;

	// WhatsApp Style Chat List: DMs + Groups, Newest Activity First
	@Override
	public List<RoomResponse> myConversations(String userId) {
		List<RoomMember> memberships = roomMemberRepository.findByUserId(userId);
		Map<String, RoomMember> myMembership = new HashMap<>();
		memberships.forEach(m -> myMembership.put(m.getRoomId(), m));
		List<Room> rooms = roomRepository.findAllById(myMembership.keySet()).stream()
				.filter(r -> r.getType() != RoomType.CHANNEL).toList();
		List<RoomMember> roomMemberships = rooms.stream().map(r -> myMembership.get(r.getId())).toList();
		Map<String, Long> unread = unreadService.counts(userId, roomMemberships);

		List<RoomMember> allMembers = roomMemberRepository.findByRoomIdIn(rooms.stream().map(Room::getId).toList());
		Map<String, List<RoomMember>> membersByRoom = new HashMap<>();
		allMembers.forEach(m -> membersByRoom.computeIfAbsent(m.getRoomId(), k -> new ArrayList<>()).add(m));
		Map<String, UserResponse> users = userMapper.mapByIds(allMembers.stream().map(RoomMember::getUserId).toList());

		return rooms.stream()
				.sorted(Comparator.comparing(Room::getLastActivityAt, Comparator.nullsLast(Comparator.reverseOrder())))
				.map(r -> roomMapper.toResponse(r, myMembership.get(r.getId()), unread.getOrDefault(r.getId(), 0L),
						membersByRoom.getOrDefault(r.getId(), List.of()), users))
				.toList();
	}

	@Override
	public RoomResponse getRoom(String roomId, String userId) {
		Room room = accessGuard.requireRoom(roomId);
		RoomMember me = accessGuard.requireRoomMember(roomId, userId);
		return toResponse(room, me);
	}

	@Override
	public RoomResponse openDirect(String otherUserId, String userId) {
		if (otherUserId.equals(userId)) {
			throw new BadRequestException("You cannot open a chat with yourself", ErrorCode.BAD_REQUEST);
		}
		User other = userService.requireUser(otherUserId);
		String key = userId.compareTo(otherUserId) < 0 ? userId + ":" + otherUserId : otherUserId + ":" + userId;
		Room existing = roomRepository.findByDirectKey(key).orElse(null);
		if (existing != null) {
			return toResponse(existing, accessGuard.requireRoomMember(existing.getId(), userId));
		}
		Instant now = Instant.now();
		Room room;
		try {
			room = roomRepository.save(Room.builder().type(RoomType.DIRECT).directKey(key)
					.createdBy(userId).createdAt(now).lastActivityAt(now).build());
		} catch (DuplicateKeyException e) {
			// Both Users Clicked "Message" At The Same Time -> Use The One That Won
			room = roomRepository.findByDirectKey(key).orElseThrow();
			return toResponse(room, accessGuard.requireRoomMember(room.getId(), userId));
		}
		RoomMember me = roomMemberRepository.save(member(room.getId(), userId, MemberRole.MEMBER, now));
		roomMemberRepository.save(member(room.getId(), other.getId(), MemberRole.MEMBER, now));
		realtimeService.toUser(other.getId(), ChatEvent.of(ChatEventType.ROOM_UPDATED, room.getId(), Map.of("action", "created")));
		return toResponse(room, me);
	}

	@Override
	public RoomResponse createGroup(CreateGroupRequest request, String userId) {
		User creator = userService.requireUser(userId);
		Set<String> memberIds = new LinkedHashSet<>(request.getMemberIds());
		memberIds.remove(userId);
		List<User> members = userRepository.findAllById(memberIds);
		if (members.isEmpty()) {
			throw new BadRequestException("Pick at least one valid member", ErrorCode.VALIDATION_FAILED);
		}
		Instant now = Instant.now();
		Room room = roomRepository.save(Room.builder().type(RoomType.GROUP).name(request.getName().trim())
				.description(request.getDescription()).iconColor(Colors.random()).createdBy(userId)
				.createdAt(now).lastActivityAt(now).build());
		RoomMember me = roomMemberRepository.save(member(room.getId(), userId, MemberRole.OWNER, now));
		for (User u : members) {
			roomMemberRepository.save(member(room.getId(), u.getId(), MemberRole.MEMBER, now));
			notificationService.notify(u.getId(), NotificationType.ADDED_TO_GROUP, "Added to " + room.getName(),
					creator.getDisplayName() + " added you to the group", "/app/chats/" + room.getId(), userId);
			realtimeService.toUser(u.getId(), ChatEvent.of(ChatEventType.ROOM_UPDATED, room.getId(), Map.of("action", "created")));
		}
		chatService.systemMessage(room, creator.getDisplayName() + " created the group \"" + room.getName() + "\"");
		return toResponse(roomRepository.findById(room.getId()).orElse(room), me);
	}

	@Override
	public RoomResponse createChannel(String serverId, CreateChannelRequest request, String userId) {
		accessGuard.requireServer(serverId);
		accessGuard.requireServerRole(serverId, userId, MemberRole.ADMIN);
		String name = request.getName().trim();
		boolean taken = roomRepository.findByServerIdOrderByPositionAsc(serverId).stream().anyMatch(r -> name.equals(r.getName()));
		if (taken) {
			throw new DuplicateResourceException("A channel named #" + name + " already exists", ErrorCode.ROOM_IS_ALREADY_CREATED,
					Map.of("name", "Channel name already used"));
		}
		Instant now = Instant.now();
		Room room = roomRepository.save(Room.builder().type(RoomType.CHANNEL).serverId(serverId).name(name)
				.description(request.getDescription()).createdBy(userId)
				.position((int) roomRepository.countByServerId(serverId)).createdAt(now).lastActivityAt(now).build());
		// Every Server Member Can See The New Channel
		List<ServerMember> serverMembers = serverMemberRepository.findByServerId(serverId);
		RoomMember me = null;
		for (ServerMember sm : serverMembers) {
			RoomMember saved = roomMemberRepository.save(member(room.getId(), sm.getUserId(), null, now));
			if (sm.getUserId().equals(userId)) {
				me = saved;
			}
		}
		auditLogService.log(serverId, userId, AuditAction.CHANNEL_CREATED, room.getId(), "#" + name);
		notifyServer(serverMembers, serverId, "channel-created");
		return toResponse(room, me);
	}

	@Override
	public void deleteRoom(String roomId, String userId) {
		Room room = accessGuard.requireRoom(roomId);
		switch (room.getType()) {
		case DIRECT -> throw new BadRequestException("Direct chats cannot be deleted", ErrorCode.BAD_REQUEST);
		case CHANNEL -> {
			accessGuard.requireServerRole(room.getServerId(), userId, MemberRole.ADMIN);
			if (roomRepository.countByServerId(room.getServerId()) <= 1) {
				throw new BadRequestException("A server needs at least one channel", ErrorCode.BAD_REQUEST);
			}
			auditLogService.log(room.getServerId(), userId, AuditAction.CHANNEL_DELETED, roomId, "#" + room.getName());
		}
		case GROUP -> {
			RoomMember me = accessGuard.requireRoomMember(roomId, userId);
			if (me.getRole() != MemberRole.OWNER) {
				throw new ForbiddenException("Only the group owner can delete the group", ErrorCode.ACCESS_DENIED);
			}
		}
		}
		List<String> memberIds = roomMemberRepository.findByRoomId(roomId).stream().map(RoomMember::getUserId).toList();
		messageRepository.deleteByRoomId(roomId);
		roomMemberRepository.deleteByRoomId(roomId);
		roomRepository.delete(room);
		memberIds.forEach(id -> unreadService.reset(id, roomId));
		realtimeService.toUsers(memberIds, ChatEvent.of(ChatEventType.ROOM_UPDATED, roomId,
				room.getServerId() != null ? Map.of("action", "deleted", "serverId", room.getServerId()) : Map.of("action", "deleted")));
	}

	@Override
	public RoomResponse addMembers(String roomId, List<String> userIds, String userId) {
		Room room = requireGroup(roomId);
		RoomMember me = accessGuard.requireRoomMember(roomId, userId);
		if (!me.getRole().atLeast(MemberRole.ADMIN)) {
			throw new ForbiddenException("Only group admins can add members", ErrorCode.ACCESS_DENIED);
		}
		User actor = userService.requireUser(userId);
		Instant now = Instant.now();
		List<String> added = new ArrayList<>();
		for (User u : userRepository.findAllById(new LinkedHashSet<>(userIds))) {
			if (!roomMemberRepository.existsByRoomIdAndUserId(roomId, u.getId())) {
				roomMemberRepository.save(member(roomId, u.getId(), MemberRole.MEMBER, now));
				added.add(u.getDisplayName());
				notificationService.notify(u.getId(), NotificationType.ADDED_TO_GROUP, "Added to " + room.getName(),
						actor.getDisplayName() + " added you to the group", "/app/chats/" + roomId, userId);
				realtimeService.toUser(u.getId(), ChatEvent.of(ChatEventType.ROOM_UPDATED, roomId, Map.of("action", "created")));
			}
		}
		if (!added.isEmpty()) {
			chatService.systemMessage(room, actor.getDisplayName() + " added " + String.join(", ", added));
		}
		return toResponse(accessGuard.requireRoom(roomId), me);
	}

	@Override
	public void removeMember(String roomId, String targetUserId, String userId) {
		Room room = requireGroup(roomId);
		RoomMember me = accessGuard.requireRoomMember(roomId, userId);
		RoomMember target = accessGuard.requireRoomMember(roomId, targetUserId);
		User actor = userService.requireUser(userId);
		boolean leaving = targetUserId.equals(userId);
		if (!leaving && (!me.getRole().atLeast(MemberRole.ADMIN) || target.getRole().atLeast(me.getRole()))) {
			throw new ForbiddenException("You cannot remove this member", ErrorCode.ACCESS_DENIED);
		}
		roomMemberRepository.delete(target);
		unreadService.reset(targetUserId, roomId);
		List<RoomMember> remaining = roomMemberRepository.findByRoomId(roomId);
		if (remaining.isEmpty()) {
			messageRepository.deleteByRoomId(roomId);
			roomRepository.delete(room);
		} else {
			// Owner Left -> Oldest Member Becomes The New Owner (WhatsApp Behaviour)
			if (target.getRole() == MemberRole.OWNER) {
				RoomMember heir = remaining.stream()
						.min(Comparator.comparing(RoomMember::getJoinedAt, Comparator.nullsLast(Comparator.naturalOrder())))
						.orElseThrow();
				heir.setRole(MemberRole.OWNER);
				roomMemberRepository.save(heir);
			}
			String text = leaving ? actor.getDisplayName() + " left"
					: actor.getDisplayName() + " removed " + userService.requireUser(targetUserId).getDisplayName();
			chatService.systemMessage(room, text);
			realtimeService.toUsers(remaining.stream().map(RoomMember::getUserId).toList(),
					ChatEvent.of(ChatEventType.ROOM_UPDATED, roomId, Map.of("action", "members")));
		}
		realtimeService.toUser(targetUserId, ChatEvent.of(ChatEventType.ROOM_UPDATED, roomId, Map.of("action", "removed")));
	}

	@Override
	public void markRead(String roomId, String userId) {
		Room room = accessGuard.requireRoom(roomId);
		RoomMember me = accessGuard.requireRoomMember(roomId, userId);
		Instant now = Instant.now();
		me.setLastReadAt(now);
		roomMemberRepository.save(me);
		unreadService.reset(userId, roomId);
		// Blue Ticks For The Other Side + Clear My Badge On All My Tabs
		realtimeService.toRoom(roomId, ChatEvent.of(ChatEventType.READ, roomId, new ReadEvent(userId, now)));
		realtimeService.toUser(userId, ChatEvent.of(ChatEventType.UNREAD, roomId, new UnreadEvent(roomId, room.getServerId(), 0)));
	}

	private Room requireGroup(String roomId) {
		Room room = accessGuard.requireRoom(roomId);
		if (room.getType() != RoomType.GROUP) {
			throw new BadRequestException("This only works for groups", ErrorCode.BAD_REQUEST);
		}
		return room;
	}

	private RoomResponse toResponse(Room room, RoomMember me) {
		long unread = me == null ? 0 : unreadService.counts(me.getUserId(), List.of(me)).getOrDefault(room.getId(), 0L);
		if (room.getType() == RoomType.CHANNEL) {
			return roomMapper.toResponse(room, me, unread, null, null);
		}
		List<RoomMember> members = roomMemberRepository.findByRoomId(room.getId());
		Map<String, UserResponse> users = userMapper.mapByIds(members.stream().map(RoomMember::getUserId).toList());
		return roomMapper.toResponse(room, me, unread, members, users);
	}

	private void notifyServer(List<ServerMember> members, String serverId, String action) {
		realtimeService.toUsers(members.stream().map(ServerMember::getUserId).toList(),
				ChatEvent.of(ChatEventType.ROOM_UPDATED, null, Map.of("action", action, "serverId", serverId)));
	}

	private static RoomMember member(String roomId, String userId, MemberRole role, Instant now) {
		return RoomMember.builder().roomId(roomId).userId(userId).role(role).joinedAt(now).lastReadAt(now).build();
	}
}
