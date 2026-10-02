package com.example.crm.service;

import java.time.Instant;

public interface TokenBlacklistService {

	void blacklist(String tokenId, Instant expiresAt);

	boolean isBlacklisted(String tokenId);
}
