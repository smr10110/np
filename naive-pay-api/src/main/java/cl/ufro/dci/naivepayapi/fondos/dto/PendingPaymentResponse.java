package cl.ufro.dci.naivepayapi.fondos.dto;

import cl.ufro.dci.naivepayapi.fondos.domain.TransactionStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response DTO for pending payment information.
 * <p>
 * Provides details of a payment awaiting approval, including
 * customer and commerce information.
 * </p>
 * 
 * @author NaivePay Development Team
 * @version 1.0
 * @since 2025-11-03
 */
public class PendingPaymentResponse {
    
    private Long id;
    private BigDecimal amount;
    private String commerceName;
    private String customerName;
    private String category;
    private String description;
    private LocalDateTime dateTime;
    private TransactionStatus status;
    private Long originAccountId;
    private Long destinationAccountId;

    // Constructors
    
    public PendingPaymentResponse() {
    }

    public PendingPaymentResponse(Long id, BigDecimal amount, String commerceName, String customerName,
                                 String category, String description, LocalDateTime dateTime,
                                 TransactionStatus status, Long originAccountId, Long destinationAccountId) {
        this.id = id;
        this.amount = amount;
        this.commerceName = commerceName;
        this.customerName = customerName;
        this.category = category;
        this.description = description;
        this.dateTime = dateTime;
        this.status = status;
        this.originAccountId = originAccountId;
        this.destinationAccountId = destinationAccountId;
    }

    // Getters and Setters
    
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCommerceName() {
        return commerceName;
    }

    public void setCommerceName(String commerceName) {
        this.commerceName = commerceName;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getDateTime() {
        return dateTime;
    }

    public void setDateTime(LocalDateTime dateTime) {
        this.dateTime = dateTime;
    }

    public TransactionStatus getStatus() {
        return status;
    }

    public void setStatus(TransactionStatus status) {
        this.status = status;
    }

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
}
