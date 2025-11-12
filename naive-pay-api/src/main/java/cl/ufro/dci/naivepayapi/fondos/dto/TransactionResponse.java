package cl.ufro.dci.naivepayapi.fondos.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response DTO for detailed transaction information.
 * <p>
 * Provides transaction data while avoiding lazy loading issues
 * by exposing only account IDs instead of complete entities.
 * </p>
 * 
 * <p><b>Design advantages:</b></p>
 * <ul>
 *   <li>Avoids LazyInitializationException during JSON serialization</li>
 *   <li>Reduces HTTP response size</li>
 *   <li>Allows client to request account details separately if needed</li>
 *   <li>Improves performance by not loading unnecessary relationships</li>
 * </ul>
 * 
 * @author NaivePay Development Team
 * @version 1.0
 * @since 2025-10-06
 */
public class TransactionResponse {
    /** Unique transaction identifier */
    private Long id;
    
    /** Transaction amount */
    private BigDecimal amount;
    
    /** Transaction date and time */
    private LocalDateTime dateTime;
    
    /** Transaction description or concept */
    private String description;
    
    /** Origin account ID (can be null for LOAD operations) */
    private Long originAccountId;
    
    /** Destination account ID */
    private Long destinationAccountId;

    /** Transaction type (LOAD, TRANSFER, PAYMENT, etc.) */
    private String type;
    
    /** Transaction status (PENDING, COMPLETED, REJECTED, CANCELED) */
    private String status;
    
    /** Customer name (for payment transactions) */
    private String customerName;
    
    /** Commerce name (for payment transactions) */
    private String commerceName;
    
    /** Payment category (for payment transactions) */
    private String paymentCategory;

    /**
     * Default constructor.
     */
    public TransactionResponse() {
    }

    /**
     * Complete constructor with all fields.
     * 
     * @param id the transaction ID
     * @param amount the transaction amount
     * @param dateTime the transaction date and time
     * @param description the transaction description
     * @param originAccountId the origin account ID (can be null)
     * @param destinationAccountId the destination account ID
     */
    public TransactionResponse(Long id, BigDecimal amount, LocalDateTime dateTime, String description,
                               Long originAccountId, Long destinationAccountId) {
        this.id = id;
        this.amount = amount;
        this.dateTime = dateTime;
        this.description = description;
        this.originAccountId = originAccountId;
        this.destinationAccountId = destinationAccountId;
    }

    // Getters and Setters
    
    /**
     * Gets the transaction ID.
     * 
     * @return the transaction ID
     */
    public Long getId() {
        return id;
    }

    /**
     * Sets the transaction ID.
     * 
     * @param id the transaction ID
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * Gets the transaction amount.
     * 
     * @return the amount
     */
    public BigDecimal getAmount() {
        return amount;
    }

    /**
     * Sets the transaction amount.
     * 
     * @param amount the amount
     */
    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    /**
     * Gets the transaction date and time.
     * 
     * @return the date and time
     */
    public LocalDateTime getDateTime() {
        return dateTime;
    }

    /**
     * Sets the transaction date and time.
     * 
     * @param dateTime the date and time
     */
    public void setDateTime(LocalDateTime dateTime) {
        this.dateTime = dateTime;
    }

    /**
     * Gets the transaction description.
     * 
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the transaction description.
     * 
     * @param description the description
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Gets the origin account ID.
     * 
     * @return the origin account ID, or null for LOAD type transactions
     */
    public Long getOriginAccountId() {
        return originAccountId;
    }

    /**
     * Sets the origin account ID.
     * 
     * @param originAccountId the origin account ID
     */
    public void setOriginAccountId(Long originAccountId) {
        this.originAccountId = originAccountId;
    }

    /**
     * Gets the destination account ID.
     * 
     * @return the destination account ID
     */
    public Long getDestinationAccountId() {
        return destinationAccountId;
    }

    /**
     * Sets the destination account ID.
     * 
     * @param destinationAccountId the destination account ID
     */
    public void setDestinationAccountId(Long destinationAccountId) {
        this.destinationAccountId = destinationAccountId;
    }

    /**
     * Gets the transaction type.
     * 
     * @return the transaction type as String
     * @since 2.0
     */
    public String getType() {
        return type;
    }

    /**
     * Sets the transaction type.
     * 
     * @param type the transaction type
     * @since 2.0
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * Gets the transaction status.
     * 
     * @return the transaction status as String
     * @since 2.0
     */
    public String getStatus() {
        return status;
    }

    /**
     * Sets the transaction status.
     * 
     * @param status the transaction status
     * @since 2.0
     */
    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * Gets the customer name.
     * 
     * @return the customer name
     * @since 2.0
     */
    public String getCustomerName() {
        return customerName;
    }

    /**
     * Sets the customer name.
     * 
     * @param customerName the customer name
     * @since 2.0
     */
    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    /**
     * Gets the commerce name.
     * 
     * @return the commerce name
     * @since 2.0
     */
    public String getCommerceName() {
        return commerceName;
    }

    /**
     * Sets the commerce name.
     * 
     * @param commerceName the commerce name
     * @since 2.0
     */
    public void setCommerceName(String commerceName) {
        this.commerceName = commerceName;
    }

    /**
     * Gets the payment category.
     * 
     * @return the payment category
     * @since 2.0
     */
    public String getPaymentCategory() {
        return paymentCategory;
    }

    /**
     * Sets the payment category.
     * 
     * @param paymentCategory the payment category
     * @since 2.0
     */
    public void setPaymentCategory(String paymentCategory) {
        this.paymentCategory = paymentCategory;
    }

    /**
     * Generates a String representation of the object.
     * 
     * @return a string with the transaction data
     */
    @Override
    public String toString() {
        return "TransactionResponse{" +
                "id=" + id +
                ", amount=" + amount +
                ", dateTime=" + dateTime +
                ", description='" + description + '\'' +
                ", originAccountId=" + originAccountId +
                ", destinationAccountId=" + destinationAccountId +
                ", type='" + type + '\'' +
                ", status='" + status + '\'' +
                ", customerName='" + customerName + '\'' +
                ", commerceName='" + commerceName + '\'' +
                ", paymentCategory='" + paymentCategory + '\'' +
                '}';
    }
}
