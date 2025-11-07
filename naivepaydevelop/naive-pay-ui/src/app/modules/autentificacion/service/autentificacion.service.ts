import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable, of, tap } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { Router } from '@angular/router';
import { DeviceFingerprintService } from '../../dispositivos/service/device-fingerprint.service';

/** Payload que envía el login */
export interface LoginRequest {
    identifier: string;
    password: string;
}

/** Respuesta del backend al iniciar sesión */
export interface LoginResponse {
    accessToken: string; // JWT
    expiresAt: string;   // ISO de expiración
    jti: string;         // identificador del token
}

/**
 * Servicio de autenticación:
 * - login(): guarda el token en sessionStorage (per-tab)
 * - logout(): cierra sesión en backend y limpia storage local
 * - clear(): utilitario para limpiar storage local
 */
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

    private scheduleAutoLogout(at: Date) {
        const ms = at.getTime() - Date.now();
        if (this.logoutTimer) {
            clearTimeout(this.logoutTimer);
            this.logoutTimer = null;
        }
        if (ms <= 0) {
            this.clear();
            void this.router.navigate(['/auth/login'], { queryParams: { reason: 'session_closed' } });
            return;
        }
        this.logoutTimer = setTimeout(() => {
            this.http.post<void>(`${this.base}/logout`, {}).subscribe({
                next: () => {
                    this.clear();
                    void this.router.navigate(['/auth/login'], { queryParams: { reason: 'session_closed' } });
                },
                error: () => {
                    this.clear();
                    void this.router.navigate(['/auth/login'], { queryParams: { reason: 'session_closed' } });
                }
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
        } catch (_) {
            // ignore
        }
    }

    /** LOGIN: guarda el token para que el interceptor lo adjunte luego */
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

    /** LOGOUT: cierra sesión server-side y limpia el storage local */
    logout(): Observable<void> {
        return this.http.post<void>(`${this.base}/logout`, {}).pipe(
            tap(() => {
                this.clear();
                void this.router.navigate(['/auth/login'], { queryParams: { reason: 'logout_ok' } });
            })
        );
    }

    /**
     * LOGOUT silencioso: cierra sesión en backend y limpia el storage local
     * sin realizar navegación. Útil para usar en guards cuando el usuario
     * entra a rutas /auth/** y se debe marcar logout automáticamente.
     */
    logoutSilent(): Observable<void> {
        return this.http.post<void>(`${this.base}/logout`, {}).pipe(
            tap(() => this.clear()),
            catchError(() => {
                // Incluso si falla el backend, limpiamos el token local
                this.clear();
                return of(void 0);
            })
        );
    }

    /** Logout usando un token específico (útil si ya se borró del storage). */
    private logoutWithToken(token: string): Observable<void> {
        const headers = new HttpHeaders({ Authorization: `Bearer ${token}` });
        return this.http.post<void>(`${this.base}/logout`, {}, { headers }).pipe(
            tap(() => this.clear()),
            catchError(() => { this.clear(); return of(void 0); })
        );
    }

    /** Limpia el token local (usado por logout y/o interceptor en 401) */
    clear(): void {
        sessionStorage.removeItem('token');
        this.currentToken = null;
        if (this.logoutTimer) {
            clearTimeout(this.logoutTimer);
            this.logoutTimer = null;
        }
    }

    /** Vigila si el token es borrado manualmente (DevTools) y hace logout server-side. */
    private startTokenWatcher(): void {
        if (this.tokenWatchTimer) return; // evitar duplicados
        // Evento storage (cambios desde otros tabs/ventanas)
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
        } catch { /* no-op en entornos sin window */ }
        this.tokenWatchTimer = setInterval(() => {
            const stored = sessionStorage.getItem('token');
            const had = !!this.currentToken;
            const hasNow = !!stored;

            // Si antes había token y ahora no, hacer logout con el último conocido
            if (had && !hasNow && this.currentToken) {
                const tokenToClose = this.currentToken;
                // No reestablecer currentToken; se limpia al completar
                this.logoutWithToken(tokenToClose).subscribe({
                    next: () => void this.router.navigate(['/auth/login'], { queryParams: { reason: 'logout_ok' } }),
                    error: () => void this.router.navigate(['/auth/login'], { queryParams: { reason: 'logout_ok' } }),
                });
            }

            // Mantener currentToken sincronizado cuando cambia por login
            if (stored && stored !== this.currentToken) this.currentToken = stored;
        }, 1000);
    }
}