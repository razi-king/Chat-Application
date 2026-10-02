package com.example.crm.exception;

import com.example.crm.enums.ErrorCode;

public class ResourceNotFoundException extends ChatAppException {
	public ResourceNotFoundException(ErrorCode errorCode) {
		super(errorCode);
	}

	public ResourceNotFoundException(String message, ErrorCode errorCode) {
		super(errorCode, message);
	}
}
