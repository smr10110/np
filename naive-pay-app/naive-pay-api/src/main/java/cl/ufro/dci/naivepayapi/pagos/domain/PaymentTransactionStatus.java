package cl.ufro.dci.naivepayapi.pagos.domain;

/**
 * Represents the possible statuses of a payment transaction throughout its lifecycle.
 * Defines the states a transaction can be in from creation to completion or cancellation.
 */
public enum PaymentTransactionStatus {
    /**
     * Transaction has been created but not yet processed.
     * Awaiting approval or cancellation.
     */
    PENDING,
    
    /**
     * Transaction has been successfully processed and approved.
     * The payment transfer has been completed successfully.
     */
    APPROVED,
    
    /**
     * Transaction was manually canceled before processing.
     * No payment transfer occurred.
     */
    CANCELED,
    
    /**
     * Transaction was rejected during processing.
     * Typically due to insufficient funds or other validation failures.
     */
    REJECTED
}