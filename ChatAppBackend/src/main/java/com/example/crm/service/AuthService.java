package com.example.crm.service;

import com.example.crm.dto.request.LoginRequest;
import com.example.crm.dto.request.RegisterRequest;
import com.example.crm.dto.response.AuthResponse;

public interface AuthService {

	AuthResponse register(RegisterRequest request);

	AuthResponse login(LoginRequest request);

	void logout(String token);
}
