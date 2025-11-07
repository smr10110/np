import {
    Component,
    OnInit,
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    inject
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, NgForm } from '@angular/forms';
import { ActivatedRoute, RouterLink, Router } from '@angular/router'; // ← + Router
import { AutentificacionService } from '../../service/autentificacion.service';

/** Datos que envía el formulario al backend. */
interface LoginRequest {
    identifier: string;     // correo
    password: string;       // contrasena
}


@Component({
    standalone: true,
    selector: 'np-login',
    imports: [CommonModule, FormsModule, RouterLink],
    templateUrl: './login.component.html',
    styleUrls: ['./login.component.css'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class LoginComponent implements OnInit {
    private readonly auth  = inject(AutentificacionService);
    private readonly route = inject(ActivatedRoute);
    private readonly cdr   = inject(ChangeDetectorRef);
    private readonly router = inject(Router);                 // ← + inyecta Router

    showPassword = false;
    loading = false;
    message = '';
    messageType: 'ok' | 'err' | '' = '';

    model: LoginRequest = {
        identifier: '',
        password: ''
    };

    ngOnInit(): void {
        const reason = this.route.snapshot.queryParamMap.get('reason');
        if (reason === 'session_closed' || reason === 'token_expired') {
            this.messageType = 'err';
            this.message = 'Tu sesión expiró. Inicia sesión nuevamente.';
            this.cdr.markForCheck();
        } else if (reason === 'logout_ok') {
            this.messageType = 'ok';
            this.message = 'Sesión cerrada correctamente.';
            this.cdr.markForCheck();
        }
    }

    togglePassword(): void { this.showPassword = !this.showPassword; }

    submit(form: NgForm): void {
        if (form.invalid || this.loading) return;

        this.loading = true;
        this.message = '';
        this.messageType = '';
        this.cdr.markForCheck();

        this.auth.login(this.model).subscribe({
            next: () => {
                this.loading = false;
                // → Redirige al dashboard (ruta raíz)
                this.router.navigateByUrl('/');               // ← + navegación
            },
            error: (err) => {
                this.loading = false;
                this.messageType = 'err';

                const code = err?.error?.error as string | undefined;

                const friendly: Record<string, string> = {
                    USER_NOT_FOUND: 'USUARIO NO EXISTE',
                    BAD_CREDENTIALS: 'CREDENCIALES INVALIDAS',
                    DEVICE_UNAUTHORIZED: 'DISPOSITIVO NO AUTORIZADO',
                    DEVICE_REQUIRED: 'DISPOSITIVO REQUERIDO',
                    // DEVICE_RECOVERY_REQUIRED: 'Se envió un código a tu correo.' // por si luego lo habilitan
                };

                this.message = 'AVISO: ' + (friendly[code ?? ''] ?? 'CREDENCIALES INVALIDAS');
                this.cdr.markForCheck();

                // Redirigir al flujo de vinculación cuando el dispositivo no está vinculado/autorizado
                if (code === 'DEVICE_REQUIRED' || code === 'DEVICE_UNAUTHORIZED') {
                    void this.router.navigate(
                        ['/auth/recover/device'],
                        { queryParams: { id: this.model.identifier } } // opcional: precargar email/RUT
                    );
                    return;
                }
            }
        });
    }
}