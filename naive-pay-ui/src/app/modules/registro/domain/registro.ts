export interface Registro {
}

export interface RegistrationStart {
    email: string;
    password: string;
}

export interface RegistrationComplete {
    email: string;
    names: string;
    lastNames: string;
    rutGeneral: string;
    verificationDigit: string;
    phoneNumber: string;
    profession: string;
    adress: string;
}

export interface SetDynamicKey {
    email: string;
    dynamicKey: string;
}

