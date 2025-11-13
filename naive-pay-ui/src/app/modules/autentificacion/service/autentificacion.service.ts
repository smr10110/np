import { Injectable, OnDestroy, inject } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable, of, tap } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { Router } from '@angular/router';
import { DeviceFingerprintService } from '../../dispositivos/service/device-fingerprint.service';

// ======================== DTOs ========================

export interface LoginRequest {
  identifier: string;
  password: string;
}

export interface LoginResponse {
  accessToken: string;
  expiresAt: string;
  jti: string;
  role: 'USER' | 'ADMIN'; // Rol del usuario (USER o ADMIN)
}

export interface ForgotPasswordRequest {
  email: string;
}

export interface ResetPasswordRequest {
  email: string;
  code: string;
  newPassword: string;
}

export interface MessageResponse {
  message: string;
}

/**
 * Servicio de autenticación que gestiona login, logout y recuperación de contraseña.
 * Implementa OnDestroy para cleanup de timers y prevenir memory leaks.
 */
@Injectable({ providedIn: 'root' })
export class AutentificacionService implements OnDestroy {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  private readonly deviceFp = inject(DeviceFingerprintService);
  private readonly base = 'http://localhost:8080/auth';

  private logoutTimer: ReturnType<typeof setTimeout> | null = null;

  constructor() {
    // Restaura sesión existente si hay token en sessionStorage
    const token = sessionStorage.getItem('token');
    if (token) this.scheduleAutoLogoutFromToken(token);
  }

  // Limpia timers al destruir el servicio para prevenir memory leaks
  ngOnDestroy(): void {
    this.cleanupTimers();
  }

  // ======================== Session Helpers ========================

  // Programa auto-logout para ejecutarse en una fecha específica de expiración
  private scheduleAutoLogout(at: Date) {
    const ms = at.getTime() - Date.now();

    if (this.logoutTimer) {
      clearTimeout(this.logoutTimer);
      this.logoutTimer = null;
    }

    // Si ya expiró, ejecuta logout inmediato
    if (ms <= 0) {
      this.clearAndRedirect('session_closed');
      return;
    }

    // Programa logout automático cuando expire el token
    this.logoutTimer = setTimeout(() => {
      this.http.post<void>(`${this.base}/logout`, {}).subscribe({
        next: () => this.clearAndRedirect('session_closed'),
        error: () => this.clearAndRedirect('session_closed')
      });
    }, ms);
  }

  // Decodifica el JWT y programa auto-logout basado en el campo 'exp' del payload
  private scheduleAutoLogoutFromToken(token: string) {
    try {
      const payloadRaw = token.split('.')[1];
      const payloadJson = JSON.parse(atob(payloadRaw.replace(/-/g, '+').replace(/_/g, '/')));
      if (payloadJson?.exp) {
        const expirationDate = new Date(payloadJson.exp * 1000);
        this.scheduleAutoLogout(expirationDate);
      }
    } catch {
      // Token inválido, ignorar
    }
  }

  // Limpia la sesión local eliminando token, rol y cancelando todos los timers
  clear(): void {
    sessionStorage.removeItem('token');
    sessionStorage.removeItem('userRole');
    this.cleanupTimers();
  }

  // Limpia sesión y redirige a login con razón especificada
  private clearAndRedirect(reason: string): void {
    this.clear();
    void this.router.navigate(['/auth/login'], { queryParams: { reason } });
  }

  // Cancela el timer de auto-logout
  private cleanupTimers(): void {
    if (this.logoutTimer) {
      clearTimeout(this.logoutTimer);
      this.logoutTimer = null;
    }
  }

  // ======================== Auth APIs ========================

  // Autentica usuario con email/RUT y contraseña, guarda token, rol y programa auto-logout
  login(req: LoginRequest): Observable<LoginResponse> {
    const headers = new HttpHeaders().set('X-Device-Fingerprint', this.deviceFp.get());
    return this.http.post<LoginResponse>(`${this.base}/login`, req, { headers }).pipe(
      tap(res => {
        sessionStorage.setItem('token', res.accessToken);
        sessionStorage.setItem('userRole', res.role); // Guardar rol del usuario
        this.scheduleAutoLogoutFromToken(res.accessToken);
      })
    );
  }

  // Cierra sesión del usuario e invalida token en backend
  logout(redirect: boolean = true): Observable<void> {
    return this.http.post<void>(`${this.base}/logout`, {}).pipe(
      tap(() => {
        this.clear();
        if (redirect) {
          void this.router.navigate(['/auth/login'], { queryParams: { reason: 'logout_ok' } });
        }
      }),
      catchError(() => {
        this.clear();
        if (redirect) {
          void this.router.navigate(['/auth/login'], { queryParams: { reason: 'logout_ok' } });
        }
        return of(void 0);
      })
    );
  }

  // ======================== Password Recovery APIs ========================

  // Solicita código de recuperación de 6 dígitos enviado al email del usuario
  requestPasswordRecovery(request: ForgotPasswordRequest): Observable<MessageResponse> {
    return this.http.post<MessageResponse>(`${this.base}/password/request`, request);
  }

  // Verifica código de 6 dígitos y actualiza contraseña del usuario
  resetPassword(request: ResetPasswordRequest): Observable<MessageResponse> {
    return this.http.post<MessageResponse>(`${this.base}/password/reset`, request);
  }

  // ======================== Role Helpers ========================

  // Verifica si el usuario actual tiene rol de administrador
  isAdmin(): boolean {
    return sessionStorage.getItem('userRole') === 'ADMIN';
  }

  // Obtiene el rol del usuario actual ('USER' o 'ADMIN'), null si no está autenticado
  getUserRole(): 'USER' | 'ADMIN' | null {
    const role = sessionStorage.getItem('userRole');
    return role === 'USER' || role === 'ADMIN' ? role : null;
  }
}