import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { PagosService } from '../servicio/pagos.service';
import { Pagos } from '../domain/pagos';

@Component({
  selector: 'app-confirmar-pago',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './confirmar-pago.component.html',
  styleUrl: './confirmar-pago.component.css'
})
export default class ConfirmarPagoComponent implements OnInit {
  transaction: Pagos | null = null;
  loading: boolean = true;
  error: string = '';
  password: string = '';
  processing: boolean = false;
  
  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private pagosService: PagosService
  ) {}

  ngOnInit(): void {
    this.loadTransaction();
  }

  loadTransaction(): void {
    const id = this.route.snapshot.paramMap.get('id');
    
    if (!id) {
      this.error = 'Invalid transaction ID';
      this.loading = false;
      return;
    }

    this.pagosService.getTransactionById(+id).subscribe({
      next: (transaction) => {
        this.transaction = transaction;
        this.loading = false;
      },
      error: (error) => {
        this.error = 'Error loading transaction';
        this.loading = false;
        console.error('Error:', error);
      }
    });
  }

  confirmarPago(): void {
    if (!this.transaction) {
      this.error = 'No transaction to confirm';
      return;
    }

    this.processing = true;
    this.error = '';

    this.pagosService.approveTransaction(this.transaction.id).subscribe({
      next: (updatedTransaction) => {
        this.processing = false;
        
        // ✅ CHECK UPDATED STATUS
        if (updatedTransaction.status === 'APPROVED') {
          alert('Payment confirmed successfully!');
          this.router.navigate(['/pagos/exitoso', updatedTransaction.id]);
        } else if (updatedTransaction.status === 'REJECTED') {
          this.error = 'Payment rejected: Insufficient funds';
          // Update local transaction to reflect the change
          this.transaction = updatedTransaction;
        } else {
          this.error = 'Unexpected transaction status: ' + updatedTransaction.status;
          this.transaction = updatedTransaction;
        }
      },
      error: (error) => {
        this.processing = false;
        this.error = 'Error confirming payment';
        console.error('Error:', error);
      }
    });
  }

  cancelarPago(): void {
    if (!this.transaction) return;

    this.processing = true;
    
    this.pagosService.cancelTransaction(this.transaction.id).subscribe({
      next: (transaction) => {
        this.processing = false;
        alert('Transaction cancelled');
        this.router.navigate(['/pagos']);
      },
      error: (error) => {
        this.processing = false;
        this.error = 'Error cancelling transaction';
        console.error('Error:', error);
      }
    });
  }

  volver(): void {
    this.router.navigate(['/pagos']);
  }
}

