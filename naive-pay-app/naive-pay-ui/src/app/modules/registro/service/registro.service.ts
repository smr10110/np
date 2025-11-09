import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

import { DeviceFingerprintService } from '../../dispositivos/service/device-fingerprint.service';


@Injectable({
    providedIn: 'root'
})
export class RegistroService {

    private apiUrl = 'http://localhost:8080/api/register';

    constructor(private http: HttpClient, private deviceFp: DeviceFingerprintService ) { }

    startRegistration(data: any): Observable<any> {
        return this.http.post(`${this.apiUrl}/start`, data);
    }

    verifyEmail(email: string, code: string): Observable<any> {
        const params = new HttpParams()
            .set('email', email)
            .set('code', code);

        return this.http.post(`${this.apiUrl}/verify`, null, { params });
    }

    resendVerificationCode(email: string): Observable<{ message: string }> {
        return this.http.post<{ message: string }>(`${this.apiUrl}/resend-code`, { email });
    }

    completeRegistration(data: any): Observable<any> {
        const device = this.deviceFp.getDeviceInfo();
        const payload = {
            ...data,
            fingerprint: device.fingerprint,
            os: device.os,
            type: device.type,
            browser: device.browser
        };
        return this.http.post(`${this.apiUrl}/complete`, payload);
    }

    setDynamicKey(data: any): Observable<any> {
        return this.http.post(`${this.apiUrl}/set-dynamic-key`, data);
    }
}
