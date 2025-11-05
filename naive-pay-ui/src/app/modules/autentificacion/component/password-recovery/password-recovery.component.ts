import { Component, ChangeDetectionStrategy, inject, signal, DestroyRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators, ValidatorFn, AbstractControl } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AutentificacionService } from '../../service/autentificacion.service';

@Component({
  selector: 'app-password-recovery',
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './password-recovery.component.html',
  styleUrl: './password-recovery.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class PasswordRecoveryComponent {
  private readonly auth = inject(AutentificacionService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly fb = inject(FormBuilder);

  // Pasos: 1 email, 2 codigo, 3 nueva contraseña, 4 exito
  protected readonly step = signal(1);
  protected readonly loading = signal(false);
  protected readonly message = signal('');
  protected readonly messageType = signal<'ok' | 'err' | ''>('');
  // Mostrar/Ocultar contraseñas
  protected readonly showNewPassword = signal(false);
  protected readonly showConfirmPassword = signal(false);

  // Forms
  protected readonly emailForm = this.fb.group({
    email: ['', [Validators.required, Validators.email]]
  });
 // 6 digitos para el codigo enviado al correo
  protected readonly verifyForm = this.fb.group({
    code: ['', [Validators.required, Validators.minLength(6), Validators.maxLength(6), Validators.pattern(/^\d{6}$/)]]
  });
// contraseñas deben coincicidir
  private readonly passwordsMatchValidator: ValidatorFn = (group: AbstractControl) => {
    const newPass = group.get('newPassword')?.value ?? '';
    const confirm = group.get('confirmPassword')?.value ?? '';
    return newPass && confirm && newPass !== confirm ? { passwordsMismatch: true } : null;
  };
// 8 caracteres minimo contraseñas
  protected readonly resetForm = this.fb.group({
    newPassword: ['', [Validators.required, Validators.minLength(8)]],
    confirmPassword: ['', [Validators.required, Validators.minLength(8)]]
  }, { validators: this.passwordsMatchValidator });

  toggleNewPassword(): void {
    this.showNewPassword.update(v => !v);
  }

  toggleConfirmPassword(): void {
    this.showConfirmPassword.update(v => !v);
  }

  // Paso 1: Solicitar Codigo
  requestCode(): void {
    if (this.emailForm.invalid || this.loading()) return;

    this.loading.set(true);
    this.message.set('');
    this.messageType.set('');

    const email = this.emailForm.value.email!;

    this.auth.requestPasswordRecovery({ email }).subscribe({
      next: (res) => {
        this.loading.set(false);
        this.messageType.set('ok');
        this.message.set(res.message);

        const timer = setTimeout(() => {
          this.step.set(2);
          this.message.set('');
          this.messageType.set('');
        }, 2000);
        this.destroyRef.onDestroy(() => clearTimeout(timer));
      },
      error: (err) => {
        this.loading.set(false);
        this.messageType.set('err');
        this.message.set(err?.error?.message || 'Error al enviar codigo. Intenta nuevamente.');
      }
    });
  }

  // Paso 2: Validar Codigo desde Backend
  proceedToPassword(): void {
    if (this.verifyForm.invalid || this.loading()) return;
    this.loading.set(true);
    this.message.set('');
    this.messageType.set('');

    const email = this.emailForm.value.email!;
    const code = this.verifyForm.value.code!;

    this.auth.verifyRecoveryCode({ email, code }).subscribe({
      next: () => {
        this.loading.set(false);
        this.step.set(3);
      },
      error: (err) => {
        this.loading.set(false);
        this.messageType.set('err');
        const errorCode = err?.error?.error || err?.error?.message || '';
        const map: Record<string, string> = {
          INVALID_CODE: 'Código inválido o expirado',
          CODE_EXPIRED: 'El código ha expirado (10 minutos)',
          CODE_ALREADY_USED: 'Este código ya fue utilizado'
        };
        this.message.set(map[errorCode] || 'No pudimos validar el código. Intenta nuevamente.');
      }
    });
  }

  // Paso 3: cambiar contraseña con confirmar contraseña
  resetPassword(): void {
    if (this.resetForm.invalid || this.loading()) return;

    const formValue = this.resetForm.value;

    this.loading.set(true);
    this.message.set('');
    this.messageType.set('');

    const email = this.emailForm.value.email!;
    const code = this.verifyForm.value.code!;

    this.auth.resetPassword({ email, code, newPassword: formValue.newPassword! }).subscribe({
      next: () => {
        this.loading.set(false);
        this.step.set(4);
      },
      error: (err) => {
        this.loading.set(false);
        this.messageType.set('err');
        const errorCode = err?.error?.error || err?.error?.message || '';
        const errorMessages: Record<string, string> = {
          'INVALID_CODE': 'Codigo invalido o expirado',
          'CODE_ALREADY_USED': 'Este codigo ya fue utilizado',
          'CODE_EXPIRED': 'El codigo ha expirado (10 minutos)',
          'PASSWORD_TOO_SHORT': 'La contrasena debe tener al menos 8 caracteres'
        };
        this.message.set(errorMessages[errorCode] || 'Error al cambiar contrasena. Verifica el codigo.');
      }
    });
  }
}