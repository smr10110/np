package cl.ufro.dci.naivepayapi.fondos.service;

import cl.ufro.dci.naivepayapi.fondos.domain.Account;
import cl.ufro.dci.naivepayapi.fondos.domain.Transaction;
import cl.ufro.dci.naivepayapi.fondos.domain.TransactionType;
import cl.ufro.dci.naivepayapi.fondos.domain.TransactionStatus;
import cl.ufro.dci.naivepayapi.fondos.dto.TransactionResponse;
import cl.ufro.dci.naivepayapi.fondos.dto.TransferRequest;
import cl.ufro.dci.naivepayapi.fondos.dto.TransferResponse;
import cl.ufro.dci.naivepayapi.fondos.dto.PendingPaymentResponse;
import cl.ufro.dci.naivepayapi.fondos.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for transaction management in the NaivePay system.
 * <p>
 * Implements the business logic for fund transfers, balance loading,
 * payments, and transactional history queries. It is the core of the funds module.
 * </p>
 * 
 * <p><b>Main responsibilities:</b></p>
 * <ul>
 *   <li>Execute transfers between user accounts</li>
 *   <li>Process balance loads from the system account</li>
 *   <li>Execute payments to merchants</li>
 *   <li>Validate sufficient balances before operations</li>
 *   <li>Maintain complete transaction history</li>
 *   <li>Guarantee atomicity in financial operations</li>
 * </ul>
 * 
 * <p><b>Transactional security guarantees:</b></p>
 * <ul>
 *   <li>All financial operations are atomic (ACID)</li>
 *   <li>Automatic rollback on any error</li>
 *   <li>Exhaustive validations before modifying balances</li>
 *   <li>Immutable record of all operations</li>
 * </ul>
 * 
 * @author NaivePay Development Team
 * @version 1.0
 * @since 2025-10-06
 * @see Transaction
 * @see TransactionType
 * @see AccountService
 */
@Service
public class TransactionService {

    private final TransactionRepository fundTransactionRepository;
    private final AccountService accountService;

    /**
     * Constructor with dependency injection.
     * 
     * @param fundTransactionRepository the transaction repository
     * @param accountService the account management service
     */
    public TransactionService(TransactionRepository fundTransactionRepository, AccountService accountService) {
        this.fundTransactionRepository = fundTransactionRepository;
        this.accountService = accountService;
    }

    /**
     * Performs a transfer between two accounts.
     * <p>
     * Executes an atomic fund transfer between two accounts, validating
     * sufficient balances and updating both accounts transactionally.
     * </p>
     * 
     * <p><b>Transfer process:</b></p>
     * <ol>
     *   <li>Validates the request data (IDs, amounts)</li>
     *   <li>Gets the origin and destination accounts</li>
     *   <li>Verifies they are not the same account</li>
     *   <li>Validates sufficient balance in origin account</li>
     *   <li>Calculates new balances (subtract from origin, add to destination)</li>
     *   <li>Updates both accounts atomically</li>
     *   <li>Creates TRANSFER type transaction record</li>
     *   <li>Persists the transaction</li>
     *   <li>Returns response with transaction ID</li>
     * </ol>
     * 
     * <p><b>Error handling:</b> Any failure in the process triggers a
     * complete rollback, maintaining data consistency.</p>
     * 
     * @param request object with transfer data
     * @return {@link TransferResponse} indicating success or failure of the operation
     */
    @Transactional
    public TransferResponse transfer(TransferRequest request) {
        try {
            // Basic validations
            validateTransferRequest(request);

            // Get accounts
            Account originAccount = accountService.getAccountById(request.getOriginAccountId());
            Account destinationAccount = accountService.getAccountById(request.getDestinationAccountId());

            // Validate not same account
            if (originAccount.getAccId().equals(destinationAccount.getAccId())) {
                return TransferResponse.error("Cannot transfer to the same account");
            }

            // Validate sufficient balance
            if (originAccount.getAccAvailableBalance().compareTo(request.getAmount()) < 0) {
                return TransferResponse.error("Insufficient balance");
            }

            // Update balances
            BigDecimal newOriginBalance = originAccount.getAccAvailableBalance().subtract(request.getAmount());
            BigDecimal newDestinationBalance = destinationAccount.getAccAvailableBalance().add(request.getAmount());

            accountService.updateAccountBalance(originAccount, newOriginBalance);
            accountService.updateAccountBalance(destinationAccount, newDestinationBalance);

            // Create transaction record
            Transaction transaction = new Transaction(
                    request.getAmount(),
                    LocalDateTime.now(),
                    request.getDescription() != null ? request.getDescription() : "Transfer",
                    TransactionType.TRANSFER,
                    originAccount,
                    destinationAccount
            );

            Transaction savedTransaction = fundTransactionRepository.save(transaction);

            return TransferResponse.success(savedTransaction.getTraId());

        } catch (IllegalArgumentException e) {
            return TransferResponse.error(e.getMessage());
        } catch (Exception e) {
            return TransferResponse.error("Error processing transfer: " + e.getMessage());
        }
    }

