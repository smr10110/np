import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClientModule } from '@angular/common/http';
import { RecompensasComponent } from '../component/recompensas/recompensas.component';
import { FormsModule } from '@angular/forms';

@NgModule({
    declarations: [RecompensasComponent],
    imports: [CommonModule, FormsModule, HttpClientModule],
    exports: [RecompensasComponent]
})
export class RecompensasModule { }
