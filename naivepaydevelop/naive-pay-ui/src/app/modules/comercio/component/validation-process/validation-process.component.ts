import {Component, Input, SimpleChanges} from '@angular/core';
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
    imports: [
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

    ngOnInit () : void{ this.loadPendingCommerces(); this.loadRejectedCommmerces(); this.loadExpiredCommmerces(); this.loadValidCommmerces()}



    loadPendingCommerces() : void {
        this.searchService.getPendingCommerce().subscribe({
            next : (data) =>{
                this.pendingCommerces = data;

            },
            error: (error) => {
                console.log("Error retrieving commerces: ", error);
            }
        })
    }

    loadRejectedCommmerces() : void {
        this.searchService.getRejectedCommerce().subscribe({
            next : (data) =>{
                this.rejectedCommerces = data;
            },
            error: (error) => {
                console.log("Error retrieving rejected: ", error);
            }
        })
    }

    loadExpiredCommmerces() : void {
        this.searchService.getExpiredCommerce().subscribe({
            next : (data) =>{
                this.expiredCommerces = data;
            },
            error: (error) => {
                console.log("Error retrieving expired commerce: ", error);
            }
        })
    }

    loadValidCommmerces() : void {
        this.searchService.getCommerces().subscribe({
            next : (data) =>{
                this.validCommerces = data;
            },
            error: (error) => {
                console.log("Error retrieving valid commerce: ", error);
            }
        })
    }


    approveCommerce(comValId: number): void {
        var dto: { categoryIds: number[] } = { categoryIds:[0]};
        this.validationService.approveCommerce(comValId, dto).subscribe({
            next: () => {
                this.loadPendingCommerces();
                this.loadValidCommmerces();
            },
            error: (error) => {
                console.error('Error approving commerce', error);
            }
        });
    }

    rejectCommerce(comValId: number): void {
        this.validationService.rejectCommerce(comValId).subscribe({
            next: () => {
                this.loadPendingCommerces();
                this.loadRejectedCommmerces();
            },
            error: (error) => {
                console.error('Error rejecting commerce', error);
            }
        });
    }

}

