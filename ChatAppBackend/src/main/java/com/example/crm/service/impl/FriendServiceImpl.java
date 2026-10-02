package com.example.crm.service.impl;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.example.crm.dto.response.FriendshipResponse;
import com.example.crm.dto.response.UserResponse;
import com.example.crm.entity.Friendship;
import com.example.crm.entity.User;
import com.example.crm.enums.ErrorCode;
import com.example.crm.enums.FriendshipStatus;
import com.example.crm.enums.NotificationType;
import com.example.crm.exception.BadRequestException;
import com.example.crm.exception.DuplicateResourceException;
import com.example.crm.exception.ForbiddenException;
import com.example.crm.exception.ResourceNotFoundException;
import com.example.crm.mapper.UserMapper;
import com.example.crm.repository.FriendshipRepository;
import com.example.crm.repository.UserRepository;
import com.example.crm.service.FriendService;
import com.example.crm.service.NotificationService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FriendServiceImpl implements FriendService {
	private final FriendshipRepository friendshipRepository;
	private final UserRepository userRepository;
	private final NotificationService notificationService;
	private final UserMapper userMapper;

	@Override
	public List<FriendshipResponse> list(String userId) {
		List<Friendship> relations = friendshipRepository.findByRequesterIdOrAddresseeId(userId, userId);
		Map<String, UserResponse> users = userMapper.mapByIds(relations.stream().map(f -> other(f, userId)).toList());
		return relations.stream().map(f -> toResponse(f, userId, users)).toList();
	}

	@Override
	public FriendshipResponse sendRequest(String username, String userId) {
		User target = userRepository.findByUsername(username.trim().toLowerCase(Locale.ROOT))
				.orElseThrow(() -> new ResourceNotFoundException("No user with username " + username, ErrorCode.USER_NOT_FOUND));
		if (target.getId().equals(userId)) {
			throw new BadRequestException(ErrorCode.CANNOT_FRIEND_SELF);
		}
		Friendship existing = friendshipRepository.findByPairKey(pairKey(userId, target.getId())).orElse(null);
		if (existing != null) {
			if (existing.getStatus() == FriendshipStatus.ACCEPTED) {
				throw new DuplicateResourceException("You are already friends", ErrorCode.FRIEND_REQUEST_EXISTS);
			}
			// They Already Asked Me -> Sending Back Means "Accept"
			if (existing.getRequesterId().equals(target.getId())) {
				return accept(existing.getId(), userId);
			}
			throw new DuplicateResourceException("Request already sent", ErrorCode.FRIEND_REQUEST_EXISTS);
		}
		Friendship saved = friendshipRepository.save(Friendship.builder()
				.requesterId(userId).addresseeId(target.getId()).pairKey(pairKey(userId, target.getId()))
				.status(FriendshipStatus.PENDING).createdAt(Instant.now()).build());
		User me = userRepository.findById(userId).orElseThrow();
		notificationService.notify(target.getId(), NotificationType.FRIEND_REQUEST, "New friend request",
				me.getDisplayName() + " (@" + me.getUsername() + ") wants to be your friend", "/app/friends", userId);
		return toResponse(saved, userId, userMapper.mapByIds(List.of(target.getId())));
	}

	@Override
	public FriendshipResponse accept(String friendshipId, String userId) {
		Friendship f = require(friendshipId);
		if (!f.getAddresseeId().equals(userId) || f.getStatus() != FriendshipStatus.PENDING) {
			throw new ForbiddenException("You cannot accept this request", ErrorCode.ACCESS_DENIED);
		}
		f.setStatus(FriendshipStatus.ACCEPTED);
		f.setRespondedAt(Instant.now());
		friendshipRepository.save(f);
		User me = userRepository.findById(userId).orElseThrow();
		notificationService.notify(f.getRequesterId(), NotificationType.FRIEND_ACCEPTED, "Friend request accepted",
				me.getDisplayName() + " accepted your friend request", "/app/friends", userId);
		return toResponse(f, userId, userMapper.mapByIds(List.of(f.getRequesterId())));
	}

	@Override
	public void remove(String friendshipId, String userId) {
		Friendship f = require(friendshipId);
		if (!f.getRequesterId().equals(userId) && !f.getAddresseeId().equals(userId)) {
			throw new ForbiddenException(ErrorCode.ACCESS_DENIED);
		}
		friendshipRepository.delete(f);
	}

	private Friendship require(String id) {
		return friendshipRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException(ErrorCode.FRIEND_REQUEST_NOT_FOUND));
	}

	private FriendshipResponse toResponse(Friendship f, String userId, Map<String, UserResponse> users) {
		String otherId = other(f, userId);
		return FriendshipResponse.builder()
				.id(f.getId())
				.user(users.getOrDefault(otherId, userMapper.unknown(otherId)))
				.status(f.getStatus())
				.incoming(f.getStatus() == FriendshipStatus.PENDING && f.getAddresseeId().equals(userId))
				.createdAt(f.getCreatedAt())
				.build();
	}

	private static String other(Friendship f, String userId) {
		return f.getRequesterId().equals(userId) ? f.getAddresseeId() : f.getRequesterId();
	}

	private static String pairKey(String a, String b) {
		return a.compareTo(b) < 0 ? a + ":" + b : b + ":" + a;
	}
}
