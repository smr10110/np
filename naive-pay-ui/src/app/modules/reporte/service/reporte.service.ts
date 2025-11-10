import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { ReportFilterDTO, SpendingReport, Transaction } from '../domain/reporte';

@Injectable({ providedIn: 'root' })
export class ReporteService {
    private baseReports  = 'http://localhost:8080/api/reports';
    private baseSpending = `${this.baseReports}/spending`;
    private baseUsage    = `${this.baseReports}/usage`;

    constructor(private http: HttpClient) {}

    private asIsoStart(d?: string): string | undefined {
        if (!d) return undefined;
        return d.length === 10 ? `${d}T00:00:00` : d;
    }
    private asIsoEnd(d?: string): string | undefined {
        if (!d) return undefined;
        return d.length === 10 ? `${d}T23:59:59` : d;
    }

    private cleanFilter(filter: Partial<ReportFilterDTO>): any {
        const out: any = {};
        Object.keys(filter || {}).forEach(k => {
            const v = (filter as any)[k];
            if (v !== '' && v !== null && v !== undefined) out[k] = v;
        });

        if ('startDate' in out) out.startDate = this.asIsoStart(out.startDate);
        if ('endDate'   in out) out.endDate   = this.asIsoEnd(out.endDate);
        return out;
    }

    buildSpending(
        filter: ReportFilterDTO,
        groupBy: 'COMMERCE' | 'TRANSACTION_TYPE' = 'COMMERCE',
        granularity: 'DAY' | 'MONTH' = 'DAY'
    ) {
        const body = this.cleanFilter(filter);
        return this.http.post<SpendingReport>(
            `${this.baseSpending}?groupBy=${groupBy}&granularity=${granularity}`,
            body
        );
    }

    avgMonthly(filter: ReportFilterDTO) {
        const body = this.cleanFilter(filter);
        return this.http.post<number>(
            `${this.baseUsage}/avg-monthly`,
            body
        );
    }

    listTransactions(filter: Partial<ReportFilterDTO>) {
        const body = this.cleanFilter(filter);
        return this.http.post<Transaction[]>(
            `${this.baseReports}/transactions`,
            body
        );
    }

    downloadCsv(filter: Partial<ReportFilterDTO>) {
        const body = this.cleanFilter(filter);
        return this.http.post(
            `${this.baseReports}/export/csv`,
            body,
            {responseType: 'blob'}
        );
    }
}
