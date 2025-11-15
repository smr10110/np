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
        this.loadData();
    }

    loadData() {
        this.loading = true;
        this.errorMessage = null;

        // Cargar cuenta de recompensas
        this.recompensasService.getAccount().subscribe({
            next: acc => {
                this.account = acc;
                this.expiringPoints = Math.floor(acc.points * 0.1);
            },
            error: err => this.handleApiError(err)
        });

        // Cargar historial
        this.recompensasService.getHistory().subscribe({
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

        if (!this.account || this.account.points < points) {
            alert("No tienes suficientes puntos para canjear");
            return;
        }

        this.recompensasService.redeemPoints(points).subscribe({
            next: tx => {
                alert(`Canje exitoso: ${tx.points} puntos`);
                this.loadData(); // Recargar datos
            },
            error: err => this.handleApiError(err)
        });
    }

    private handleApiError(error: any) {
        console.error('Error en API:', error);
        this.errorMessage = "Error al cargar los datos. Intenta nuevamente.";
        this.loading = false;
    }

    // Método para reintentar la carga
    retry() {
        this.errorMessage = null;
        this.loadData();
    }

    translateType(type: string): string {
        switch(type){
            case "ACCUMULATE":
                return "Acumulación";
            case "REDEEM":
                return "Canje";
            default:
                return type;
        }
    }

    translateState(state: string): string {
        switch(state) {
            case "PENDING":
                return "Pendiente";
            case "APPROVED":
                return "Aprobado";
            case "REJECTED":
                return "Rechazado";
            case "CANCELED":
                return "Cancelado";
            case "COMPLETED":
                return "Completado";
            default:
                return state;
        }
    }
}