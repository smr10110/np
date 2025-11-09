import { Component } from '@angular/core';
import { RouterModule, RouterOutlet } from '@angular/router';
import { GridShapeComponent } from '../../../shared/components/common/grid-shape/grid-shape.component';
import { ThemeToggleTwoComponent } from '../../../shared/components/common/theme-toggle-two/theme-toggle-two.component';

@Component({
  selector: 'app-auth-component',
  imports: [
    GridShapeComponent,
    RouterModule,
    RouterOutlet,
    ThemeToggleTwoComponent,
  ],
  templateUrl: './auth.component.html',
  styles: ``
})
export default class AuthPageLayoutComponent {

}
