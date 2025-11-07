/**
 * Interfaces para el módulo de Fondos
 * Define los tipos de datos que retorna la API de fondos
 */

export interface AccountBalanceResponse {
    accountId: number
    userId: number
    availableBalance: number
    lastUpdate: string // ISO 8601 datetime
}

export interface TransactionResponse {
    id: number
    amount: number
    dateTime: string // ISO 8601 datetime
    description: string
    originAccountId: number
    destinationAccountId: number
}

export interface TransferResponse {
    success: boolean
    message: string
    transactionId: number | null
}
