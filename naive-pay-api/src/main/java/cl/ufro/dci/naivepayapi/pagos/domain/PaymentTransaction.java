package cl.ufro.dci.naivepayapi.pagos.domain;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Represents a payment transaction entity in the system.
 * Stores information about money transfers between accounts including origin, destination, amounts, and status.
 */
@Data
@Entity
public class PaymentTransaction {

    /**
     * Unique identifier for the payment transaction.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * ID of the account from which the payment originates.
     */
    private Long originAccount;

    /**
     * ID of the account receiving the payment.
     */
    private Long destinationAccount;

    /**
     * Name of the customer making the payment.
     */
    private String customer;

    /**
     * Name of the commerce or business receiving the payment.
     */
    private String commerce;

    /**
     * Amount of money being transferred in the transaction.
     */
    private BigDecimal amount;

    /**
     * Category or description of the payment transaction.
     */
    private String category;

    /**
     * Current status of the payment transaction.
     */
    @Enumerated(EnumType.STRING)
    private PaymentTransactionStatus status;

    /**
     * Timestamp when the transaction was created.
     */
    private LocalDateTime createdAt;

    /**
     * Pre-persistence callback to set default values before saving the entity.
     * Sets the creation timestamp and initializes the transaction status to PENDING.
     */
    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.status = PaymentTransactionStatus.PENDING;
    }
}