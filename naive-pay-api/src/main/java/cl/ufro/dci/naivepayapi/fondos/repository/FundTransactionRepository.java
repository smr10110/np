package cl.ufro.dci.naivepayapi.fondos.repository;

import cl.ufro.dci.naivepayapi.fondos.domain.Account;
import cl.ufro.dci.naivepayapi.fondos.domain.FundTransaction;
import cl.ufro.dci.naivepayapi.fondos.domain.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

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
}
