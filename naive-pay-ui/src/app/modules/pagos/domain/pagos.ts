export interface Pagos {
  id: number;
  originAccount: number;
  destinationAccount: number;
  customer: string;
  commerce: string;
  amount: number;
  category: string;
  status: 'PENDING' | 'APPROVED' | 'CANCELED'| 'REJECTED';
  createdAt: string;
}

// DTO para transacciones pendientes
export interface PendingTransactionDTO {
  id: number;
  amount: number;
  commerce: string;
}