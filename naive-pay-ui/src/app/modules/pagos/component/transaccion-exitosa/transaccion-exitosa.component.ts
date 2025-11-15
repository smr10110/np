import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { PagosService } from '../../servicio/pagos.service';
import { Pagos } from '../../domain/pagos';

@Component({
  selector: 'app-transaccion-exitosa',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './transaccion-exitosa.component.html',
  styleUrl: './transaccion-exitosa.component.css'
})
export default class TransaccionExitosaComponent implements OnInit {
  transaction: Pagos | null = null;
  loading: boolean = true;
  transactionId: number | null = null;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private pagosService: PagosService
  ) { }

  ngOnInit(): void {
    this.transactionId = Number(this.route.snapshot.paramMap.get('id'));
    this.loadTransaction();
  }

  loadTransaction(): void {
    if (!this.transactionId) {
      this.loading = false;
      return;
    }

    this.pagosService.getTransactionById(this.transactionId).subscribe({
      next: (transaction) => {
        this.transaction = transaction;
        this.loading = false;
      },
      error: (error) => {
        console.error('Error al cargar transacción:', error);
        this.loading = false;
      }
    });
  }

  descargarComprobante(): void {
    if (!this.transaction) return;

    const contenido = `
      <!DOCTYPE html>
      <html>
      <head>
        <title>Payment Receipt - NaivePay</title>
        <style>
          body { font-family: Arial, sans-serif; margin: 20px; }
          .header { text-align: center; border-bottom: 2px solid #10B981; padding-bottom: 10px; margin-bottom: 20px; }
          .logo { font-size: 24px; font-weight: bold; color: #3B82F6; }
          .success-badge { background: #10B981; color: white; padding: 5px 10px; border-radius: 20px; font-size: 14px; }
          .section { margin-bottom: 12px; padding: 8px 0; border-bottom: 1px solid #E5E7EB; }
          .label { font-weight: bold; color: #555; display: inline-block; width: 140px; }
          .value { color: #111827; }
          .total { font-size: 18px; font-weight: bold; color: #10B981; margin-top: 15px; }
          .footer { margin-top: 30px; text-align: center; font-size: 12px; color: #888; }
        </style>
      </head>
      <body>
        <div class="header">
          <div class="logo">NaivePay</div>
          <h2>SUCCESSFUL PAYMENT RECEIPT</h2>
          <div class="success-badge">✅ TRANSACTION APPROVED</div>
        </div>

        <div class="section">
          <span class="label">Transaction ID:</span>
          <span class="value">${this.transaction.id}</span>
        </div>

        <div class="section">
          <span class="label">Date and Time:</span>
          <span class="value">${new Date().toLocaleString()}</span>
        </div>

        <div class="section">
          <span class="label">Customer:</span>
          <span class="value">${this.transaction.customer}</span>
        </div>

        <div class="section">
          <span class="label">Commerce:</span>
          <span class="value">${this.transaction.commerce}</span>
        </div>

        <div class="section">
          <span class="label">Description:</span>
          <span class="value">${this.transaction.category || 'Service payment'}</span>
        </div>

        <div class="section total">
          <span class="label">TOTAL AMOUNT:</span>
          <span class="value">$ ${this.transaction.amount.toLocaleString()}</span>
        </div>

        <div class="footer">
          <p>Thank you for your preference!</p>
          <p>NaivePay - Secure payment system</p>
          <p>Issue date: ${new Date().toLocaleString()}</p>
        </div>
      </body>
      </html>
    `;
    const ventana = window.open('', '_blank');
    if (ventana) {
      ventana.document.write(contenido);
      ventana.document.close();
      ventana.print();
    }
  }

  verMasTransacciones(): void {
    this.router.navigate(['/pagos']);
  }

  volverInicio(): void {
    this.router.navigate(['/']);
  }

  realizarOtroPago(): void {
    this.router.navigate(['/pagos']);
  }
}