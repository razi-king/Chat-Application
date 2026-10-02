package com.example.crm.exception;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.example.crm.dto.Response;
import com.example.crm.enums.ErrorCode;

import lombok.extern.slf4j.Slf4j;

@ControllerAdvice
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RestResponseExceptionHandler {

	// All Our Own Exceptions (NotFound, Duplicate, Forbidden, RateLimit ...)
	@ExceptionHandler(ChatAppException.class)
	public ResponseEntity<Response<Void>> handleChatAppException(ChatAppException ex) {
		log.debug("Handled {} -> {}", ex.getErrorCode(), ex.getMessage());
		return build(ex.getErrorCode(), ex.getMessage(), ex.getMeta());
	}

	// @Valid Failed -> Return Every Field Error In errorMeta So The Form Can Show It Under The Field
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<Response<Void>> handleValidation(MethodArgumentNotValidException ex) {
		Map<String, String> fieldErrors = new LinkedHashMap<>();
		ex.getBindingResult().getFieldErrors()
				.forEach(fe -> fieldErrors.putIfAbsent(fe.getField(), fe.getDefaultMessage()));
		return build(ErrorCode.VALIDATION_FAILED, "Please fix the highlighted fields", fieldErrors);
	}

	@ExceptionHandler({ HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class })
	public ResponseEntity<Response<Void>> handleBadBody(Exception ex) {
		return build(ErrorCode.BAD_REQUEST, "Malformed request", null);
	}

	@ExceptionHandler(HttpRequestMethodNotSupportedException.class)
	public ResponseEntity<Response<Void>> handleMethod(HttpRequestMethodNotSupportedException ex) {
		return build(ErrorCode.BAD_REQUEST, ex.getMessage(), null);
	}

	@ExceptionHandler(NoResourceFoundException.class)
	public ResponseEntity<Response<Void>> handleNoResource(NoResourceFoundException ex) {
		Response<Void> body = Response.error(ErrorCode.BAD_REQUEST, "Endpoint not found", null);
		body.setErrorCode("REQ_404");
		return ResponseEntity.status(404).body(body);
	}

	@ExceptionHandler(AccessDeniedException.class)
	public ResponseEntity<Response<Void>> handleAccessDenied(AccessDeniedException ex) {
		return build(ErrorCode.ACCESS_DENIED, ErrorCode.ACCESS_DENIED.getDefaultMessage(), null);
	}

	// Unique Index Violation That No Service Translated Itself (e.g. Two Identical Requests Racing)
	@ExceptionHandler(DuplicateKeyException.class)
	public ResponseEntity<Response<Void>> handleDuplicateKey(DuplicateKeyException ex) {
		log.warn("Duplicate key: {}", ex.getMostSpecificCause().getMessage());
		return build(ErrorCode.CONFLICT, ErrorCode.CONFLICT.getDefaultMessage(), null);
	}

	// MongoDB Down / Timed Out -> 503 Tells The Client "Retry Later", Unlike A 500 Which Means "Bug"
	@ExceptionHandler({ DataAccessResourceFailureException.class, TransientDataAccessException.class })
	public ResponseEntity<Response<Void>> handleDatabaseUnavailable(DataAccessException ex) {
		log.error("Database unavailable: {}", ex.getMostSpecificCause().getMessage());
		return build(ErrorCode.DATABASE_UNAVAILABLE, ErrorCode.DATABASE_UNAVAILABLE.getDefaultMessage(), null);
	}

	// Anything We Did Not Expect -> 500 Without Leaking Internals
	@ExceptionHandler(Exception.class)
	public ResponseEntity<Response<Void>> handleUnexpected(Exception ex) {
		log.error("Unexpected error", ex);
		return build(ErrorCode.INTERNAL_ERROR, ErrorCode.INTERNAL_ERROR.getDefaultMessage(), null);
	}

	private ResponseEntity<Response<Void>> build(ErrorCode code, String message, Object meta) {
		return ResponseEntity.status(code.getStatus()).body(Response.error(code, message, meta));
	}
}
