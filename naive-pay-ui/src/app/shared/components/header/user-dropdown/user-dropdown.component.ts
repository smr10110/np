import {
    Component,
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    inject,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { DropdownComponent } from '../../ui/dropdown/dropdown.component';
import { DropdownItemTwoComponent } from '../../ui/dropdown/dropdown-item/dropdown-item.component-two';
import { AutentificacionService } from '../../../../modules/autentificacion/service/autentificacion.service';
import { finalize } from 'rxjs/operators';

@Component({
    selector: 'app-user-dropdown',
    templateUrl: './user-dropdown.component.html',
    imports: [CommonModule, DropdownComponent, DropdownItemTwoComponent],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class UserDropdownComponent {
    private readonly auth = inject(AutentificacionService);
    private readonly router = inject(Router);
    private readonly cdr = inject(ChangeDetectorRef);

    isOpen = false;
    loading = false;

    toggleDropdown(): void { this.isOpen = !this.isOpen; }
    closeDropdown(): void { this.isOpen = false; }

    /** Cierra sesión y redirige mostrando aviso. Siempre resetea el estado visual. */
    onLogout(): void {
        if (this.loading) return;

        this.loading = true;
        this.closeDropdown();                 // cerrar UI inmediatamente
        this.cdr.markForCheck();

        this.auth.logout().pipe(
            finalize(() => {                   // SIEMPRE: reset visual
                this.loading = false;
                this.cdr.markForCheck();
            })
        ).subscribe({
            next: () => {
                void this.router.navigate(['/auth/login'], {
                    queryParams: { reason: 'logout_ok' },
                    replaceUrl: true,              // evita volver con back
                });
            },
            error: () => {
                this.auth.clear();               // por si falló el backend
                void this.router.navigate(['/auth/login'], {
                    queryParams: { reason: 'logout_ok' },
                    replaceUrl: true,
                });
            },
        });
    }
}