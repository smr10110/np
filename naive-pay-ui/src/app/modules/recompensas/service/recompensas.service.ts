import { Injectable, inject } from "@angular/core";
import { HttpClient, HttpHeaders } from "@angular/common/http";
import { Observable } from "rxjs";
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

    constructor(private http: HttpClient) {}

    private getAuthHeaders(): HttpHeaders {
        const token = sessionStorage.getItem('token');
        let headers = new HttpHeaders ({
            'Content-Type' : 'aplication/json'
        });
        if (token) {
            headers = headers.set('Authorization', `Bearer ${token}`);
        }
        return headers;
    }

    getAccount(): Observable<RewardAccountDTO> {
        return this.http.get<RewardAccountDTO>(
            `${this.baseUrl}/account`,
            { headers: this.getAuthHeaders() }
        );
    }

    getHistory(): Observable<RewardTransactionDTO[]> {
        return this.http.get<RewardTransactionDTO[]>(
            `${this.baseUrl}/history`,
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

    redeemPoints(points: number, description?: string): Observable<RewardTransactionDTO> {
        return this.http.post<RewardTransactionDTO>(
            `${this.baseUrl}/redeem`,
            null,
            {
                headers: this.getAuthHeaders(),
                params: { points, description: description || '' }
            }
        );
    }

    getPromotions(): Observable<any[]> {
        return this.http.get<any[]>(`${this.baseUrl}/promotions`, {
            headers: this.getAuthHeaders()
        });
    }
}