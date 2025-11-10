import { Injectable } from '@angular/core';
import {HttpClient} from "@angular/common/http";

interface Commerce {
    comName: string;
    comTaxId: string;
    comLocation: string;
    comEmail: string;
    comContactPhone: string;
    comDescription: string;
}

@Injectable({
  providedIn: 'root'
})


export class IncomeCommerceServiceService {

    private commerceValidation :string = "http://localhost:8080/";

    private http : HttpClient;


    constructor(http: HttpClient) {
        this.http = http;
    }

    public approveCommerce(id : number, dto : { categoryIds: number[] }) {
        const url = `${this.commerceValidation}validation/approve/${id}`;
        return this.http.put(url, dto);
    }

    public rejectCommerce(id:number) {
        const url = `${this.commerceValidation}validation/reject/${id}`;
        return this.http.put(url, {}); // PUT sin body
    }

    public submitCommerce(commerceIn:Commerce){

        const commerce: Commerce = {
            comName: commerceIn.comName,
            comTaxId: commerceIn.comTaxId,
            comLocation: commerceIn.comLocation,
            comEmail: commerceIn.comEmail,
            comContactPhone: commerceIn.comContactPhone,
            comDescription: commerceIn.comDescription
        }

        const url = `${this.commerceValidation}validation/submit`;
        return this.http.post(url, commerce);

    }



}
