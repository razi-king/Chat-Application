package com.example.crm.security;

import org.springframework.stereotype.Component;

import com.example.crm.entity.Room;
import com.example.crm.entity.RoomMember;
import com.example.crm.entity.Server;
import com.example.crm.entity.ServerMember;
import com.example.crm.enums.ErrorCode;
import com.example.crm.enums.MemberRole;
import com.example.crm.exception.ForbiddenException;
import com.example.crm.exception.ResourceNotFoundException;
import com.example.crm.repository.RoomMemberRepository;
import com.example.crm.repository.RoomRepository;
import com.example.crm.repository.ServerMemberRepository;
import com.example.crm.repository.ServerRepository;

import lombok.RequiredArgsConstructor;

/**
 * Central place for "is this user allowed?" checks. Every service calls these,
 * so permission rules live in one file instead of being copied everywhere.
 */
@Component
@RequiredArgsConstructor
public class AccessGuard {
	private final RoomRepository roomRepository;
	private final RoomMemberRepository roomMemberRepository;
	private final ServerRepository serverRepository;
	private final ServerMemberRepository serverMemberRepository;

	public Room requireRoom(String roomId) {
		return roomRepository.findById(roomId)
				.orElseThrow(() -> new ResourceNotFoundException("Room not found for id " + roomId, ErrorCode.ROOM_NOT_FOUND));
	}

	public RoomMember requireRoomMember(String roomId, String userId) {
		return roomMemberRepository.findByRoomIdAndUserId(roomId, userId)
				.orElseThrow(() -> new ForbiddenException(ErrorCode.NOT_ROOM_MEMBER));
	}

	public boolean isRoomMember(String roomId, String userId) {
		return roomMemberRepository.existsByRoomIdAndUserId(roomId, userId);
	}

	public Server requireServer(String serverId) {
		return serverRepository.findById(serverId)
				.orElseThrow(() -> new ResourceNotFoundException("Server not found for id " + serverId, ErrorCode.SERVER_NOT_FOUND));
	}

	public ServerMember requireServerMember(String serverId, String userId) {
		return serverMemberRepository.findByServerIdAndUserId(serverId, userId)
				.orElseThrow(() -> new ForbiddenException(ErrorCode.NOT_SERVER_MEMBER));
	}

	public ServerMember requireServerRole(String serverId, String userId, MemberRole minimum) {
		ServerMember member = requireServerMember(serverId, userId);
		if (!member.getRole().atLeast(minimum)) {
			throw new ForbiddenException("Only " + minimum.name().toLowerCase() + "s can do this", ErrorCode.ACCESS_DENIED);
		}
		return member;
	}

	/**
	 * Moderation rights inside a room: server ADMIN+ for channels, group ADMIN+ for groups.
	 */
	public boolean canModerate(Room room, String userId) {
		if (room.getServerId() != null) {
			return serverMemberRepository.findByServerIdAndUserId(room.getServerId(), userId)
					.map(m -> m.getRole().atLeast(MemberRole.ADMIN)).orElse(false);
		}
		return roomMemberRepository.findByRoomIdAndUserId(room.getId(), userId)
				.map(m -> m.getRole() != null && m.getRole().atLeast(MemberRole.ADMIN)).orElse(false);
	}
}
