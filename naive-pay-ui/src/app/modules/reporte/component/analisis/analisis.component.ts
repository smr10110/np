import { Component, Input, OnChanges, SimpleChanges, ChangeDetectorRef } from '@angular/core';
import { CommonModule, DecimalPipe } from '@angular/common';
import { SpendingReport, Transaction } from '../../domain/reporte';
import { NgApexchartsModule, ApexAxisChartSeries, ApexChart, ApexDataLabels, ApexTooltip, ApexXAxis, ApexYAxis, ApexTitleSubtitle } from 'ng-apexcharts';

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

  selectedTab: 'gastos' | 'ingresos' | 'total' = 'total';

  categoryChartOptions: any;
  dateChartOptions: any;
  comercioChartOptions: any;
  comparativoChartOptions: any;

  showCat: boolean = false;
  showCom: boolean = false;
  showDate: boolean = false;
  showComp: boolean = false;

  constructor(private cdr: ChangeDetectorRef) {}

  ngOnInit() {
    this.prepareCharts();
  }

  ngOnChanges(changes: SimpleChanges) {
    if (changes['avg']) {
      console.log('AnalisisComponent avg recibido:', changes['avg'].currentValue);
      this.cdr.detectChanges();
    }
    if (changes['txs'] && !changes['txs'].firstChange) {
      this.prepareCharts();
    }
  }

  setTab(tab: 'gastos' | 'ingresos' | 'total') {
    this.selectedTab = tab;
    this.prepareCharts();
    // Reiniciar acordeones al cambiar de pestaña (todos ocultos)
    this.showCat = false;
    this.showCom = false;
    this.showDate = false;
    this.showComp = false;
  }

  prepareCharts() {
    const gastos = this.txs.filter(tx => tx.amount < 0);
    const ingresos = this.txs.filter(tx => tx.amount > 0);
    if (this.selectedTab === 'gastos') {
      this.categoryChartOptions = this.buildChart(gastos, 'category', 'Gastos por Categoría');
      this.comercioChartOptions = this.buildChart(gastos, 'commerce', 'Gastos por Comercio');
      this.dateChartOptions = this.buildChart(gastos, 'createdAt', 'Gastos por Fecha');
    } else if (this.selectedTab === 'ingresos') {
      this.categoryChartOptions = this.buildChart(ingresos, 'category', 'Ingresos por Descripción');
      this.dateChartOptions = this.buildChart(ingresos, 'createdAt', 'Ingresos por Fecha');
    } else if (this.selectedTab === 'total') {
      this.comparativoChartOptions = this.buildComparativoChart(gastos, ingresos);
    }
  }

  buildChart(data: Transaction[], key: string, title: string) {
    const grouped = data.reduce((acc, tx) => {
      let k: string;
      if (key === 'createdAt') {
        k = tx.createdAt ? tx.createdAt.slice(0, 10) : 'Sin dato';
      } else if (key === 'category') {
        k = tx.category || 'Sin dato';
      } else if (key === 'commerce') {
        k = tx.commerce || 'Sin dato';
      } else {
        k = 'Sin dato';
      }
      acc[k] = (acc[k] || 0) + Math.abs(tx.amount);
      return acc;
    }, {} as Record<string, number>);
    return {
      chart: { type: 'bar', height: 320 },
      series: [{ name: title, data: Object.values(grouped) }],
      xaxis: { categories: Object.keys(grouped) },
      dataLabels: { enabled: false },
      tooltip: { y: { formatter: (v: number) => (v ?? 0).toLocaleString() } },
      yaxis: { labels: { formatter: (v: number) => (v ?? 0).toLocaleString() } },
      title: { text: title },
    };
  }

  buildComparativoChart(gastos: Transaction[], ingresos: Transaction[]) {
    const totalGastos = gastos.reduce((sum, tx) => sum + Math.abs(tx.amount), 0);
    const totalIngresos = ingresos.reduce((sum, tx) => sum + tx.amount, 0);
    return {
      chart: { type: 'bar', height: 320 },
      series: [
        { name: 'Gastos', data: [totalGastos] },
        { name: 'Ingresos', data: [totalIngresos] }
      ],
      xaxis: { categories: ['Comparativo'] },
      dataLabels: { enabled: false },
      tooltip: { y: { formatter: (v: number) => (v ?? 0).toLocaleString() } },
      yaxis: { labels: { formatter: (v: number) => (v ?? 0).toLocaleString() } },
      title: { text: 'Comparativo Ingreso/Gasto' },
    };
  }

  keys(obj: Record<string, number> | undefined | null) {
    return Object.keys(obj ?? {});
  }

  getAvgMensual(): number | undefined {
    const gastos = this.txs.filter(tx => tx.amount < 0);
    if (gastos.length === 0) return undefined;
    const meses = new Set(gastos.map(tx => tx.createdAt.substring(0,7)));
    const totalGastos = gastos.reduce((sum, tx) => sum + Math.abs(tx.amount), 0);
    return meses.size > 0 ? totalGastos / meses.size : undefined;
  }

  getAvgMensualIngresos(): number | undefined {
    const ingresos = this.txs.filter(tx => tx.amount > 0);
    if (ingresos.length === 0) return undefined;
    const meses = new Set(ingresos.map(tx => tx.createdAt.substring(0,7)));
    const totalIngresos = ingresos.reduce((sum, tx) => sum + tx.amount, 0);
    return meses.size > 0 ? totalIngresos / meses.size : undefined;
  }
}
