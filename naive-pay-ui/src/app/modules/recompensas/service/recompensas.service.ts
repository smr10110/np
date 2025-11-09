import { Injectable, inject } from "@angular/core";
import { HttpClient, HttpHeaders } from "@angular/common/http";
import { Observable } from "rxjs";
import { DeviceFingerprintService } from "../../dispositivos/service/device-fingerprint.service";
export interface RewardAccountDTO {
    accountId: number;
    userId: number;
    points: number;
    description: string;
    lastUpdate: string;
}

export interface RewardTransactionDTO {
    id: number;
    userId: number;
    points: number;
    description: string;
    type: string;
    status: string;
    createdAt: string;
}

@Injectable({
    providedIn: 'root'
})
export class RecompensasService {
    private baseUrl = "http://localhost:8080/api/rewards";
    private deviceFp = inject(DeviceFingerprintService);
    constructor(private http: HttpClient) {}

    /** Obtiene el token actual y lo incluye en los encabezados */
    private getAuthHeaders(): HttpHeaders {
        const token = sessionStorage.getItem('token');
        let headers = new HttpHeaders({
            'Content-Type': 'application/json'
        });
        if (token) {
            headers = headers.set('Authorization', `Bearer ${token}`);
        }
        const fingerprint = this.deviceFp.get();
        if (fingerprint) {
            headers = headers.set('X-Device-Fingerprint', fingerprint);
        }

        return headers;
    }

    getAccount(userId: number): Observable<RewardAccountDTO> {
        return this.http.get<RewardAccountDTO>(
            `${this.baseUrl}/account/${userId}`,
            { headers: this.getAuthHeaders() }
        );
    }

    getHistory(userId: number): Observable<RewardTransactionDTO[]> {
        return this.http.get<RewardTransactionDTO[]>(
            `${this.baseUrl}/history/${userId}`,
            { headers: this.getAuthHeaders() }
        );
    }

    accumulatePoints(userId: number, points: number, description?: string): Observable<RewardTransactionDTO> {
        return this.http.post<RewardTransactionDTO>(
            `${this.baseUrl}/accumulate`,
            null,
            {
                headers: this.getAuthHeaders(),
                params: { userId, points, description: description || '' }
            }
        );
    }

    redeemPoints(userId: number, points: number, description?: string): Observable<RewardTransactionDTO> {
        return this.http.post<RewardTransactionDTO>(
            `${this.baseUrl}/redeem`,
            null,
            {
                headers: this.getAuthHeaders(),
                params: { userId, points, description: description || '' }
            }
        );
    }

    getPromotions(): Observable<any[]> {
        return this.http.get<any[]>(`${this.baseUrl}/promotions`, {
            headers: this.getAuthHeaders()
        });
    }
}
