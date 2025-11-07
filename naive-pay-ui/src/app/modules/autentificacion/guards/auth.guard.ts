import { CanActivateFn, Router } from '@angular/router';
import { inject } from '@angular/core';

/**
 * Bloquea el acceso a rutas privadas si no hay token en sessionStorage.
 * Redirige al login con motivo "session_closed".
 */
export const authGuard: CanActivateFn = () => {
    const hasToken = !!sessionStorage.getItem('token');
    if (hasToken) return true;

    const router = inject(Router);
    return router.createUrlTree(['/auth/login']);
};