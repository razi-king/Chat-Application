package com.example.crm.exception;

import com.example.crm.enums.ErrorCode;

public class BadRequestException extends ChatAppException {
	public BadRequestException(ErrorCode errorCode) {
		super(errorCode);
	}

	public BadRequestException(String message, ErrorCode errorCode) {
		super(errorCode, message);
	}
}
