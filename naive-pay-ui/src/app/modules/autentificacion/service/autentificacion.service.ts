import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable, of, tap } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { Router } from '@angular/router';
import { DeviceFingerprintService } from '../../dispositivos/service/device-fingerprint.service';

// Payloads / DTOs
export interface LoginRequest {
  identifier: string;
  password: string;
}

export interface LoginResponse {
  accessToken: string;
  expiresAt: string;
  jti: string;
}

export interface ForgotPasswordRequest { email: string; }

export interface ResetPasswordRequest {
  email: string;
  code: string;
  newPassword: string;
}

export interface VerifyCodeRequest {
  email: string;
  code: string;
}

export interface MessageResponse { message: string; }

@Injectable({ providedIn: 'root' })
export class AutentificacionService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  private readonly deviceFp = inject(DeviceFingerprintService);
  private readonly base = 'http://localhost:8080/auth';

  private logoutTimer: any;
  private tokenWatchTimer: any;
  private currentToken: string | null = null;

  constructor() {
    const token = sessionStorage.getItem('token');
    this.currentToken = token;
    if (token) this.scheduleAutoLogoutFromToken(token);
    this.startTokenWatcher();
  }

  // Session helpers
  private scheduleAutoLogout(at: Date) {
    const ms = at.getTime() - Date.now();
    if (this.logoutTimer) { clearTimeout(this.logoutTimer); this.logoutTimer = null; }
    if (ms <= 0) {
      this.clear();
      void this.router.navigate(['/auth/login'], { queryParams: { reason: 'session_closed' } });
      return;
    }
    this.logoutTimer = setTimeout(() => {
      this.http.post<void>(`${this.base}/logout`, {}).subscribe({
        next: () => { this.clear(); void this.router.navigate(['/auth/login'], { queryParams: { reason: 'session_closed' } }); },
        error: () => { this.clear(); void this.router.navigate(['/auth/login'], { queryParams: { reason: 'session_closed' } }); }
      });
    }, ms);
  }

  private scheduleAutoLogoutFromToken(token: string) {
    try {
      const payloadRaw = token.split('.')[1];
      const payloadJson = JSON.parse(atob(payloadRaw.replace(/-/g, '+').replace(/_/g, '/')));
      if (payloadJson && payloadJson.exp) {
        const expMs = payloadJson.exp * 1000;
        this.scheduleAutoLogout(new Date(expMs));
      }
    } catch { /* ignore */ }
  }

  // Auth APIs
  login(req: LoginRequest): Observable<LoginResponse> {
    const headers = new HttpHeaders().set('X-Device-Fingerprint', this.deviceFp.get());
    return this.http.post<LoginResponse>(`${this.base}/login`, req, { headers }).pipe(
      tap(res => {
        sessionStorage.setItem('token', res.accessToken);
        this.currentToken = res.accessToken;
        this.scheduleAutoLogoutFromToken(res.accessToken);
      })
    );
  }

  logout(): Observable<void> {
    return this.http.post<void>(`${this.base}/logout`, {}).pipe(
      tap(() => {
        this.clear();
        void this.router.navigate(['/auth/login'], { queryParams: { reason: 'logout_ok' } });
      })
    );
  }

  logoutSilent(): Observable<void> {
    return this.http.post<void>(`${this.base}/logout`, {}).pipe(
      tap(() => this.clear()),
      catchError(() => { this.clear(); return of(void 0); })
    );
  }

  private logoutWithToken(token: string): Observable<void> {
    const headers = new HttpHeaders({ Authorization: `Bearer ${token}` });
    return this.http.post<void>(`${this.base}/logout`, {}, { headers }).pipe(
      tap(() => this.clear()),
      catchError(() => { this.clear(); return of(void 0); })
    );
  }

  clear(): void {
    sessionStorage.removeItem('token');
    this.currentToken = null;
    if (this.logoutTimer) { clearTimeout(this.logoutTimer); this.logoutTimer = null; }
  }

  private startTokenWatcher(): void {
    if (this.tokenWatchTimer) return;
    try {
      window.addEventListener('storage', (ev: StorageEvent) => {
        if (ev.key === 'token' && ev.newValue === null && this.currentToken) {
          const tokenToClose = this.currentToken;
          this.logoutWithToken(tokenToClose).subscribe({
            next: () => void this.router.navigate(['/auth/login'], { queryParams: { reason: 'logout_ok' } }),
            error: () => void this.router.navigate(['/auth/login'], { queryParams: { reason: 'logout_ok' } }),
          });
        }
      });
    } catch { /* no-op */ }
    this.tokenWatchTimer = setInterval(() => {
      const stored = sessionStorage.getItem('token');
      const had = !!this.currentToken;
      const hasNow = !!stored;
      if (had && !hasNow && this.currentToken) {
        const tokenToClose = this.currentToken;
        this.logoutWithToken(tokenToClose).subscribe({
          next: () => void this.router.navigate(['/auth/login'], { queryParams: { reason: 'logout_ok' } }),
          error: () => void this.router.navigate(['/auth/login'], { queryParams: { reason: 'logout_ok' } }),
        });
      }
      if (stored && stored !== this.currentToken) this.currentToken = stored;
    }, 1000);
  }

  // Password recovery APIs
  requestPasswordRecovery(request: ForgotPasswordRequest): Observable<MessageResponse> {
    return this.http.post<MessageResponse>(`${this.base}/password/request`, request);
  }

  verifyRecoveryCode(request: VerifyCodeRequest): Observable<MessageResponse> {
    return this.http.post<MessageResponse>(`${this.base}/password/verify`, request);
  }

  resetPassword(request: ResetPasswordRequest): Observable<MessageResponse> {
    return this.http.post<MessageResponse>(`${this.base}/password/reset`, request);
  }
}
