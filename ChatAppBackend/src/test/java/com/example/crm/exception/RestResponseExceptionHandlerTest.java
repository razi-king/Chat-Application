package com.example.crm.exception;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.example.crm.dto.request.SendMessageRequest;
import com.example.crm.enums.ErrorCode;

import jakarta.validation.Valid;

/**
 * Verifies the error-flow contract: whatever goes wrong, the client receives the same
 * Response envelope with the right HTTP status and error code, and never a stack trace.
 */
class RestResponseExceptionHandlerTest {
	private MockMvc mvc;

	@RestController
	static class FailingController {
		@GetMapping("/not-found")
		void notFound() {
			throw new ResourceNotFoundException("Room not found for id x", ErrorCode.ROOM_NOT_FOUND);
		}

		@GetMapping("/forbidden")
		void forbidden() {
			throw new ForbiddenException(ErrorCode.NOT_ROOM_MEMBER);
		}

		@GetMapping("/db-down")
		void dbDown() {
			throw new DataAccessResourceFailureException("Timed out after 30000 ms while waiting for mongo:27017");
		}

		@GetMapping("/bug")
		void bug() {
			throw new IllegalStateException("secret internal detail");
		}

		@PostMapping("/messages")
		void send(@Valid @RequestBody SendMessageRequest request) {
		}
	}

	@BeforeEach
	void setUp() {
		mvc = MockMvcBuilders.standaloneSetup(new FailingController())
				.setControllerAdvice(new RestResponseExceptionHandler())
				.build();
	}

	@Test
	void customNotFound_maps404WithCode() throws Exception {
		mvc.perform(get("/not-found"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.errorCode").value("ROOM_404"));
	}

	@Test
	void customForbidden_maps403() throws Exception {
		mvc.perform(get("/forbidden"))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.errorCode").value("ROOM_403"));
	}

	// Validation Errors Name Each Broken Field, So The Frontend Shows Them Under The Inputs
	@Test
	void invalidBody_400WithFieldErrors() throws Exception {
		mvc.perform(post("/messages").contentType(MediaType.APPLICATION_JSON).content("{\"content\":\"   \"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errorCode").value("VAL_400"))
				.andExpect(jsonPath("$.errorMeta.content").exists());
	}

	@Test
	void malformedJson_400() throws Exception {
		mvc.perform(post("/messages").contentType(MediaType.APPLICATION_JSON).content("{not json"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errorCode").value("REQ_400"));
	}

	// MongoDB Down Is A 503 ("Retry Later"), Not A 500 ("Bug"), And Hides The Driver Message
	@Test
	void databaseUnavailable_503WithoutInternalDetails() throws Exception {
		mvc.perform(get("/db-down"))
				.andExpect(status().isServiceUnavailable())
				.andExpect(jsonPath("$.errorCode").value("DB_503"))
				.andExpect(content().string(not(containsString("mongo:27017"))));
	}

	@Test
	void unexpectedException_500WithoutLeakingMessage() throws Exception {
		mvc.perform(get("/bug"))
				.andExpect(status().isInternalServerError())
				.andExpect(jsonPath("$.errorCode").value("SYS_500"))
				.andExpect(content().string(not(containsString("secret internal detail"))));
	}
}
