import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Pagos, PendingTransactionDTO } from '../domain/pagos';

@Injectable({
  providedIn: 'root'
})
export class PagosService {
  private apiUrl = 'http://localhost:8080/api/payments';

  constructor(private http: HttpClient) { }

  // Get all pending transactions
  getPendingTransactions(): Observable<PendingTransactionDTO[]> {
    return this.http.get<PendingTransactionDTO[]>(`${this.apiUrl}/pending`);
  }

  // Get transaction by ID
  getTransactionById(id: number): Observable<Pagos> {
    return this.http.get<Pagos>(`${this.apiUrl}/${id}`);
  }

  // Approve transaction
  approveTransaction(id: number): Observable<Pagos> {
    return this.http.put<Pagos>(`${this.apiUrl}/${id}/approve`, {});
  }

  // Cancel transaction
  cancelTransaction(id: number): Observable<Pagos> {
    return this.http.put<Pagos>(`${this.apiUrl}/${id}/cancel`, {});
  }
}