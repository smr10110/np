import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { RecompensasService } from '../../service/recompensas.service';

@Component({
    selector: 'app-canje-puntos',
    standalone: true,
    imports: [CommonModule, FormsModule],
    templateUrl: './canje-puntos.component.html',
    styleUrls: ['./canje-puntos.component.css']
})
export class CanjePuntosComponent {
    userId: number | null = null;
    account: any = null;
    exchangeType = '';
    exchangePoints = 0;
    resume: any = null;
    loading = false;        // para mostrar spinner/estado
    processing = false;     // para deshabilitar botones mientras se procesa

    constructor(
        private recompensasService: RecompensasService,
        private router: Router
    ) {}

    ngOnInit() {
        // obtener userId desde token (igual que en RecompensasComponent)
        const token = localStorage.getItem('token');
        if (token) {
            try {
                const payload = JSON.parse(atob(token.split('.')[1]));
                this.userId = Number(payload.id || payload.userId || payload.sub) || null;
            } catch (e) {
                console.error('Error al decodificar token:', e);
            }
        }

        if (!this.userId) {
            // fallback: si no hay token redirigir a login o usar 1 por defecto
            console.warn('No se encontró userId en token - redirigiendo a login');
            this.router.navigate(['/auth/login']);
            return;
        }

        this.loadAccount();
    }

    loadAccount() {
        if (!this.userId) return;
        this.loading = true;
        this.recompensasService.getAccount(this.userId).subscribe({
            next: acc => {
                this.account = acc;
                this.loading = false;
            },
            error: err => {
                console.error('Error al cargar cuenta:', err);
                this.loading = false;
            }
        });
    }

    cancel() {
        this.router.navigate(['/recompensas']);
    }

    confirmExchange() {
        if (!this.exchangeType || this.exchangePoints <= 0) {
            alert('Completa todos los campos antes de confirmar');
            return;
        }
        if (!this.account) {
            alert('Cuenta no cargada');
            return;
        }
        if (this.exchangePoints > this.account.points) {
            alert('No tienes suficientes puntos');
            return;
        }
        if (!this.userId) {
            alert('Usuario no autenticado');
            return;
        }

        this.processing = true;
        // Llamada real al backend para ejecutar el canje
        this.recompensasService.redeemPoints(this.userId, this.exchangePoints, this.exchangeType).subscribe({
            next: tx => {
                // tx contiene la transaccion retornada por el backend
                this.resume = {
                    type: this.exchangeType,
                    points: this.exchangePoints,
                    balance: (this.account.points ?? 0) - this.exchangePoints,
                    state: tx?.status || 'COMPLETED',
                    transaction: tx
                };
                // recargar cuenta para asegurar consistencia
                this.recompensasService.getAccount(this.userId!).subscribe({
                    next: acc => this.account = acc,
                    error: err => console.error(err)
                });
                this.processing = false;
            },
            error: err => {
                console.error('Error al procesar canje:', err);
                alert('Error al procesar el canje. Revisa la consola.');
                this.processing = false;
            }
        });
    }

    return() {
        this.router.navigate(['/recompensas']);
    }
}
