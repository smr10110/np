package cl.ufro.dci.naivepayapi.fondos.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * JPA Entity representing a financial transaction in the NaivePay system.
 * <p>
 * Records all operations involving fund movements between accounts,
 * providing a complete and auditable transaction history.
 * </p>
 * 
 * <p><b>Supported transaction types:</b></p>
 * <ul>
 *   <li><b>LOAD:</b> Balance load from system account</li>
 *   <li><b>TRANSFER:</b> Transfer between users</li>
 *   <li><b>PAYMENT:</b> Payment to merchant</li>
 * </ul>
 * 
 * <p><b>Key Features:</b></p>
 * <ul>
 *   <li>Immutable after creation (not modified or deleted)</li>
 *   <li>Many-to-One relationship with Account (origin and destination)</li>
 *   <li>Uses LAZY loading to optimize queries</li>
 *   <li>Supports unidirectional transactions (originAccount can be null in LOAD)</li>
 * </ul>
 * 
 * @author NaivePay Development Team
 * @version 1.0
 * @since 2025-10-06
 * @see Account
 * @see TransactionType
 */
@Entity(name = "FundTransaction")
@Table(name = "transactions")
public class FundTransaction {
    /**
     * Unique identifier of the transaction.
     * Generated automatically by the database.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Transaction amount.
     * <p>
     * Uses BigDecimal to guarantee precision in monetary operations.
     * Format: 15 total digits, 2 for decimals.
     * Must always be a positive value.
     * </p>
     */
    @Column(precision = 15, scale = 2, nullable = false)
    private BigDecimal amount;

    /**
     * Exact date and time when the transaction was executed.
     * Used for sorting and auditing.
     */
    @Column(name = "date_time", nullable = false)
    private LocalDateTime dateTime;

    /**
     * Description or concept of the transaction.
     * Provides context about the purpose of the fund movement.
     */
    @Column(name = "description", length = 255)
    private String description;

    /**
     * Type of transaction (LOAD, TRANSFER, PAYMENT).
     * Allows classification and filtering of operations.
     * 
     * @see TransactionType
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 20)
    private TransactionType type;

    /**
     * Origin account of the funds.
     * <p>
     * Can be null in LOAD type transactions where funds
     * come from the system.
     * </p>
     * <p>
     * Uses LAZY loading to avoid unnecessary loads.
     * </p>
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "origin_account_id")
    private Account originAccount;

    /**
     * Destination account of the funds.
     * <p>
     * Must always have a value, as funds always
     * arrive to a specific account.
     * </p>
     * <p>
     * Uses LAZY loading to avoid unnecessary loads.
     * </p>
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "destination_account_id")
    private Account destinationAccount;

    /**
     * Default constructor required by JPA.
     */
    public FundTransaction() {
    }

    /**
     * Full constructor to create a new transaction.
     * 
     * @param amount the transaction amount (must be positive)
     * @param dateTime the transaction date and time
     * @param description the transaction description or concept
     * @param type the transaction type (LOAD, TRANSFER, PAYMENT)
     * @param originAccount the origin account (can be null in LOAD)
     * @param destinationAccount the destination account (required)
     */
    public FundTransaction(BigDecimal amount, LocalDateTime dateTime, String description,
                      TransactionType type, Account originAccount, Account destinationAccount) {
        this.amount = amount;
        this.dateTime = dateTime;
        this.description = description;
        this.type = type;
        this.originAccount = originAccount;
        this.destinationAccount = destinationAccount;
    }

    /**
     * Gets the unique identifier of the transaction.
     * 
     * @return the transaction ID
     */
    public Long getId() {
        return id;
    }

    /**
     * Sets the transaction identifier.
     * 
     * @param id the transaction ID
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * Gets the transaction amount.
     * 
     * @return the amount as BigDecimal
     */
    public BigDecimal getAmount() {
        return amount;
    }

    /**
     * Sets the transaction amount.
     * 
     * @param amount the amount (must be greater than zero)
     */
    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    /**
     * Gets the transaction date and time.
     * 
     * @return the transaction date and time
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
     * @return the description or concept
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the transaction description.
     * 
     * @param description the description (maximum 255 characters)
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Gets the transaction type.
     * 
     * @return the type (LOAD, TRANSFER or PAYMENT)
     */
    public TransactionType getType() {
        return type;
    }

    /**
     * Sets the transaction type.
     * 
     * @param type the transaction type
     */
    public void setType(TransactionType type) {
        this.type = type;
    }

    /**
     * Gets the origin account of the funds.
     * <p>
     * <b>Note:</b> Can be null in LOAD type transactions.
     * </p>
     * 
     * @return the origin account or null
     */
    public Account getOriginAccount() {
        return originAccount;
    }

    /**
     * Sets the origin account.
     * 
     * @param originAccount the origin account
     */
    public void setOriginAccount(Account originAccount) {
        this.originAccount = originAccount;
    }

    /**
     * Gets the destination account of the funds.
     * 
     * @return the destination account
     */
    public Account getDestinationAccount() {
        return destinationAccount;
    }

    /**
     * Sets the destination account.
     * 
     * @param destinationAccount the destination account
     */
    public void setDestinationAccount(Account destinationAccount) {
        this.destinationAccount = destinationAccount;
    }

    /**
     * Compares this transaction with another object for equality.
     * <p>
     * Two transactions are considered equal if they have the same ID.
     * </p>
     * 
     * @param o the object to compare
     * @return {@code true} if the objects are equal, {@code false} otherwise
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FundTransaction that = (FundTransaction) o;
        return Objects.equals(id, that.id);
    }

    /**
     * Calculates the hash code of the transaction.
     * 
     * @return the hash code based on the ID
     */
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    /**
     * Generates a String representation of the transaction.
     * <p>
     * Avoids loading related entities by showing only account IDs.
     * </p>
     * 
     * @return a string with the transaction data
     */
    @Override
    public String toString() {
        return "Transaction{" +
                "id=" + id +
                ", amount=" + amount +
                ", dateTime=" + dateTime +
                ", description='" + description + '\'' +
                ", type=" + type +
                ", originAccount=" + (originAccount != null ? originAccount.getId() : "null") +
                ", destinationAccount=" + (destinationAccount != null ? destinationAccount.getId() : "null") +
                '}';
    }
}