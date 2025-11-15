export interface ReportFilterDTO {
    startDate?: string;
    endDate?: string;
    status?: string;
    commerce?: string;
    description?: string;
    minAmount?: number;
    maxAmount?: number;
    userId?: string | number;
}

export interface SpendingReport {
    totalSpent: number;
    byCategory: Record<string, number>;
    byDate: { period: string; total: number }[];
}

export interface Transaction {
    traId: number;
    traAmount: number;
    traDateTime: string;
    traDescription: string;
    traType: string;
    traCustomerName?: string;
    traCommerceName?: string;
    traPaymentCategory?: string;
    traStatus?: string;
    accIdOriginId?: number;
    accIdDestinationId?: number;
    originUserName?: string;
    destinationUserName?: string;
}