import { Component, ElementRef, ViewChild, inject } from '@angular/core';
import { SidebarService } from '../../services/sidebar.service';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { ThemeToggleButtonComponent } from '../common/theme-toggle/theme-toggle-button.component';
import { NotificationDropdownComponent } from './notification-dropdown/notification-dropdown.component';
import { UserDropdownComponent } from './user-dropdown/user-dropdown.component';
import { CountdownTimerComponent } from '../common/countdown-timer/countdown-timer.component';
import { AutentificacionService } from '../../../modules/autentificacion/service/autentificacion.service';

@Component({
  selector: 'app-header',
  imports: [
    CommonModule,
    RouterModule,
    ThemeToggleButtonComponent,
    NotificationDropdownComponent,
    UserDropdownComponent,
    CountdownTimerComponent,
  ],
  templateUrl: './app-header.component.html',
})
export class AppHeaderComponent {
  public isApplicationMenuOpen = false;
  public readonly isMobileOpen$;
  public readonly sessionExpiration$;

  @ViewChild('searchInput') searchInput!: ElementRef<HTMLInputElement>;

  private readonly authService = inject(AutentificacionService);

  constructor(public sidebarService: SidebarService) {
    this.isMobileOpen$ = this.sidebarService.isMobileOpen$;
    this.sessionExpiration$ = this.authService.sessionExpiration$;
  }

  public handleToggle() {
    if (window.innerWidth >= 1280) {
      this.sidebarService.toggleExpanded();
    } else {
      this.sidebarService.toggleMobileOpen();
    }
  }

  public toggleApplicationMenu() {
    this.isApplicationMenuOpen = !this.isApplicationMenuOpen;
  }

  public ngAfterViewInit() {
    document.addEventListener('keydown', this.handleKeyDown);
  }

  public ngOnDestroy() {
    document.removeEventListener('keydown', this.handleKeyDown);
  }

  public handleKeyDown = (event: KeyboardEvent) => {
    if ((event.metaKey || event.ctrlKey) && event.key === 'k') {
      event.preventDefault();
      this.searchInput?.nativeElement.focus();
    }
  };
}
