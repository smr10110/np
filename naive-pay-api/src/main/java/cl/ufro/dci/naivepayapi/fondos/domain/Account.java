package cl.ufro.dci.naivepayapi.fondos.domain;

import cl.ufro.dci.naivepayapi.registro.domain.User;
import jakarta.persistence.*;
import lombok.Data;
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
@Data
@Entity
@Table(name = "account")
public class Account {
    /**
     * Unique identifier of the account.
     * Generated automatically by the database.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "acc_id")
    private Long accId;

    /**
     * User who owns the account.
     * Must be unique in the system (one user = one account).
     * Foreign key relationship to app_user table.
     * Can be null for system account (user_id = 0).
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "use_id", referencedColumnName = "useId", unique = true, nullable = true)
    private User user;

    /**
     * Available balance in the account.
     * <p>
     * Uses BigDecimal to guarantee precision in monetary operations.
     * Format: 15 total digits, 2 for decimals (e.g.: 9999999999999.99)
     * </p>
     */
    @Column(name = "acc_available_balance", precision = 15, scale = 2, nullable = false)
    private BigDecimal accAvailableBalance = BigDecimal.ZERO;

    /**
     * Date and time when the account was created.
     * This field is immutable after creation.
     */
    @Column(name = "acc_creation_date", nullable = false, updatable = false)
    private LocalDateTime accCreationDate;

    /**
     * Date and time of the last balance update.
     * Automatically updated when balance is modified.
     */
    @Column(name = "acc_last_update", nullable = false)
    private LocalDateTime accLastUpdate;

    /**
     * Default constructor.
     * Initializes creation and update dates to current time.
     */
    public Account() {
        this.accCreationDate = LocalDateTime.now();
        this.accLastUpdate = LocalDateTime.now();
    }

    /**
     * Constructor with user.
     * Recommended constructor for creating accounts.
     * 
     * @param user the user who owns the account (must be persisted)
     */
    public Account(User user) {
        this();
        this.user = user;
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
        this.accAvailableBalance = newBalance;
        this.accLastUpdate = LocalDateTime.now();
    }

}