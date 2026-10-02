package com.example.crm.dto;

import java.util.Date;

import org.slf4j.MDC;

import com.example.crm.enums.ErrorCode;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * Every API (REST and WebSocket errors) answers with this envelope.
 */
@Data
public class Response<T> {
	@Schema(description = "Status Of The API Call", example = "true")
	private Boolean success;

	@Schema(description = "Any Object Or List Returned On Successful API Calls")
	private T data;

	@Schema(description = "Time Stamp Of The API Call In dd-MM-yyyy HH:mm:ss Format", example = "25-09-2025 16:45:00")
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy HH:mm:ss")
	private Date timestamp;

	@Schema(description = "API Response Message", example = "Operation Successful")
	private String message;

	@JsonInclude(Include.NON_NULL)
	@Schema(description = "API Error Code (If Any)", example = "ROOM_404")
	private String errorCode;

	@JsonInclude(Include.NON_NULL)
	@Schema(description = "Correlation id of the failed request; quote it when reporting a problem", example = "9f1c2a7b3d4e5f60")
	private String requestId;

	@JsonInclude(Include.NON_EMPTY)
	@Schema(description = "Additional Error Meta Data (Optional)", example = "{\"name\":\"must not be blank\"}")
	private Object errorMeta;

	public Response() {
		this.timestamp = new Date();
	}

	public void fail() {
		this.setSuccess(Boolean.FALSE);
	}

	public void success() {
		this.setSuccess(Boolean.TRUE);
	}

	public static <T> Response<T> ok(T data, String message) {
		Response<T> response = new Response<>();
		response.success();
		response.setData(data);
		response.setMessage(message);
		return response;
	}

	public static Response<Void> error(ErrorCode errorCode, String message, Object meta) {
		Response<Void> response = new Response<>();
		response.fail();
		response.setErrorCode(errorCode.getCode());
		response.setMessage(message);
		response.setErrorMeta(meta);
		// Only Errors Carry It: That Is When Someone Needs To Find The Matching Log Lines
		response.setRequestId(MDC.get("requestId"));
		return response;
	}
}
