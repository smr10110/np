import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
    selector: 'app-session-timer',
    standalone: true,
    imports: [CommonModule],
    template: `
    <div class="flex items-center gap-2 px-3 py-1 rounded-lg bg-gradient-to-r from-orange-500 to-red-500 text-white shadow-md">
      <svg
        class="w-4 h-4"
        fill="none"
        stroke="currentColor"
        viewBox="0 0 24 24"
      >
        <path
          stroke-linecap="round"
          stroke-linejoin="round"
          stroke-width="2"
          d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z"
        />
      </svg>
      <span class="text-sm font-semibold">{{ timeLeft }}</span>
    </div>
  `
})
export class SessionTimerComponent implements OnInit, OnDestroy {
    timeLeft = '10:00';
    private interval: any = null;
    private expirationTime: number | null = null;

    ngOnInit(): void {
        this.initializeTimer();
        this.startTimer();
    }

    ngOnDestroy(): void {
        if (this.interval) {
            clearInterval(this.interval);
        }
    }

    private initializeTimer(): void {
        const token = sessionStorage.getItem('token');
        if (!token) {
            return;
        }

        try {
            // Decodificar el JWT para obtener el tiempo de expiración
            const payloadRaw = token.split('.')[1];
            const payloadJson = JSON.parse(atob(payloadRaw.replace(/-/g, '+').replace(/_/g, '/')));

            if (payloadJson?.exp) {
                this.expirationTime = payloadJson.exp * 1000; // Convertir a milisegundos
            }
        } catch (error) {
            console.error('Error decodificando token:', error);
        }
    }

    private startTimer(): void {
        this.updateTimeLeft();

        // Actualizar cada segundo
        this.interval = setInterval(() => {
            this.updateTimeLeft();
        }, 1000);
    }

    private updateTimeLeft(): void {
        if (!this.expirationTime) {
            this.timeLeft = '00:00';
            return;
        }

        const now = Date.now();
        const remainingMs = this.expirationTime - now;

        if (remainingMs <= 0) {
            this.timeLeft = '00:00';
            // Detener el interval cuando llegue a 0
            // El logout lo maneja automáticamente AutentificacionService
            if (this.interval) {
                clearInterval(this.interval);
                this.interval = null;
            }
            return;
        }

        // Convertir a minutos y segundos
        const totalSeconds = Math.floor(remainingMs / 1000);
        const minutes = Math.floor(totalSeconds / 60);
        const seconds = totalSeconds % 60;

        this.timeLeft = `${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}`;
    }
}