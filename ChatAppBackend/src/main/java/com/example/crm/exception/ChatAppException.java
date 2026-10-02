package com.example.crm.exception;

import com.example.crm.enums.ErrorCode;

/**
 * Base class of all our own exceptions. RestResponseExceptionHandler turns any of these
 * into a Response with the matching HTTP status and error code.
 */
public class ChatAppException extends RuntimeException {
	private final ErrorCode errorCode;
	private final transient Object meta;

	public ChatAppException(ErrorCode errorCode) {
		this(errorCode, errorCode.getDefaultMessage(), null);
	}

	public ChatAppException(ErrorCode errorCode, String message) {
		this(errorCode, message, null);
	}

	public ChatAppException(ErrorCode errorCode, String message, Object meta) {
		super(message);
		this.errorCode = errorCode;
		this.meta = meta;
	}

	public ErrorCode getErrorCode() {
		return errorCode;
	}

	public Object getMeta() {
		return meta;
	}
}
