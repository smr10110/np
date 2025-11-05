import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { AppHeaderComponent } from '../shared/components/header/app-header.component';
import { AppSidebarComponent } from '../shared/components/app-sidebar/app-sidebar.component';
import { BackdropComponent } from '../shared/components/backdrop/backdrop.component';
import { SidebarService } from '../shared/services/sidebar.service';
import { navItems, otherItems } from './dashboard-navbar-items';

@Component({
  selector: 'app-dashboard-component',
  imports: [
    CommonModule,
    RouterModule,
    AppHeaderComponent,
    AppSidebarComponent,
    BackdropComponent
  ],
  templateUrl: './dashboard.component.html',
})

export default class DashboardComponent {
  private readonly sidebarService = inject(SidebarService);
  public readonly isExpanded$;
  public readonly isHovered$;
  public readonly isMobileOpen$;

  constructor() {
    this.isExpanded$ = this.sidebarService.isExpanded$;
    this.isHovered$ = this.sidebarService.isHovered$;
    this.isMobileOpen$ = this.sidebarService.isMobileOpen$;
    this.sidebarService.setMenuItems(navItems);
    this.sidebarService.setOtherMenuItems(otherItems);
  }

  get containerClasses(): string[] {
    return [
      'flex-1',
      'transition-all',
      'duration-300',
      'ease-in-out',
      (this.isExpanded$ || this.isHovered$) ? 'xl:ml-[290px]' : 'xl:ml-[90px]',
      this.isMobileOpen$ ? 'ml-0' : ''
    ];
  }
}
