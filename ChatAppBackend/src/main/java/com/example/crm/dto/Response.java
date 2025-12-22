package com.example.crm.dto;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class Response {
	@Schema(
			description = "Status Of The API Call",
			type = "boolean",
			example = "true"
	)
	private Boolean Success;
	
	@Schema(
			description = "Any Object Or List Returned On Successful API Calls",
			type = "object",
			example = "{...}"
	)
	private Object data;
	
	@Schema(
			description = "Time Stamp Of The API Call In dd-mm-yyyy hh:mm:ss Format ",
			type = "string",
			example = "25-09-25 16:45:00"
	)
	@JsonFormat(shape = JsonFormat.Shape.STRING,pattern ="dd-MM-yyyy hh:mm:ss")
	private Date timestamp;
	
	@Schema(
			description = "API Response Message",
			type = "string",
			example = "Operation Successful"
	)
	private String message;
	
	@Schema(
			description = "API Error Code (If Any)",
			type = "string",
			example = "ERR001"
	)
	private String errorCode;
	
	@JsonInclude(Include.NON_EMPTY)
	@Schema(
			description = "Additional Error Meta Data (Optional)",
			type = "object",
			example = "{\"Details\":\"Invalid User Input\"}"
	)
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
}
