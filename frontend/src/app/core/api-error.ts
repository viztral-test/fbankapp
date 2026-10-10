import { HttpErrorResponse } from '@angular/common/http';

import { API_BASE_URL } from './api.config';
import { ApiError } from './models';

/** True when the request never got an HTTP response (backend down, CORS, offline). */
export function isNetworkError(err: unknown): boolean {
  return err instanceof HttpErrorResponse && err.status === 0;
}

/** Turns a failed API call into a message that can be shown to the user. */
export function apiErrorMessage(err: unknown, fallback: string): string {
  if (isNetworkError(err)) {
    return `Cannot reach the server. Check that the backend is running on ${API_BASE_URL}.`;
  }
  const message = apiErrorBody(err)?.message;
  return typeof message === 'string' && message ? message : fallback;
}

/** Per-field messages from a 400 validation failure, or an empty object. */
export function apiFieldErrors(err: unknown): Record<string, string> {
  return apiErrorBody(err)?.fieldErrors ?? {};
}

function apiErrorBody(err: unknown): Partial<ApiError> | null {
  if (err instanceof HttpErrorResponse && err.error && typeof err.error === 'object') {
    return err.error as Partial<ApiError>;
  }
  return null;
}
