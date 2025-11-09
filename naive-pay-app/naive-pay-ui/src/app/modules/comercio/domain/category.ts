import {Commerce} from "./commerce";

export interface CommerceCategory {
    catId: number;
    catName: string;
    catDescription: string;
    catCommerces: Commerce[];
}