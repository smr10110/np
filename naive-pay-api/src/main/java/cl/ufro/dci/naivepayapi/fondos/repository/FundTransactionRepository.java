package cl.ufro.dci.naivepayapi.fondos.repository;

import cl.ufro.dci.naivepayapi.fondos.domain.Account;
import cl.ufro.dci.naivepayapi.fondos.domain.FundTransaction;
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
 * Provides data access methods for the FundTransaction entity.
 */
@Repository
public interface FundTransactionRepository extends JpaRepository<FundTransaction, Long> {
    
    /**
     * Finds all transactions where the account is origin or destination.
     * Useful for getting complete account history.
     * 
     * @param originAccount origin account
     * @param destinationAccount destination account (must be same as origin)
     * @return List of transactions ordered by date descending
     */
    List<FundTransaction> findByOriginAccountOrDestinationAccountOrderByDateTimeDesc(
            Account originAccount, 
            Account destinationAccount
    );
    
    /**
     * Finds transactions by origin account.
     * 
     * @param originAccount origin account
     * @return List of transactions ordered by date descending
     */
    List<FundTransaction> findByOriginAccountOrderByDateTimeDesc(Account originAccount);
    
    /**
     * Finds transactions by destination account.
     * 
     * @param destinationAccount destination account
     * @return List of transactions ordered by date descending
     */
    List<FundTransaction> findByDestinationAccountOrderByDateTimeDesc(Account destinationAccount);
    
    /**
     * Finds transactions by type.
     * 
     * @param type transaction type
     * @return List of transactions of specified type
     */
    List<FundTransaction> findByTypeOrderByDateTimeDesc(TransactionType type);
    
    /**
     * Finds transactions of an account in a date range.
     * 
     * @param account account (origin or destination)
     * @param startDate start date
     * @param endDate end date
     * @return List of transactions in range
     */
    @Query("SELECT t FROM FundTransaction t WHERE " +
           "(t.originAccount = :account OR t.destinationAccount = :account) " +
           "AND t.dateTime BETWEEN :startDate AND :endDate " +
           "ORDER BY t.dateTime DESC")
    List<FundTransaction> findByAccountAndDateRange(
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
    @Query("SELECT t FROM FundTransaction t WHERE " +
           "(t.originAccount = :account OR t.destinationAccount = :account) " +
           "AND t.type = :type " +
           "ORDER BY t.dateTime DESC")
    List<FundTransaction> findByAccountAndType(
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
    List<FundTransaction> findByTypeAndStatusOrderByDateTimeDesc(
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
    List<FundTransaction> findByStatusOrderByDateTimeDesc(TransactionStatus status);
    
    /**
     * Finds a specific transaction by ID and status.
     * Useful for validating transaction state before operations.
     * 
     * @param id transaction ID
     * @param status expected status
     * @return Optional containing the transaction if found
     * @since 2.0
     */
    Optional<FundTransaction> findByIdAndStatus(Long id, TransactionStatus status);
    
    /**
     * Finds all pending transactions (status = PENDING).
     * Shortcut method for approval workflows.
     * 
     * @return List of pending transactions
     * @since 2.0
     */
    @Query("SELECT t FROM FundTransaction t WHERE t.status = 'PENDING' ORDER BY t.dateTime DESC")
    List<FundTransaction> findAllPendingTransactions();
}
