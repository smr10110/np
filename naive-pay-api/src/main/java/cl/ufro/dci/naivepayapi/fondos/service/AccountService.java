package cl.ufro.dci.naivepayapi.fondos.service;

import cl.ufro.dci.naivepayapi.fondos.domain.Account;
import cl.ufro.dci.naivepayapi.fondos.dto.AccountBalanceResponse;
import cl.ufro.dci.naivepayapi.fondos.repository.AccountRepository;
import cl.ufro.dci.naivepayapi.registro.domain.User;
import cl.ufro.dci.naivepayapi.registro.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Service for account management in the NaivePay system.
 * <p>
 * Implements the business logic related to user accounts,
 * including creation, querying, balance updates, and validations.
 * </p>
 * 
 * <p><b>Main responsibilities:</b></p>
 * <ul>
 *   <li>Create new accounts for registered users</li>
 *   <li>Query account information and balances</li>
 *   <li>Update balances in a transactional manner</li>
 *   <li>Manage the special system account for fund loading</li>
 *   <li>Validate account existence</li>
 * </ul>
 * 
 * <p><b>Transactional guarantees:</b></p>
 * <ul>
 *   <li>All write operations are transactional</li>
 *   <li>Read-only queries are optimized with {@code readOnly=true}</li>
 *   <li>Automatic rollback in case of errors</li>
 * </ul>
 * 
 * @author NaivePay Development Team
 * @version 1.0
 * @since 2025-10-06
 * @see Account
 * @see AccountRepository
 * @see AccountBalanceResponse
 */
