package com.example.crm.security;

import java.io.IOException;
import java.util.List;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.example.crm.exception.ChatAppException;
import com.example.crm.enums.ErrorCode;
import com.example.crm.service.TokenBlacklistService;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
	public static final String AUTH_ERROR_ATTRIBUTE = "auth.errorCode";

	private final JwtService jwtService;
	private final TokenBlacklistService tokenBlacklistService;

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		String token = extractToken(request.getHeader("Authorization"));
		if (token != null) {
			try {
				Claims claims = jwtService.parse(token);
				if (tokenBlacklistService.isBlacklisted(claims.getId())) {
					request.setAttribute(AUTH_ERROR_ATTRIBUTE, ErrorCode.TOKEN_EXPIRED);
				} else {
					AuthUser user = jwtService.toAuthUser(claims);
					UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(user, token, List.of());
					SecurityContextHolder.getContext().setAuthentication(auth);
				}
			} catch (ChatAppException e) {
				// Let The Entry Point Explain Why (Expired vs Invalid)
				request.setAttribute(AUTH_ERROR_ATTRIBUTE, e.getErrorCode());
			}
		}
		chain.doFilter(request, response);
	}

	public static String extractToken(String header) {
		if (header != null && header.startsWith("Bearer ")) {
			return header.substring(7).trim();
		}
		return null;
	}
}
