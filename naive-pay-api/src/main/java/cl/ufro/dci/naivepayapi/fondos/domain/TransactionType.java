package cl.ufro.dci.naivepayapi.fondos.domain;

/**
 * Enum that defines transaction types in the NaivePay system.
 * Allows classification and auditing of operations performed on accounts.
 * 
 * @author NaivePay Development Team
 * @version 2.0
 * @since 2025-10-06
 */
public enum TransactionType {
    /**
     * Balance load to account from system.
     * Origin account can be null or a special system account.
     */
    LOAD,
    
    /**
     * Transfer between user accounts.
     * Requires both origin and destination accounts.
     */
    TRANSFER,
    
    /**
     * Payment to a commerce or service - COMPLETED.
     * Origin account is the user, destination is the commerce.
     * Funds have been transferred.
     */
    PAYMENT,
    
    /**
     * Payment to a commerce - PENDING APPROVAL.
     * Transaction created but awaiting manual approval.
     * No funds transferred yet.
     * 
     * @since 2.0
     */
    PAYMENT_PENDING,
    
    /**
     * Payment to a commerce - REJECTED.
     * Transaction was rejected (e.g., insufficient funds).
     * No funds transferred.
     * 
     * @since 2.0
     */
    PAYMENT_REJECTED,
    
    /**
     * Payment to a commerce - CANCELED.
     * Transaction was manually canceled before approval.
     * No funds transferred.
     * 
     * @since 2.0
     */
    PAYMENT_CANCELED
}
