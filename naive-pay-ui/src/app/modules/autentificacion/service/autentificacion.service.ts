import { Injectable, OnDestroy, inject } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable, of, tap, BehaviorSubject } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { Router } from '@angular/router';
import { DeviceFingerprintService } from '../../dispositivos/service/device-fingerprint.service';
import { InactivityWarningService } from '../../../shared/services/inactivity-warning.service';

// ======================== DTOs ========================

export interface LoginRequest {
  identifier: string;
  password: string;
}

export interface LoginResponse {
  accessToken: string;
  expiresAt: string;
  jti: string;
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

export interface SessionStatusResponse {
  minutesRemaining: number;
  minutesUntilInactivity: number;
  minutesUntilMaxExpiration: number;
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
  private readonly inactivityWarningService = inject(InactivityWarningService);
  private readonly base = 'http://localhost:8080/auth';

  private logoutTimer: ReturnType<typeof setTimeout> | null = null;
  private inactivityCheckTimer: ReturnType<typeof setInterval> | null = null;
  private warningShown = false;

  // Observable para exponer fecha de expiración de sesión
  private sessionExpirationSubject = new BehaviorSubject<Date | null>(null);
  public sessionExpiration$ = this.sessionExpirationSubject.asObservable();

  constructor() {
    // Restaura sesión existente si hay token en sessionStorage
    const token = sessionStorage.getItem('token');
    if (token) {
      this.scheduleAutoLogoutFromToken(token);
      this.startInactivityMonitoring();
    }
  }

  // Limpia timers al destruir el servicio para prevenir memory leaks
  ngOnDestroy(): void {
    this.cleanupTimers();
    this.stopInactivityMonitoring();
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

    // Actualizar observable de expiración
    this.sessionExpirationSubject.next(at);

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

  // Inicia monitoreo de inactividad mediante polling cada 1 minuto
  private startInactivityMonitoring(): void {
    this.stopInactivityMonitoring();

    this.inactivityCheckTimer = setInterval(() => {
      this.http.get<SessionStatusResponse>(`${this.base}/session-status`)
        .subscribe({
          next: (res) => {
            // Si queda 1 minuto o menos (considerando el límite más restrictivo) y no hemos mostrado advertencia
            if (res.minutesRemaining <= 1 && !this.warningShown) {
              this.warningShown = true;
              this.showInactivityWarning();
            }
          },
          error: (err) => {
            if (err.status === 401) {
              this.stopInactivityMonitoring();
              this.clearAndRedirect('session_closed');
            }
          }
        });
    }, 60000);  // Cada 1 minuto
  }

  // Detiene monitoreo de inactividad
  private stopInactivityMonitoring(): void {
    if (this.inactivityCheckTimer) {
      clearInterval(this.inactivityCheckTimer);
      this.inactivityCheckTimer = null;
    }
    this.warningShown = false;
  }

  // Muestra advertencia de inactividad al usuario
  private showInactivityWarning(): void {
    // Mostrar modal de advertencia
    this.inactivityWarningService.show();
  }

  // Método público para resetear inactividad (llamado desde app.component)
  resetInactivity(): void {
    // Hacer un request para resetear la actividad automáticamente
    this.http.get(`${this.base}/session-status`).subscribe();
    this.warningShown = false;  // Resetear para mostrar próxima advertencia si es necesario
  }

  // Limpia la sesión local eliminando token y cancelando todos los timers
  clear(): void {
    sessionStorage.removeItem('token');
    this.cleanupTimers();
    this.stopInactivityMonitoring();
    this.sessionExpirationSubject.next(null);
    this.inactivityWarningService.hide();
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

  // Autentica usuario con email/RUT y contraseña, guarda token y programa auto-logout
  login(req: LoginRequest): Observable<LoginResponse> {
    const headers = new HttpHeaders().set('X-Device-Fingerprint', this.deviceFp.get());
    return this.http.post<LoginResponse>(`${this.base}/login`, req, { headers }).pipe(
      tap(res => {
        sessionStorage.setItem('token', res.accessToken);
        this.scheduleAutoLogoutFromToken(res.accessToken);
        this.startInactivityMonitoring();
      })
    );
  }

  // Cierra sesión del usuario e invalida token en backend
  logout(redirect: boolean = true): Observable<void> {
    this.stopInactivityMonitoring();
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
}