package com.example.crm.security;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.example.crm.entity.User;
import com.example.crm.enums.ErrorCode;
import com.example.crm.exception.UnauthorizedException;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {
	private final SecretKey key;
	private final Duration expiration;

	public JwtService(@Value("${app.jwt.secret}") String secret,
			@Value("${app.jwt.expiration-hours:72}") long expirationHours) {
		this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
		this.expiration = Duration.ofHours(expirationHours);
	}

	public String generateToken(User user) {
		Instant now = Instant.now();
		return Jwts.builder()
				.id(UUID.randomUUID().toString())
				.subject(user.getId())
				.claim("username", user.getUsername())
				.issuedAt(Date.from(now))
				.expiration(Date.from(now.plus(expiration)))
				.signWith(key)
				.compact();
	}

	public Instant expiresAt(String token) {
		return parse(token).getExpiration().toInstant();
	}

	/**
	 * @throws UnauthorizedException with TOKEN_EXPIRED or UNAUTHORIZED when the token is not usable
	 */
	public Claims parse(String token) {
		try {
			return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
		} catch (ExpiredJwtException e) {
			throw new UnauthorizedException(ErrorCode.TOKEN_EXPIRED);
		} catch (JwtException | IllegalArgumentException e) {
			throw new UnauthorizedException("Invalid token", ErrorCode.UNAUTHORIZED);
		}
	}

	public AuthUser toAuthUser(Claims claims) {
		return new AuthUser(claims.getSubject(), claims.get("username", String.class));
	}
}
