import { Component, Input, Output, EventEmitter } from '@angular/core';
import { Transaction } from '../../domain/reporte';
import { DatePipe, DecimalPipe } from '@angular/common';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-report-table',
  templateUrl: './report-table.component.html',
  styleUrls: ['./report-table.component.css'],
  imports: [CommonModule, DatePipe, DecimalPipe],
})
export class ReportTableComponent {
  @Input() pagedTxs: Transaction[] = [];
  @Input() filteredTxs: Transaction[] = [];
  @Input() currentPage: number = 1;
  @Input() totalPages: number = 1;
  @Input() loading: boolean = false;
  @Input() error: string = '';
  @Output() prevPage = new EventEmitter<void>();
  @Output() nextPage = new EventEmitter<void>();
}
