// src/app/modules/autentificacion/component/vincular-dispositivo/vincular-dispositivo.component.ts
import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { DispositivosService, LoginResponse } from '../../service/dispositivos.service';

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
        private readonly router: Router
    ) {}

    ngOnInit(): void {
        // Si llegamos desde /auth/login con ?id=<email/rut>, precargar
        const qpId = this.route.snapshot.queryParamMap.get('id');
        if (qpId) this.identifier = qpId;
    }

    // Paso 1: solicitar código
    requestCode(): void {
        if (!this.identifier?.trim()) return;
        this.loading = true; this.error = null;
        this.dispositivos.recoverRequest(this.identifier.trim()).subscribe({
            next: (res) => {
                this.recoveryId = res.recoveryId;
                this.step = 'verify';
                this.loading = false;
            },
            error: (err) => {
                this.loading = false;
                this.error = err?.error?.error || 'No se pudo enviar el código.';
            }
        });
    }

    // Paso 2: verificar código y vincular
    verifyCode(): void {
        if (!this.recoveryId || !this.code.trim()) return;
        this.loading = true;
        this.errorMsg = null;

        this.dispositivos.recoverVerify(this.recoveryId, this.code)
            .subscribe({
                next: (res) => {
                    sessionStorage.setItem('token', res.accessToken);
                    this.router.navigateByUrl('/');
                },
                error: () => {
                    this.loading = false;
                    this.errorMsg = 'No se pudo verificar el código.';
                }
            });
    }
}
