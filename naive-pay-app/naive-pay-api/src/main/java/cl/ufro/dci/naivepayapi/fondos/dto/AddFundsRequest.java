package cl.ufro.dci.naivepayapi.fondos.dto;

import java.math.BigDecimal;

/**
 * Request DTO for loading balance to an account.
 * <p>
 * Used when a user wants to add funds to their account.
 * In a production environment, this would be integrated with a
 * real payment gateway (credit card, bank transfer, etc.).
 * </p>
 * 
 * <p><b>Required validations:</b></p>
 * <ul>
 *   <li>userId must exist in the system</li>
 *   <li>amount must be greater than zero</li>
 *   <li>The user must have an associated account</li>
 * </ul>
 * 
 * @author NaivePay Development Team
 * @version 1.0
 * @since 2025-10-06
 */
public class AddFundsRequest {
    /** ID of the user requesting the fund load */
    private Long userId;
    
    /** Amount to add to the account (must be greater than zero) */
    private BigDecimal amount;

    /**
     * Default constructor.
     */
    public AddFundsRequest() {
    }

    /**
     * Constructor with all fields.
     * 
     * @param userId the user ID
     * @param amount the amount to add
     */
    public AddFundsRequest(Long userId, BigDecimal amount) {
        this.userId = userId;
        this.amount = amount;
    }

    // Getters and Setters
    
    /**
     * Gets the user ID.
     * 
     * @return the user ID
     */
    public Long getUserId() {
        return userId;
    }

    /**
     * Sets the user ID.
     * 
     * @param userId the user ID
     */
    public void setUserId(Long userId) {
        this.userId = userId;
    }

    /**
     * Gets the amount to add.
     * 
     * @return the amount to add
     */
    public BigDecimal getAmount() {
        return amount;
    }

    /**
     * Sets the amount to add.
     * 
     * @param amount the amount (must be greater than zero)
     */
    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    /**
     * Generates a String representation of the object.
     * 
     * @return a string with the request data
     */
    @Override
    public String toString() {
        return "AddFundsRequest{" +
                "userId=" + userId +
                ", amount=" + amount +
                '}';
    }
}