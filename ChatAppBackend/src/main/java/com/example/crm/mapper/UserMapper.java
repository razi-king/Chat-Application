package com.example.crm.mapper;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.example.crm.dto.response.UserResponse;
import com.example.crm.entity.User;
import com.example.crm.repository.UserRepository;
import com.example.crm.service.PresenceService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class UserMapper {
	private final UserRepository userRepository;
	private final PresenceService presenceService;

	public UserResponse toResponse(User user) {
		return toResponse(user, presenceService.isOnline(user.getId()));
	}

	/** Same as toResponse but includes private fields (email) - only for the logged in user. */
	public UserResponse toSelf(User user) {
		UserResponse response = toResponse(user, true);
		response.setEmail(user.getEmail());
		return response;
	}

	/** Loads many users in ONE query and marks who is online with ONE Redis call. */
	public Map<String, UserResponse> mapByIds(Collection<String> ids) {
		Set<String> unique = new HashSet<>(ids);
		unique.remove(null);
		Map<String, UserResponse> result = new HashMap<>();
		if (unique.isEmpty()) {
			return result;
		}
		List<User> users = userRepository.findAllById(unique);
		Set<String> online = presenceService.onlineOf(unique);
		users.forEach(u -> result.put(u.getId(), toResponse(u, online.contains(u.getId()))));
		return result;
	}

	public UserResponse unknown(String id) {
		return UserResponse.builder().id(id).username("deleted").displayName("Deleted User").avatarColor("#64748b").build();
	}

	private UserResponse toResponse(User user, boolean online) {
		return UserResponse.builder()
				.id(user.getId())
				.username(user.getUsername())
				.displayName(user.getDisplayName())
				.avatarColor(user.getAvatarColor())
				.about(user.getAbout())
				.customStatus(user.getCustomStatus())
				.online(online)
				.lastSeenAt(user.getLastSeenAt())
				.createdAt(user.getCreatedAt())
				.build();
	}
}
