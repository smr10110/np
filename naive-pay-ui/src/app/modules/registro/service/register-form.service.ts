// src/app/modules/registro/service/registro-form.service.ts
import { Injectable } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';

@Injectable({
    providedIn: 'root'
})
export class RegisterFormService {

    constructor(private fb: FormBuilder) { }

    public createStartForm(): FormGroup {
        return this.fb.group({
            email: ['', [Validators.required, Validators.email]],
            password: ['', [Validators.required, Validators.minLength(8)]]
        });
    }

    public createVerifyForm(): FormGroup {
        return this.fb.group({
            code: ['', [Validators.required, Validators.minLength(6), Validators.maxLength(6)]]
        });
    }

    public createCompleteForm(): FormGroup {
        return this.fb.group({
            names: ['', Validators.required],
            lastNames: ['', Validators.required],
            rutGeneral: ['', [Validators.required, Validators.pattern(/^[0-9]+$/)]],
            verificationDigit: ['', [Validators.required, Validators.pattern(/^[0-9kK]$/)]],
            phoneNumber: ['', [Validators.required, Validators.maxLength(9), Validators.pattern(/^[0-9]+$/), Validators.minLength(9)]],
            profession: ['', Validators.required],
            adress: ['', Validators.required]
        });
    }

    public createSetKeyForm(): FormGroup {
        return this.fb.group({
            dynamicKey: ['', [Validators.required, Validators.pattern(/^\d{6}$/), Validators.maxLength(6), Validators.minLength(6)]]
        });
    }
}
