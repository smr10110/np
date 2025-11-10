import { Routes } from '@angular/router';

export const RECOMPENSAS_ROUTES: Routes = [
    {
        path: '',
        loadComponent: () =>
            import('./component/recompensas/recompensas.component')
                .then(m => m.RecompensasComponent),
        title: 'Recompensas | Naive-Pay',
    },
    {
        path: 'canje',
        loadComponent: () =>
            import('./component/canje-puntos/canje-puntos.component')
                .then(m => m.CanjePuntosComponent),
        title: 'Canje de Puntos | Naive-Pay',
    },
    {
        path: 'promociones',
        loadComponent: () =>
            import('./component/promociones/promociones.component')
                .then(m => m.PromocionesComponent),
        title: 'Promociones | Naive-Pay',
    },
];
