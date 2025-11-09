package cl.ufro.dci.naivepayapi.fondos.dto;

import java.math.BigDecimal;

/**
 * Request DTO for transfers between accounts.
 * <p>
 * Used primarily by the Payments module to request
 * fund transfers between users and merchants.
 * </p>
 * 
 * <p><b>Required validations:</b></p>
 * <ul>
 *   <li>originAccountId and destinationAccountId must exist</li>
 *   <li>amount must be greater than zero</li>
 *   <li>originAccountId must have sufficient balance</li>
 *   <li>originAccountId and destinationAccountId must be different</li>
 * </ul>
 * 
 * @author NaivePay Development Team
 * @version 1.0
 * @since 2025-10-06
 */
public class TransferRequest {
    /** ID of the origin account of the funds */
    private Long originAccountId;
    
    /** ID of the destination account of the funds */
    private Long destinationAccountId;
    
    /** Amount to transfer (must be greater than zero) */
    private BigDecimal amount;
    
    /** Description or concept of the transfer */
    private String description;

    /**
     * Default constructor.
     */
    public TransferRequest() {
    }

    /**
     * Full constructor with all fields.
     * 
     * @param originAccountId the origin account ID
     * @param destinationAccountId the destination account ID
     * @param amount the amount to transfer
     * @param description the transfer description
     */
    public TransferRequest(Long originAccountId, Long destinationAccountId, BigDecimal amount, String description) {
        this.originAccountId = originAccountId;
        this.destinationAccountId = destinationAccountId;
        this.amount = amount;
        this.description = description;
    }

    /**
     * Gets the origin account ID.
     * 
     * @return the origin account ID
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
     * Gets the amount to transfer.
     * 
     * @return the amount to transfer
     */
    public BigDecimal getAmount() {
        return amount;
    }

    /**
     * Sets the amount to transfer.
     * 
     * @param amount the amount (must be greater than zero)
     */
    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    /**
     * Gets the transfer description.
     * 
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the transfer description.
     * 
     * @param description the description or concept
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Generates a String representation of the object.
     * 
     * @return a string with the request data
     */
    @Override
    public String toString() {
        return "TransferRequest{" +
                "originAccountId=" + originAccountId +
                ", destinationAccountId=" + destinationAccountId +
                ", amount=" + amount +
                ", description='" + description + '\'' +
                '}';
    }
}
