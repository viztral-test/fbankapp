import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { Observable, catchError, finalize, map, of, tap } from 'rxjs';

import { API_BASE_URL } from './api.config';
import { LoginRequest, LoginResponse } from './models';

export interface Session {
  token: string;
  expiresAt: string;
  username: string;
  displayName: string;
}

const STORAGE_KEY = 'fbankapp.session';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly sessionState = signal<Session | null>(readStoredSession());

  readonly session = this.sessionState.asReadonly();

  login(credentials: LoginRequest): Observable<Session> {
    return this.http.post<LoginResponse>(`${API_BASE_URL}/api/auth/login`, credentials).pipe(
      map(({ token, expiresAt, username, displayName }) => ({ token, expiresAt, username, displayName })),
      tap((session) => this.storeSession(session)),
    );
  }

  /** Ends the token on the server, then always clears the local session, even if the call fails. */
  logout(): Observable<void> {
    return this.http.post<void>(`${API_BASE_URL}/api/auth/logout`, null).pipe(
      catchError(() => of(undefined)),
      finalize(() => this.clearSession()),
    );
  }

  /** Checks the stored session against its expiry time, dropping it once the token has expired. */
  hasValidSession(): boolean {
    const session = this.sessionState();
    if (!session) {
      return false;
    }
    if (isExpired(session)) {
      this.clearSession();
      return false;
    }
    return true;
  }

  clearSession(): void {
    this.sessionState.set(null);
    try {
      sessionStorage.removeItem(STORAGE_KEY);
    } catch {
      // Storage is unavailable; the in-memory session is already cleared
    }
  }

  private storeSession(session: Session): void {
    this.sessionState.set(session);
    try {
      sessionStorage.setItem(STORAGE_KEY, JSON.stringify(session));
    } catch {
      // Storage is unavailable; the session still works until the page is reloaded
    }
  }
}

function isExpired(session: Session): boolean {
  const expiresAt = Date.parse(session.expiresAt);
  return Number.isNaN(expiresAt) || expiresAt <= Date.now();
}

function readStoredSession(): Session | null {
  try {
    const raw = sessionStorage.getItem(STORAGE_KEY);
    if (!raw) {
      return null;
    }
    const session = JSON.parse(raw) as Session;
    return session?.token && !isExpired(session) ? session : null;
  } catch {
    return null;
  }
}
