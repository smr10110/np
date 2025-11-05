import { CanActivateFn, Router } from '@angular/router';
import { inject } from '@angular/core';
import { map } from 'rxjs/operators';
import { AutentificacionService } from '../service/autentificacion.service';

/**
 * Si el usuario autenticado navega a cualquier ruta /auth/**,
 * se marca logout: se envía al backend y se limpia el token local.
 * Luego se permite la navegación (login, register, etc.).
 */
export const authEntryGuard: CanActivateFn = () => {
    const hasToken = !!sessionStorage.getItem('token');
    if (!hasToken) return true;

    const auth = inject(AutentificacionService);
    const router = inject(Router);
    return auth.logoutSilent().pipe(
        map(() => router.createUrlTree(['/auth/login'], { queryParams: { reason: 'logout_ok' } }))
    );
};
