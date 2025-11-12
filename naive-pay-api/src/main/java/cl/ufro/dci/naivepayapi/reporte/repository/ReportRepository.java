package cl.ufro.dci.naivepayapi.reporte.repository;

import cl.ufro.dci.naivepayapi.pagos.domain.PaymentTransaction;
import cl.ufro.dci.naivepayapi.pagos.domain.PaymentTransactionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Read-only repository for transaction reporting.
 *
 * Exposes parameterized queries to retrieve transactions filtered by:
 * <ul>
 *   <li>User (as origin or destination account).</li>
 *   <li>Date range (inclusive).</li>
 *   <li>Transaction status.</li>
 *   <li>Commerce.</li>
 *   <li>Description text (LIKE).</li>
 *   <li>Minimum and maximum amounts.</li>
 * </ul>
 *
 * Results are ordered by {@code createdAt} descending.
 *
 * <h2>Considerations</h2>
 * <ul>
 *   <li>{@code null} parameters disable their corresponding filter.</li>
 *   <li>For large result sets, consider a paginated variant using {@code Pageable}.</li>
 *   <li><strong>Attention:</strong> the “description” filter applies {@code LIKE} on the
 *       {@code category} field. Verify that this is the correct field (it might need to be {@code description}).</li>
 *   <li>For case-insensitive searches, use {@code LOWER(field)} and {@code LOWER(:param)}.</li>
 *   <li>Recommended indexes on {@code createdAt}, {@code status}, {@code commerce}, {@code amount},
 *       and any involved join columns.</li>
 * </ul>
 *
 * @since 1.0
 */
@Repository
public interface ReportRepository extends JpaRepository<PaymentTransaction, Long> {

    /**
     * Retrieves transactions filtered by user and optional criteria.
     *
     * A transaction qualifies if the user appears as the origin <em>or</em> destination account.
     * Each {@code null} parameter disables its corresponding filter.
     *
     * <h3>Ordering</h3>
     * <p>Results are ordered by {@code t.createdAt} descending.</p>
     *
     * <h3>Important note</h3>
     * <p>
     * The {@code description} parameter is matched against {@code t.category} via {@code LIKE %:description%}.
     * If the domain has a {@code description} field, consider replacing
     * {@code t.category} with {@code t.description} and, if needed, make it case-insensitive:
     * {@code LOWER(t.description) LIKE CONCAT('%', LOWER(:description), '%')}.
     * </p>
     *
     * @param userId      user ID (compared against {@code originAccount} or {@code destinationAccount}).
     * @param startDate   minimum (inclusive) {@code createdAt}; if {@code null}, no start filter.
     * @param endDate     maximum (inclusive) {@code createdAt}; if {@code null}, no end filter.
     * @param status      transaction status; if {@code null}, no status filter.
     * @param commerce    exact commerce; if {@code null}, no commerce filter.
     * @param description text to search with {@code LIKE} in the field referenced by the query; if {@code null}, no filter.
     * @param minAmount   minimum amount (inclusive); if {@code null}, no minimum filter.
     * @param maxAmount   maximum amount (inclusive); if {@code null}, no maximum filter.
     * @return list of transactions matching the filters, ordered by date descending.
     */
    @Query(value = """
           SELECT t.* FROM payment_transaction t
           WHERE (t.origin_account = :userId OR t.destination_account = :userId)
             AND (CAST(:startDate AS timestamp) IS NULL OR t.created_at >= CAST(:startDate AS timestamp))
             AND (CAST(:endDate AS timestamp) IS NULL OR t.created_at <= CAST(:endDate AS timestamp))
             AND (CAST(:status AS text) IS NULL OR t.status = CAST(:status AS text))
             AND (CAST(:commerce AS text) IS NULL OR t.commerce = CAST(:commerce AS text))
             AND (CAST(:description AS text) IS NULL OR lower(t.category::text) LIKE '%' || lower(CAST(:description AS text)) || '%')
             AND (CAST(:minAmount AS numeric) IS NULL OR t.amount >= CAST(:minAmount AS numeric))
             AND (CAST(:maxAmount AS numeric) IS NULL OR t.amount <= CAST(:maxAmount AS numeric))
           ORDER BY t.created_at DESC
           """, nativeQuery = true)
    List<PaymentTransaction> findFilteredTransactions(
            @Param("userId") Long userId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("status") PaymentTransactionStatus status,
            @Param("commerce") String commerce,
            @Param("description") String description,
            @Param("minAmount") BigDecimal minAmount,
            @Param("maxAmount") BigDecimal maxAmount
    );

}
