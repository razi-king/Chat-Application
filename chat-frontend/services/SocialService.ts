import api, { ApiEndpoints, request } from "./api";
import type { Friendship, Notification, PageResponse } from "@/types";

export class FriendService {
  static list() {
    return request<Friendship[]>(api.get(ApiEndpoints.Friends));
  }

  static sendRequest(username: string) {
    return request<Friendship>(api.post(`${ApiEndpoints.Friends}/requests`, { username }));
  }

  static accept(friendshipId: string) {
    return request<Friendship>(api.post(`${ApiEndpoints.Friends}/${friendshipId}/accept`));
  }

  static remove(friendshipId: string) {
    return request<void>(api.delete(`${ApiEndpoints.Friends}/${friendshipId}`));
  }
}

export class NotificationService {
  static list(page = 0) {
    return request<PageResponse<Notification>>(api.get(ApiEndpoints.Notifications, { params: { page } }));
  }

  static unreadCount() {
    return request<{ count: number }>(api.get(`${ApiEndpoints.Notifications}/unread-count`));
  }

  static markRead(id: string) {
    return request<void>(api.post(`${ApiEndpoints.Notifications}/${id}/read`));
  }

  static markAllRead() {
    return request<void>(api.post(`${ApiEndpoints.Notifications}/read-all`));
  }
}
