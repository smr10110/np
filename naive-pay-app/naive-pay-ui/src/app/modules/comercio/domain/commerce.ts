export interface Commerce {
    comId: number;
    comInfo: {
        comName: string;
        comTaxId: string;
        comLocation: string;
        comEmail: string;
        comContactPhone: string;
        comDescription: string;
    };
    comIsVerified: boolean;
    comValidUntil: Date;
    comCategoriesNames: string[];
}
