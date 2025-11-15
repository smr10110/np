import { Component } from '@angular/core';
import {HttpClient} from "@angular/common/http";
import {FormsModule, NgForm} from "@angular/forms";
import {lastValueFrom} from "rxjs";
import {CommonModule} from "@angular/common";
import {ButtonComponent} from "@shared/components/ui/button/button.component";
import {IncomeCommerceServiceService} from "../../service/income-commerce-service.service";


interface CommerceValidationResponse {
    comId: number;
    comInfo: {
        comName: string;
        comTaxId: string;
        comLocation: string;
        comEmail: string;
        comContactPhone: string;
        comDescription: string;
    };
}

interface Commerce {
    comName: string;
    comTaxId: string;
    comLocation: string;
    comEmail: string;
    comContactPhone: string;
    comDescription: string;
}
@Component({
  selector: 'app-income-commerce-form',
    standalone:true,
    imports: [CommonModule, FormsModule],
  templateUrl: './income-commerce-form.component.html',
  styleUrl: './income-commerce-form.component.css'
})
export class IncomeCommerceFormComponent {


    incomeCommerce : IncomeCommerceServiceService;


    isSaving :boolean = false;

    validationId : number | null = null;

    commerceData: Commerce= {
        comName: '',
        comTaxId: '',
        comLocation: '',
        comEmail: '',
        comContactPhone: '',
        comDescription: '',
    };
    errorMessage: string | null = null;

    constructor(incomeCommerceService: IncomeCommerceServiceService) {
        this.incomeCommerce = incomeCommerceService
    }

    async saveCommerce(form: NgForm): Promise<void>{
        this.isSaving = true;
        this.validationId = null;
        this.errorMessage = null;

        const payload: Commerce = {
            comName: this.commerceData.comName,
            comTaxId: this.commerceData.comTaxId,
            comLocation: this.commerceData.comLocation,
            comEmail: this.commerceData.comEmail,
            comContactPhone: this.commerceData.comContactPhone,
            comDescription: this.commerceData.comDescription,
        };


        this.incomeCommerce.submitCommerce(payload).subscribe({
            next : (payload) =>{
            this.isSaving = false;
            form.resetForm()

            },
            error: (error) => {
                console.log("Error retrieving commerces: ", error);
            }})


    }
}
