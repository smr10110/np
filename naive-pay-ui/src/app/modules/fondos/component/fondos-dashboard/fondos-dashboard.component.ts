import { Component, type OnInit, inject } from "@angular/core"
import { CommonModule } from "@angular/common"
import { ButtonComponent } from "@shared/components/ui/button/button.component"
import { BadgeComponent } from "@shared/components/ui/badge/badge.component"
import { AddFundsModalComponent } from "../add-funds-modal/add-funds-modal.component"
import { FondosService } from "../../service/fondos.service"
import type { TransactionResponse } from "../../domain/fondos.interface"

/**
 * Componente principal del dashboard de fondos
 * Muestra el saldo disponible y el historial de transacciones del usuario autenticado
 * 
 * NOTA: El userId real se obtiene del token JWT en el backend.
 * El interceptor HTTP automáticamente agrega el header Authorization con el token.
 */
@Component({
    selector: "app-fondos-dashboard",
    standalone: true,
    imports: [CommonModule, ButtonComponent, BadgeComponent, AddFundsModalComponent],
    templateUrl: "./fondos-dashboard.component.html",
    styleUrls: ["./fondos-dashboard.component.css"],
})
export class FondosDashboardComponent implements OnInit {
    private fondosService = inject(FondosService)

    // accountId para identificar transacciones en la UI (se obtendrá del balance)
    accountId: number | null = null

    // Estado del componente
    balance = 0
    lastUpdate = new Date()
    transactions: TransactionResponse[] = []
    isModalOpen = false
    isLoading = false
    errorMessage = ""

    ngOnInit(): void {
        this.loadData()
    }

    /**
     * Carga el saldo y el historial de transacciones del usuario autenticado
     * El userId se obtiene automáticamente del token JWT en el backend
     */
    loadData(): void {
        this.isLoading = true
        this.errorMessage = ""
        
        // Cargar saldo
        this.fondosService.getBalance().subscribe({
            next: (data) => {
                this.balance = data.availableBalance
                this.lastUpdate = new Date(data.lastUpdate)
                this.accountId = data.accountId // Guardar accountId para identificar transacciones
            },
            error: (error) => {
                console.error("Error al cargar saldo:", error)
                this.errorMessage = "Error al cargar el saldo. Intente nuevamente."
            },
        })
        
        // Cargar historial de transacciones
        this.fondosService.getTransactionHistory().subscribe({
            next: (data) => {
                this.transactions = data.sort((a, b) => new Date(b.dateTime).getTime() - new Date(a.dateTime).getTime())
                this.isLoading = false
            },
            error: (error) => {
                console.error("Error al cargar transacciones:", error)
                this.errorMessage = "Error al cargar las transacciones. Intente nuevamente."
                this.isLoading = false
            },
        })
    }

    /**
     * Abre el modal para cargar fondos
     */
    openAddFundsModal(): void {
        this.isModalOpen = true
    }

    /**
     * Cierra el modal de carga de fondos
     */
    closeModal(): void {
        this.isModalOpen = false
    }

    /**
     * Maneja el evento cuando se cargan fondos exitosamente
     * Recarga los datos y cierra el modal
     */
    onFundsAdded(): void {
        this.loadData()
        this.closeModal()
    }

    /**
     * Determina si una transacción es ingreso o egreso
     * @param tx Transacción a evaluar
     * @returns 'ingreso' si el destino es la cuenta del usuario, 'egreso' en caso contrario
     */
    getTransactionType(tx: TransactionResponse): "ingreso" | "egreso" {
        return tx.destinationAccountId === this.accountId ? "ingreso" : "egreso"
    }

    /**
     * Obtiene el color del badge según el tipo de transacción
     * @param tx Transacción a evaluar
     * @returns 'success' para ingresos, 'error' para egresos
     */
    getBadgeColor(tx: TransactionResponse): "success" | "error" {
        return this.getTransactionType(tx) === "ingreso" ? "success" : "error"
    }

    /**
     * Obtiene las clases CSS para el monto según el tipo de transacción
     * @param tx Transacción a evaluar
     * @returns Clases CSS para colorear el monto
     */
    getAmountClass(tx: TransactionResponse): string {
        return this.getTransactionType(tx) === "ingreso"
            ? "text-green-600 dark:text-green-500"
            : "text-red-600 dark:text-red-500"
    }

    /**
     * Formatea el monto con signo + o - según el tipo de transacción
     * @param tx Transacción a evaluar
     * @returns Signo '+' para ingresos, '-' para egresos
     */
    getAmountSign(tx: TransactionResponse): string {
        return this.getTransactionType(tx) === "ingreso" ? "+" : "-"
    }
}
