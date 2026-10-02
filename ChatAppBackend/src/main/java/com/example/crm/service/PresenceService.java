package com.example.crm.service;

import java.util.Collection;
import java.util.Set;

public interface PresenceService {

	void connected(String userId);

	void disconnected(String userId);

	boolean isOnline(String userId);

	Set<String> onlineOf(Collection<String> userIds);
}
