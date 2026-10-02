package com.example.crm.mapper;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.example.crm.dto.response.LastMessageResponse;
import com.example.crm.dto.response.RoomMemberResponse;
import com.example.crm.dto.response.RoomResponse;
import com.example.crm.dto.response.UserResponse;
import com.example.crm.entity.Room;
import com.example.crm.entity.RoomMember;
import com.example.crm.enums.RoomType;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class RoomMapper {
	private final UserMapper userMapper;

	/**
	 * @param me      the viewer's membership (for myRole / muted), may be null
	 * @param members all members (only for DIRECT / GROUP), may be null for channels
	 * @param users   users already loaded for those members
	 */
	public RoomResponse toResponse(Room room, RoomMember me, long unread, List<RoomMember> members,
			Map<String, UserResponse> users) {
		RoomResponse.RoomResponseBuilder builder = RoomResponse.builder()
				.id(room.getId())
				.name(room.getName())
				.description(room.getDescription())
				.type(room.getType())
				.serverId(room.getServerId())
				.iconColor(room.getIconColor())
				.createdBy(room.getCreatedBy())
				.position(room.getPosition())
				.lastMessage(toLastMessage(room.getLastMessage()))
				.unreadCount(unread)
				.createdAt(room.getCreatedAt())
				.lastActivityAt(room.getLastActivityAt());
		if (me != null) {
			builder.myRole(me.getRole()).muted(me.isMuted());
		}
		if (members != null && users != null) {
			builder.members(members.stream()
					.map(m -> RoomMemberResponse.builder()
							.user(users.getOrDefault(m.getUserId(), userMapper.unknown(m.getUserId())))
							.role(m.getRole())
							.lastReadAt(m.getLastReadAt())
							.joinedAt(m.getJoinedAt())
							.build())
					.toList());
			if (room.getType() == RoomType.DIRECT && me != null) {
				members.stream().filter(m -> !m.getUserId().equals(me.getUserId())).findFirst()
						.ifPresent(other -> builder.otherUser(users.get(other.getUserId())));
			}
		}
		return builder.build();
	}

	private LastMessageResponse toLastMessage(Room.LastMessage lm) {
		if (lm == null) {
			return null;
		}
		return LastMessageResponse.builder()
				.messageId(lm.getMessageId())
				.senderId(lm.getSenderId())
				.senderName(lm.getSenderName())
				.preview(lm.getPreview())
				.sentAt(lm.getSentAt())
				.build();
	}
}
