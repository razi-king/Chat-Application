package com.example.crm.service;

import java.util.Collection;
import java.util.Map;

import com.example.crm.entity.RoomMember;

public interface UnreadService {

	/** Adds one unread message for the user in the room and returns the new count. */
	long increment(RoomMember member);

	void reset(String userId, String roomId);

	/** Unread count per roomId for the given memberships of one user. */
	Map<String, Long> counts(String userId, Collection<RoomMember> memberships);
}
