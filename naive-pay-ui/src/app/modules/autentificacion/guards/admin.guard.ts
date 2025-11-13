import { CanActivateFn, Router } from '@angular/router';
import { inject } from '@angular/core';

/**
 * Guard de protección para rutas administrativas.
 * Verifica que el usuario tenga rol 'ADMIN' en sessionStorage.
 * Si no es admin, redirige a la página principal (dashboard).
 */
export const adminGuard: CanActivateFn = () => {
  const role = sessionStorage.getItem('userRole');

  // Verificar que el usuario sea ADMIN
  if (role === 'ADMIN') {
    return true;
  }

  // Si no es ADMIN, redirigir al dashboard
  const router = inject(Router);
  console.warn('Acceso denegado: Se requiere rol ADMIN');
  return router.createUrlTree(['/']);
};
