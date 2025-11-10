import { Component, inject } from '@angular/core';
import { RouterModule } from '@angular/router';
import { CommonModule } from '@angular/common';
import { InactivityWarningModalComponent } from './shared/components/common/inactivity-warning-modal/inactivity-warning-modal.component';
import { InactivityWarningService } from './shared/services/inactivity-warning.service';
import { AutentificacionService } from './modules/autentificacion/service/autentificacion.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [
    RouterModule,
    CommonModule,
    InactivityWarningModalComponent,
  ],
  templateUrl: './app.component.html',
  styles: ``,
})
export class AppComponent {
  title = 'Angular';

  private readonly inactivityWarningService = inject(InactivityWarningService);
  private readonly authService = inject(AutentificacionService);

  public readonly showInactivityWarning$ = this.inactivityWarningService.showModal$;

  onContinueSession(): void {
    this.inactivityWarningService.hide();
    // Hacer un request para resetear la actividad
    this.authService.resetInactivity();
  }

  onLogout(): void {
    this.inactivityWarningService.hide();
    this.authService.logout(true).subscribe();
  }
}
