package com.example.crm.config;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * Gives every HTTP request a correlation id. It is put into the logging MDC (so EVERY log line of
 * that request carries it), returned in the X-Request-Id header and included in error responses.
 * A user reporting "it failed, requestId=ab12..." lets us grep the exact log lines of that request.
 *
 * Runs before Spring Security, so even 401 responses carry the id.
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestIdFilter extends OncePerRequestFilter {
	public static final String HEADER = "X-Request-Id";
	public static final String MDC_KEY = "requestId";
	// Only Accept Short, Safe Ids From Clients -> No Log Injection Through The Header
	private static final Pattern SAFE_ID = Pattern.compile("^[a-zA-Z0-9-]{8,64}$");

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		String incoming = request.getHeader(HEADER);
		String requestId = incoming != null && SAFE_ID.matcher(incoming).matches()
				? incoming
				: UUID.randomUUID().toString().replace("-", "").substring(0, 16);
		long start = System.nanoTime();
		MDC.put(MDC_KEY, requestId);
		response.setHeader(HEADER, requestId);
		try {
			chain.doFilter(request, response);
		} finally {
			long tookMs = (System.nanoTime() - start) / 1_000_000;
			// Path Only (No Query String, No Headers) -> Tokens And Personal Data Never Reach The Logs
			if (request.getRequestURI().startsWith("/api/")) {
				log.info("{} {} -> {} ({} ms)", request.getMethod(), request.getRequestURI(), response.getStatus(), tookMs);
			}
			MDC.remove(MDC_KEY);
		}
	}
}
