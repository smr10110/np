import { Injectable } from '@angular/core';
import { UAParser } from 'ua-parser-js';

@Injectable({ providedIn: 'root' })
export class DeviceFingerprintService {
    /** Clave fija usada para almacenar la fingerprint en LOCAL STORAGE (NO SESSION STORAGE) */
    private readonly KEY = 'np_device_fp';

    /** Obtiene o crea la fingerprint del dispositivo actual */
    getFingerprint(): string {
        let fp = localStorage.getItem(this.KEY);
        if (!fp) {
            const newFp =
                (crypto as any)?.randomUUID?.() ??
                'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, (c: string) => {
                    const r = crypto.getRandomValues(new Uint8Array(1))[0] & 15;
                    const v = c === 'x' ? r : (r & 0x3) | 0x8;
                    return v.toString(16);
                });
            localStorage.setItem(this.KEY, newFp);
            fp = newFp;
        }
        return fp!;
    }

    get(): string {
        return this.getFingerprint();
    }

    /** Obtiene información técnica del dispositivo usando UAParser */
    getDeviceInfo() {
        const parser = new UAParser();
        const result = parser.getResult();

        const os = result.os?.name ?? 'Desconocido';
        const browser = result.browser?.name ?? 'Desconocido';
        const deviceType = result.device?.type
            ? result.device.type.toUpperCase()
            : 'DESKTOP';

        const language = navigator.language;
        const timezone = Intl.DateTimeFormat().resolvedOptions().timeZone;

        return {
            fingerprint: this.getFingerprint(),
            os,
            browser,
            type: deviceType,
            language,
            timezone
        };
    }
}
