package com.example.crm.service.impl;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.example.crm.dto.request.UpdateProfileRequest;
import com.example.crm.dto.response.DashboardResponse;
import com.example.crm.dto.response.UserResponse;
import com.example.crm.entity.Friendship;
import com.example.crm.entity.RoomMember;
import com.example.crm.entity.User;
import com.example.crm.enums.ErrorCode;
import com.example.crm.enums.FriendshipStatus;
import com.example.crm.enums.RoomType;
import com.example.crm.exception.ResourceNotFoundException;
import com.example.crm.mapper.UserMapper;
import com.example.crm.repository.FriendshipRepository;
import com.example.crm.repository.NotificationRepository;
import com.example.crm.repository.RoomMemberRepository;
import com.example.crm.repository.RoomRepository;
import com.example.crm.repository.ServerMemberRepository;
import com.example.crm.repository.UserRepository;
import com.example.crm.service.PresenceService;
import com.example.crm.service.UnreadService;
import com.example.crm.service.UserService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
	private final UserRepository userRepository;
	private final FriendshipRepository friendshipRepository;
	private final ServerMemberRepository serverMemberRepository;
	private final RoomMemberRepository roomMemberRepository;
	private final RoomRepository roomRepository;
	private final NotificationRepository notificationRepository;
	private final UnreadService unreadService;
	private final PresenceService presenceService;
	private final UserMapper userMapper;

	@Override
	public User requireUser(String userId) {
		return userRepository.findById(userId)
				.orElseThrow(() -> new ResourceNotFoundException("User not found for id " + userId, ErrorCode.USER_NOT_FOUND));
	}

	@Override
	public UserResponse getMe(String userId) {
		return userMapper.toSelf(requireUser(userId));
	}

	@Override
	public UserResponse updateMe(String userId, UpdateProfileRequest request) {
		User user = requireUser(userId);
		if (request.getDisplayName() != null) {
			user.setDisplayName(request.getDisplayName().trim());
		}
		if (request.getAbout() != null) {
			user.setAbout(request.getAbout().trim());
		}
		if (request.getCustomStatus() != null) {
			user.setCustomStatus(request.getCustomStatus().trim());
		}
		if (request.getAvatarColor() != null) {
			user.setAvatarColor(request.getAvatarColor());
		}
		return userMapper.toSelf(userRepository.save(user));
	}

	@Override
	public UserResponse getById(String userId) {
		return userMapper.toResponse(requireUser(userId));
	}

	@Override
	public List<UserResponse> search(String query, String meId) {
		if (query == null || query.isBlank()) {
			return List.of();
		}
		String q = query.trim();
		List<String> ids = userRepository.findTop20ByUsernameContainingIgnoreCaseOrDisplayNameContainingIgnoreCase(q, q)
				.stream().map(User::getId).filter(id -> !id.equals(meId)).toList();
		var users = userMapper.mapByIds(ids);
		return ids.stream().map(users::get).filter(u -> u != null).toList();
	}

	@Override
	public DashboardResponse dashboard(String userId) {
		List<Friendship> relations = friendshipRepository.findByRequesterIdOrAddresseeId(userId, userId);
		Set<String> friendIds = new HashSet<>();
		long pending = 0;
		for (Friendship f : relations) {
			if (f.getStatus() == FriendshipStatus.ACCEPTED) {
				friendIds.add(f.getRequesterId().equals(userId) ? f.getAddresseeId() : f.getRequesterId());
			} else if (f.getAddresseeId().equals(userId)) {
				pending++;
			}
		}
		List<RoomMember> memberships = roomMemberRepository.findByUserId(userId);
		long conversations = roomRepository.findAllById(memberships.stream().map(RoomMember::getRoomId).toList())
				.stream().filter(r -> r.getType() != RoomType.CHANNEL).count();
		long unread = unreadService.counts(userId, memberships).values().stream().mapToLong(Long::longValue).sum();
		return DashboardResponse.builder()
				.friends(friendIds.size())
				.pendingRequests(pending)
				.servers(serverMemberRepository.findByUserId(userId).size())
				.conversations(conversations)
				.unreadMessages(unread)
				.unreadNotifications(notificationRepository.countByUserIdAndReadFalse(userId))
				.onlineFriends(presenceService.onlineOf(friendIds).size())
				.build();
	}
}
