import {Component, Input, SimpleChanges} from '@angular/core';
import {SearchCommerceServiceService} from "../../service/search-commerce-service.service";
import {Commerce} from "../../domain/commerce";
import {CommerceCategory} from "../../domain/category";
import {ButtonComponent} from "@shared/components/ui/button/button.component";

@Component({
    selector: 'app-commerce-list',
    imports: [
        ButtonComponent
    ],
    templateUrl: './commerce-list.component.html',
    standalone: true,
    styleUrl: './commerce-list.component.css'
})


export class CommerceListComponent {

    commerces : Commerce[] = []

    filteredCommerces : Commerce[] = []

    allCategories : CommerceCategory[] = []

    selectedCategories : string[] = []

    categoryMenuOpen : boolean = false


    constructor(private searchService : SearchCommerceServiceService) {
    }

    ngOnInit () : void{
        this.loadCommerces();
        this.loadAllCategories();
        this.filteredCommerces = this.commerces

    }



    onCategoryChange(event : Event, categoryName : string) : void {
        const isChecked = (event.target as HTMLInputElement).checked;

        if(isChecked){
            if(!this.selectedCategories.includes(categoryName)){
                this.selectedCategories.push(categoryName);
            }
        } else {
            this.selectedCategories =   this.selectedCategories.filter(name=> name !== categoryName);
        }
    }

    loadAllCategories () : void {
        this.searchService.getAllCategories().subscribe({
            next:(data) => {
                this.allCategories = data;
            },
            error:(error) =>{

            }
        })
    }

    onCategoriesButtonPress(): void{
        this.categoryMenuOpen = !this.categoryMenuOpen;
        this.loadAllCategories()
    }


    loadCommerces() : void {
        this.searchService.getCommerces().subscribe({
            next : (data) =>{
                this.commerces = data;

            },
            error: (error) => {
                console.log("Error retrieving commerces: ", error);
        }
        })
    }

    loadCategoryCommerce() : void {
        this.searchService.getCommercesbyCategory(this.selectedCategories).subscribe({
            next: (data) => {
                this.commerces = data;

            },
            error: (error) => {
            }
        })
    }

}
