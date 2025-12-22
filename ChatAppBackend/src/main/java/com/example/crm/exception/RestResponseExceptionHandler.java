package com.example.crm.exception;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import com.example.crm.dto.Response;

import lombok.extern.slf4j.Slf4j;

@ControllerAdvice
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RestResponseExceptionHandler {
	@ExceptionHandler(DuplicateResourceException.class)
	public ResponseEntity<Object> handleDuplicateResource(DuplicateResourceException dr) {
		Response response = new Response();
		response.fail();
		response.setMessage(dr.getMessage());
		response.setErrorCode(dr.getErrorCode().toString());
		return buildResponseEntity(response, HttpStatus.BAD_REQUEST);
	}
	@ExceptionHandler(ResourceNotFoundException.class)
	public ResponseEntity<Object> handleResourceNotFound(ResourceNotFoundException rn) {
		Response response = new Response();
		response.fail();
		response.setMessage(rn.getMessage());
		response.setErrorCode(rn.getErrorCode().toString());
		return buildResponseEntity(response, HttpStatus.BAD_REQUEST);
	}
	private ResponseEntity<Object> buildResponseEntity(Response response, HttpStatus httpStatus) {
		return new ResponseEntity<>(response, httpStatus);
	}
}
