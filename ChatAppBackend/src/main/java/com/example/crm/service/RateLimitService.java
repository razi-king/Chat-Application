package com.example.crm.service;

public interface RateLimitService {

	/**
	 * @throws com.example.crm.exception.RateLimitException when the user did the action
	 *         more than {@code limit} times inside {@code windowSeconds}
	 */
	void check(String userId, String action, int limit, int windowSeconds);

	/**
	 * Read-only half of {@link #check}: throws when the limit is already reached but does not count.
	 * Paired with {@link #record} so only actions that really happened are counted
	 * (used for messages, where idempotent retries must not consume quota).
	 */
	void ensureBelowLimit(String userId, String action, int limit, int windowSeconds);

	void record(String userId, String action, int windowSeconds);
}
