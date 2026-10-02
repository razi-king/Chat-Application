package com.example.crm.controler;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.crm.dto.Response;
import com.example.crm.dto.request.LoginRequest;
import com.example.crm.dto.request.RegisterRequest;
import com.example.crm.dto.response.AuthResponse;
import com.example.crm.dto.response.UserResponse;
import com.example.crm.security.AuthUser;
import com.example.crm.security.JwtAuthenticationFilter;
import com.example.crm.service.AuthService;
import com.example.crm.service.UserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {
	private final AuthService authService;
	private final UserService userService;

	@PostMapping("/register")
	public ResponseEntity<Response<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(Response.ok(authService.register(request), "Account Created Successfully"));
	}

	@PostMapping("/login")
	public ResponseEntity<Response<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
		return ResponseEntity.ok(Response.ok(authService.login(request), "Logged In Successfully"));
	}

	@PostMapping("/logout")
	public ResponseEntity<Response<Void>> logout(@RequestHeader("Authorization") String authorization) {
		authService.logout(JwtAuthenticationFilter.extractToken(authorization));
		return ResponseEntity.ok(Response.ok(null, "Logged Out Successfully"));
	}

	@GetMapping("/me")
	public ResponseEntity<Response<UserResponse>> me(@AuthenticationPrincipal AuthUser me) {
		return ResponseEntity.ok(Response.ok(userService.getMe(me.id()), "Current User"));
	}
}
