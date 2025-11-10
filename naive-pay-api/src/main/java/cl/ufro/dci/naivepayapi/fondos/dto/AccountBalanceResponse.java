package cl.ufro.dci.naivepayapi.fondos.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response DTO for account balance queries.
 * <p>
 * Provides relevant account information without exposing the complete entity,
 * following the DTO (Data Transfer Object) design pattern.
 * </p>
 * 
 * <p><b>Advantages of using this DTO:</b></p>
 * <ul>
 *   <li>Decouples presentation layer from domain model</li>
 *   <li>Avoids lazy loading issues in JSON serialization</li>
 *   <li>Controls exactly what information is exposed to the client</li>
 *   <li>Facilitates entity changes without affecting the API</li>
 * </ul>
 * 
 * @author NaivePay Development Team
 * @version 1.0
 * @since 2025-10-06
 */
public class AccountBalanceResponse {
    /** Unique identifier of the account */
    private Long accountId;
    
    /** Identifier of the owner user */
    private Long userId;
    
    /** Available balance in the account */
    private BigDecimal availableBalance;
    
    /** Date and time of the last balance update */
    private LocalDateTime lastUpdate;

    /**
     * Default constructor.
     */
    public AccountBalanceResponse() {
    }

    /**
     * Full constructor with all fields.
     * 
     * @param accountId the account ID
     * @param userId the owner user ID
     * @param availableBalance the available balance
     * @param lastUpdate the last update date
     */
    public AccountBalanceResponse(Long accountId, Long userId, BigDecimal availableBalance, LocalDateTime lastUpdate) {
        this.accountId = accountId;
        this.userId = userId;
        this.availableBalance = availableBalance;
        this.lastUpdate = lastUpdate;
    }

    /**
     * Gets the account identifier.
     * 
     * @return the account ID
     */
    public Long getAccountId() {
        return accountId;
    }

    /**
     * Sets the account identifier.
     * 
     * @param accountId the account ID
     */
    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    /**
     * Gets the user identifier.
     * 
     * @return the user ID
     */
    public Long getUserId() {
        return userId;
    }

    /**
     * Sets the user identifier.
     * 
     * @param userId the user ID
     */
    public void setUserId(Long userId) {
        this.userId = userId;
    }

    /**
     * Gets the available balance.
     * 
     * @return the available balance
     */
    public BigDecimal getAvailableBalance() {
        return availableBalance;
    }

    /**
     * Sets the available balance.
     * 
     * @param availableBalance the available balance
     */
    public void setAvailableBalance(BigDecimal availableBalance) {
        this.availableBalance = availableBalance;
    }

    /**
     * Gets the last update date.
     * 
     * @return the last update date
     */
    public LocalDateTime getLastUpdate() {
        return lastUpdate;
    }

    /**
     * Sets the last update date.
     * 
     * @param lastUpdate the last update date
     */
    public void setLastUpdate(LocalDateTime lastUpdate) {
        this.lastUpdate = lastUpdate;
    }

    /**
     * Generates a String representation of the object.
     * 
     * @return a string with the balance data
     */
    @Override
    public String toString() {
        return "AccountBalanceResponse{" +
                "accountId=" + accountId +
                ", userId=" + userId +
                ", availableBalance=" + availableBalance +
                ", lastUpdate=" + lastUpdate +
                '}';
    }
}
