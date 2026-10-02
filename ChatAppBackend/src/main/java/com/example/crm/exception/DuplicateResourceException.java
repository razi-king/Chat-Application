package com.example.crm.exception;

import com.example.crm.enums.ErrorCode;

public class DuplicateResourceException extends ChatAppException {
	public DuplicateResourceException(ErrorCode errorCode) {
		super(errorCode);
	}

	public DuplicateResourceException(String message, ErrorCode errorCode) {
		super(errorCode, message);
	}

	// meta Example: {"username": "Username is already taken"} -> Shown Under That Form Field
	public DuplicateResourceException(String message, ErrorCode errorCode, Object meta) {
		super(errorCode, message, meta);
	}
}
