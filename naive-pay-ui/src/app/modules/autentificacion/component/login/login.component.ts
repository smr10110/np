import {
    Component,
    OnInit,
    ChangeDetectionStrategy,
    inject,
    signal
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { ActivatedRoute, RouterLink, Router } from '@angular/router';
import { AutentificacionService } from '../../service/autentificacion.service';

/** Datos que envía el formulario al backend. */
interface LoginRequest {
    identifier: string;     // correo
    password: string;       // contrasena
}


@Component({
    selector: 'np-login',
    imports: [CommonModule, ReactiveFormsModule, RouterLink],
    templateUrl: './login.component.html',
    styleUrls: ['./login.component.css'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class LoginComponent implements OnInit {
    private readonly auth  = inject(AutentificacionService);
    private readonly route = inject(ActivatedRoute);
    private readonly router = inject(Router);
    private readonly fb = inject(FormBuilder);

    // State management con signals
    protected readonly showPassword = signal(false);
    protected readonly loading = signal(false);
    protected readonly message = signal('');
    protected readonly messageType = signal<'ok' | 'err' | ''>('');
    protected readonly remainingAttempts = signal(5);

    // Popup informativo de cuenta bloqueada
    protected readonly blockedDialogOpen = signal(false);
    protected readonly blockedDialogMessage = signal('');

    // Reactive Form
    protected readonly loginForm = this.fb.group({
        identifier: ['', [Validators.required]],
        password: ['', [Validators.required, Validators.minLength(8)]]
    });

    ngOnInit(): void {
        const reason = this.route.snapshot.queryParamMap.get('reason');
        if (reason === 'session_closed' || reason === 'token_expired') {
            this.messageType.set('err');
            this.message.set('Tu sesión expiró. Inicia sesión nuevamente.');
        } else if (reason === 'logout_ok') {
            this.messageType.set('ok');
            this.message.set('Sesión cerrada correctamente.');
        }
    }

    togglePassword(): void {
        this.showPassword.update(show => !show);
    }

    submit(): void {
        if (this.loginForm.invalid || this.loading()) return;

        this.loading.set(true);
        this.message.set('');
        this.messageType.set('');

        const formValue = this.loginForm.value;
        const loginData: LoginRequest = {
            identifier: formValue.identifier!,
            password: formValue.password!
        };

        this.auth.login(loginData).subscribe({
            next: () => {
                this.loading.set(false);
                // Resetear intentos al loguearse exitosamente
                this.remainingAttempts.set(5);
                // → Redirige al dashboard (ruta raíz)
                this.router.navigateByUrl('/');
            },
            error: (err) => {
                this.loading.set(false);
                this.messageType.set('err');

                const code = err?.error?.error as string | undefined;
                const backendRemainingAttempts = err?.error?.remainingAttempts as number | undefined;

                // Manejo del contador de intentos
                if (code === 'BAD_CREDENTIALS') {
                    // Usar el valor del backend si está disponible, sino decrementar local
                    if (backendRemainingAttempts !== undefined) {
                        this.remainingAttempts.set(backendRemainingAttempts);
                    } else {
                        this.remainingAttempts.update(attempts => attempts - 1);
                    }
                    this.message.set(`CREDENCIALES INVALIDAS\nTe quedan ${this.remainingAttempts()} intentos`);
                } else if (code === 'ACCOUNT_BLOCKED') {
                    this.remainingAttempts.set(0);
                    // Mostrar popup informativo (solo informar al usuario)
                    this.blockedDialogMessage.set(
                        'Tu cuenta ha sido bloqueada por seguridad. ' +
                        'Te enviamos un correo con instrucciones para recuperarla. '
                    );
                    this.blockedDialogOpen.set(true);
                    // Mantener el mensaje inline minimo para accesibilidad
                    this.message.set('CUENTA BLOQUEADA');
                } else {
                    const friendly: Record<string, string> = {
                        USER_NOT_FOUND: 'USUARIO NO EXISTE',
                        DEVICE_UNAUTHORIZED: 'DISPOSITIVO NO AUTORIZADO',
                        DEVICE_REQUIRED: 'DISPOSITIVO REQUERIDO'
                    };
                    this.message.set(friendly[code ?? ''] ?? 'CREDENCIALES INVALIDAS');
                }

                // Redirigir al flujo de vinculación cuando el dispositivo no está vinculado/autorizado
                if (code === 'DEVICE_REQUIRED' || code === 'DEVICE_UNAUTHORIZED') {
                    void this.router.navigate(
                        ['/auth/recover/device'],
                        { queryParams: { id: formValue.identifier } }
                    );
                    return;
                }
            }
        });
    }

    // Cerrar popup de cuenta bloqueada
    closeBlockedDialog(): void {
        this.blockedDialogOpen.set(false);
    }
}
