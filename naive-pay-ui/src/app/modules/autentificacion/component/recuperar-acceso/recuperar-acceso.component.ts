import { Component, ChangeDetectionStrategy } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'np-recuperar-acceso',
  imports: [RouterLink],
  templateUrl: './recuperar-acceso.component.html',
  styleUrls: ['./recuperar-acceso.component.css'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class RecuperarAccesoComponent {}

