package cl.ufro.dci.naivepayapi.fondos.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * JPA Entity representing a bank account in the NaivePay system.
 * <p>
 * Each user in the system has a unique associated account that allows:
 * <ul>
 *   <li>Maintaining an available balance</li>
 *   <li>Performing and receiving transfers</li>
 *   <li>Making payments to merchants</li>
 * </ul>
 * </p>
 * 
 * <p><b>Key Features:</b></p>
 * <ul>
 *   <li>1:1 relationship with User (unique userId)</li>
 *   <li>Balance with 2 decimal precision using BigDecimal</li>
 *   <li>Audit trail with creation and last update dates</li>
 * </ul>
 * 
 * @author NaivePay Development Team
 * @version 1.0
 * @since 2025-10-06
 */
@Entity
@Table(name = "accounts")
public class Account {
    /**
     * Unique identifier of the account.
     * Generated automatically by the database.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Identifier of the user who owns the account.
     * Must be unique in the system (one user = one account).
     */
    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    /**
     * Available balance in the account.
     * <p>
     * Uses BigDecimal to guarantee precision in monetary operations.
     * Format: 15 total digits, 2 for decimals (e.g.: 9999999999999.99)
     * </p>
     */
    @Column(name = "available_balance", precision = 15, scale = 2, nullable = false)
    private BigDecimal availableBalance = BigDecimal.ZERO;

    /**
     * Date and time when the account was created.
     * This field is immutable after creation.
     */
    @Column(name = "creation_date", nullable = false, updatable = false)
    private LocalDateTime creationDate;

    /**
     * Date and time of the last balance update.
     * Automatically updated when balance is modified.
     */
    @Column(name = "last_update", nullable = false)
    private LocalDateTime lastUpdate;

    /**
     * Default constructor.
     * Initializes creation and update dates to current time.
     */
    public Account() {
        this.creationDate = LocalDateTime.now();
        this.lastUpdate = LocalDateTime.now();
    }

    /**
     * Constructor with user ID.
     * 
     * @param userId the identifier of the user who owns the account
     */
    public Account(Long userId) {
        this();
        this.userId = userId;
    }

    /**
     * Gets the unique identifier of the account.
     * 
     * @return the account ID
     */
    public Long getId() { return id; }
    
    /**
     * Sets the account identifier.
     * 
     * @param id the new account ID
     */
    public void setId(Long id) { this.id = id; }

    /**
     * Gets the identifier of the owner user.
     * 
     * @return the user ID
     */
    public Long getUserId() { return userId; }
    
    /**
     * Sets the identifier of the owner user.
     * 
     * @param userId the user ID
     */
    public void setUserId(Long userId) { this.userId = userId; }

    /**
     * Gets the available balance in the account.
     * 
     * @return the available balance as BigDecimal
     */
    public BigDecimal getAvailableBalance() { return availableBalance; }
    
    /**
     * Sets the available balance in the account.
     * <p>
     * <b>Note:</b> To update the balance while maintaining audit trail,
     * use {@link #updateBalance(BigDecimal)} instead.
     * </p>
     * 
     * @param availableBalance the new available balance
     */
    public void setAvailableBalance(BigDecimal availableBalance) { 
        this.availableBalance = availableBalance;
    }

    /**
     * Updates the account balance and records the modification date.
     * <p>
     * This method is the recommended way to update the balance, as it
     * automatically updates the {@code lastUpdate} field.
     * </p>
     * 
     * @param newBalance the new balance to set
     */
    public void updateBalance(BigDecimal newBalance) {
        this.availableBalance = newBalance;
        this.lastUpdate = LocalDateTime.now();
    }

    /**
     * Gets the account creation date.
     * 
     * @return the creation date and time
     */
    public LocalDateTime getCreationDate() { return creationDate; }
    
    /**
     * Sets the creation date.
     * 
     * @param creationDate the creation date
     */
    public void setCreationDate(LocalDateTime creationDate) { this.creationDate = creationDate; }

    /**
     * Gets the date of the last balance update.
     * 
     * @return the last update date and time
     */
    public LocalDateTime getLastUpdate() { return lastUpdate; }
    
    /**
     * Sets the last update date.
     * 
     * @param lastUpdate the last update date
     */
    public void setLastUpdate(LocalDateTime lastUpdate) { this.lastUpdate = lastUpdate; }

    /**
     * Compares this account with another object for equality.
     * <p>
     * Two accounts are considered equal if they have the same ID.
     * </p>
     * 
     * @param o the object to compare
     * @return {@code true} if the objects are equal, {@code false} otherwise
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Account account = (Account) o;
        return id != null && id.equals(account.id);
    }

    /**
     * Calculates the hash code of the account.
     * 
     * @return the hash code based on the class
     */
    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    /**
     * Generates a String representation of the account.
     * 
     * @return a string with the account data
     */
    @Override
    public String toString() {
        return "Account{" +
                "id=" + id +
                ", userId=" + userId +
                ", availableBalance=" + availableBalance +
                ", creationDate=" + creationDate +
                ", lastUpdate=" + lastUpdate +
                '}';
    }
}