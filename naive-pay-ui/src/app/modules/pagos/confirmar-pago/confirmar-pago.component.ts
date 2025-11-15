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
      next: (response) => {
        this.processing = false;
        
        if (response.success) {
          alert('Payment confirmed successfully!');
          this.router.navigate(['/pagos/exitoso', response.transactionId]);
        } else {
          this.error = response.message || 'Payment approval failed';
        }
      },
      error: (error) => {
        console.error('Error approving payment:', error);
        this.processing = false;
        
        // Check if it's an authentication error
        if (error.status === 401) {
          this.error = 'Session expired. Please login again.';
          setTimeout(() => {
            this.router.navigate(['/auth/login']);
          }, 2000);
        } else {
          this.error = error.error?.message || 'Error confirming payment. Please try again.';
        }
      }
    });
  }

  cancelarPago(): void {
    if (!this.transaction) return;

    this.processing = true;
    
    this.pagosService.cancelTransaction(this.transaction.id).subscribe({
      next: (response) => {
        this.processing = false;
        if (response.success) {
          alert('Transaction cancelled');
          this.router.navigate(['/pagos']);
        } else {
          this.error = response.message || 'Cancellation failed';
        }
      },
      error: (error) => {
        this.processing = false;
        if (error.status === 401) {
          this.error = 'Session expired. Please login again.';
          setTimeout(() => {
            this.router.navigate(['/auth/login']);
          }, 2000);
        } else {
          this.error = error.error?.message || 'Error cancelling transaction';
        }
        console.error('Error:', error);
      }
    });
  }

  volver(): void {
    this.router.navigate(['/pagos']);
  }
}

