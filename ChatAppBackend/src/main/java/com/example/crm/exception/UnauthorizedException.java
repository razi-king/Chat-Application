package com.example.crm.exception;

import com.example.crm.enums.ErrorCode;

public class UnauthorizedException extends ChatAppException {
	public UnauthorizedException(ErrorCode errorCode) {
		super(errorCode);
	}

	public UnauthorizedException(String message, ErrorCode errorCode) {
		super(errorCode, message);
	}
}
