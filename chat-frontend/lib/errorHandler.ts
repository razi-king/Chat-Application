import toast from "react-hot-toast";
import { ApiError, ErrorCodes, isApiError } from "./ApiError";

// Friendly Text Per Error Code (Falls Back To The Server's Message)
const friendlyMessages: Record<string, string> = {
  [ErrorCodes.NETWORK_ERROR]: "Can't reach the server. If it was asleep (free hosting) it is waking up now, so try again in ~30 seconds.",
  [ErrorCodes.TIMEOUT]: "The request timed out. Please try again.",
  [ErrorCodes.INTERNAL_ERROR]: "Something broke on our side. Please try again.",
  [ErrorCodes.DATABASE_UNAVAILABLE]: "Our database is temporarily unreachable. Please retry in a moment.",
  [ErrorCodes.TOKEN_EXPIRED]: "Your session expired. Please log in again.",
  [ErrorCodes.UNAUTHORIZED]: "Please log in to continue.",
  [ErrorCodes.ACCESS_DENIED]: "You don't have permission to do that.",
  [ErrorCodes.NOT_ROOM_MEMBER]: "You are not part of this conversation.",
  [ErrorCodes.NOT_SERVER_MEMBER]: "You are not a member of this server.",
  [ErrorCodes.INVITE_EXPIRED]: "This invite link has expired.",
  [ErrorCodes.INVITE_NOT_FOUND]: "This invite link is invalid.",
};

export function toApiError(error: unknown): ApiError {
  if (isApiError(error)) return error;
  if (error instanceof Error) return new ApiError(error.message, ErrorCodes.UNKNOWN);
  return new ApiError("Unexpected error", ErrorCodes.UNKNOWN);
}

export function getErrorMessage(error: unknown): string {
  const e = toApiError(error);
  return friendlyMessages[e.code] ?? e.message;
}

interface HandleOptions {
  // Formik's setErrors -> Field Errors From errorMeta Appear Under The Matching Input
  setFieldErrors?: (errors: Record<string, string>) => void;
  // Don't Toast (e.g. When The Page Shows The Error Itself)
  silent?: boolean;
  fallback?: string;
}

/**
 * The single place UI code sends errors to.
 *  - validation errors -> under the form fields
 *  - everything else   -> a toast with a friendly message
 */
export function handleError(error: unknown, options: HandleOptions = {}): ApiError {
  const e = toApiError(error);
  if (e.meta && options.setFieldErrors) {
    options.setFieldErrors(e.meta);
  }
  if (!options.silent) {
    const base = friendlyMessages[e.code] ?? e.message ?? options.fallback;
    // Server Failures Show The Reference So A Bug Report Can Be Matched To The Backend Logs
    const text = e.status >= 500 && e.requestId ? `${base} (ref: ${e.requestId})` : base;
    toast.error(text, { id: e.code === ErrorCodes.RATE_LIMITED ? "rate" : undefined });
  }
  if (process.env.NODE_ENV !== "production") {
    console.warn(`[${e.code}] ${e.message}`, e.meta ?? "");
  }
  return e;
}
