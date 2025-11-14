// src/app/modules/autentificacion/component/vincular-dispositivo/vincular-dispositivo.component.ts
import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { DispositivosService } from '../../service/dispositivos.service';
import { AutentificacionService } from '../../../autentificacion/service/autentificacion.service';

@Component({
    standalone: true,
    selector: 'np-vincular-dispositivo',
    imports: [CommonModule, FormsModule],
    templateUrl: './vincular-dispositivo.component.html',
    styleUrls: ['./vincular-dispositivo.component.css']
})
export class VincularDispositivoComponent implements OnInit {
    step: 'request' | 'verify' | 'success' = 'request';
    identifier = '';
    code = '';
    recoveryId: string | null = null;
    errorMsg: string | null = null;

    loading = false;
    error: string | null = null;

    constructor(
        private readonly dispositivos: DispositivosService,
        private readonly route: ActivatedRoute,
        private readonly router: Router,
        private readonly auth: AutentificacionService
    ) {}

    ngOnInit(): void {
        // Si llegamos desde /auth/login con ?id=<email/rut>, precargar
        const qpId = this.route.snapshot.queryParamMap.get('id');
        if (qpId) this.identifier = qpId;
    }

    // Paso 1: solicitar codigo
    requestCode(): void {
        if (!this.identifier?.trim()) return;
        this.loading = true; this.error = null;
        console.info('[DeviceLink] solicitando codigo', { identifier: this.identifier });
        this.dispositivos.recoverRequest(this.identifier.trim()).subscribe({
            next: (res) => {
                this.recoveryId = res.recoveryId;
                this.step = 'verify';
                this.loading = false;
                console.info('[DeviceLink] codigo enviado', { recoveryId: this.recoveryId });
            },
            error: (err) => {
                this.loading = false;
                this.error = err?.error?.error || 'No se pudo enviar el codigo.';
                console.warn('[DeviceLink] error solicitando codigo', this.error);
            }
        });
    }

    // Paso 2: verificar codigo y vincular
    verifyCode(): void {
        if (!this.recoveryId || !this.code.trim()) return;
        this.loading = true;
        this.errorMsg = null;
        console.info('[DeviceLink] verificando codigo', { recoveryId: this.recoveryId });

        this.dispositivos.recoverVerify(this.recoveryId, this.code)
            .subscribe({
                next: (res) => {
                    console.info('[DeviceLink] verificacion exitosa, restaurando sesion', { role: res.role });
                    this.auth.restoreSession(res.accessToken, res.role);
                    this.loading = false;
                    void this.router.navigateByUrl('/');
                },
                error: () => {
                    this.loading = false;
                    this.errorMsg = 'No se pudo verificar el codigo.';
                    console.warn('[DeviceLink] error verificando codigo');
                }
            });
    }
}
