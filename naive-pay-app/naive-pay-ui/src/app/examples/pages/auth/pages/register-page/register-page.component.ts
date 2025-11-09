import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { CheckboxComponent } from '../../../../../shared/components/form/input/checkbox.component';
import { InputFieldComponent } from '../../../../../shared/components/form/input/input-field.component';
import { LabelComponent } from '../../../../../shared/components/form/label/label.component';

@Component({
  selector: 'register-page',
  imports: [
    CommonModule,
    LabelComponent,
    CheckboxComponent,
    InputFieldComponent,
    RouterModule,
    FormsModule,
  ],
  templateUrl: './register-page.component.html',
  styles: ``
})
export default class RegisterPageComponent {
  public showPassword = false;
  public isChecked = false;

  public fname = '';
  public lname = '';
  public email = '';
  public password = '';

  public togglePasswordVisibility() {
    this.showPassword = !this.showPassword;
  }

  public onSignIn() {
    console.log('First Name:', this.fname);
    console.log('Last Name:', this.lname);
    console.log('Email:', this.email);
    console.log('Password:', this.password);
    console.log('Remember Me:', this.isChecked);
  }
}
