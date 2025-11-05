import { Component, Input, Output, EventEmitter, inject } from "@angular/core"
import { CommonModule } from "@angular/common"
import { ReactiveFormsModule, FormBuilder, Validators } from "@angular/forms"
import { ModalComponent } from "@shared/components/ui/modal/modal.component"
import { ButtonComponent } from "@shared/components/ui/button/button.component"
import { FondosService } from "../../service/fondos.service"

/**
 * Componente modal para cargar fondos a la cuenta del usuario autenticado
 * El userId se obtiene automáticamente del token JWT en el backend
 */
@Component({
    selector: "app-add-funds-modal",
    standalone: true,
    imports: [CommonModule, ReactiveFormsModule, ModalComponent, ButtonComponent],
    templateUrl: "./add-funds-modal.component.html",
    styleUrls: ["./add-funds-modal.component.css"],
})
export class AddFundsModalComponent {
    @Input() isOpen = false
    @Output() close = new EventEmitter<void>()
    @Output() fundsAdded = new EventEmitter<void>()

    private fondosService = inject(FondosService)
    private fb = inject(FormBuilder)

    isLoading = false
    errorMessage = ""
    successMessage = ""

    // Formulario reactivo con validaciones
    addFundsForm = this.fb.group({
        amount: [null as number | null, [Validators.required, Validators.min(1)]],
    })

    /**
     * Cierra el modal y resetea el formulario
     */
    onClose(): void {
        this.addFundsForm.reset()
        this.errorMessage = ""
        this.successMessage = ""
        this.close.emit()
    }

    /**
     * Maneja el envío del formulario
     * Valida y envía la petición para cargar fondos
     */
    onSubmit(): void {
        // Validar formulario
        if (this.addFundsForm.invalid) {
            this.addFundsForm.markAllAsTouched()
            return
        }

        this.isLoading = true
        this.errorMessage = ""
        this.successMessage = ""

        const amount = this.addFundsForm.value.amount!

        // Llamar al servicio para cargar fondos
        // El userId se obtiene automáticamente del token JWT en el backend
        this.fondosService.addFunds(amount).subscribe({
            next: (response) => {
                this.isLoading = false
                if (response.success) {
                    this.successMessage = response.message
                    // Emitir evento de éxito después de un breve delay
                    setTimeout(() => {
                        this.fundsAdded.emit()
                        this.onClose()
                    }, 1000)
                } else {
                    this.errorMessage = response.message
                }
            },
            error: (error) => {
                console.error("Error al cargar fondos:", error)
                this.errorMessage = error.error?.message || "Error al cargar fondos. Intente nuevamente."
                this.isLoading = false
            },
        })
    }

    /**
     * Verifica si un campo del formulario es inválido y ha sido tocado
     * @param fieldName Nombre del campo a validar
     * @returns true si el campo es inválido y ha sido tocado
     */
    isFieldInvalid(fieldName: string): boolean {
        const field = this.addFundsForm.get(fieldName)
        return !!(field && field.invalid && field.touched)
    }

    /**
     * Obtiene el mensaje de error para un campo específico
     * @param fieldName Nombre del campo
     * @returns Mensaje de error correspondiente
     */
    getErrorMessage(fieldName: string): string {
        const field = this.addFundsForm.get(fieldName)
        if (field?.hasError("required")) {
            return "Este campo es requerido"
        }
        if (field?.hasError("min")) {
            return "El monto debe ser mayor a 0"
        }
        return ""
    }
}