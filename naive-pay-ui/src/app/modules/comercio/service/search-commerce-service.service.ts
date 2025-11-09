import { Injectable } from '@angular/core';
import {HttpClient, HttpHeaders, HttpParams} from '@angular/common/http';
import {Observable} from "rxjs";
import {CommerceCategory} from "../domain/category";
import {Commerce} from "../domain/commerce";
import {CommerceInfo} from "../domain/commerceInfo";


export interface CommerceValidation {
    comValId: number ;
    comInfo: CommerceInfo;
    documentation: string[];
    comStatus: 'PENDING' | 'VALIDATED' | 'INVALID' | 'EXPIRED';
}



@Injectable({
  providedIn: 'root'
})
export class SearchCommerceServiceService {
  
	private commerceSearchEP = "http://localhost:8080/"


	constructor(private http: HttpClient){}

	public getCommerces(): Observable<Commerce[]>{
		return this.http.get<Commerce[]>(`${this.commerceSearchEP}commerce/all`)
	}

	public getCommercesbyCategory(categories:string[]): Observable<Commerce[]> {
		let url = `${this.commerceSearchEP}categories`;
		if (categories.length >0) {
			url+=`?category=${categories.join(',')}`;
		}
		return this.http.get<Commerce[]>(url)
		
	}

    public getAllCategories():Observable<CommerceCategory[]>{
        return this.http.get<CommerceCategory[]>(`${this.commerceSearchEP}categories`)

    }

    public getPendingCommerce():Observable<CommerceValidation[]>{
        let url = `${this.commerceSearchEP}validation/pending`
        return this.http.get<CommerceValidation[]>(url);
    }

    public getRejectedCommerce(): Observable<CommerceValidation[]>{
        let url = `${this.commerceSearchEP}validation/rejected`
        return this.http.get<CommerceValidation[]>(url);
    }

    public getExpiredCommerce(): Observable<CommerceValidation[]>{
        let url = `${this.commerceSearchEP}validation/expired`
        return this.http.get<CommerceValidation[]>(url);
    }

    public getValidCommerce(): Observable<CommerceValidation[]>{
        let url = `${this.commerceSearchEP}commerce/all`
        return this.http.get<CommerceValidation[]>(url);

    }

    public getCommerceByCategories(selectedCategories : String[]): Observable<Commerce[]>{
        let url = `${this.commerceSearchEP}categories/search`

        let parameters = new HttpParams();

        if (selectedCategories && selectedCategories.length > 0){
            const categoryString = selectedCategories.join(',')
            parameters = parameters.set('category', categoryString)

        }

        return this.http.get<Commerce[]>(url, {params : parameters})


    }
}	
