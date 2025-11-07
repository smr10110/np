import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { AppHeaderComponent } from '../shared/components/header/app-header.component';
import { AppSidebarComponent } from '../shared/components/app-sidebar/app-sidebar.component';
import { BackdropComponent } from '../shared/components/backdrop/backdrop.component';
import { SidebarService } from '../shared/services/sidebar.service';
import { exampleNavItems, exampleOtherItems } from './example-navbar-items';

@Component({
  selector: 'app-example',
  imports: [
    CommonModule,
    RouterModule,
    AppHeaderComponent,
    AppSidebarComponent,
    BackdropComponent
  ],
  templateUrl: './example.component.html',
})

export default class ExampleComponent {
  private readonly sidebarService = inject(SidebarService);
  public readonly isExpanded$;
  public readonly isHovered$;
  public readonly isMobileOpen$;

  constructor() {
    this.isExpanded$ = this.sidebarService.isExpanded$;
    this.isHovered$ = this.sidebarService.isHovered$;
    this.isMobileOpen$ = this.sidebarService.isMobileOpen$;
    this.sidebarService.setMenuItems(exampleNavItems);
    this.sidebarService.setOtherMenuItems(exampleOtherItems);
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
