import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../services/auth.service';
import { CheckboxComponent } from '../../../../../shared/components/form/input/checkbox.component';
import { InputFieldComponent } from '../../../../../shared/components/form/input/input-field.component';
import { LabelComponent } from '../../../../../shared/components/form/label/label.component';
import { ButtonComponent } from '../../../../../shared/components/ui/button/button.component';

@Component({
  selector: 'app-login-page',
  imports: [
    CommonModule,
    LabelComponent,
    CheckboxComponent,
    ButtonComponent,
    InputFieldComponent,
    RouterModule,
    FormsModule,
  ],
  templateUrl: './login-page.component.html',
  styles: ``
})
export default class LoginPageComponent {
  private readonly authService = inject(AuthService);
  public showPassword = false;
  public isChecked = false;

  public email = '';
  public password = '';

  public togglePasswordVisibility(): void {
    this.showPassword = !this.showPassword;
  }

  public onSignIn(): void {
    this.authService.login(this.email, this.password);
  }
}
