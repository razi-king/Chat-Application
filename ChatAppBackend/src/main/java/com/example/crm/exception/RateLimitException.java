package com.example.crm.exception;

import com.example.crm.enums.ErrorCode;

public class RateLimitException extends ChatAppException {
	public RateLimitException(ErrorCode errorCode) {
		super(errorCode);
	}

	public RateLimitException(String message, ErrorCode errorCode) {
		super(errorCode, message);
	}
}
