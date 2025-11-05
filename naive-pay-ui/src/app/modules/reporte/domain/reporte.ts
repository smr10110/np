import { HttpParams } from "@angular/common/http";

export interface ReportFilterDTO {
    startDate?: string;
    endDate?: string;
    status?: string;
    commerce?: string;
    description?: string;
    minAmount?: number;
    maxAmount?: number;
}

export interface SpendingReport {
    totalSpent: number;
    byCategory: Record<string, number>;
    byDate: { period: string; total: number }[];
}

export interface Transaction {
    id: number;
    originAccount: string;
    destinationAccount: string;
    createdAt: string;
    amount: number;
    status: string;
    commerce: string;
    category?: string;
}

export function cleanFilter(filters: ReportFilterDTO): ReportFilterDTO {
    const cleaned: any = {};
    for (const key in filters) {
        if (filters.hasOwnProperty(key)) {
            const value = (filters as any)[key];
            if (value !== '' && value !== null && value !== undefined) {
                cleaned[key] = value;
            }
        }
    }
    return cleaned as ReportFilterDTO;
}

export function filtersToParams(filters: ReportFilterDTO): HttpParams {
    let params = new HttpParams();
    const cleanedFilters = cleanFilter(filters) as Record<string, any>;

    for (const key in cleanedFilters) {
        if (cleanedFilters.hasOwnProperty(key)) {
            const value = cleanedFilters[key];
            params = params.set(key, value.toString());
        }
    }
    return params;
}