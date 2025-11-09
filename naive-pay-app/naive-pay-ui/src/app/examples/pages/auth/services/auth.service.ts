import { Injectable } from "@angular/core";

@Injectable({ providedIn: 'root' })
export class AuthService {
  
  public login(email: string, password: string): void {
    console.log('Logging in user with email:', email, 'and password:', password);
  }

  public register(email: string, password: string): void {
    console.log('Registering user with email:', email, 'and password:', password);
  }
}