package com.example.crm.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.example.crm.dto.request.LoginRequest;
import com.example.crm.dto.request.RegisterRequest;
import com.example.crm.dto.response.AuthResponse;
import com.example.crm.dto.response.UserResponse;
import com.example.crm.entity.User;
import com.example.crm.enums.ErrorCode;
import com.example.crm.exception.ChatAppException;
import com.example.crm.exception.DuplicateResourceException;
import com.example.crm.exception.RateLimitException;
import com.example.crm.exception.UnauthorizedException;
import com.example.crm.mapper.UserMapper;
import com.example.crm.repository.UserRepository;
import com.example.crm.security.JwtService;
import com.example.crm.service.RateLimitService;
import com.example.crm.service.TokenBlacklistService;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {
	@Mock
	private UserRepository userRepository;
	@Mock
	private PasswordEncoder passwordEncoder;
	@Mock
	private JwtService jwtService;
	@Mock
	private TokenBlacklistService tokenBlacklistService;
	@Mock
	private RateLimitService rateLimitService;
	@Mock
	private UserMapper userMapper;
	@InjectMocks
	private AuthServiceImpl authService;

	private RegisterRequest register;

	@BeforeEach
	void setUp() {
		register = new RegisterRequest();
		register.setUsername("  Razi_K ");
		register.setEmail("RAZI@Example.com");
		register.setPassword("secret123");
		register.setDisplayName("Razi Khan");
	}

	private void stubTokenIssuing() {
		when(jwtService.generateToken(any())).thenReturn("jwt");
		when(jwtService.expiresAt("jwt")).thenReturn(Instant.now().plusSeconds(3600));
		when(userMapper.toSelf(any())).thenReturn(UserResponse.builder().id("u1").build());
	}

	// Protects: Passwords Are Never Stored In Plaintext And Usernames Are Normalised (No "Razi" vs "razi" Duplicates)
	@Test
	void register_valid_hashesPasswordAndNormalisesIdentity() {
		when(userRepository.existsByUsername("razi_k")).thenReturn(false);
		when(userRepository.existsByEmail("razi@example.com")).thenReturn(false);
		when(passwordEncoder.encode("secret123")).thenReturn("$2a$hash");
		when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
		stubTokenIssuing();

		AuthResponse response = authService.register(register);

		ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
		verify(userRepository).save(saved.capture());
		assertThat(saved.getValue().getUsername()).isEqualTo("razi_k");
		assertThat(saved.getValue().getEmail()).isEqualTo("razi@example.com");
		assertThat(saved.getValue().getPasswordHash()).isEqualTo("$2a$hash").isNotEqualTo("secret123");
		assertThat(response.getToken()).isEqualTo("jwt");
	}

	// Protects: Duplicate Registration Returns 409 With The Field Name So The Form Can Highlight It
	@Test
	void register_duplicateUsername_conflictWithFieldMeta() {
		when(userRepository.existsByUsername("razi_k")).thenReturn(true);

		assertThatThrownBy(() -> authService.register(register))
				.isInstanceOf(DuplicateResourceException.class)
				.satisfies(ex -> {
					ChatAppException e = (ChatAppException) ex;
					assertThat(e.getErrorCode()).isEqualTo(ErrorCode.USERNAME_TAKEN);
					assertThat(((Map<?, ?>) e.getMeta()).containsKey("username")).isTrue();
				});
		verify(userRepository, never()).save(any());
	}

	@Test
	void register_duplicateEmail_conflict() {
		when(userRepository.existsByUsername("razi_k")).thenReturn(false);
		when(userRepository.existsByEmail("razi@example.com")).thenReturn(true);

		assertThatThrownBy(() -> authService.register(register))
				.isInstanceOf(DuplicateResourceException.class)
				.extracting(ex -> ((ChatAppException) ex).getErrorCode())
				.isEqualTo(ErrorCode.EMAIL_TAKEN);
	}

	@Test
	void login_validByEmail_returnsToken() {
		User user = User.builder().id("u1").username("razi").email("razi@nexus.dev").passwordHash("$2a$hash").build();
		when(userRepository.findByEmail("razi@nexus.dev")).thenReturn(Optional.of(user));
		when(passwordEncoder.matches("secret123", "$2a$hash")).thenReturn(true);
		stubTokenIssuing();

		LoginRequest login = new LoginRequest();
		login.setIdentifier("Razi@Nexus.dev");
		login.setPassword("secret123");

		assertThat(authService.login(login).getToken()).isEqualTo("jwt");
	}

	// Protects: Wrong Password And Unknown User Give The SAME Error -> Attackers Cannot Discover Which Usernames Exist
	@Test
	void login_wrongPasswordAndUnknownUser_sameGenericError() {
		User user = User.builder().id("u1").username("razi").passwordHash("$2a$hash").build();
		when(userRepository.findByUsername("razi")).thenReturn(Optional.of(user));
		when(passwordEncoder.matches("bad", "$2a$hash")).thenReturn(false);
		when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

		LoginRequest wrongPassword = new LoginRequest();
		wrongPassword.setIdentifier("razi");
		wrongPassword.setPassword("bad");
		LoginRequest unknownUser = new LoginRequest();
		unknownUser.setIdentifier("ghost");
		unknownUser.setPassword("bad");

		assertThatThrownBy(() -> authService.login(wrongPassword)).isInstanceOf(UnauthorizedException.class)
				.extracting(ex -> ((ChatAppException) ex).getErrorCode()).isEqualTo(ErrorCode.INVALID_CREDENTIALS);
		assertThatThrownBy(() -> authService.login(unknownUser)).isInstanceOf(UnauthorizedException.class)
				.extracting(ex -> ((ChatAppException) ex).getErrorCode()).isEqualTo(ErrorCode.INVALID_CREDENTIALS);
		verify(jwtService, never()).generateToken(any());
	}

	// Protects: Brute Force -> The Limiter Runs BEFORE The Database / BCrypt Work
	@Test
	void login_rateLimited_rejectedBeforePasswordCheck() {
		org.mockito.Mockito.doThrow(new RateLimitException(ErrorCode.RATE_LIMITED))
				.when(rateLimitService).check(eq("razi"), eq("login"), anyInt(), anyInt());
		LoginRequest login = new LoginRequest();
		login.setIdentifier("razi");
		login.setPassword("x");

		assertThatThrownBy(() -> authService.login(login)).isInstanceOf(RateLimitException.class);
		verify(userRepository, never()).findByUsername(anyString());
		verify(passwordEncoder, never()).matches(anyString(), anyString());
	}
}
