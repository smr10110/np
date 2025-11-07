import { Injectable } from '@angular/core';
import {
    HttpInterceptor,
    HttpRequest,
    HttpHandler,
    HttpEvent,
    HttpErrorResponse,
} from '@angular/common/http';

import { Router } from '@angular/router';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';

import { DeviceFingerprintService } from '../../modules/dispositivos/service/device-fingerprint.service';

@Injectable()
export class AuthInterceptor implements HttpInterceptor {

    /** Endpoints públicos a los que NO se debe enviar Authorization */
    private readonly PUBLIC_PATHS: readonly string[] = [
        '/auth/login',
        '/auth/register',
        '/api/register',
        '/api/dispositivos/recover',
        '/api/devices/recover',
    ];

    constructor(
        private readonly router: Router,
        private readonly deviceFp: DeviceFingerprintService
    ) {}

    intercept(req: HttpRequest<unknown>, next: HttpHandler): Observable<HttpEvent<unknown>> {
        // --- 1) Construcción de la request a enviar ---
        // Si la URL coincide con un endpoint público, no adjuntamos el header.
        // Si NO es pública y existe token en sessionStorage, clonamos la request con Authorization.

        const pathname = (() => {
            try { return new URL(req.url, window.location.origin).pathname; }
            catch { return req.url; }
        })();

        const isPublic = this.PUBLIC_PATHS.some(p => req.url.includes(p));
        const token = sessionStorage.getItem('token');

        const fingerprint = this.deviceFp.get(); // ➜ Devices Cambio
        let headers = req.headers.set('X-Device-Fingerprint', fingerprint);
        if (!isPublic && token) {
            headers = headers.set('Authorization', `Bearer ${token}`);
        }
        const requestToSend = req.clone({ headers });

        // --- 2) Manejo de respuestas con error ---
        // Si el backend responde 401 (token expirado/inválido/sesión cerrada),
        // se limpia el token local y se redirige al login.
        return next.handle(requestToSend).pipe(
            catchError((err: HttpErrorResponse) => {
                const isRecovery = pathname.startsWith('/api/devices/recover') ||
                    pathname.startsWith('/api/dispositivos/recover');
                if (!isRecovery && err.status === 401) {
                    sessionStorage.removeItem('token');
                    void this.router.navigate(['/auth/login'], { queryParams: { reason: 'session_closed' } });
                }
                return throwError(() => err);
            })
        );
    }
}
