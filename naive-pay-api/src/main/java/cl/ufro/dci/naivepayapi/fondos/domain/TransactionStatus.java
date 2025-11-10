package cl.ufro.dci.naivepayapi.fondos.domain;

/**
 * Enum that defines the lifecycle status of transactions requiring approval.
 * <p>
 * Used for transactions that need manual review before completion,
 * such as payments to merchants that require validation.
 * </p>
 * 
 * @author NaivePay Development Team
 * @version 1.0
 * @since 2025-11-03
 */
public enum TransactionStatus {
    /**
     * Transaction has been created but not yet processed.
     * Awaiting approval or cancellation.
     */
    PENDING,
    
    /**
     * Transaction has been successfully processed and completed.
     * Funds have been transferred between accounts.
     */
    COMPLETED,
    
    /**
     * Transaction was rejected during processing.
     * Typically due to insufficient funds or validation failures.
     */
    REJECTED,
    
    /**
     * Transaction was manually canceled before processing.
     * No fund transfer occurred.
     */
    CANCELED
}
