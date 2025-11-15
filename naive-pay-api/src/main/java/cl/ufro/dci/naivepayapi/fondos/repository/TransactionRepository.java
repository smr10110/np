package cl.ufro.dci.naivepayapi.fondos.repository;

import cl.ufro.dci.naivepayapi.fondos.domain.Account;
import cl.ufro.dci.naivepayapi.fondos.domain.Transaction;
import cl.ufro.dci.naivepayapi.fondos.domain.TransactionType;
import cl.ufro.dci.naivepayapi.fondos.domain.TransactionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for fund transaction management in the NaivePay system.
 * Provides data access methods for the Transaction entity.
 */
@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    
    /**
     * Finds all transactions where the account is origin or destination.
     * Useful for getting complete account history.
     * 
     * @param originAccount origin account
     * @param destinationAccount destination account (must be same as origin)
     * @return List of transactions ordered by date descending
     */
    List<Transaction> findByAccIdOriginOrAccIdDestinationOrderByTraDateTimeDesc(
            Account originAccount, 
            Account destinationAccount
    );
    
    /**
     * Finds transactions by origin account.
     * 
     * @param originAccount origin account
     * @return List of transactions ordered by date descending
     */
    List<Transaction> findByAccIdOriginOrderByTraDateTimeDesc(Account originAccount);
    
    /**
     * Finds transactions by destination account.
     * 
     * @param destinationAccount destination account
     * @return List of transactions ordered by date descending
     */
    List<Transaction> findByAccIdDestinationOrderByTraDateTimeDesc(Account destinationAccount);
    
    /**
     * Finds transactions by type.
     * 
     * @param type transaction type
     * @return List of transactions of specified type
     */
    List<Transaction> findByTraTypeOrderByTraDateTimeDesc(TransactionType type);
    
    /**
     * Finds transactions of an account in a date range.
     * 
     * @param account account (origin or destination)
     * @param startDate start date
     * @param endDate end date
     * @return List of transactions in range
     */
    @Query("SELECT t FROM Transaction t WHERE " +
           "(t.accIdOrigin = :account OR t.accIdDestination = :account) " +
           "AND t.traDateTime BETWEEN :startDate AND :endDate " +
           "ORDER BY t.traDateTime DESC")
    List<Transaction> findByAccountAndDateRange(
            @Param("account") Account account,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );
    
    /**
     * Finds transactions by type for a specific account.
     * 
     * @param account account (origin or destination)
     * @param type transaction type
     * @return List of transactions of specified type
     */
    @Query("SELECT t FROM Transaction t WHERE " +
           "(t.accIdOrigin = :account OR t.accIdDestination = :account) " +
           "AND t.traType = :type " +
           "ORDER BY t.traDateTime DESC")
    List<Transaction> findByAccountAndType(
            @Param("account") Account account,
            @Param("type") TransactionType type
    );
    
    /**
     * Finds transactions by type and status.
     * Useful for querying pending, rejected, or canceled transactions.
     * 
     * @param type transaction type
     * @param status transaction status
     * @return List of transactions matching type and status
     * @since 2.0
     */
    List<Transaction> findByTraTypeAndTraStatusOrderByTraDateTimeDesc(
            TransactionType type, 
            TransactionStatus status
    );
    
    /**
     * Finds transactions by status only.
     * 
     * @param status transaction status
     * @return List of transactions with specified status
     * @since 2.0
     */
    List<Transaction> findByTraStatusOrderByTraDateTimeDesc(TransactionStatus status);
    
    /**
     * Finds a specific transaction by ID and status.
     * Useful for validating transaction state before operations.
     * 
     * @param id transaction ID
     * @param status expected status
     * @return Optional containing the transaction if found
     * @since 2.0
     */
    Optional<Transaction> findByTraIdAndTraStatus(Long id, TransactionStatus status);
    
    /**
     * Finds all pending transactions (status = PENDING).
     * Shortcut method for approval workflows.
     * 
     * @return List of pending transactions
     * @since 2.0
     */
    @Query("SELECT t FROM Transaction t WHERE t.traStatus = 'PENDING' ORDER BY t.traDateTime DESC")
    List<Transaction> findAllPendingTransactions();
}
