package com.example.crm.service;

import java.util.List;

import com.example.crm.dto.request.UpdateProfileRequest;
import com.example.crm.dto.response.DashboardResponse;
import com.example.crm.dto.response.UserResponse;
import com.example.crm.entity.User;

public interface UserService {

	User requireUser(String userId);

	UserResponse getMe(String userId);

	UserResponse updateMe(String userId, UpdateProfileRequest request);

	UserResponse getById(String userId);

	List<UserResponse> search(String query, String meId);

	DashboardResponse dashboard(String userId);
}
