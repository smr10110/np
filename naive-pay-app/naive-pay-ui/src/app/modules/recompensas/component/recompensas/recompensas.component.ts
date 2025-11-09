import { Component, OnInit } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { RecompensasService, RewardAccountDTO, RewardTransactionDTO } from "../../service/recompensas.service";
import { Router } from "@angular/router";

@Component({
    selector: 'app-recompensas',
    standalone: true,
    imports: [CommonModule, DatePipe],
    templateUrl: './recompensas.component.html',
    styleUrls: ['./recompensas.component.css']
})
export class RecompensasComponent implements OnInit {
    userId: number | null = null;
    account?: RewardAccountDTO;
    history: RewardTransactionDTO[] = [];
    promotions: string[] = [];
    loading = false;
    expiringPoints = 0;
    daysToExpire = 7;
    showPromotions = false;
    errorMessage: string | null = null;

    constructor(
        private recompensasService: RecompensasService,
        private router: Router
    ) {}

    goToCanje() {
        this.router.navigate(['/recompensas/canje']);
    }

    goToPromos() {
        this.router.navigate(['/recompensas/promociones']);
    }

    ngOnInit(): void {
        // 🔹 Obtener ID de usuario autenticado desde el token JWT en sessionStorage
        const token = sessionStorage.getItem('token');
        if (token) {
            try {
                const payload = JSON.parse(atob(token.split('.')[1]));
                this.userId = payload.id || payload.userId || payload.sub;
                console.log("Usuario autenticado con ID:", this.userId);

                if (this.userId) {
                    this.loadData();
                }
            } catch (e) {
                console.error("Error al decodificar el token:", e);
                this.handleAuthError();
            }
        } else {
            console.warn("No se encontró token de autenticación.");
            this.handleAuthError();
        }
    }

    loadData() {
        this.loading = true;
        this.errorMessage = null;

        if (!this.userId) {
            this.errorMessage = "No se pudo identificar al usuario";
            this.loading = false;
            return;
        }

        // Cargar cuenta de recompensas
        this.recompensasService.getAccount(this.userId).subscribe({
            next: acc => {
                this.account = acc;
                this.expiringPoints = Math.floor(acc.points * 0.1);
            },
            error: err => this.handleApiError(err)
        });

        // Cargar historial
        this.recompensasService.getHistory(this.userId).subscribe({
            next: hist => this.history = hist,
            error: err => this.handleApiError(err)
        });

        // Cargar promociones
        this.recompensasService.getPromotions().subscribe({
            next: promos => this.promotions = promos,
            error: err => this.handleApiError(err),
            complete: () => this.loading = false
        });
    }

    redeem(points: number) {
        if (!this.userId) {
            this.errorMessage = "Usuario no identificado";
            return;
        }

        if (!this.account || this.account.points < points) {
            alert("No tienes suficientes puntos para canjear");
            return;
        }

        this.recompensasService.redeemPoints(this.userId, points).subscribe({
            next: tx => {
                alert(`Canje exitoso: ${tx.points} puntos`);
                this.loadData(); // Recargar datos
            },
            error: err => this.handleApiError(err)
        });
    }

    private handleApiError(error: any) {
        console.error('Error en API:', error);

        const errorCode = error?.error?.error;

        // Manejar diferentes tipos de errores
        if (error.status === 401) {
            if (errorCode === 'TOKEN_EXPIRED' || errorCode === 'TOKEN_INVALID') {
                this.errorMessage = "Tu sesión ha expirado";
                this.redirectToLogin();
            } else {
                this.errorMessage = "No autorizado";
                this.redirectToLogin();
            }
        } else if (error.status === 403) {
            if (errorCode === 'DEVICE_UNAUTHORIZED' || errorCode === 'DEVICE_REQUIRED') {
                this.errorMessage = "Dispositivo no autorizado";
                this.redirectToDeviceRecovery();
            } else {
                this.errorMessage = "Acceso denegado";
            }
        } else if (error.status === 404) {
            this.errorMessage = "Recurso no encontrado";
        } else {
            this.errorMessage = "Error al cargar los datos. Intenta nuevamente.";
        }

        this.loading = false;
    }

    private handleAuthError() {
        this.errorMessage = "Debes iniciar sesión para ver tus recompensas";
        setTimeout(() => {
            this.redirectToLogin();
        }, 2000);
    }

    private redirectToLogin() {
        this.router.navigate(['/auth/login'], {
            queryParams: { reason: 'session_closed' }
        });
    }

    private redirectToDeviceRecovery() {
        // Obtener el identificador del usuario para pre-cargar en el recovery
        const token = sessionStorage.getItem('token');
        let identifier = '';

        if (token) {
            try {
                const payload = JSON.parse(atob(token.split('.')[1]));
                identifier = payload.email || payload.identifier || '';
            } catch (e) {
                console.error('Error al obtener identifier del token:', e);
            }
        }

        this.router.navigate(['/auth/recover/device'], {
            queryParams: identifier ? { id: identifier } : {}
        });
    }

    // Método para reintentar la carga
    retry() {
        this.errorMessage = null;
        this.loadData();
    }

    // Método para cerrar sesión
    logout() {
        sessionStorage.removeItem('token');
        this.redirectToLogin();
    }
}