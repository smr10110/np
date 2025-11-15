import { Component, Input, OnChanges, SimpleChanges, ChangeDetectorRef } from '@angular/core';
import { CommonModule, DecimalPipe } from '@angular/common';
import { SpendingReport, Transaction } from '../../domain/report';
import { NgApexchartsModule } from 'ng-apexcharts';

@Component({
    selector: 'app-analisis',
    templateUrl: './analisis.component.html',
    styleUrls: ['./analisis.component.css'],
    imports: [CommonModule, DecimalPipe, NgApexchartsModule]
})
export class AnalisisComponent implements OnChanges {
    @Input() report?: SpendingReport;
    private _avg?: number;

    @Input()
    set avg(val: number | undefined) {
        this._avg = val;
    }
    get avg(): number | undefined {
        return this._avg;
    }

    @Input() txs: Transaction[] = [];
    @Input() loading: boolean = false;
    @Input() error: string = '';

    selectedTab: 'expenses' | 'income' | 'total' = 'total';

    categoryChartOptions: any;
    dateChartOptions: any;
    commerceChartOptions: any;
    comparativeChartOptions: any;

    showCategory: boolean = false;
    showCommerce: boolean = false;
    showDate: boolean = false;
    showComparative: boolean = false;

    constructor(private cdr: ChangeDetectorRef) {}

    ngOnInit() {
        this.prepareCharts();
    }

    ngOnChanges(changes: SimpleChanges) {
        if (changes['avg']) {
            console.log('AnalisisComponent avg received:', changes['avg'].currentValue);
            this.cdr.detectChanges();
        }
        if (changes['txs'] && !changes['txs'].firstChange) {
            this.prepareCharts();
        }
    }

    setTab(tab: 'expenses' | 'income' | 'total') {
        this.selectedTab = tab;
        this.prepareCharts();

        // Reset all accordions when changing tabs
        this.showCategory = false;
        this.showCommerce = false;
        this.showDate = false;
        this.showComparative = false;
    }

    prepareCharts() {
        const expenses = this.txs.filter(tx => tx.traAmount < 0);
        const income = this.txs.filter(tx => tx.traAmount > 0);

        if (this.selectedTab === 'expenses') {
            this.categoryChartOptions = this.buildChart(expenses, 'category', 'Gastos por Categoría');
            this.commerceChartOptions = this.buildChart(expenses, 'commerce', 'Gastos por Comercio');
            this.dateChartOptions = this.buildChart(expenses, 'traDateTime', 'Gastos por Fecha');
        } else if (this.selectedTab === 'income') {
            this.categoryChartOptions = this.buildChart(income, 'category', 'Ingresos por Descripción');
            this.dateChartOptions = this.buildChart(income, 'traDateTime', 'Ingresos por Fecha');
        } else if (this.selectedTab === 'total') {
            this.comparativeChartOptions = this.buildComparativeChart(expenses, income);
        }
    }

    buildChart(data: Transaction[], key: string, title: string) {
        const grouped = data.reduce((acc, tx) => {
            let k: string;
            if (key === 'traDateTime') {
                k = tx.traDateTime ? tx.traDateTime.slice(0, 10) : 'Sin datos';
            } else if (key === 'category') {
                k = tx.traPaymentCategory || 'Sin datos';
            } else if (key === 'commerce') {
                k = tx.traCommerceName || 'Sin datos';
            } else {
                k = 'Sin datos';
            }
            acc[k] = (acc[k] || 0) + Math.abs(tx.traAmount);
            return acc;
        }, {} as Record<string, number>);

        // Round all values to 2 decimal places to avoid floating point issues
        const roundedData = Object.values(grouped).map(v => parseFloat((Math.round(v * 100) / 100).toFixed(2)));

        return {
            chart: { type: 'bar', height: 320 },
            series: [{ name: title, data: roundedData }],
            xaxis: { categories: Object.keys(grouped) },
            dataLabels: { enabled: false },
            tooltip: { y: { formatter: (v: number) => v.toFixed(2) } },
            yaxis: {
                decimalsInFloat: 0,
                labels: {
                    formatter: (v: number) => parseFloat(v.toFixed(2)).toString()
                }
            },
            title: { text: title },
        };
    }

    buildComparativeChart(expenses: Transaction[], income: Transaction[]) {
        const totalExpenses = expenses.reduce((sum, tx) => sum + Math.abs(tx.traAmount), 0);
        const totalIncome = income.reduce((sum, tx) => sum + tx.traAmount, 0);

        // Round to 2 decimal places
        const roundedExpenses = parseFloat((Math.round(totalExpenses * 100) / 100).toFixed(2));
        const roundedIncome = parseFloat((Math.round(totalIncome * 100) / 100).toFixed(2));

        return {
            chart: { type: 'bar', height: 320 },
            series: [
                { name: 'Gastos', data: [roundedExpenses] },
                { name: 'Ingresos', data: [roundedIncome] }
            ],
            xaxis: { categories: ['Comparativo'] },
            dataLabels: { enabled: false },
            tooltip: { y: { formatter: (v: number) => v.toFixed(2) } },
            yaxis: {
                decimalsInFloat: 0,
                labels: {
                    formatter: (v: number) => parseFloat(v.toFixed(2)).toString()
                }
            },
            title: { text: 'Comparativo Ingreso/Gasto' },
        };
    }

    getMonthlyAverageExpenses(): number | undefined {
        const expenses = this.txs.filter(tx => tx.traAmount < 0);
        if (expenses.length === 0) return undefined;
        const months = new Set(expenses.map(tx => tx.traDateTime.substring(0, 7)));
        const totalExpenses = expenses.reduce((sum, tx) => sum + Math.abs(tx.traAmount), 0);
        return months.size > 0 ? totalExpenses / months.size : undefined;
    }

    getMonthlyAverageIncome(): number | undefined {
        const income = this.txs.filter(tx => tx.traAmount > 0);
        if (income.length === 0) return undefined;
        const months = new Set(income.map(tx => tx.traDateTime.substring(0, 7)));
        const totalIncome = income.reduce((sum, tx) => sum + tx.traAmount, 0);
        return months.size > 0 ? totalIncome / months.size : undefined;
    }
}
