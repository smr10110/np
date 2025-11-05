import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

import { ReporteService } from '../../service/reporte.service';
import { ReportFilterDTO, SpendingReport, Transaction } from '../../domain/reporte';

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
    selector: 'app-reporte',
    templateUrl: './reporte.component.html',
    styleUrls: ['./reporte.component.css'],
    imports: [CommonModule, FormsModule, AnalisisComponent, ReportTableComponent],
})
export class ReporteComponent implements OnInit {
    availableStatuses = ['', 'PENDING', 'APPROVED', 'CANCELED', 'REJECTED'];
    filter: ReportFilterDTO = {};

    report?: SpendingReport;
    avg?: number;
    txs: Transaction[] = [];
    loading = false;
    error = '';

    activeTab: 'reportes' | 'analisis' = 'reportes';
    fromDate: string = '';
    toDate: string = '';

    userId: string = '';

    isAdminMode: boolean = false;

    // Paginación
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

    constructor(private reporteService: ReporteService) {}

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

        this.loadTransactions();
        this.loadReport();
        this.loadMonthlyAverage();
    }

    loadReport() {
        this.loading = true;
        this.error = '';
        this.reporteService
            .buildSpending(this.filter, 'COMMERCE', 'DAY')
            .subscribe({
                next: (r) => {
                    this.report = r;
                    this.loading = false;
                },
                error: (e) => {
                    this.error = this.msg(e);
                    this.loading = false;
                },
            });
    }

    loadMonthlyAverage() {
        this.loading = true;
        this.error = '';
        let filterToUse = { ...this.filter };
        if (this.isAdminMode && this.userId) {
            (filterToUse as any).userId = this.userId.trim();
        }
        this.reporteService.avgMonthly(filterToUse).subscribe({
            next: (r) => {
                this.avg = r;
                this.loading = false;
            },
            error: (e) => {
                this.error = this.msg(e);
                this.loading = false;
            },
        });
    }

    applyFilters(
        from: string, to: string, status: string, commerce: string,
        description: string, minAmount: string, maxAmount: string, userId?: string
    ) {
        const uiFilters: UiTxFilter = {
            from: from,
            to: to,
            status: status.trim(),
            commerce: commerce.trim(),
            description: description.trim(),
            minAmount: minAmount ? parseFloat(minAmount) : undefined,
            maxAmount: maxAmount ? parseFloat(maxAmount) : undefined,
        };
        if (this.isAdminMode && userId) {
            (uiFilters as any).userId = userId.trim();
            this.userId = userId.trim();
        } else {
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

        this.reporteService.listTransactions(this.filter).subscribe({
            next: (r) => {
                if (this.isAdminMode && this.userId) {
                    const userIdStr = this.userId.toString().trim().toLowerCase();
                    this.txs = r
                        .filter(tx => {
                            const origin = tx.originAccount?.toString().trim().toLowerCase();
                            const dest = tx.destinationAccount?.toString().trim().toLowerCase();
                            // Solo incluir si userId es origen o destino, pero no ambos
                            return (origin === userIdStr && dest !== userIdStr) || (dest === userIdStr && origin !== userIdStr);
                        })
                        .map(tx => {
                            const origin = tx.originAccount?.toString().trim().toLowerCase();
                            const dest = tx.destinationAccount?.toString().trim().toLowerCase();
                            let type: 'ingreso' | 'gasto' = 'gasto';
                            if (dest === userIdStr && origin !== userIdStr) type = 'ingreso';
                            if (origin === userIdStr && dest !== userIdStr) type = 'gasto';
                            return { ...tx, type };
                        });
                    this.updateAvgMonthlyValue();
                } else {
                    this.txs = r.map(tx => ({ ...tx }));
                    this.updateAvgMonthlyValue();
                }
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
        console.log('Datos filtrados:', JSON.stringify(this.filteredTxs, null, 2));
        const totalTxs = this.filteredTxs.length;
        const gastos = this.filteredTxs.filter(tx => tx.amount < 0);
        console.log('Gastos detectados:', JSON.stringify(gastos, null, 2));
        const totalGastos = gastos.length;
        console.log('Transacciones filtradas:', totalTxs, 'Gastos encontrados:', totalGastos);
        if (totalTxs === 0) {
            this.avgMonthly.value = undefined;
            console.log('Promedio mensual: sin transacciones');
            return;
        }
        if (totalGastos === 0) {
            this.avgMonthly.value = undefined;
            console.log('Promedio mensual: sin gastos');
            return;
        }
        // Agrupar por mes usando createdAt
        const meses = new Set(gastos.map(tx => tx.createdAt.substring(0,7))); // yyyy-MM
        const sumaGastos = gastos.reduce((sum, tx) => sum + Math.abs(tx.amount), 0);
        this.avgMonthly.value = meses.size > 0 ? sumaGastos / meses.size : undefined;
        console.log('Promedio mensual calculado:', this.avgMonthly.value, 'Meses:', Array.from(meses), 'Total gastos:', sumaGastos);
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
        if (this.isAdminMode && (f as any).userId) patch.userId = (f as any).userId;
        return patch as ReportFilterDTO;
    }

    private asIsoStart(yyyyMmDd: string) {
        return `${yyyyMmDd}T00:00:00`;
    }
    private asIsoEnd(yyyyMmDd: string) {
        return `${yyyyMmDd}T23:59:59`;
    }

    keys(obj: Record<string, number> | undefined | null) {
        return Object.keys(obj ?? {});
    }

    private msg(e: any) {
        return e?.error?.message || e?.message || 'Error';
    }

    downloadCsv() {
        this.reporteService.downloadCsv(this.filter).subscribe({
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
                this.error = 'Error descargando CSV';
                console.error(err);
            },
        });
    }

    get filteredReport(): SpendingReport | undefined {
        return this.report;
    }

    get filteredTxs(): Transaction[] {
        return this.txs;
    }
}
