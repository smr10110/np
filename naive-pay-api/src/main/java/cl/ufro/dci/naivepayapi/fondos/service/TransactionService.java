package cl.ufro.dci.naivepayapi.fondos.service;

import cl.ufro.dci.naivepayapi.fondos.domain.Account;
import cl.ufro.dci.naivepayapi.fondos.domain.FundTransaction;
import cl.ufro.dci.naivepayapi.fondos.domain.TransactionType;
import cl.ufro.dci.naivepayapi.fondos.dto.TransactionResponse;
import cl.ufro.dci.naivepayapi.fondos.dto.TransferRequest;
import cl.ufro.dci.naivepayapi.fondos.dto.TransferResponse;
import cl.ufro.dci.naivepayapi.fondos.repository.FundTransactionRepository;
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
 * @see FundTransaction
 * @see TransactionType
 * @see AccountService
 */
@Service
public class TransactionService {

    private final FundTransactionRepository fundTransactionRepository;
    private final AccountService accountService;

    /**
     * Constructor with dependency injection.
     * 
     * @param fundTransactionRepository the transaction repository
     * @param accountService the account management service
     */
    public TransactionService(FundTransactionRepository fundTransactionRepository, AccountService accountService) {
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
            if (originAccount.getId().equals(destinationAccount.getId())) {
                return TransferResponse.error("Cannot transfer to the same account");
            }

            // Validate sufficient balance
            if (originAccount.getAvailableBalance().compareTo(request.getAmount()) < 0) {
                return TransferResponse.error("Insufficient balance");
            }

            // Update balances
            BigDecimal newOriginBalance = originAccount.getAvailableBalance().subtract(request.getAmount());
            BigDecimal newDestinationBalance = destinationAccount.getAvailableBalance().add(request.getAmount());

            accountService.updateAccountBalance(originAccount, newOriginBalance);
            accountService.updateAccountBalance(destinationAccount, newDestinationBalance);

            // Create transaction record
            FundTransaction transaction = new FundTransaction(
                    request.getAmount(),
                    LocalDateTime.now(),
                    request.getDescription() != null ? request.getDescription() : "Transfer",
                    TransactionType.TRANSFER,
                    originAccount,
                    destinationAccount
            );

            FundTransaction savedTransaction = fundTransactionRepository.save(transaction);

            return TransferResponse.success(savedTransaction.getId());

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
            BigDecimal newBalance = userAccount.getAvailableBalance().add(amount);
            accountService.updateAccountBalance(userAccount, newBalance);

            // Create LOAD type transaction record
            FundTransaction transaction = new FundTransaction(
                    amount,
                    LocalDateTime.now(),
                    "Balance load",
                    TransactionType.LOAD,
                    systemAccount,
                    userAccount
            );

            FundTransaction savedTransaction = fundTransactionRepository.save(transaction);

            return TransferResponse.success(savedTransaction.getId());

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
            if (userAccount.getAvailableBalance().compareTo(amount) < 0) {
                return TransferResponse.error("Insufficient balance");
            }

            // Update balances
            BigDecimal newUserBalance = userAccount.getAvailableBalance().subtract(amount);
            BigDecimal newCommerceBalance = commerceAccount.getAvailableBalance().add(amount);

            accountService.updateAccountBalance(userAccount, newUserBalance);
            accountService.updateAccountBalance(commerceAccount, newCommerceBalance);

            // Create PAYMENT type transaction record
            FundTransaction transaction = new FundTransaction(
                    amount,
                    LocalDateTime.now(),
                    description != null ? description : "Payment to commerce",
                    TransactionType.PAYMENT,
                    userAccount,
                    commerceAccount
            );

            FundTransaction savedTransaction = fundTransactionRepository.save(transaction);

            return TransferResponse.success(savedTransaction.getId());

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
        
        List<FundTransaction> transactions = fundTransactionRepository
                .findByOriginAccountOrDestinationAccountOrderByDateTimeDesc(account, account);

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
        FundTransaction transaction = fundTransactionRepository.findById(transactionId)
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
            return account.getAvailableBalance().compareTo(amount) >= 0;
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
     * Converts a FundTransaction entity to a response DTO.
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
    private TransactionResponse mapToTransactionResponse(FundTransaction transaction) {
        return new TransactionResponse(
                transaction.getId(),
                transaction.getAmount(),
                transaction.getDateTime(),
                transaction.getDescription(),
                transaction.getOriginAccount() != null ? transaction.getOriginAccount().getId() : null,
                transaction.getDestinationAccount() != null ? transaction.getDestinationAccount().getId() : null
        );
    }
}
