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

  protected readonly step = signal(1); // 1: email, 2: código + nueva contraseña, 3: éxito
  protected readonly loading = signal(false);
  protected readonly message = signal('');
  protected readonly messageType = signal<'ok' | 'err' | ''>(''); // Type-safe union type
  // Mostrar/Ocultar contraseñas
  protected readonly showNewPassword = signal(false);
  protected readonly showConfirmPassword = signal(false);

  protected readonly emailForm = this.fb.group({
    email: ['', [Validators.required, Validators.email]]
  });

  // verifica que contraseñas coincidan
  private readonly passwordsMatchValidator: ValidatorFn = (group: AbstractControl) => {
    const newPass = group.get('newPassword')?.value ?? '';
    const confirm = group.get('confirmPassword')?.value ?? '';
    return newPass && confirm && newPass !== confirm ? { passwordsMismatch: true } : null;
  };

  // Paso 2: formulario combinado con código + contraseñas
  protected readonly resetForm = this.fb.group({
    code: ['', [Validators.required, Validators.minLength(6), Validators.maxLength(6), Validators.pattern(/^\d{6}$/)]],
    newPassword: ['', [Validators.required, Validators.minLength(8)]],
    confirmPassword: ['', [Validators.required, Validators.minLength(8)]]
  }, { validators: this.passwordsMatchValidator });

  toggleNewPassword(): void {
    this.showNewPassword.update(v => !v);
  }

  toggleConfirmPassword(): void {
    this.showConfirmPassword.update(v => !v);
  }

  // Paso 1: Solicitar código de recuperación
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

        // Transición automática al siguiente paso después de 2 segundos
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

  // Paso 2: Cambiar contraseña con código de verificación
  resetPassword(): void {
    if (this.resetForm.invalid || this.loading()) return;

    const formValue = this.resetForm.value;

    this.loading.set(true);
    this.message.set('');
    this.messageType.set('');

    const email = this.emailForm.value.email!;
    const code = formValue.code!;

    this.auth.resetPassword({ email, code, newPassword: formValue.newPassword! }).subscribe({
      next: () => {
        this.loading.set(false);
        this.step.set(3);
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