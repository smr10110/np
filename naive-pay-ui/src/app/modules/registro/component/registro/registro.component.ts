import { Component } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { RegistroService } from '../../service/registro.service';
import { RegisterFormService } from '../../service/register-form.service';
import { finalize } from 'rxjs/operators';

@Component({
    selector: 'app-registro',
    standalone: true,
    imports: [
        CommonModule,
        ReactiveFormsModule,
        RouterLink
    ],
    templateUrl: './registro.component.html',
    styleUrls: ['./registro.component.css']
})
export class RegistroComponent {

    // Variable para manejar el estado de carga
    isLoading = false;
    // Se crean los pasos del registro
    currentStep: 'start' | 'verify' | 'complete' | 'setKey' | 'success' = 'start';

    startForm: FormGroup;
    verifyForm: FormGroup;
    completeForm: FormGroup;
    setKeyForm: FormGroup;

    errorMessage: string | null = null;
    userEmail: string = '';

    constructor(private registroService: RegistroService,
                private formService: RegisterFormService) {

        this.startForm = this.formService.createStartForm();
        this.verifyForm = this.formService.createVerifyForm();
        this.completeForm = this.formService.createCompleteForm();
        this.setKeyForm = this.formService.createSetKeyForm();
    }

    onStartSubmit(): void {
        if (this.startForm.invalid) return;

        this.isLoading = true;
        this.errorMessage = null;

        this.userEmail = this.startForm.value.email;
        this.registroService.startRegistration(this.startForm.value).pipe(
            finalize(() => this.isLoading = false)
        ).subscribe({
            next: () => {
                this.currentStep = 'verify';
            },
            error: (err) => {
                this.errorMessage = err.error?.error || 'Ocurrió un error al iniciar el registro.';
            }
        });
    }

    onVerifySubmit(): void {
        if (this.verifyForm.invalid) return;
        const { code } = this.verifyForm.value;
        this.registroService.verifyEmail(this.userEmail, code).subscribe({
            next: () => {
                this.currentStep = 'complete';
                this.errorMessage = null;
            },
            error: (err) => this.errorMessage = err.error?.error || 'Código de verificación incorrecto.'
        });
    }

    resendVerificationCode(): void {
        const email = this.startForm.get('email')?.value;
        if (!email) {
            // Manejar el caso en que el email no está disponible
            console.error('Email no encontrado para reenviar el código.');
            return;
        }

        this.registroService.resendVerificationCode(email).subscribe({
            next: (response) => {
                console.log(response.message);
                // Opcional: Mostrar un mensaje de éxito al usuario (ej. con un toast)
                alert('Se ha reenviado un nuevo código a tu correo.');
            },
            error: (err) => {
                console.error('Error al reenviar el código:', err);
                // Opcional: Mostrar un mensaje de error al usuario
                alert(err.error.error || 'Ocurrió un error al reenviar el código.');
            }
        });
    }

    onCompleteSubmit(): void {
        if (this.completeForm.invalid) return;
        const completeData = { email: this.userEmail, ...this.completeForm.value };
        this.registroService.completeRegistration(completeData).subscribe({
            next: () => {
                this.currentStep = 'setKey';
                this.errorMessage = null;
            },
            error: (err) => this.errorMessage = err.error?.error || 'Error al completar el registro.'
        });
    }

    onSetKeySubmit(): void {
        if (this.setKeyForm.invalid) return;
        const keyData = { email: this.userEmail, ...this.setKeyForm.value };
        this.registroService.setDynamicKey(keyData).subscribe({
            next: () => {
                this.currentStep = 'success';
                this.errorMessage = null;
            },
            error: (err) => this.errorMessage = err.error?.error || 'Error al crear la clave dinámica.'
        });
    }
}
