package com.example.crm.enums;

public enum MemberRole {
	OWNER(3), ADMIN(2), MEMBER(1);

	private final int rank;

	MemberRole(int rank) {
		this.rank = rank;
	}

	public boolean atLeast(MemberRole other) {
		return this.rank >= other.rank;
	}
}
