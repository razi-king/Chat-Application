package com.example.crm.service;

/**
 * Brute-force protection for login: after too many WRONG passwords in a row the account is locked
 * for a while, even if the attacker slows down. A successful login clears the counter.
 */
public interface LoginAttemptService {

	/**
	 * @throws com.example.crm.exception.RateLimitException (ACCOUNT_LOCKED) while the account is locked
	 */
	void ensureNotLocked(String accountKey);

	/** Counts one failed attempt; locks the account when the limit is reached. */
	void recordFailure(String accountKey);

	/** Correct password: forget the failed attempts. */
	void recordSuccess(String accountKey);
}
