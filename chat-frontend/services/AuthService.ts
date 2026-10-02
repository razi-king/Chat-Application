import api, { ApiEndpoints, request } from "./api";
import type { AuthResponse, Dashboard, User } from "@/types";

export interface RegisterPayload {
  username: string;
  email: string;
  password: string;
  displayName: string;
}

export class AuthService {
  static register(body: RegisterPayload) {
    return request<AuthResponse>(api.post(`${ApiEndpoints.Auth}/register`, body));
  }

  static login(identifier: string, password: string) {
    return request<AuthResponse>(api.post(`${ApiEndpoints.Auth}/login`, { identifier, password }));
  }

  static logout() {
    return request<void>(api.post(`${ApiEndpoints.Auth}/logout`));
  }

  static me() {
    return request<User>(api.get(`${ApiEndpoints.Auth}/me`));
  }
}

export class UserService {
  static updateMe(body: Partial<Pick<User, "displayName" | "about" | "customStatus" | "avatarColor">>) {
    return request<User>(api.put(`${ApiEndpoints.Users}/me`, body));
  }

  static dashboard() {
    return request<Dashboard>(api.get(`${ApiEndpoints.Users}/me/dashboard`));
  }

  static search(q: string) {
    return request<User[]>(api.get(`${ApiEndpoints.Users}/search`, { params: { q } }));
  }

  static getUser(userId: string) {
    return request<User>(api.get(`${ApiEndpoints.Users}/${userId}`));
  }
}
