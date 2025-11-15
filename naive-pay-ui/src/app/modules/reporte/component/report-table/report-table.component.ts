import { Component, Input, Output, EventEmitter } from '@angular/core';
import { Transaction } from '../../domain/report';
import { DatePipe, DecimalPipe } from '@angular/common';
import { CommonModule } from '@angular/common';

/**
 * Reusable table component for displaying filtered and paginated transactions.
 *
 * Displays transaction date, commerce, category, state, and amount
 * (positive for income, negative for expenses).
 *
 * Provides pager controls and emits events for navigation.
 */
@Component({
    selector: 'app-report-table',
    templateUrl: './report-table.component.html',
    styleUrls: ['./report-table.component.css'],
    standalone: true,
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
