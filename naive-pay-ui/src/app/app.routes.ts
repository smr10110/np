import { Routes } from '@angular/router';
import { authGuard } from './modules/autentificacion/guards/auth.guard';
import { authEntryGuard } from './modules/autentificacion/guards/auth-entry.guard';

export const routes: Routes = [
    {
        path: '',
        canActivate: [authGuard],
        loadComponent: () => import('./dashboard/dashboard.component'),
        children: [
            {
                path: '',
                loadComponent: () => import('./dashboard/pages/home/ecommerce.component'),
                pathMatch: 'full',
                title: 'Inicio | Naive-Pay',
            },

            {   // dispositivos
                path: 'devices',
                loadComponent: () =>
                    import('./modules/dispositivos/component/dispositivos/dispositivos.component')
                        .then(m => m.DispositivosComponent),
                title: 'Devices | Naive-Pay',
            },
            //recompensas
            {
                path: 'recompensas',
                loadChildren: () =>
                    import('./modules/recompensas/recompensas.routes')
                        .then(m => m.RECOMPENSAS_ROUTES),
            },


            // 👇 Rutas de tu feature (pagos)
            {
                path: 'pagos',
                loadComponent: () => import('./modules/pagos/component/pagos/pagos.component'),
                title: 'Transacciones Pendientes | Naive-Pay',
            },
            {
                path: 'pagos/confirmar/:id',
                loadComponent: () => import('./modules/pagos/confirmar-pago/confirmar-pago.component'),
                title: 'Confirmar Pago | Naive-Pay',
            }

            , {
                path: 'pagos/exitoso/:id',
                loadComponent: () => import('./modules/pagos/component/transaccion-exitosa/transaccion-exitosa.component'),
                title: 'Pago Exitoso | Naive-Pay',
            }
            // 👇 Ruta que viene de develop (reportes)
            ,{
                path: 'report',
                loadComponent: () =>
                    import('./modules/reporte/component/reporte/reporte.component')
                        .then(m => m.ReporteComponent),
                title: 'Reportes | Naive-Pay',
            },
            // 👇 Ruta de develop (autentificación modularizada)

            // 👇 Rutas de comercios
            {
                path: 'registercommerce',
                loadComponent: () => import('./modules/comercio/component/income-commerce-form/income-commerce-form.component').then(m => m.IncomeCommerceFormComponent),
                title: 'Nuevo Comercio | Naive-Pay',
            },
            {
                path: 'validatecommerce',
                loadComponent: () => import('./modules/comercio/component/validation-process/validation-process.component').then(m => m.ValidationProcessComponent),
                title: 'Validar Comercio | Naive-Pay',
            },
            {
                path: 'commerces',
                loadComponent: () => import('./modules/comercio/component/commerce-list/commerce-list.component').then(m => m.CommerceListComponent),
                title: 'Validar Comercio | Naive-Pay',
            },
            // 👇 Módulo de Fondos
            {
                path: 'fondos',
                loadComponent: () => import('./modules/fondos/fondos'),
                title: 'Fondos | Naive‑Pay',
            }
        ],
    },
    {
        path: 'examples',
        loadComponent: () => import('./examples/example.component'),
        children: [
            {
                path: '',
                loadComponent: () => import('./examples/pages/dashboard/ecommerce/ecommerce.component'),
                pathMatch: 'full',
                title: 'Ejemplos',
            },
            { path: 'calendar', loadComponent: () => import('./examples/pages/calender/calender.component'), title: 'Calendario' },
            { path: 'profile', loadComponent: () => import('./examples/pages/profile/profile.component'), title: 'Perfil' },
            { path: 'form-elements', loadComponent: () => import('./examples/pages/forms/form-elements/form-elements.component'), title: 'Formularios' },
            { path: 'basic-tables', loadComponent: () => import('./examples/pages/tables/basic-tables/basic-tables.component'), title: 'Tablas' },
            { path: 'avatar', loadComponent: () => import('./examples/pages/ui-elements/avatar-element/avatar-element.component'), title: 'Avatares' },
            { path: 'badge', loadComponent: () => import('./examples/pages/ui-elements/badges/badges.component'), title: 'Insignias' },
            { path: 'buttons', loadComponent: () => import('./examples/pages/ui-elements/buttons/buttons.component'), title: 'Botones' },
            { path: 'images', loadComponent: () => import('./examples/pages/ui-elements/images/images.component'), title: 'Imágenes' },
            { path: 'videos', loadComponent: () => import('./examples/pages/ui-elements/videos/videos.component'), title: 'Videos' },
        ],
    },
    // 👇 Nuevos módulos de develop
    // auth pages (ejemplo legacy)
    {
        path: 'auth',
        canActivate: [authEntryGuard],
        loadComponent: () => import('./examples/pages/auth/auth.component'),
        children: [
            { path: 'login',
                loadComponent: () => import('./modules/autentificacion/component/login/login.component').then(c => c.LoginComponent),
                title: 'Iniciar sesión | Naive-Pay' },
                        { path: 'password-recovery',
                loadComponent: () => import('./modules/autentificacion/component/password-recovery/password-recovery.component').then(c => c.PasswordRecoveryComponent),
                title: 'Recuperar Contraseña | Naive-Pay' },{ path: 'recover',
                loadComponent: () => import('./modules/autentificacion/component/recuperar-acceso/recuperar-acceso.component').then(m => m.RecuperarAccesoComponent),
                title: 'Recuperar Acceso | Naive-Pay' },
            { path: 'recover/device',
                loadComponent: () => import('./modules/dispositivos/component/vincular-dispositivo/vincular-dispositivo.component').then(m => m.VincularDispositivoComponent),
                title: 'Vincular Dispositivo | Naive-Pay' },
            {
                path: 'register',
                loadComponent: () =>
                    import('./modules/registro/component/registro/registro.component')
                        .then(m => m.RegistroComponent),
                title: 'Registro | Naive-Pay',
            },
        ],
    },
    // error pages
    {
        path: '**',
        loadComponent: () => import('./error/not-found-error.component'),
        title: 'Naive-Pay - Página no encontrada',
    },
];