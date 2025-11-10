import { Component, EventEmitter, Output, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-inactivity-warning-modal',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './inactivity-warning-modal.component.html',
  styles: `
    :host {
      display: block;
    }
  `
})
export class InactivityWarningModalComponent implements OnInit, OnDestroy {
  @Output() continue = new EventEmitter<void>();
  @Output() logout = new EventEmitter<void>();

  timeLeft = {
    minutes: 1,
    seconds: 0,
    totalSeconds: 60,
  };

  private intervalId: any;

  ngOnInit(): void {
    this.startCountdown();
  }

  ngOnDestroy(): void {
    if (this.intervalId) {
      clearInterval(this.intervalId);
    }
  }

  private startCountdown(): void {
    this.intervalId = setInterval(() => {
      this.timeLeft.totalSeconds--;

      if (this.timeLeft.totalSeconds > 0) {
        this.timeLeft.minutes = Math.floor(this.timeLeft.totalSeconds / 60);
        this.timeLeft.seconds = this.timeLeft.totalSeconds % 60;
      } else {
        this.timeLeft.minutes = 0;
        this.timeLeft.seconds = 0;
        clearInterval(this.intervalId);
        // Auto-logout cuando el tiempo se acaba
        this.onLogout();
      }
    }, 1000);
  }

  onContinue(): void {
    this.continue.emit();
  }

  onLogout(): void {
    this.logout.emit();
  }

  formatTime(value: number): string {
    return value.toString().padStart(2, '0');
  }
}
