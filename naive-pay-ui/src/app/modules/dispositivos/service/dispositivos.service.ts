import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Device, DeviceLog } from '../domain/dispositivos';
import { DeviceFingerprintService } from './device-fingerprint.service';

export interface LoginResponse {
    accessToken: string;
    expiresAt: string;
    jti: string;
}


@Injectable({ providedIn: 'root' })
export class DispositivosService {
    private base = 'http://localhost:8080/api/devices';

    constructor(
        private http: HttpClient,
        private deviceFp: DeviceFingerprintService
    ) {}

    private publicHeaders(): HttpHeaders {
        const device = this.deviceFp.getDeviceInfo();
        return new HttpHeaders()
            .set('X-Device-Fingerprint', device.fingerprint)
            .set('X-Device-OS', device.os)
            .set('X-Device-Type', device.type)
            .set('X-Device-Browser', device.browser);
    }

    recoverRequest(identifier: string): Observable<{ message: string; recoveryId: string }> {
        return this.http.post<{ message: string; recoveryId: string }>(
            `${this.base}/recover/request`,
            { identifier },
            { headers: this.publicHeaders() }
        );
    }

    recoverVerify(recoveryId: string, code: string): Observable<LoginResponse> {
        return this.http.post<LoginResponse>(
            `${this.base}/recover/verify`,
            { recoveryId, code },
            { headers: this.publicHeaders() }
        );
    }

    /** Obtiene el token JWT desde `sessionStorage`. */
    getToken(): string | null {
        return sessionStorage.getItem('token');
    }

    /**
     * Construye los headers comunes para las solicitudes HTTP.
     * Incluye:
     *  - Authorization: token JWT del usuario.
     *  - X-Device-Fingerprint: huella digital del dispositivo actual.
     */
    buildHeaders(): HttpHeaders {
        const device = this.deviceFp.getDeviceInfo();

        let headers = new HttpHeaders()
            .set('X-Device-Fingerprint', device.fingerprint)
            .set('X-Device-OS', device.os)
            .set('X-Device-Type', device.type)
            .set('X-Device-Browser', device.browser);

        const token = this.getToken();
        if (token) headers = headers.set('Authorization', `Bearer ${token}`);

        return headers;
    }

    /** Obtiene el dispositivo actualmente vinculado al usuario. */
    current(): Observable<Device | { message: string }> {
        return this.http.get<Device | { message: string }>(`${this.base}/current`, { //Change to current
            headers: this.buildHeaders()
        });
    }

    /** Obtiene el historial (logs) de actividad del dispositivo. */
    logs(): Observable<DeviceLog[]> {
        return this.http.get<DeviceLog[]>(`${this.base}/logs`, {
            headers: this.buildHeaders()
        });
    }

    /** Desvincula el dispositivo actual de la cuenta del usuario. */
    unlink(): Observable<any> {
        return this.http.delete(`${this.base}/unlink`, {
            headers: this.buildHeaders()
        });
    }
}
