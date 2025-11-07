package cl.ufro.dci.naivepayapi.fondos.domain;

/**
 * Enum that defines transaction types in the NaivePay system.
 * Allows classification and auditing of operations performed on accounts.
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
     * Payment to a commerce or service.
     * Origin account is the user, destination is the commerce.
     */
    PAYMENT
}
