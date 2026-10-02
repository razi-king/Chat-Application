package com.example.crm.service.impl;

import java.util.Locale;
import java.util.Map;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.crm.dto.request.LoginRequest;
import com.example.crm.dto.request.RegisterRequest;
import com.example.crm.dto.response.AuthResponse;
import com.example.crm.entity.User;
import com.example.crm.enums.ErrorCode;
import com.example.crm.exception.DuplicateResourceException;
import com.example.crm.exception.UnauthorizedException;
import com.example.crm.mapper.UserMapper;
import com.example.crm.repository.UserRepository;
import com.example.crm.security.JwtService;
import com.example.crm.service.AuthService;
import com.example.crm.service.RateLimitService;
import com.example.crm.service.TokenBlacklistService;
import com.example.crm.util.Colors;

import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtService jwtService;
	private final TokenBlacklistService tokenBlacklistService;
	private final RateLimitService rateLimitService;
	private final UserMapper userMapper;

	@Override
	public AuthResponse register(RegisterRequest request) {
		String username = request.getUsername().trim().toLowerCase(Locale.ROOT);
		String email = request.getEmail().trim().toLowerCase(Locale.ROOT);
		if (userRepository.existsByUsername(username)) {
			String msg = ErrorCode.USERNAME_TAKEN.getDefaultMessage();
			throw new DuplicateResourceException(msg, ErrorCode.USERNAME_TAKEN, Map.of("username", msg));
		}
		if (userRepository.existsByEmail(email)) {
			String msg = ErrorCode.EMAIL_TAKEN.getDefaultMessage();
			throw new DuplicateResourceException(msg, ErrorCode.EMAIL_TAKEN, Map.of("email", msg));
		}
		User user = User.builder()
				.username(username)
				.email(email)
				.passwordHash(passwordEncoder.encode(request.getPassword()))
				.displayName(request.getDisplayName().trim())
				.avatarColor(Colors.random())
				.about("Hey there! I am using Nexus.")
				.build();
		return buildAuthResponse(userRepository.save(user));
	}

	@Override
	public AuthResponse login(LoginRequest request) {
		String identifier = request.getIdentifier().trim().toLowerCase(Locale.ROOT);
		// Max 10 Attempts Per Minute Per Account (Redis)
		rateLimitService.check(identifier, "login", 10, 60);
		User user = (identifier.contains("@") ? userRepository.findByEmail(identifier) : userRepository.findByUsername(identifier))
				.orElseThrow(() -> new UnauthorizedException(ErrorCode.INVALID_CREDENTIALS));
		if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
			throw new UnauthorizedException(ErrorCode.INVALID_CREDENTIALS);
		}
		return buildAuthResponse(user);
	}

	@Override
	public void logout(String token) {
		Claims claims = jwtService.parse(token);
		tokenBlacklistService.blacklist(claims.getId(), claims.getExpiration().toInstant());
	}

	private AuthResponse buildAuthResponse(User user) {
		String token = jwtService.generateToken(user);
		return AuthResponse.builder()
				.token(token)
				.expiresAt(jwtService.expiresAt(token))
				.user(userMapper.toSelf(user))
				.build();
	}
}
