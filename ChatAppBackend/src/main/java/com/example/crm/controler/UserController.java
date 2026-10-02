package com.example.crm.controler;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.crm.dto.Response;
import com.example.crm.dto.request.UpdateProfileRequest;
import com.example.crm.dto.response.DashboardResponse;
import com.example.crm.dto.response.UserResponse;
import com.example.crm.security.AuthUser;
import com.example.crm.service.UserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {
	private final UserService userService;

	@GetMapping("/me")
	public ResponseEntity<Response<UserResponse>> me(@AuthenticationPrincipal AuthUser me) {
		return ResponseEntity.ok(Response.ok(userService.getMe(me.id()), "Your Profile"));
	}

	@PutMapping("/me")
	public ResponseEntity<Response<UserResponse>> updateMe(@AuthenticationPrincipal AuthUser me,
			@Valid @RequestBody UpdateProfileRequest request) {
		return ResponseEntity.ok(Response.ok(userService.updateMe(me.id(), request), "Profile Updated"));
	}

	@GetMapping("/me/dashboard")
	public ResponseEntity<Response<DashboardResponse>> dashboard(@AuthenticationPrincipal AuthUser me) {
		return ResponseEntity.ok(Response.ok(userService.dashboard(me.id()), "Dashboard Stats"));
	}

	@GetMapping("/search")
	public ResponseEntity<Response<List<UserResponse>>> search(@AuthenticationPrincipal AuthUser me, @RequestParam("q") String q) {
		return ResponseEntity.ok(Response.ok(userService.search(q, me.id()), "Search Results"));
	}

	@GetMapping("/{userId}")
	public ResponseEntity<Response<UserResponse>> getUser(@PathVariable String userId) {
		return ResponseEntity.ok(Response.ok(userService.getById(userId), "User Profile"));
	}
}
