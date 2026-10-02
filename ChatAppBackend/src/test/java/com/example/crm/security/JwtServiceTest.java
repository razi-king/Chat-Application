package com.example.crm.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import com.example.crm.entity.User;
import com.example.crm.enums.ErrorCode;
import com.example.crm.exception.ChatAppException;
import com.example.crm.exception.UnauthorizedException;

import io.jsonwebtoken.Claims;

class JwtServiceTest {
	private static final String SECRET = "test-secret-that-is-at-least-32-bytes-long!!";
	private final User user = User.builder().id("u1").username("razi").build();

	@Test
	void generatedToken_parsesBackToSameUser() {
		JwtService jwt = new JwtService(SECRET, 1);
		Claims claims = jwt.parse(jwt.generateToken(user));

		AuthUser authUser = jwt.toAuthUser(claims);
		assertThat(authUser.id()).isEqualTo("u1");
		assertThat(authUser.username()).isEqualTo("razi");
		// Every Token Has A Unique Id So Logout Can Blacklist Exactly That Token
		assertThat(claims.getId()).isNotBlank();
	}

	// Protects: A Token Signed With A Different Key (Forged) Is Rejected
	@Test
	void tokenFromAnotherSecret_rejected() {
		String forged = new JwtService("another-secret-that-is-also-32-bytes-long!!", 1).generateToken(user);

		assertThatThrownBy(() -> new JwtService(SECRET, 1).parse(forged))
				.isInstanceOf(UnauthorizedException.class)
				.extracting(ex -> ((ChatAppException) ex).getErrorCode()).isEqualTo(ErrorCode.UNAUTHORIZED);
	}

	// Protects: Expired Tokens Get Their Own Code So The UI Can Say "Session Expired" Instead Of "Invalid"
	@Test
	void expiredToken_tokenExpiredCode() {
		JwtService alreadyExpired = new JwtService(SECRET, -1);
		String token = alreadyExpired.generateToken(user);

		assertThatThrownBy(() -> alreadyExpired.parse(token))
				.isInstanceOf(UnauthorizedException.class)
				.extracting(ex -> ((ChatAppException) ex).getErrorCode()).isEqualTo(ErrorCode.TOKEN_EXPIRED);
	}

	@Test
	void garbage_rejected() {
		assertThatThrownBy(() -> new JwtService(SECRET, 1).parse("not.a.jwt")).isInstanceOf(UnauthorizedException.class);
	}
}
