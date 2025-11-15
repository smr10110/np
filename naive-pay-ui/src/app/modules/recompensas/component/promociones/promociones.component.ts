import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { RecompensasService } from '../../service/recompensas.service';

@Component({
    selector: 'app-promociones',
    standalone: true,
    imports: [CommonModule, FormsModule],
    templateUrl: './promociones.component.html',
    styleUrls: ['./promociones.component.css']
})
export class PromocionesComponent {
    promotions: any[] = [];
    selectedPromo: any = null;
    filter = { category: "", date: "", state: "" };

    constructor(
        private recompensasService: RecompensasService,
        private router: Router
    ) {}

    ngOnInit() {
        this.loadPromos();
    }

    loadPromos() {
        this.recompensasService.getPromotions().subscribe({
            next: promos => {
                this.promotions = promos.map((p, i) => ({
                    title: p,
                    description:
                        i === 0
                            ? 'Obtén el doble de puntos en tus compras de productos electrónicos durante este mes.'
                            : i === 1
                                ? 'Triplica tus puntos en supermercados participantes durante todo el mes.'
                                : 'Canjea tus puntos y recibe $1000 de descuento por cada 1000 puntos.',
                    points: 1000 + i * 500,
                    category: i % 2 === 0 ? 'Compras' : 'Canje',
                    expire: '31-10-2025',
                    state: i % 2 === 0 ? 'Activa' : 'Finalizada',
                    bonus: i % 2 === 0
                        ? 'Acumula puntos más rápido y obtén beneficios exclusivos en tiendas asociadas.'
                        : 'Ahorra más en tus compras al canjear tus puntos de forma inteligente.',
                    restricitions: 'Promoción válida hasta agotar stock o fecha de vencimiento.'
                }));
            },
            error: err => console.error(err)
        });
    }

    applyFilter() {
        console.log('Filtro aplicado:', this.filter);

        let leaked = [...this.promotions];

        if (this.filter.category.trim()) {
            leaked = leaked.filter(p =>
                p.category.toLowerCase().includes(this.filter.category.toLowerCase())
            );
        }

        if (this.filter.state.trim()) {
            leaked = leaked.filter(p =>
                p.state.toLowerCase() === this.filter.state.toLowerCase()
            );
        }

        if (this.filter.date.trim()) {
            leaked = leaked.filter(p => p.expire === this.filter.date);
        }

        this.promotions = leaked;
    }

    seeDetail(promo: any) {
        this.selectedPromo = promo;
    }

    returnList() {
        this.selectedPromo = null;
    }

    return() {
        this.router.navigate(['/recompensas']);
    }
}

