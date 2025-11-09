package cl.ufro.dci.naivepayapi.fondos.dto;

import java.math.BigDecimal;

/**
 * Request DTO for creating a payment transaction pending approval.
 * <p>
 * Used when a payment requires manual validation before executing
 * the actual fund transfer.
 * </p>
 * 
 * @author NaivePay Development Team
 * @version 1.0
 * @since 2025-11-03
 */
public class PendingPaymentRequest {
    
    /** ID of the account from which payment will be made */
    private Long originAccountId;
    
    /** ID of the commerce account receiving the payment */
    private Long destinationAccountId;
    
    /** Payment amount */
    private BigDecimal amount;
    
    /** Payment description */
    private String description;
    
    /** Name of the customer making the payment */
    private String customerName;
    
    /** Name of the commerce receiving the payment */
    private String commerceName;
    
    /** Payment category (e.g., "Food", "Transport") */
    private String category;

    // Constructors
    
    public PendingPaymentRequest() {
    }

    public PendingPaymentRequest(Long originAccountId, Long destinationAccountId, BigDecimal amount,
                                String description, String customerName, String commerceName, String category) {
        this.originAccountId = originAccountId;
        this.destinationAccountId = destinationAccountId;
        this.amount = amount;
        this.description = description;
        this.customerName = customerName;
        this.commerceName = commerceName;
        this.category = category;
    }

    // Getters and Setters
    
    public Long getOriginAccountId() {
        return originAccountId;
    }

    public void setOriginAccountId(Long originAccountId) {
        this.originAccountId = originAccountId;
    }

    public Long getDestinationAccountId() {
        return destinationAccountId;
    }

    public void setDestinationAccountId(Long destinationAccountId) {
        this.destinationAccountId = destinationAccountId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getCommerceName() {
        return commerceName;
    }

    public void setCommerceName(String commerceName) {
        this.commerceName = commerceName;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }
}
