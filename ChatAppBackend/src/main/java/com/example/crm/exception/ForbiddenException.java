package com.example.crm.exception;

import com.example.crm.enums.ErrorCode;

public class ForbiddenException extends ChatAppException {
	public ForbiddenException(ErrorCode errorCode) {
		super(errorCode);
	}

	public ForbiddenException(String message, ErrorCode errorCode) {
		super(errorCode, message);
	}
}
