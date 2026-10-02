import type { ApiResponse } from "@/types";

/**
 * The ONE error type the whole frontend deals with.
 * Every failure (HTTP error, network down, timeout, WebSocket error frame) becomes an ApiError,
 * carrying the same errorCode the backend's ErrorCode enum sends.
 */
export class ApiError extends Error {
  readonly code: string;
  readonly status: number;
  readonly meta?: Record<string, string>;
  // Backend Correlation Id: Matches The Server Log Lines Of The Failed Request
  readonly requestId?: string;

  constructor(message: string, code: string, status = 0, meta?: Record<string, string>, requestId?: string) {
    super(message);
    this.name = "ApiError";
    this.code = code;
    this.status = status;
    this.meta = meta;
    this.requestId = requestId;
  }

  static fromResponse(body: Partial<ApiResponse<unknown>> | undefined, status: number): ApiError {
    const meta =
      body?.errorMeta && typeof body.errorMeta === "object" ? (body.errorMeta as Record<string, string>) : undefined;
    return new ApiError(body?.message || "Something went wrong", body?.errorCode || `HTTP_${status}`, status, meta, body?.requestId);
  }

  static network(): ApiError {
    return new ApiError("Cannot reach the server. Is the backend running?", ErrorCodes.NETWORK_ERROR, 0);
  }

  static timeout(): ApiError {
    return new ApiError("The server took too long to answer", ErrorCodes.TIMEOUT, 0);
  }

  get isAuthError(): boolean {
    return this.status === 401;
  }

  get isValidation(): boolean {
    return this.code === ErrorCodes.VALIDATION_FAILED || (this.meta !== undefined && Object.keys(this.meta).length > 0);
  }
}

// Same Codes As ErrorCode.java + Client-Only Ones
export const ErrorCodes = {
  NETWORK_ERROR: "NET_000",
  TIMEOUT: "NET_408",
  UNKNOWN: "CLI_000",
  VALIDATION_FAILED: "VAL_400",
  BAD_REQUEST: "REQ_400",
  INTERNAL_ERROR: "SYS_500",
  RATE_LIMITED: "RATE_429",
  CONFLICT: "REQ_409",
  DATABASE_UNAVAILABLE: "DB_503",
  UNAUTHORIZED: "AUTH_401",
  INVALID_CREDENTIALS: "AUTH_402",
  TOKEN_EXPIRED: "AUTH_403",
  ACCESS_DENIED: "AUTH_404",
  USER_NOT_FOUND: "USER_404",
  USERNAME_TAKEN: "USER_409",
  EMAIL_TAKEN: "USER_410",
  ROOM_NOT_FOUND: "ROOM_404",
  NOT_ROOM_MEMBER: "ROOM_403",
  SERVER_NOT_FOUND: "SRV_404",
  NOT_SERVER_MEMBER: "SRV_403",
  INVITE_NOT_FOUND: "INV_404",
  INVITE_EXPIRED: "INV_410",
  MESSAGE_NOT_FOUND: "MSG_404",
  FRIEND_REQUEST_EXISTS: "FRD_409",
} as const;

export const isApiError = (e: unknown): e is ApiError => e instanceof ApiError;
