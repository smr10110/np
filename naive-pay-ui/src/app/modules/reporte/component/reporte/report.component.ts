import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

import { ReportService } from '../../service/report.service';
import { ReportFilterDTO, SpendingReport, Transaction } from '../../domain/report';

import { AnalisisComponent } from '../analisis/analisis.component';
import { ReportTableComponent } from '../report-table/report-table.component';

type UiTxFilter = {
    from?: string;        // yyyy-MM-dd
    to?: string;          // yyyy-MM-dd
    description?: string;
    status?: string;
    commerce?: string;
    minAmount?: number;
    maxAmount?: number;
};

@Component({
    standalone: true,
    selector: 'app-report',
    templateUrl: './report.component.html',
    styleUrls: ['./report.component.css'],
    imports: [CommonModule, FormsModule, AnalisisComponent, ReportTableComponent],
})
export class ReportComponent implements OnInit {
    availableStatuses: string[] = ['', 'PENDING', 'COMPLETED', 'CANCELED', 'REJECTED'];
    filter: ReportFilterDTO = {};

    report?: SpendingReport;
    avg?: number;
    txs: Transaction[] = [];
    loading = false;
    error = '';

    activeTab: 'reports' | 'analysis' = 'reports';
    fromDate: string = '';
    toDate: string = '';

    userId: string = '';

    isAdminMode: boolean = false;

    // Pagination
    pageSize: number = 25;
    currentPage: number = 1;
    get totalPages(): number {
        return Math.ceil(this.filteredTxs.length / this.pageSize) || 1;
    }
    get pagedTxs(): Transaction[] {
        const start = (this.currentPage - 1) * this.pageSize;
        return this.filteredTxs.slice(start, start + this.pageSize);
    }
    goToPage(page: number) {
        if (page >= 1 && page <= this.totalPages) {
            this.currentPage = page;
        }
    }
    nextPage() {
        if (this.currentPage < this.totalPages) {
            this.currentPage++;
        }
    }
    prevPage() {
        if (this.currentPage > 1) {
            this.currentPage--;
        }
    }

    constructor(private reportService: ReportService) {}

    ngOnInit(): void {
        this.setDefaultDates();
        this.reloadData();
    }

    setDefaultDates() {
        const now = new Date();
        const firstDay = new Date(now.getFullYear(), now.getMonth(), 1);
        const lastDay = new Date(now.getFullYear(), now.getMonth() + 1, 0);
        this.fromDate = firstDay.toISOString().slice(0, 10);
        this.toDate = lastDay.toISOString().slice(0, 10);
    }

    reloadData(): void {
        this.txs = [];
        this.report = undefined;
        this.avg = undefined;
        this.error = ''; // Clear previous errors

        this.loadTransactions();
        // Load report and average only if not in admin mode
        // or if in admin mode but filtering is done on the backend
        if (!this.isAdminMode || !this.userId) {
            this.loadReport();
            this.loadMonthlyAverage();
        } else {
            // In admin mode, also load report and average with userId
            this.loadReport();
            this.loadMonthlyAverage();
        }
    }

    loadReport() {
        this.loading = true;
        this.error = '';
        this.reportService
            .buildSpending(this.filter, 'COMMERCE', 'DAY')
            .subscribe({
                next: (r) => {
                    this.report = r;
                    this.loading = false;
                },
                error: (e) => {
                    // Do not set error here, only log
                    console.error('Error loading report:', e);
                    this.loading = false;
                },
            });
    }

    loadMonthlyAverage() {
        this.loading = true;
        this.error = '';
        let filterToUse = { ...this.filter };
        this.reportService.avgMonthly(filterToUse).subscribe({
            next: (r) => {
                this.avg = r;
                this.loading = false;
            },
            error: (e) => {
                // Do not set error here, only log
                console.error('Error loading monthly average:', e);
                this.loading = false;
            },
        });
    }

    applyFilters(
        from: string, to: string, status: string, commerce: string,
        description: string, minAmount: string, maxAmount: string, userId?: string | number
    ) {
        const uiFilters: UiTxFilter = {
            from: from,
            to: to,
            status: status?.trim(),
            commerce: commerce?.trim(),
            description: description?.trim(),
            minAmount: minAmount ? parseFloat(minAmount) : undefined,
            maxAmount: maxAmount ? parseFloat(maxAmount) : undefined,
        };
        if (this.isAdminMode) {
            // In admin mode, always pass userId (can be undefined to see all)
            if (userId) {
                (uiFilters as any).userId = String(userId).trim();
                this.userId = String(userId).trim();
            } else {
                // In admin mode without userId, pass null to get all transactions
                (uiFilters as any).userId = null;
                this.userId = '';
            }
        } else {
            // Normal user mode, do not pass userId
            this.userId = '';
        }
        this.filter = this.applyUiFiltersToDto(uiFilters);
        this.reloadData();
    }

