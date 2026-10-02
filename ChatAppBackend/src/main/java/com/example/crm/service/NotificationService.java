package com.example.crm.service;

import com.example.crm.dto.PageResponse;
import com.example.crm.dto.response.NotificationResponse;
import com.example.crm.enums.NotificationType;

public interface NotificationService {

	void notify(String userId, NotificationType type, String title, String body, String link, String actorId);

	PageResponse<NotificationResponse> list(String userId, int page, int size);

	long unreadCount(String userId);

	void markRead(String notificationId, String userId);

	void markAllRead(String userId);
}
