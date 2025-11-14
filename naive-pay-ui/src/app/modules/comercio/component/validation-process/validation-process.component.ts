import {Component} from '@angular/core';
import {CommonModule} from '@angular/common';
import {CommerceValidation, SearchCommerceServiceService} from "../../service/search-commerce-service.service";
import {IncomeCommerceServiceService} from "../../service/income-commerce-service.service";
import {ButtonComponent} from "@shared/components/ui/button/button.component";
import {Commerce} from "../../domain/commerce";

export interface CommerceResponse {
    id: string;
    name: string;
    taxId: string;
    categories: string[];
    location?: string;
}
export interface CommerceCreation {
    name: string;
    taxId: string;
    categories: string[];
    location?: string;
}
export interface CommerceUpdate {
    name?: string;
    categories?: string[];
    location?: string;
}
// Use the shared interface from service
export interface CommerceInfo {
    name: string;
    taxId: string;
    location: string;
    email: string;
    contact: string;
    description: string;
}




@Component({
    selector: 'app-validation-process',
    standalone: true,
    imports: [
        CommonModule,
        ButtonComponent
    ],
    templateUrl: './validation-process.component.html',
    styleUrl: './validation-process.component.css'
})
export class ValidationProcessComponent {

    pendingCommerces : CommerceValidation[] = [];
    rejectedCommerces : CommerceValidation[] = [];
    expiredCommerces : CommerceValidation[] = [];
    validCommerces : Commerce[] = [];


    constructor(private searchService : SearchCommerceServiceService, private validationService : IncomeCommerceServiceService) {
    }

    ngOnInit () : void{
        console.info('[Validation] Inicializando vista de validacion de comercios');
        this.loadPendingCommerces();
        this.loadRejectedCommmerces();
        this.loadExpiredCommmerces();
        this.loadValidCommmerces()
    }



    loadPendingCommerces() : void {
        this.searchService.getPendingCommerce().subscribe({
            next : (data) =>{
                this.pendingCommerces = data;
                console.info('[Validation] Pendientes cargados', { total: data.length });
            },
            error: (error) => {
                console.error("[Validation] Error al obtener pendientes", error);
            }
        })
    }

    loadRejectedCommmerces() : void {
        this.searchService.getRejectedCommerce().subscribe({
            next : (data) =>{
                this.rejectedCommerces = data;
                console.info('[Validation] Rechazados cargados', { total: data.length });
            },
            error: (error) => {
                console.error("[Validation] Error al obtener rechazados", error);
            }
        })
    }

    loadExpiredCommmerces() : void {
        this.searchService.getExpiredCommerce().subscribe({
            next : (data) =>{
                this.expiredCommerces = data;
                console.info('[Validation] Expirados cargados', { total: data.length });
            },
            error: (error) => {
                console.error("[Validation] Error al obtener expirados", error);
            }
        })
    }

    loadValidCommmerces() : void {
        this.searchService.getCommerces().subscribe({
            next : (data) =>{
                this.validCommerces = data;
                console.info('[Validation] Comercios validados cargados', { total: data.length });
            },
            error: (error) => {
                console.error("[Validation] Error al obtener comercios validados", error);
            }
        })
    }


    approveCommerce(comValId: number): void {
        var dto: { categoryIds: number[] } = { categoryIds:[0]};
        console.info('[Validation] Aprobando comercio', { comValId });
        this.validationService.approveCommerce(comValId, dto).subscribe({
            next: () => {
                console.info('[Validation] Comercio aprobado', { comValId });
                this.loadPendingCommerces();
                this.loadValidCommmerces();
            },
            error: (error) => {
                console.error('[Validation] Error aprobando comercio', error);
            }
        });
    }

    rejectCommerce(comValId: number): void {
        console.info('[Validation] Rechazando comercio', { comValId });
        this.validationService.rejectCommerce(comValId).subscribe({
            next: () => {
                console.info('[Validation] Comercio rechazado', { comValId });
                this.loadPendingCommerces();
                this.loadRejectedCommmerces();
            },
            error: (error) => {
                console.error('[Validation] Error rechazando comercio', error);
            }
        });
    }

}