@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;

    /**
     * Special system user ID.
     * <p>
     * This account is used as the origin in balance load transactions (LOAD).
     * It has an "infinite" balance to allow any load operation.
     * In production, this corresponds to the admin@admin.com user (first user created).
     * </p>
     */
    public static final Long SYSTEM_ACCOUNT_USER_ID = 1L;

    /**
     * Constructor with dependency injection.
     * 
     * @param accountRepository the account repository
     * @param userRepository the user repository
     */
    public AccountService(AccountRepository accountRepository, UserRepository userRepository) {
        this.accountRepository = accountRepository;
        this.userRepository = userRepository;
    }

    /**
     * Gets the balance of an account by user ID.
     * <p>
     * Performs an optimized read-only query to obtain
     * the balance information of the account associated with the user.
     * </p>
     * 
     * @param userId the ID of the account owner user
     * @return an {@link AccountBalanceResponse} object with balance information
     * @throws IllegalArgumentException if no account exists for the specified user
     */
    @Transactional(readOnly = true)
    public AccountBalanceResponse getAccountBalance(Long userId) {
        Account account = accountRepository.findByUserUseId(userId)
                .orElseThrow(() -> new IllegalArgumentException("No account exists for user with ID: " + userId));
        
        return mapToBalanceResponse(account);
    }

    /**
     * Creates a new account for a user.
     * <p>
     * Validates that the user doesn't already have an existing account before creating a new one.
     * The account is created with an initial balance of 0 (zero).
     * </p>
     * 
     * <p><b>Creation process:</b></p>
     * <ol>
     *   <li>Verifies that no previous account exists for the user</li>
     *   <li>Creates a new Account instance with balance 0</li>
     *   <li>Persists the account in the database</li>
     *   <li>Returns the created account information</li>
     * </ol>
     * 
     * @param userId the ID of the user for whom the account is created
     * @return an {@link AccountBalanceResponse} object with the created account information
     * @throws IllegalArgumentException if an account already exists for the user
     */
    @Transactional
    public AccountBalanceResponse createAccount(Long userId) {
        // Validate that account doesn't already exist
        if (accountRepository.existsByUserUseId(userId)) {
            throw new IllegalArgumentException("An account already exists for user with ID: " + userId);
        }

        // Get user from database
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));

        // Create new account with initial balance of 0
        Account newAccount = new Account(user);
        Account savedAccount = accountRepository.save(newAccount);

        return mapToBalanceResponse(savedAccount);
    }

    /**
     * Checks if an account exists for a user.
     * <p>
     * Useful method for validations prior to account creation
     * or to verify if a user is enabled to perform transactions.
     * </p>
     * 
     * @param userId the ID of the user to check
     * @return {@code true} if an account exists, {@code false} otherwise
     */
    @Transactional(readOnly = true)
    public boolean accountExists(Long userId) {
        return accountRepository.existsByUserUseId(userId);
    }

    /**
     * Gets an account by user ID (internal use).
     * <p>
     * Internal access method used primarily by the transaction
     * service to obtain complete Account entities.
     * </p>
     * 
     * @param userId the ID of the account owner user
     * @return the complete {@link Account} entity
     * @throws IllegalArgumentException if no account exists for the user
     */
    @Transactional(readOnly = true)
    public Account getAccountByUserId(Long userId) {
        return accountRepository.findByUserUseId(userId)
                .orElseThrow(() -> new IllegalArgumentException("No account exists for user with ID: " + userId));
    }

    /**
     * Gets an account by its direct ID (internal use).
     * <p>
     * Internal access method used to obtain accounts when
     * the account ID is known directly, not the user ID.
     * </p>
     * 
     * @param accountId the account ID
     * @return the complete {@link Account} entity
     * @throws IllegalArgumentException if no account exists with the specified ID
     */
    @Transactional(readOnly = true)
    public Account getAccountById(Long accountId) {
        return accountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("No account exists with ID: " + accountId));
    }

    /**
     * Gets or creates the special system account.
     * <p>
     * This special account (userID = 0) is used as the origin in LOAD type
     * transactions to simulate fund loading from an external source.
     * </p>
     * 
     * <p><b>System account characteristics:</b></p>
     * <ul>
     *   <li>Fixed UserID: {@value #SYSTEM_ACCOUNT_USER_ID}</li>
     *   <li>"Infinite" balance ({@link Long#MAX_VALUE})</li>
     *   <li>Automatically created if it doesn't exist</li>
     *   <li>Does not represent a real user account</li>
     * </ul>
     * 
     * @return the system account
     */
    @Transactional
    public Account getOrCreateSystemAccount() {
        return accountRepository.findByUserUseId(SYSTEM_ACCOUNT_USER_ID)
                .orElseGet(() -> {
                    // Get or create system user
                    User systemUser = userRepository.findById(SYSTEM_ACCOUNT_USER_ID)
                            .orElseGet(() -> {
                                User newSystemUser = new User();
                                newSystemUser.setUseId(SYSTEM_ACCOUNT_USER_ID);
                                newSystemUser.setUseNames("Sistema");
                                newSystemUser.setUseLastNames("NaivePay");
                                return userRepository.save(newSystemUser);
                            });
                    
                    Account systemAccount = new Account(systemUser);
                    systemAccount.setAccAvailableBalance(BigDecimal.valueOf(Long.MAX_VALUE)); // "Infinite" balance for system
                    return accountRepository.save(systemAccount);
                });
    }

    /**
     * Updates an account balance (internal use).
     * <p>
     * Transactional method that updates an account balance and
     * automatically records the last modification date.
     * </p>
     * 
     * <p><b>Important:</b> This method does not perform business validations
     * such as verifying sufficient funds. Those validations must
     * be done in the transaction service before calling this method.</p>
     * 
     * @param account the account to update
     * @param newBalance the new balance to set
     * @return the updated and persisted account
     */
    @Transactional
    public Account updateAccountBalance(Account account, BigDecimal newBalance) {
        account.updateBalance(newBalance);
        return accountRepository.save(account);
    }

    /**
     * Converts an Account entity to a response DTO.
     * <p>
     * Private helper method that performs the mapping from JPA entity
     * to the DTO exposed by the API, avoiding direct exposure of the domain model.
     * </p>
     * 
     * @param account the Account entity to convert
     * @return an {@link AccountBalanceResponse} object with the relevant data
     */
    private AccountBalanceResponse mapToBalanceResponse(Account account) {
        return new AccountBalanceResponse(
                account.getAccId(),
                account.getUser().getUseId(),
                account.getAccAvailableBalance(),
                account.getAccLastUpdate()
        );
    }
}
