export interface Transaction {
    id?: number;
    date: string;                 // p.ej. "2025-10-03T12:34:56" o "2025-10-03"
    amount: number;
    status?: string;              // APPROVED / DECLINED / etc.
    commerce?: string;            // nombre del comercio
    transactionType?: string;
    category?: string;
    description?: string;
}


export interface ReportFilterDTO {
    startDate?: string;           // ISO "YYYY-MM-DDTHH:mm:ss"
    endDate?: string;             // ISO "YYYY-MM-DDTHH:mm:ss"
    status?: string;
    commerce?: string;


    category?: string;
    type?: string;
    minAmount?: number;
    maxAmount?: number;


    limit?: number;
    offset?: number;
}


export interface SpendingReport {
    totalSpent: number;
    byCategory: Record<string, number>;
    byDate: Array<{
        period: string;
        total: number;
    }>;
}


export interface TransactionsResponse {
    items: Transaction[];
    total?: number;
}
