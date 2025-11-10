import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { CommonModule, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { DispositivosService } from '../../service/dispositivos.service';
import { DeviceLog } from '../../domain/dispositivos';

type DeviceAction = 'ALL' | 'LINK' | 'UPDATE' | 'REPLACED' | 'UNLINK';

@Component({
    selector: 'app-dispositivos',
    standalone: true,
    imports: [CommonModule, FormsModule, DatePipe],
    templateUrl: './dispositivos.component.html',
    styleUrls: ['./dispositivos.component.css']
})
export class DispositivosComponent implements OnInit {
    logs: DeviceLog[] = [];
    filteredLogs: DeviceLog[] = [];

    loading = false;
    errorMsg: string | null = null;
    successMsg: string | null = null;

    // filtros
    actionFilter: DeviceAction = 'ALL';
    dateFrom: string | null = null;

    private openIds = new Set<number>();

    constructor(
        private readonly dispositivos: DispositivosService,
        private readonly router: Router
    ) {}

    ngOnInit(): void {
        this.verHistorial();
    }

    verHistorial(): void {
        this.reload(() => {
            // abrir el más reciente
            if (this.logs.length && this.logs[0].id) {
                this.openIds.add(this.logs[0].id!);
            }
        });
    }

    reload(after?: () => void): void {
        this.errorMsg = null;
        this.dispositivos.logs().subscribe({
            next: (data) => {
                // ordenamos de más nuevo a más viejo
                this.logs = [...data].sort(
                    (a, b) =>
                        new Date(b.createdAt ?? 0).getTime() -
                        new Date(a.createdAt ?? 0).getTime()
                );
                this.applyFilters();
                after?.();
            },
            error: (err) => {
                this.logs = [];
                this.filteredLogs = [];
                this.errorMsg = this.mapDeviceError(err);
            }
        });
    }

    unlink(): void {
        if (this.loading) return;
        if (!confirm('Desvincular cerrará tu sesión en este navegador. ¿Continuar?')) return;

        this.loading = true;
        this.errorMsg = null;
        this.successMsg = null;

        this.dispositivos.unlink().subscribe({
            next: () => {
                sessionStorage.removeItem('token');
                localStorage.removeItem('token');
                this.successMsg = 'Dispositivo desvinculado. Redirigiendo al inicio de sesión…';
                this.loading = false;
                setTimeout(() => {
                    void this.router.navigate(['/auth/login'], {
                        queryParams: { reason: 'session_closed' }
                    });
                }, 700);
            },
            error: (err) => {
                this.loading = false;
                this.errorMsg = this.mapDeviceError(err) || 'No se pudo desvincular el dispositivo.';
            }
        });
    }

    // ---------- filtros ----------

    onActionFilterChange(val: DeviceAction) {
        this.actionFilter = val;
        this.applyFilters();
    }

    onDateFromChange(val: string) {
        this.dateFrom = val || null;
        this.applyFilters();
    }

    private applyFilters() {
        let result = [...this.logs];

        // por acción
        if (this.actionFilter !== 'ALL') {
            result = result.filter(l => l.action?.toUpperCase() === this.actionFilter);
        }

        // por fecha desde
        if (this.dateFrom) {
            const fromTs = new Date(this.dateFrom).getTime();
            result = result.filter(l => {
                if (!l.createdAt) return false;
                return new Date(l.createdAt).getTime() >= fromTs;
            });
        }

        this.filteredLogs = result;
    }

    // ---------- UI helpers ----------

    toggle(id: number): void {
        if (this.openIds.has(id)) this.openIds.delete(id);
        else this.openIds.add(id);
    }

    isOpen(id: number): boolean {
        return this.openIds.has(id);
    }

    trackByLogId = (_: number, log: DeviceLog) => log.id ?? log.createdAt;

    private mapDeviceError(err: any): string {
        const status = err?.status;
        const msg = err?.error?.message || err?.error?.error || '';
        if (status === 403 && msg === 'DEVICE_REQUIRED')
            return 'Este recurso requiere un dispositivo vinculado.';
        if (status === 403 && msg === 'DEVICE_UNAUTHORIZED')
            return 'Este dispositivo no está autorizado para tu cuenta.';
        return 'No se pudo cargar la actividad del dispositivo.';
    }
}
