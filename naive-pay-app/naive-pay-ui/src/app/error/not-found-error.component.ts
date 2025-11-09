import { Component } from '@angular/core';
import { RouterModule } from '@angular/router';
import { GridShapeComponent } from '../shared/components/common/grid-shape/grid-shape.component';

@Component({
  selector: 'app-not-found',
  imports: [
    GridShapeComponent,
    RouterModule,
  ],
  templateUrl: './not-found-error.component.html',
  styles: ``
})
export default class NotFoundErrorComponent {

  currentYear: number = new Date().getFullYear();
}
