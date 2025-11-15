import { Injectable, inject } from "@angular/core"
import { HttpClient } from "@angular/common/http"
import type { Observable } from "rxjs"
import type { AccountBalanceResponse, TransactionResponse, TransferResponse } from "../domain/fondos.interface"

/**
 * Servicio para gestionar operaciones de fondos
 * Maneja la comunicación con la API de fondos del backend
 * 
 * IMPORTANTE: Las operaciones requieren autenticación JWT.
 * El userId se obtiene automáticamente del token en el backend.
 */
@Injectable({
    providedIn: "root",
})
export class FondosService {
    private http = inject(HttpClient)
    private baseUrl = "http://localhost:8080/api/funds"

    /**
     * Obtiene el saldo disponible de la cuenta del usuario autenticado
     * El userId se extrae del token JWT en el backend
     * @returns Observable con los datos del saldo
     */
    getBalance(): Observable<AccountBalanceResponse> {
        return this.http.get<AccountBalanceResponse>(`${this.baseUrl}/accounts/balance`)
    }

    /**
     * Agrega fondos a la cuenta del usuario autenticado
     * El userId se extrae del token JWT en el backend
     * @param amount Monto a cargar (debe ser > 0)
     * @returns Observable con la respuesta de la transacción
     */
    addFunds(amount: number): Observable<TransferResponse> {
        return this.http.post<TransferResponse>(`${this.baseUrl}/transactions/add-funds?amount=${amount}`, null)
    }

    /**
     * Obtiene el historial de transacciones del usuario autenticado
     * El userId se extrae del token JWT en el backend
     * @returns Observable con el array de transacciones
     */
    getTransactionHistory(): Observable<TransactionResponse[]> {
        return this.http.get<TransactionResponse[]>(`${this.baseUrl}/transactions/history`)
    }
}