    /**
     * Loads balance to an account from the system account.
     * <p>
     * Simulates adding funds to a user account from an external source.
     * In a production environment, this would be integrated with a real payment gateway.
     * </p>
     * 
     * <p><b>Load process:</b></p>
     * <ol>
     *   <li>Validates that the amount is greater than zero</li>
     *   <li>Gets the user account</li>
     *   <li>Gets or creates the system account</li>
     *   <li>Calculates the new balance (current balance + amount)</li>
     *   <li>Updates the user balance</li>
     *   <li>Creates LOAD type transaction record</li>
     *   <li>Persists the transaction</li>
     *   <li>Returns response with transaction ID</li>
     * </ol>
     * 
     * @param userId the ID of the user receiving the funds
     * @param amount the amount to load (must be positive)
     * @return {@link TransferResponse} indicating success or failure of the operation
     */
    @Transactional
    public TransferResponse addFunds(Long userId, BigDecimal amount) {
        try {
            // Validate amount
            if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
                return TransferResponse.error("Amount must be greater than zero");
            }
            
            // Get user account
            Account userAccount = accountService.getAccountByUserId(userId);

            // Get or create system account
            Account systemAccount = accountService.getOrCreateSystemAccount();

            // Update user balance
            BigDecimal newBalance = userAccount.getAccAvailableBalance().add(amount);
            accountService.updateAccountBalance(userAccount, newBalance);

            // Create LOAD type transaction record
            Transaction transaction = new Transaction(
                    amount,
                    LocalDateTime.now(),
                    "Balance load",
                    TransactionType.LOAD,
                    systemAccount,
                    userAccount
            );

            Transaction savedTransaction = fundTransactionRepository.save(transaction);

            return TransferResponse.success(savedTransaction.getTraId());

        } catch (IllegalArgumentException e) {
            return TransferResponse.error(e.getMessage());
        } catch (Exception e) {
            return TransferResponse.error("Error loading funds: " + e.getMessage());
        }
    }

    /**
     * Performs a payment from a user account to a merchant account.
     * <p>
     * Similar to a transfer but specifically designed for payments
     * to merchants, with PAYMENT transaction type for better traceability.
     * </p>
     * 
     * <p><b>Differences from transfer():</b></p>
     * <ul>
     *   <li>Transaction type: PAYMENT instead of TRANSFER</li>
     *   <li>Default description oriented to payments</li>
     *   <li>Specific semantics for commercial operations</li>
     * </ul>
     * 
     * <p><b>Typical use:</b> Called by the Payments module when approving
     * a payment request to a merchant.</p>
     * 
     * @param userAccountId the ID of the user account (payer)
     * @param commerceAccountId the ID of the merchant account (receiver)
     * @param amount the payment amount (must be positive)
     * @param description payment description (can be null)
     * @return {@link TransferResponse} indicating success or failure of the operation
     */
    @Transactional
    public TransferResponse payment(Long userAccountId, Long commerceAccountId, BigDecimal amount, String description) {
        try {
            // Validate amount
            if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
                return TransferResponse.error("Amount must be greater than zero");
            }

            // Get accounts
            Account userAccount = accountService.getAccountById(userAccountId);
            Account commerceAccount = accountService.getAccountById(commerceAccountId);

            // Validate sufficient balance
            if (userAccount.getAccAvailableBalance().compareTo(amount) < 0) {
                return TransferResponse.error("Insufficient balance");
            }

            // Update balances
            BigDecimal newUserBalance = userAccount.getAccAvailableBalance().subtract(amount);
            BigDecimal newCommerceBalance = commerceAccount.getAccAvailableBalance().add(amount);

            accountService.updateAccountBalance(userAccount, newUserBalance);
            accountService.updateAccountBalance(commerceAccount, newCommerceBalance);

            // Create PAYMENT type transaction record
            Transaction transaction = new Transaction(
                    amount,
                    LocalDateTime.now(),
                    description != null ? description : "Payment to commerce",
                    TransactionType.PAYMENT,
                    userAccount,
                    commerceAccount
            );

            Transaction savedTransaction = fundTransactionRepository.save(transaction);

            return TransferResponse.success(savedTransaction.getTraId());

        } catch (IllegalArgumentException e) {
            return TransferResponse.error(e.getMessage());
        } catch (Exception e) {
            return TransferResponse.error("Error processing payment: " + e.getMessage());
        }
    }

    /**
     * Gets the transaction history of a user.
     * <p>
     * Returns all transactions where the user's account
     * appears as origin OR as destination, ordered by date descending
     * (most recent first).
     * </p>
     * 
     * <p><b>Includes:</b></p>
     * <ul>
     *   <li>Received balance loads (LOAD)</li>
     *   <li>Sent and received transfers (TRANSFER)</li>
     *   <li>Payments made and received (PAYMENT)</li>
     * </ul>
     * 
     * @param userId the user ID
     * @return list of {@link TransactionResponse} with complete history
     * @throws IllegalArgumentException if the user has no account
     */
    @Transactional(readOnly = true)
    public List<TransactionResponse> getTransactionHistory(Long userId) {
        Account account = accountService.getAccountByUserId(userId);
        
        List<Transaction> transactions = fundTransactionRepository
                .findByAccIdOriginOrAccIdDestinationOrderByTraDateTimeDesc(account, account);

        return transactions.stream()
                .map(this::mapToTransactionResponse)
                .collect(Collectors.toList());
    }

    /**
     * Gets the details of a specific transaction.
     * <p>
     * Allows querying detailed information of a transaction
     * using its unique identifier.
     * </p>
     * 
     * @param transactionId the ID of the transaction to query
     * @return {@link TransactionResponse} with the transaction details
     * @throws IllegalArgumentException if no transaction exists with that ID
     */
    @Transactional(readOnly = true)
    public TransactionResponse getTransactionById(Long transactionId) {
        Transaction transaction = fundTransactionRepository.findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("No transaction exists with ID: " + transactionId));

        return mapToTransactionResponse(transaction);
    }

    /**
     * Validates if an account has sufficient balance for a specific amount.
     * <p>
     * Validation method used primarily by other modules
     * (e.g.: Payments module) to verify funds before creating payment
     * or transfer requests.
     * </p>
     * 
     * <p><b>Recommended use:</b> Call this method before attempting
     * operations that require funds, to provide immediate feedback
     * to the user.</p>
     * 
     * @param accountId the ID of the account to validate
     * @param amount the amount to validate
     * @return {@code true} if there is sufficient balance, {@code false} otherwise
     */
    @Transactional(readOnly = true)
    public boolean validateBalance(Long accountId, BigDecimal amount) {
        try {
            Account account = accountService.getAccountById(accountId);
            return account.getAccAvailableBalance().compareTo(amount) >= 0;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Validates the data of a transfer request.
     * <p>
     * Performs basic data integrity validations before
     * processing a transfer. Does not validate account existence
     * or sufficient balances (those validations are done later).
     * </p>
     * 
     * <p><b>Validations performed:</b></p>
     * <ul>
     *   <li>Request is not null</li>
     *   <li>Origin account ID is present</li>
     *   <li>Destination account ID is present</li>
     *   <li>Amount is greater than zero</li>
     * </ul>
     * 
     * @param request the request to validate
     * @throws IllegalArgumentException if any validation fails
     */
    private void validateTransferRequest(TransferRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Transfer request cannot be null");
        }
        if (request.getOriginAccountId() == null) {
            throw new IllegalArgumentException("Origin account ID is required");
        }
        if (request.getDestinationAccountId() == null) {
            throw new IllegalArgumentException("Destination account ID is required");
        }
        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }
    }

    /**
     * Converts a Transaction entity to a response DTO.
     * <p>
     * Private helper method that performs the mapping avoiding
     * lazy loading issues by exposing only the IDs of related accounts.
     * </p>
     * 
     * <p><b>Design advantages:</b></p>
     * <ul>
     *   <li>Prevents LazyInitializationException</li>
     *   <li>Reduces JSON response size</li>
     *   <li>Decouples domain model from API</li>
     * </ul>
     * 
     * @param transaction the entity to convert
     * @return a {@link TransactionResponse} object with the relevant data
     */
    private TransactionResponse mapToTransactionResponse(Transaction transaction) {
        TransactionResponse response = new TransactionResponse();
        response.setId(transaction.getTraId());
        response.setAmount(transaction.getTraAmount());
        response.setDateTime(transaction.getTraDateTime());
        response.setDescription(transaction.getTraDescription());
        response.setOriginAccountId(transaction.getAccIdOrigin() != null ? transaction.getAccIdOrigin().getAccId() : null);
        response.setDestinationAccountId(transaction.getAccIdDestination() != null ? transaction.getAccIdDestination().getAccId() : null);
        response.setType(transaction.getTraType() != null ? transaction.getTraType().name() : null);
        response.setStatus(transaction.getTraStatus() != null ? transaction.getTraStatus().name() : null);
        response.setCustomerName(transaction.getTraCustomerName());
        response.setCommerceName(transaction.getTraCommerceName());
        response.setPaymentCategory(transaction.getTraPaymentCategory());
        return response;
    }

    /**
     * Creates a payment transaction pending approval.
     * <p>
     * Creates a transaction record with PAYMENT_PENDING type that requires
     * manual approval before executing the fund transfer. The transaction
     * is stored but balances are NOT modified until approved.
     * </p>
     * 
     * <p><b>Process:</b></p>
     * <ol>
     *   <li>Validates request data (accounts, amount)</li>
     *   <li>Verifies accounts exist</li>
     *   <li>Creates transaction with PENDING status</li>
     *   <li>Returns transaction ID for approval workflow</li>
     * </ol>
     * 
     * <p><b>Use case:</b> Payment to commerce requiring manual validation
     * before execution (fraud prevention, limit verification, etc.)</p>
     * 
     * @param originAccountId ID of the payer account
     * @param destinationAccountId ID of the commerce account
     * @param amount payment amount
     * @param description payment description
     * @param customerName name of the customer making the payment
     * @param commerceName name of the commerce receiving the payment
     * @param category payment category (e.g., "Food", "Transport")
     * @return {@link TransferResponse} with transaction ID if successful
     * @since 2.0
     */
    @Transactional
    public TransferResponse createPendingPayment(Long originAccountId, Long destinationAccountId,
                                                 BigDecimal amount, String description,
                                                 String customerName, String commerceName, 
                                                 String category) {
        try {
            // Validate amount
            if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
                return TransferResponse.error("Amount must be greater than zero");
            }

            // Get accounts (validates they exist)
            Account originAccount = accountService.getAccountById(originAccountId);
            Account destinationAccount = accountService.getAccountById(destinationAccountId);

            // Validate not same account
            if (originAccount.getAccId().equals(destinationAccount.getAccId())) {
                return TransferResponse.error("Cannot transfer to the same account");
            }

            // Create PENDING transaction (NO balance modification yet)
            Transaction transaction = new Transaction(
                    amount,
                    LocalDateTime.now(),
                    description != null ? description : "Payment to " + commerceName,
                    TransactionType.PAYMENT_PENDING,
                    originAccount,
                    destinationAccount,
                    customerName,
                    commerceName,
                    category,
                    TransactionStatus.PENDING
            );

            Transaction savedTransaction = fundTransactionRepository.save(transaction);

            return TransferResponse.success(savedTransaction.getTraId());

        } catch (IllegalArgumentException e) {
            return TransferResponse.error(e.getMessage());
        } catch (Exception e) {
            return TransferResponse.error("Error creating pending payment: " + e.getMessage());
        }
    }

    /**
     * Approves a pending payment and executes the fund transfer.
     * <p>
     * Changes a PAYMENT_PENDING transaction to PAYMENT (completed),
     * executing the actual balance transfer between accounts.
     * </p>
     * 
     * <p><b>Process:</b></p>
     * <ol>
     *   <li>Finds the pending transaction</li>
     *   <li>Validates it is in PENDING status</li>
     *   <li>Verifies sufficient balance in origin account</li>
     *   <li>If insufficient: marks as REJECTED</li>
     *   <li>If sufficient: executes transfer and marks as COMPLETED</li>
     *   <li>Updates transaction type to PAYMENT</li>
     * </ol>
     * 
     * <p><b>Atomicity:</b> All operations are transactional. If any step
     * fails, all changes are rolled back.</p>
     * 
     * @param transactionId ID of the pending transaction to approve
     * @return {@link TransferResponse} indicating success or failure
     * @since 2.0
     */
    @Transactional
    public TransferResponse approvePendingPayment(Long transactionId) {
        try {
            // Find pending transaction
            Transaction pendingTransaction = fundTransactionRepository.findById(transactionId)
                    .orElseThrow(() -> new IllegalArgumentException("Transaction not found with ID: " + transactionId));

            // Validate it is pending
            if (pendingTransaction.getTraStatus() != TransactionStatus.PENDING ||
                pendingTransaction.getTraType() != TransactionType.PAYMENT_PENDING) {
                return TransferResponse.error("Transaction is not pending approval");
            }

            // Get accounts
            Account originAccount = pendingTransaction.getAccIdOrigin();
            Account destinationAccount = pendingTransaction.getAccIdDestination();

            // Validate sufficient balance
            if (originAccount.getAccAvailableBalance().compareTo(pendingTransaction.getTraAmount()) < 0) {
                // Mark as rejected
                pendingTransaction.setTraType(TransactionType.PAYMENT_REJECTED);
                pendingTransaction.setTraStatus(TransactionStatus.REJECTED);
                fundTransactionRepository.save(pendingTransaction);
                return TransferResponse.error("Insufficient balance - Payment rejected");
            }

            // Execute transfer
            BigDecimal newOriginBalance = originAccount.getAccAvailableBalance()
                    .subtract(pendingTransaction.getTraAmount());
            BigDecimal newDestinationBalance = destinationAccount.getAccAvailableBalance()
                    .add(pendingTransaction.getTraAmount());

            accountService.updateAccountBalance(originAccount, newOriginBalance);
            accountService.updateAccountBalance(destinationAccount, newDestinationBalance);

            // Update transaction to approved
            pendingTransaction.setTraType(TransactionType.PAYMENT);
            pendingTransaction.setTraStatus(TransactionStatus.COMPLETED);
            Transaction approvedTransaction = fundTransactionRepository.save(pendingTransaction);

            return TransferResponse.success(approvedTransaction.getTraId());

        } catch (IllegalArgumentException e) {
            return TransferResponse.error(e.getMessage());
        } catch (Exception e) {
            return TransferResponse.error("Error approving payment: " + e.getMessage());
        }
    }

    /**
     * Cancels a pending payment.
     * <p>
     * Marks a PAYMENT_PENDING transaction as canceled. No fund transfer
     * occurs. The transaction remains in history for audit purposes.
     * </p>
     * 
     * <p><b>Validations:</b></p>
     * <ul>
     *   <li>Transaction must exist</li>
     *   <li>Transaction must be in PENDING status</li>
     *   <li>Cannot cancel already approved/rejected/canceled transactions</li>
     * </ul>
     * 
     * @param transactionId ID of the transaction to cancel
     * @return {@link TransferResponse} indicating success or failure
     * @since 2.0
     */
    @Transactional
    public TransferResponse cancelPendingPayment(Long transactionId) {
        try {
            Transaction pendingTransaction = fundTransactionRepository.findById(transactionId)
                    .orElseThrow(() -> new IllegalArgumentException("Transaction not found with ID: " + transactionId));

            if (pendingTransaction.getTraStatus() != TransactionStatus.PENDING) {
                return TransferResponse.error("Transaction is not pending - cannot cancel");
            }

            // Mark as canceled
            pendingTransaction.setTraType(TransactionType.PAYMENT_CANCELED);
            pendingTransaction.setTraStatus(TransactionStatus.CANCELED);
            fundTransactionRepository.save(pendingTransaction);

            return TransferResponse.success(transactionId);

        } catch (IllegalArgumentException e) {
            return TransferResponse.error(e.getMessage());
        } catch (Exception e) {
            return TransferResponse.error("Error canceling payment: " + e.getMessage());
        }
    }

    /**
     * Rejects a pending payment.
     * <p>
     * Similar to cancel but indicates an administrative rejection
     * (e.g., fraud detection, policy violation). Used by automated
     * systems or manual review processes.
     * </p>
     * 
     * @param transactionId ID of the transaction to reject
     * @param reason rejection reason (for audit)
     * @return {@link TransferResponse} indicating success or failure
     * @since 2.0
     */
    @Transactional
    public TransferResponse rejectPendingPayment(Long transactionId, String reason) {
        try {
            Transaction pendingTransaction = fundTransactionRepository.findById(transactionId)
                    .orElseThrow(() -> new IllegalArgumentException("Transaction not found with ID: " + transactionId));

            if (pendingTransaction.getTraStatus() != TransactionStatus.PENDING) {
                return TransferResponse.error("Transaction is not pending - cannot reject");
            }

            // Mark as rejected
            pendingTransaction.setTraType(TransactionType.PAYMENT_REJECTED);
            pendingTransaction.setTraStatus(TransactionStatus.REJECTED);
            if (reason != null && !reason.isEmpty()) {
                pendingTransaction.setTraDescription(pendingTransaction.getTraDescription() + " [Rejected: " + reason + "]");
            }
            fundTransactionRepository.save(pendingTransaction);

            return TransferResponse.success(transactionId);

        } catch (IllegalArgumentException e) {
            return TransferResponse.error(e.getMessage());
        } catch (Exception e) {
            return TransferResponse.error("Error rejecting payment: " + e.getMessage());
        }
    }

    /**
     * Gets all pending payment transactions.
     * <p>
     * Returns all transactions with PENDING status and PAYMENT_PENDING type,
     * useful for admin dashboards or approval workflows.
     * </p>
     * 
     * @return List of {@link PendingPaymentResponse} with pending payments
     * @since 2.0
     */
    @Transactional(readOnly = true)
    public List<PendingPaymentResponse> getPendingPayments() {
        List<Transaction> pending = fundTransactionRepository
                .findByTraTypeAndTraStatusOrderByTraDateTimeDesc(TransactionType.PAYMENT_PENDING, TransactionStatus.PENDING);
        
        return pending.stream()
                .map(this::mapToPendingPaymentResponse)
                .collect(Collectors.toList());
    }

    /**
     * Gets pending payments for a specific account (WHERE user is the DESTINATION/owner).
     * <p>
     * **OWNERSHIP MODEL**: Returns payments where the specified user is the DESTINATION,
     * meaning they are the commerce/receiver who must approve or reject the payment.
     * </p>
     * <p>
     * This is the correct method for showing "Pagos Pendientes" in the UI,
     * as it only shows payments that THIS user has authority to approve/reject.
     * </p>
     * 
     * @param accountId ID of the destination account (commerce receiving payment)
     * @return List of pending payments TO that account (awaiting approval)
     * @since 2.0
     */
    @Transactional(readOnly = true)
    public List<PendingPaymentResponse> getPendingPaymentsByDestinationAccount(Long accountId) {
        Account account = accountService.getAccountById(accountId);
        
        List<Transaction> pending = fundTransactionRepository
                .findByTraTypeAndTraStatusOrderByTraDateTimeDesc(TransactionType.PAYMENT_PENDING, TransactionStatus.PENDING);
        
        // Filter by DESTINATION (ownership): User can only approve payments TO them
        return pending.stream()
                .filter(t -> t.getAccIdDestination().getAccId().equals(account.getAccId()))
                .map(this::mapToPendingPaymentResponse)
                .collect(Collectors.toList());
    }

    /**
     * Gets pending payments for a specific account.
     * <p>
     * Useful for showing a user their pending outgoing payments.
     * </p>
     * 
     * @param accountId ID of the origin account
     * @return List of pending payments from that account
     * @since 2.0
     * @deprecated Use {@link #getPendingPaymentsByDestinationAccount(Long)} for approval workflow.
     *             This method shows outgoing payments (origin), not incoming (destination).
     */
    @Deprecated
    @Transactional(readOnly = true)
    public List<PendingPaymentResponse> getPendingPaymentsByAccount(Long accountId) {
        Account account = accountService.getAccountById(accountId);
        
        List<Transaction> pending = fundTransactionRepository
                .findByTraTypeAndTraStatusOrderByTraDateTimeDesc(TransactionType.PAYMENT_PENDING, TransactionStatus.PENDING);
        
        return pending.stream()
                .filter(t -> t.getAccIdOrigin().getAccId().equals(account.getAccId()))
                .map(this::mapToPendingPaymentResponse)
                .collect(Collectors.toList());
    }

    /**
     * Gets the complete history including pending, approved, rejected, and canceled payments.
     * <p>
     * Extended version of getTransactionHistory that includes all transaction
     * states, not just completed ones.
     * </p>
     * 
     * @param userId ID of the user
     * @return List of all transactions with extended information
     * @since 2.0
     */
    @Transactional(readOnly = true)
    public List<TransactionResponse> getExtendedTransactionHistory(Long userId) {
        Account account = accountService.getAccountByUserId(userId);
        
        List<Transaction> transactions = fundTransactionRepository
                .findByAccIdOriginOrAccIdDestinationOrderByTraDateTimeDesc(account, account);

        return transactions.stream()
                .map(this::mapToExtendedTransactionResponse)
                .collect(Collectors.toList());
    }

    /**
     * Converts a Transaction to PendingPaymentResponse.
     * <p>
     * Maps transaction entity to a DTO specific for pending payments
     * with extended information (customer, commerce, category).
     * </p>
     * 
     * @param transaction the transaction entity
     * @return PendingPaymentResponse DTO
     * @since 2.0
     */
    private PendingPaymentResponse mapToPendingPaymentResponse(Transaction transaction) {
        return new PendingPaymentResponse(
                transaction.getTraId(),
                transaction.getTraAmount(),
                transaction.getTraCommerceName(),
                transaction.getTraCustomerName(),
                transaction.getTraPaymentCategory(),
                transaction.getTraDescription(),
                transaction.getTraDateTime(),
                transaction.getTraStatus(),
                transaction.getAccIdOrigin() != null ? transaction.getAccIdOrigin().getAccId() : null,
                transaction.getAccIdDestination() != null ? transaction.getAccIdDestination().getAccId() : null
        );
    }

    /**
     * Converts Transaction to extended TransactionResponse.
     * <p>
     * Includes additional fields for transactions with approval workflow.
     * </p>
     * 
     * @param transaction the transaction entity
     * @return Extended TransactionResponse DTO
     * @since 2.0
     */
    private TransactionResponse mapToExtendedTransactionResponse(Transaction transaction) {
        TransactionResponse response = new TransactionResponse();
        response.setId(transaction.getTraId());
        response.setAmount(transaction.getTraAmount());
        response.setDateTime(transaction.getTraDateTime());
        response.setDescription(transaction.getTraDescription());
        response.setOriginAccountId(transaction.getAccIdOrigin() != null ? transaction.getAccIdOrigin().getAccId() : null);
        response.setDestinationAccountId(transaction.getAccIdDestination() != null ? transaction.getAccIdDestination().getAccId() : null);
        
        // Add extended fields if available
        response.setType(transaction.getTraType().name());
        response.setStatus(transaction.getTraStatus() != null ? transaction.getTraStatus().name() : null);
        response.setCustomerName(transaction.getTraCustomerName());
        response.setCommerceName(transaction.getTraCommerceName());
        response.setPaymentCategory(transaction.getTraPaymentCategory());
        
        return response;
    }
}