    resetFilters(from?: any, to?: any, status?: any, commerce?: any, description?: any, min?: any, max?: any, userIdInput?: any): void {
        this.filter = {
            startDate: '',
            endDate: '',
            status: '',
            commerce: '',
        };
        this.setDefaultDates();
        if (from) from.value = this.fromDate;
        if (to) to.value = this.toDate;
        if (status) status.value = '';
        if (commerce) commerce.value = '';
        if (description) description.value = '';
        if (min) min.value = '';
        if (max) max.value = '';
        if (userIdInput) userIdInput.value = '';
        this.userId = '';
        this.reloadData();
    }

    loadTransactions() {
        this.loading = true;
        this.error = '';

        this.reportService.listTransactions(this.filter).subscribe({
            next: (r) => {
                // In admin mode with userId, filtering is already done on the backend
                // Just store the results without additional frontend filtering
                this.txs = r.map(tx => ({ ...tx }));
                this.updateAvgMonthlyValue();
                this.loading = false;
            },
            error: (e) => {
                if (e?.status === 404) {
                    this.txs = [];
                    this.error = '';
                    this.updateAvgMonthlyValue();
                } else {
                    this.error = this.msg(e);
                    this.updateAvgMonthlyValue();
                }
                this.loading = false;
            },
        });
    }

    avgMonthly = { value: undefined as number | undefined };

    updateAvgMonthlyValue() {
        console.log('Filtered data:', JSON.stringify(this.filteredTxs, null, 2));
        const totalTxs = this.filteredTxs.length;
        const expenses = this.filteredTxs.filter(tx => tx.traAmount < 0);
        console.log('Detected expenses:', JSON.stringify(expenses, null, 2));
        const totalExpenses = expenses.length;
        console.log('Filtered transactions:', totalTxs, 'Found expenses:', totalExpenses);
        if (totalTxs === 0) {
            this.avgMonthly.value = undefined;
            console.log('Monthly average: no transactions');
            return;
        }
        if (totalExpenses === 0) {
            this.avgMonthly.value = undefined;
            console.log('Monthly average: no expenses');
            return;
        }
        // Group by month using traDateTime
        const months = new Set(expenses.map(tx => tx.traDateTime.substring(0,7))); // yyyy-MM
        const totalExpenseAmount = expenses.reduce((sum, tx) => sum + Math.abs(tx.traAmount), 0);
        this.avgMonthly.value = months.size > 0 ? totalExpenseAmount / months.size : undefined;
        console.log('Calculated monthly average:', this.avgMonthly.value, 'Months:', Array.from(months), 'Total expenses:', totalExpenseAmount);
    }

    private applyUiFiltersToDto(f?: UiTxFilter): ReportFilterDTO {
        const patch: any = {};
        if (f?.from) patch.startDate = this.asIsoStart(f.from);
        if (f?.to) patch.endDate = this.asIsoEnd(f.to);
        if (f?.description) patch.description = f.description;
        if (f?.status) patch.status = f.status;
        if (f?.commerce) patch.commerce = f.commerce;
        if (typeof f?.minAmount === 'number') patch.minAmount = f.minAmount;
        if (typeof f?.maxAmount === 'number') patch.maxAmount = f.maxAmount;
        // Only add userId if present in filter
        if ((f as any).userId) patch.userId = (f as any).userId;
        return patch as ReportFilterDTO;
    }

    private asIsoStart(yyyyMmDd: string) {
        return `${yyyyMmDd}T00:00:00`;
    }
    private asIsoEnd(yyyyMmDd: string) {
        return `${yyyyMmDd}T23:59:59`;
    }

    private msg(e: any) {
        return e?.error?.message || e?.message || 'Error';
    }

    downloadCsv() {
        this.reportService.downloadCsv(this.filter).subscribe({
            next: (blob) => {
                const url = window.URL.createObjectURL(blob);
                const a = document.createElement('a');
                a.href = url;
                a.download = 'transactions.csv';
                document.body.appendChild(a);
                a.click();
                document.body.removeChild(a);
                window.URL.revokeObjectURL(url);
            },
            error: (err) => {
                this.error = 'Error downloading CSV';
                console.error(err);
            },
        });
    }

    get filteredTxs(): Transaction[] {
        return this.txs;
    }
}
