package com.example.crm.enums;

import org.springframework.http.HttpStatus;

/**
 * Every error the API can return. The code travels to the frontend in Response.errorCode,
 * the HttpStatus decides the response status, and the message is the default text.
 */
public enum ErrorCode {
	// Generic
	VALIDATION_FAILED("VAL_400", HttpStatus.BAD_REQUEST, "Validation failed"),
	BAD_REQUEST("REQ_400", HttpStatus.BAD_REQUEST, "Bad request"),
	INTERNAL_ERROR("SYS_500", HttpStatus.INTERNAL_SERVER_ERROR, "Something went wrong"),
	RATE_LIMITED("RATE_429", HttpStatus.TOO_MANY_REQUESTS, "You are doing that too fast"),
	CONFLICT("REQ_409", HttpStatus.CONFLICT, "This was already done"),
	DATABASE_UNAVAILABLE("DB_503", HttpStatus.SERVICE_UNAVAILABLE, "Service temporarily unavailable, please retry"),

	// Auth
	UNAUTHORIZED("AUTH_401", HttpStatus.UNAUTHORIZED, "Please login to continue"),
	INVALID_CREDENTIALS("AUTH_402", HttpStatus.UNAUTHORIZED, "Invalid username or password"),
	TOKEN_EXPIRED("AUTH_403", HttpStatus.UNAUTHORIZED, "Session expired, please login again"),
	ACCESS_DENIED("AUTH_404", HttpStatus.FORBIDDEN, "You do not have permission to do this"),

	// User
	USER_NOT_FOUND("USER_404", HttpStatus.NOT_FOUND, "User not found"),
	USERNAME_TAKEN("USER_409", HttpStatus.CONFLICT, "Username is already taken"),
	EMAIL_TAKEN("USER_410", HttpStatus.CONFLICT, "Email is already registered"),

	// Room
	ROOM_NOT_FOUND("ROOM_404", HttpStatus.NOT_FOUND, "Room not found"),
	ROOM_IS_ALREADY_CREATED("ROOM_409", HttpStatus.CONFLICT, "Room already exists"),
	NOT_ROOM_MEMBER("ROOM_403", HttpStatus.FORBIDDEN, "You are not a member of this room"),

	// Server (Discord style community)
	SERVER_NOT_FOUND("SRV_404", HttpStatus.NOT_FOUND, "Server not found"),
	NOT_SERVER_MEMBER("SRV_403", HttpStatus.FORBIDDEN, "You are not a member of this server"),
	ALREADY_SERVER_MEMBER("SRV_409", HttpStatus.CONFLICT, "You are already a member of this server"),
	OWNER_CANNOT_LEAVE("SRV_410", HttpStatus.BAD_REQUEST, "Owner cannot leave, delete the server instead"),

	// Invite
	INVITE_NOT_FOUND("INV_404", HttpStatus.NOT_FOUND, "Invite not found"),
	INVITE_EXPIRED("INV_410", HttpStatus.GONE, "This invite has expired"),

	// Message
	MESSAGE_NOT_FOUND("MSG_404", HttpStatus.NOT_FOUND, "Message not found"),
	MESSAGE_NOT_OWNED("MSG_403", HttpStatus.FORBIDDEN, "You can only change your own messages"),

	// Friends
	FRIEND_REQUEST_EXISTS("FRD_409", HttpStatus.CONFLICT, "Friend request already exists"),
	FRIEND_REQUEST_NOT_FOUND("FRD_404", HttpStatus.NOT_FOUND, "Friend request not found"),
	CANNOT_FRIEND_SELF("FRD_400", HttpStatus.BAD_REQUEST, "You cannot add yourself"),

	// Notification
	NOTIFICATION_NOT_FOUND("NTF_404", HttpStatus.NOT_FOUND, "Notification not found");

	private final String code;
	private final HttpStatus status;
	private final String defaultMessage;

	ErrorCode(String code, HttpStatus status, String defaultMessage) {
		this.code = code;
		this.status = status;
		this.defaultMessage = defaultMessage;
	}

	public String getCode() {
		return code;
	}

	public HttpStatus getStatus() {
		return status;
	}

	public String getDefaultMessage() {
		return defaultMessage;
	}
}
