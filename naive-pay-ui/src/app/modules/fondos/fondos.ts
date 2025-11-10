import { Component } from '@angular/core';
import { FondosDashboardComponent } from './component/fondos-dashboard/fondos-dashboard.component';

/**
 * Componente principal del módulo de fondos
 * Muestra el dashboard con saldo y transacciones
 */
@Component({
  selector: 'app-fondos',
  standalone: true,
  imports: [FondosDashboardComponent],
  templateUrl: './fondos.html',
  styleUrl: './fondos.css'
})
export default class Fondos {

}
