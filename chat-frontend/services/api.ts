import axios, { AxiosError, AxiosInstance, AxiosResponse, InternalAxiosRequestConfig } from "axios";
import { ApiError, ErrorCodes } from "@/lib/ApiError";
import type { ApiResponse } from "@/types";

export const ApiBase: string = process.env.NEXT_PUBLIC_API_BASE_URL || "http://localhost:8080/api/v1";
export const WsUrl: string = process.env.NEXT_PUBLIC_WS_URL || "ws://localhost:8080/ws";

export const TOKEN_KEY = "nexus.token";
// Fired When The Backend Says Our Token Is No Longer Valid -> AuthContext Logs Out
export const AUTH_EXPIRED_EVENT = "nexus:auth-expired";

export const getToken = (): string | null => {
  if (typeof window === "undefined") return null;
  try {
    return localStorage.getItem(TOKEN_KEY);
  } catch {
    return null;
  }
};

// Paths Are Relative To baseURL, So "/rooms" -> http://localhost:8080/api/v1/rooms
export const ApiEndpoints = {
  Auth: "/auth",
  Users: "/users",
  Rooms: "/rooms",
  Messages: "/messages",
  Servers: "/servers",
  Friends: "/friends",
  Notifications: "/notifications",
  Invites: "/invites",
} as const;

const api: AxiosInstance = axios.create({
  baseURL: ApiBase,
  headers: { "Content-Type": "application/json" },
  // Generous Because A Sleeping Free-Tier Backend Needs Up To ~60 s For Its First Response
  timeout: 60000,
});

// Api Request Interceptor: Attach The JWT
api.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const token = getToken();
  if (token) config.headers.Authorization = `Bearer ${token}`;
  if (process.env.NODE_ENV !== "production") {
    console.debug(`Api Request ${config.method?.toUpperCase()} ${config.url}`);
  }
  return config;
});

// Api Response Interceptor: EVERY Failure Becomes An ApiError And Is Rejected (Never Swallowed)
api.interceptors.response.use(
  (response: AxiosResponse) => response,
  (error: AxiosError<ApiResponse<unknown>>) => {
    if (error.code === "ECONNABORTED") return Promise.reject(ApiError.timeout());
    if (!error.response) return Promise.reject(ApiError.network());

    const apiError = ApiError.fromResponse(error.response.data, error.response.status);
    const isAuthCall = error.config?.url?.startsWith(`${ApiEndpoints.Auth}/login`) || error.config?.url?.startsWith(`${ApiEndpoints.Auth}/register`);
    if (apiError.status === 401 && !isAuthCall && typeof window !== "undefined") {
      window.dispatchEvent(new CustomEvent(AUTH_EXPIRED_EVENT, { detail: apiError.code }));
    }
    return Promise.reject(apiError.code ? apiError : new ApiError("Unexpected error", ErrorCodes.UNKNOWN, error.response.status));
  }
);

// Unwrap Response<T> -> T So Services Return Plain Data
export async function request<T>(promise: Promise<AxiosResponse<ApiResponse<T>>>): Promise<T> {
  const res = await promise;
  return res.data.data;
}

export default api;
