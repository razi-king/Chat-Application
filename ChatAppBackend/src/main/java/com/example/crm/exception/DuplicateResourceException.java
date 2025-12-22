package com.example.crm.exception;

import com.example.crm.enums.ErrorCode;

public class DuplicateResourceException extends RuntimeException {
	private ErrorCode errorCode;
	private String message;
	
	public DuplicateResourceException(String message, ErrorCode errorCode) {
		this.message = message;
		this.errorCode = errorCode;
	}
	// Getter And Setters
	public ErrorCode getErrorCode() {
		return errorCode;
	}
	public void setErrorCode(ErrorCode errorCode) {
		this.errorCode = errorCode;
	}
	public String getMessage() {
		return message;
	}
	public void setMessage(String message) {
		this.message = message;
	}
	
}
