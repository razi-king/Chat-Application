package com.example.crm.exception;

import com.example.crm.enums.ErrorCode;

public class ResourceNotFoundException extends RuntimeException{
	private String message;
	private ErrorCode errorCode;
	public ResourceNotFoundException(String message, ErrorCode errorCode) {
		this.message = message;
		this.errorCode = errorCode;
	}
	public String getMessage() {
		return message;
	}
	public void setMessage(String message) {
		this.message = message;
	}
	public ErrorCode getErrorCode() {
		return errorCode;
	}
	public void setErrorCode(ErrorCode errorCode) {
		this.errorCode = errorCode;
	}
	
}
