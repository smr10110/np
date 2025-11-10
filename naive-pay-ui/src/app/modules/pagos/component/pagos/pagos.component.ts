import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClientModule } from '@angular/common/http';
import { PagosService } from '../../servicio/pagos.service';
import { PendingTransactionDTO } from '../../domain/pagos';
import { Router } from '@angular/router';
@Component({
  selector: 'app-pagos',
  standalone: true,
  imports: [CommonModule, HttpClientModule],
  templateUrl: './pagos.component.html',
  styleUrl: './pagos.component.css'
})
export default class PagosComponent implements OnInit {
  pendingTransactions: PendingTransactionDTO[] = [];
  loading: boolean = true;
  error: string = '';

  constructor(private pagosService: PagosService,private router: Router ) {

  }

  ngOnInit(): void {
    this.loadPendingTransactions();
  }

  loadPendingTransactions(): void {
    this.loading = true;
    this.error = '';

    this.pagosService.getPendingTransactions().subscribe({
      next: (transactions) => {
        this.pendingTransactions = transactions;
        this.loading = false;
      },
      error: (error) => {
        this.error = 'Error al cargar las transacciones pendientes';
        this.loading = false;
        console.error('Error:', error);
      }
    });
  }

  // Método para cuando hagas click en una transacción (para el siguiente componente)
 onTransactionClick(transactionId: number): void {
  this.router.navigate(['/pagos/confirmar', transactionId]);
}

  // Método para recargar la lista
  refresh(): void {
    this.loadPendingTransactions();
  }
}