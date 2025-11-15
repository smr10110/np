package cl.ufro.dci.naivepayapi.fondos.domain;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

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
@Data
@Entity(name = "Transaction")
@Table(name = "transaction")
public class Transaction {
    /**
     * Unique identifier of the transaction.
     * Generated automatically by the database.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "tra_id")
    private Long traId;

    /**
     * Transaction amount.
     * <p>
     * Uses BigDecimal to guarantee precision in monetary operations.
     * Format: 15 total digits, 2 for decimals.
     * Must always be a positive value.
     * </p>
     */
    @Column(name = "tra_amount", precision = 15, scale = 2, nullable = false)
    private BigDecimal traAmount;

    /**
     * Exact date and time when the transaction was executed.
     * Used for sorting and auditing.
     */
    @Column(name = "tra_date_time", nullable = false)
    private LocalDateTime traDateTime;

    /**
     * Description or concept of the transaction.
     * Provides context about the purpose of the fund movement.
     */
    @Column(name = "tra_description", length = 255)
    private String traDescription;

    /**
     * Type of transaction (LOAD, TRANSFER, PAYMENT).
     * Allows classification and filtering of operations.
     * 
     * @see TransactionType
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "tra_type", nullable = false, length = 20)
    private TransactionType traType;

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
    @JoinColumn(name = "acc_id_origin", nullable = true)
    private Account accIdOrigin;

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
    @JoinColumn(name = "acc_id_destination", nullable = false)
    private Account accIdDestination;

    /**
     * Name of the customer making the payment.
     * <p>
     * Optional field, used primarily for payment transactions
     * that require customer identification.
     * </p>
     * 
     * @since 2.0
     */
    @Column(name = "tra_customer_name")
    private String traCustomerName;

    /**
     * Name of the commerce or business receiving the payment.
     * <p>
     * Optional field, used for payment transactions to identify
     * the merchant or service provider.
     * </p>
     * 
     * @since 2.0
     */
    @Column(name = "tra_commerce_name")
    private String traCommerceName;

    /**
     * Category or type of payment.
     * <p>
     * Optional field, allows classification of payments
     * (e.g., "Food", "Transport", "Entertainment").
     * </p>
     * 
     * @since 2.0
     */
    @Column(name = "tra_payment_category")
    private String traPaymentCategory;

    /**
     * Current status of the transaction.
     * <p>
     * Used for transactions that require approval workflow.
     * For legacy transactions (LOAD, TRANSFER, PAYMENT), this field is null.
     * </p>
     * 
     * @see TransactionStatus
     * @since 2.0
     */
    @Column(name = "tra_status")
    @Enumerated(EnumType.STRING)
    private TransactionStatus traStatus;

    /**
     * Default constructor required by JPA.
     */
    public Transaction() {
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
    public Transaction(BigDecimal amount, LocalDateTime dateTime, String description,
                      TransactionType type, Account originAccount, Account destinationAccount) {
        this.traAmount = amount;
        this.traDateTime = dateTime;
        this.traDescription = description;
        this.traType = type;
        this.accIdOrigin = originAccount;
        this.accIdDestination = destinationAccount;
    }

    /**
     * Extended constructor for transactions with approval workflow.
     * Used primarily for payment transactions requiring manual approval.
     * 
     * @param amount the transaction amount (must be positive)
     * @param dateTime the transaction date and time
     * @param description the transaction description or concept
     * @param type the transaction type
     * @param originAccount the origin account (can be null in LOAD)
     * @param destinationAccount the destination account (required)
     * @param customerName name of the customer
     * @param commerceName name of the commerce
     * @param paymentCategory category of the payment
     * @param status transaction status
     * @since 2.0
     */
    public Transaction(BigDecimal amount, LocalDateTime dateTime, String description,
                          TransactionType type, Account originAccount, Account destinationAccount,
                          String customerName, String commerceName, String paymentCategory,
                          TransactionStatus status) {
        this(amount, dateTime, description, type, originAccount, destinationAccount);
        this.traCustomerName = customerName;
        this.traCommerceName = commerceName;
        this.traPaymentCategory = paymentCategory;
        this.traStatus = status;
    }

}