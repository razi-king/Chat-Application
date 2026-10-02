package com.example.crm.service.impl;

import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.example.crm.dto.event.ChatEvent;
import com.example.crm.dto.request.CreateServerRequest;
import com.example.crm.dto.response.RoomResponse;
import com.example.crm.dto.response.ServerDetailResponse;
import com.example.crm.dto.response.ServerMemberResponse;
import com.example.crm.dto.response.ServerResponse;
import com.example.crm.dto.response.UserResponse;
import com.example.crm.entity.Room;
import com.example.crm.entity.RoomMember;
import com.example.crm.entity.Server;
import com.example.crm.entity.ServerMember;
import com.example.crm.entity.User;
import com.example.crm.enums.AuditAction;
import com.example.crm.enums.ChatEventType;
import com.example.crm.enums.ErrorCode;
import com.example.crm.enums.MemberRole;
import com.example.crm.enums.NotificationType;
import com.example.crm.enums.RoomType;
import com.example.crm.exception.BadRequestException;
import com.example.crm.exception.ForbiddenException;
import com.example.crm.mapper.RoomMapper;
import com.example.crm.mapper.UserMapper;
import com.example.crm.repository.AuditLogRepository;
import com.example.crm.repository.InviteRepository;
import com.example.crm.repository.MessageRepository;
import com.example.crm.repository.RoomMemberRepository;
import com.example.crm.repository.RoomRepository;
import com.example.crm.repository.ServerMemberRepository;
import com.example.crm.repository.ServerRepository;
import com.example.crm.security.AccessGuard;
import com.example.crm.service.AuditLogService;
import com.example.crm.service.ChatService;
import com.example.crm.service.NotificationService;
import com.example.crm.service.RealtimeService;
import com.example.crm.service.ServerService;
import com.example.crm.service.UnreadService;
import com.example.crm.service.UserService;
import com.example.crm.util.Colors;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ServerServiceImpl implements ServerService {
	private static final List<String[]> DEFAULT_CHANNELS = List.of(
			new String[] { "general", "Talk about anything" },
			new String[] { "announcements", "Important updates" },
			new String[] { "off-topic", "Memes, music and everything else" });

	private final ServerRepository serverRepository;
	private final ServerMemberRepository serverMemberRepository;
	private final RoomRepository roomRepository;
	private final RoomMemberRepository roomMemberRepository;
	private final MessageRepository messageRepository;
	private final InviteRepository inviteRepository;
	private final AuditLogRepository auditLogRepository;
	private final AccessGuard accessGuard;
	private final UserService userService;
	private final ChatService chatService;
	private final AuditLogService auditLogService;
	private final NotificationService notificationService;
	private final UnreadService unreadService;
	private final RealtimeService realtimeService;
	private final UserMapper userMapper;
	private final RoomMapper roomMapper;

	@Override
	public ServerResponse create(CreateServerRequest request, String userId) {
		User owner = userService.requireUser(userId);
		Instant now = Instant.now();
		Server server = serverRepository.save(Server.builder()
				.name(request.getName().trim()).description(request.getDescription())
				.iconColor(Colors.orRandom(request.getIconColor())).ownerId(userId)
				.discoverable(request.isDiscoverable()).createdAt(now).build());
		ServerMember ownerMember = serverMemberRepository.save(ServerMember.builder()
				.serverId(server.getId()).userId(userId).role(MemberRole.OWNER).joinedAt(now).build());
		Room general = null;
		for (int i = 0; i < DEFAULT_CHANNELS.size(); i++) {
			Room channel = roomRepository.save(Room.builder().type(RoomType.CHANNEL).serverId(server.getId())
					.name(DEFAULT_CHANNELS.get(i)[0]).description(DEFAULT_CHANNELS.get(i)[1]).createdBy(userId)
					.position(i).createdAt(now).lastActivityAt(now).build());
			roomMemberRepository.save(RoomMember.builder().roomId(channel.getId()).userId(userId)
					.joinedAt(now).lastReadAt(now).build());
			if (i == 0) {
				general = channel;
			}
		}
		auditLogService.log(server.getId(), userId, AuditAction.SERVER_CREATED, server.getId(), server.getName());
		chatService.systemMessage(general, "Welcome to " + server.getName() + "! " + owner.getDisplayName()
				+ " created this server. Invite your friends to get started.");
		return toResponse(server, ownerMember, 1, 0);
	}

	@Override
	public List<ServerResponse> mine(String userId) {
		List<ServerMember> memberships = serverMemberRepository.findByUserId(userId);
		Map<String, ServerMember> byServer = memberships.stream().collect(Collectors.toMap(ServerMember::getServerId, m -> m));
		Map<String, Long> unreadByServer = unreadByServer(userId);
		return serverRepository.findAllById(byServer.keySet()).stream()
				.sorted(Comparator.comparing(s -> byServer.get(s.getId()).getJoinedAt(), Comparator.nullsLast(Comparator.naturalOrder())))
				.map(s -> toResponse(s, byServer.get(s.getId()), serverMemberRepository.countByServerId(s.getId()),
						unreadByServer.getOrDefault(s.getId(), 0L)))
				.toList();
	}

	@Override
	public List<ServerResponse> discover(String userId) {
		Set<String> mine = serverMemberRepository.findByUserId(userId).stream().map(ServerMember::getServerId).collect(Collectors.toSet());
		return serverRepository.findTop30ByDiscoverableTrueOrderByCreatedAtDesc().stream()
				.map(s -> {
					ServerResponse r = toResponse(s, null, serverMemberRepository.countByServerId(s.getId()), 0);
					if (mine.contains(s.getId())) {
						r.setMyRole(MemberRole.MEMBER);
					}
					return r;
				})
				.toList();
	}

	@Override
	public ServerDetailResponse detail(String serverId, String userId) {
		Server server = accessGuard.requireServer(serverId);
		ServerMember me = accessGuard.requireServerMember(serverId, userId);
		List<Room> channels = roomRepository.findByServerIdOrderByPositionAsc(serverId);

		Map<String, RoomMember> myChannelMemberships = new HashMap<>();
		roomMemberRepository.findByUserId(userId).forEach(m -> myChannelMemberships.put(m.getRoomId(), m));
		List<RoomMember> mineHere = channels.stream().map(c -> myChannelMemberships.get(c.getId())).filter(m -> m != null).toList();
		Map<String, Long> unread = unreadService.counts(userId, mineHere);
		List<RoomResponse> channelResponses = channels.stream()
				.map(c -> roomMapper.toResponse(c, myChannelMemberships.get(c.getId()), unread.getOrDefault(c.getId(), 0L), null, null))
				.toList();

		List<ServerMember> members = serverMemberRepository.findByServerId(serverId);
		Map<String, UserResponse> users = userMapper.mapByIds(members.stream().map(ServerMember::getUserId).toList());
		List<ServerMemberResponse> memberResponses = members.stream()
				.map(m -> toMemberResponse(m, users))
				.sorted(Comparator.comparing((ServerMemberResponse m) -> m.getRole().ordinal())
						.thenComparing(m -> m.getUser().getDisplayName(), String.CASE_INSENSITIVE_ORDER))
				.toList();

		long totalUnread = unread.values().stream().mapToLong(Long::longValue).sum();
		return ServerDetailResponse.builder()
				.server(toResponse(server, me, members.size(), totalUnread))
				.channels(channelResponses)
				.members(memberResponses)
				.build();
	}

	@Override
	public ServerResponse update(String serverId, CreateServerRequest request, String userId) {
		Server server = accessGuard.requireServer(serverId);
		ServerMember me = accessGuard.requireServerRole(serverId, userId, MemberRole.ADMIN);
		server.setName(request.getName().trim());
		server.setDescription(request.getDescription());
		server.setIconColor(Colors.orRandom(request.getIconColor()));
		server.setDiscoverable(request.isDiscoverable());
		serverRepository.save(server);
		auditLogService.log(serverId, userId, AuditAction.SERVER_UPDATED, serverId, server.getName());
		broadcastToMembers(serverId, "updated");
		return toResponse(server, me, serverMemberRepository.countByServerId(serverId), 0);
	}

	@Override
	public void delete(String serverId, String userId) {
		Server server = accessGuard.requireServer(serverId);
		accessGuard.requireServerRole(serverId, userId, MemberRole.OWNER);
		List<String> memberIds = serverMemberRepository.findByServerId(serverId).stream().map(ServerMember::getUserId).toList();
		List<String> roomIds = roomRepository.findByServerIdOrderByPositionAsc(serverId).stream().map(Room::getId).toList();
		messageRepository.deleteByRoomIdIn(roomIds);
		roomMemberRepository.deleteByRoomIdIn(roomIds);
		roomRepository.deleteByServerId(serverId);
		serverMemberRepository.deleteByServerId(serverId);
		inviteRepository.deleteByServerId(serverId);
		auditLogRepository.deleteByServerId(serverId);
		serverRepository.delete(server);
		for (String memberId : memberIds) {
			roomIds.forEach(roomId -> unreadService.reset(memberId, roomId));
		}
		realtimeService.toUsers(memberIds, ChatEvent.of(ChatEventType.ROOM_UPDATED, null,
				Map.of("action", "server-deleted", "serverId", serverId)));
	}

	@Override
	public ServerResponse join(String serverId, String userId) {
		Server server = accessGuard.requireServer(serverId);
		if (!server.isDiscoverable()) {
			throw new ForbiddenException("This server is invite only", ErrorCode.ACCESS_DENIED);
		}
		return addMember(server, userId);
	}

	@Override
	public ServerResponse addMember(Server server, String userId) {
		ServerMember existing = serverMemberRepository.findByServerIdAndUserId(server.getId(), userId).orElse(null);
		if (existing != null) {
			return toResponse(server, existing, serverMemberRepository.countByServerId(server.getId()), 0);
		}
		User user = userService.requireUser(userId);
		Instant now = Instant.now();
		ServerMember member = serverMemberRepository.save(ServerMember.builder()
				.serverId(server.getId()).userId(userId).role(MemberRole.MEMBER).joinedAt(now).build());
		List<Room> channels = roomRepository.findByServerIdOrderByPositionAsc(server.getId());
		for (Room channel : channels) {
			if (!roomMemberRepository.existsByRoomIdAndUserId(channel.getId(), userId)) {
				roomMemberRepository.save(RoomMember.builder().roomId(channel.getId()).userId(userId)
						.joinedAt(now).lastReadAt(now).build());
			}
		}
		auditLogService.log(server.getId(), userId, AuditAction.MEMBER_JOINED, userId, user.getDisplayName());
		if (!channels.isEmpty()) {
			chatService.systemMessage(channels.get(0), user.getDisplayName() + " joined the server. Say hi!");
		}
		if (!server.getOwnerId().equals(userId)) {
			notificationService.notify(server.getOwnerId(), NotificationType.SERVER_JOINED, "New member in " + server.getName(),
					user.getDisplayName() + " joined your server", "/app/servers/" + server.getId(), userId);
		}
		broadcastToMembers(server.getId(), "members");
		return toResponse(server, member, serverMemberRepository.countByServerId(server.getId()), 0);
	}

	@Override
	public void leave(String serverId, String userId) {
		Server server = accessGuard.requireServer(serverId);
		ServerMember me = accessGuard.requireServerMember(serverId, userId);
		if (me.getRole() == MemberRole.OWNER) {
			throw new BadRequestException(ErrorCode.OWNER_CANNOT_LEAVE);
		}
		removeFromServer(server, me);
		auditLogService.log(serverId, userId, AuditAction.MEMBER_LEFT, userId, null);
	}

	@Override
	public void kick(String serverId, String targetUserId, String userId) {
		Server server = accessGuard.requireServer(serverId);
		ServerMember me = accessGuard.requireServerRole(serverId, userId, MemberRole.ADMIN);
		ServerMember target = serverMemberRepository.findByServerIdAndUserId(serverId, targetUserId)
				.orElseThrow(() -> new BadRequestException("User is not in this server", ErrorCode.NOT_SERVER_MEMBER));
		if (targetUserId.equals(userId) || target.getRole().atLeast(me.getRole())) {
			throw new ForbiddenException("You can only kick members below your role", ErrorCode.ACCESS_DENIED);
		}
		removeFromServer(server, target);
		auditLogService.log(serverId, userId, AuditAction.MEMBER_KICKED, targetUserId,
				userService.requireUser(targetUserId).getDisplayName());
		realtimeService.toUser(targetUserId, ChatEvent.of(ChatEventType.ROOM_UPDATED, null,
				Map.of("action", "kicked", "serverId", serverId)));
	}

	@Override
	public ServerMemberResponse updateRole(String serverId, String targetUserId, MemberRole role, String userId) {
		accessGuard.requireServer(serverId);
		accessGuard.requireServerRole(serverId, userId, MemberRole.OWNER);
		if (role == MemberRole.OWNER) {
			throw new BadRequestException("Ownership transfer is not supported", ErrorCode.BAD_REQUEST);
		}
		ServerMember target = serverMemberRepository.findByServerIdAndUserId(serverId, targetUserId)
				.orElseThrow(() -> new BadRequestException("User is not in this server", ErrorCode.NOT_SERVER_MEMBER));
		if (target.getRole() == MemberRole.OWNER) {
			throw new ForbiddenException("The owner's role cannot be changed", ErrorCode.ACCESS_DENIED);
		}
		target.setRole(role);
		serverMemberRepository.save(target);
		auditLogService.log(serverId, userId, AuditAction.ROLE_CHANGED, targetUserId, role.name());
		broadcastToMembers(serverId, "members");
		return toMemberResponse(target, userMapper.mapByIds(List.of(targetUserId)));
	}

	private void removeFromServer(Server server, ServerMember member) {
		serverMemberRepository.delete(member);
		List<Room> channels = roomRepository.findByServerIdOrderByPositionAsc(server.getId());
		for (Room channel : channels) {
			roomMemberRepository.findByRoomIdAndUserId(channel.getId(), member.getUserId()).ifPresent(roomMemberRepository::delete);
			unreadService.reset(member.getUserId(), channel.getId());
		}
		broadcastToMembers(server.getId(), "members");
	}

	private Map<String, Long> unreadByServer(String userId) {
		List<RoomMember> memberships = roomMemberRepository.findByUserId(userId);
		Map<String, String> roomToServer = new HashMap<>();
		roomRepository.findAllById(memberships.stream().map(RoomMember::getRoomId).toList()).forEach(r -> {
			if (r.getServerId() != null) {
				roomToServer.put(r.getId(), r.getServerId());
			}
		});
		List<RoomMember> channelMemberships = memberships.stream().filter(m -> roomToServer.containsKey(m.getRoomId())).toList();
		Map<String, Long> result = new HashMap<>();
		unreadService.counts(userId, channelMemberships)
				.forEach((roomId, count) -> result.merge(roomToServer.get(roomId), count, Long::sum));
		return result;
	}

	private void broadcastToMembers(String serverId, String action) {
		List<String> ids = serverMemberRepository.findByServerId(serverId).stream().map(ServerMember::getUserId).toList();
		realtimeService.toUsers(ids, ChatEvent.of(ChatEventType.ROOM_UPDATED, null, Map.of("action", action, "serverId", serverId)));
	}

	private ServerMemberResponse toMemberResponse(ServerMember m, Map<String, UserResponse> users) {
		return ServerMemberResponse.builder()
				.user(users.getOrDefault(m.getUserId(), userMapper.unknown(m.getUserId())))
				.role(m.getRole()).nickname(m.getNickname()).joinedAt(m.getJoinedAt()).build();
	}

	private ServerResponse toResponse(Server s, ServerMember me, long memberCount, long unread) {
		return ServerResponse.builder()
				.id(s.getId()).name(s.getName()).description(s.getDescription()).iconColor(s.getIconColor())
				.ownerId(s.getOwnerId()).discoverable(s.isDiscoverable()).memberCount(memberCount)
				.myRole(me == null ? null : me.getRole()).unreadCount(unread).createdAt(s.getCreatedAt())
				.build();
	}
}
