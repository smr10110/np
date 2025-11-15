package cl.ufro.dci.naivepayapi.reporte.repository;

import cl.ufro.dci.naivepayapi.reporte.dto.TransactionDTO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import cl.ufro.dci.naivepayapi.fondos.domain.Transaction;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Read-only repository for fund transaction reporting.
 *
 * Filters by:
 * <ul>
 *   <li>User (via Account → User → useId)</li>
 *   <li>Date range</li>
 *   <li>Status</li>
 *   <li>Commerce name</li>
 *   <li>Payment category (LIKE)</li>
 *   <li>Amount range</li>
 * </ul>
 *
 * Results are ordered by transaction date descending.
 *
 * @since 2.0
 */
@Repository
public interface ReportRepository extends JpaRepository<Transaction, Long> {

    @Query(nativeQuery = true, value = """
        SELECT 
          t.tra_id,
          t.tra_amount,
          CAST(t.tra_date_time AS TIMESTAMP) as tra_date_time,
          t.tra_description,
          t.tra_type,
          t.tra_customer_name,
          t.tra_commerce_name,
          t.tra_payment_category,
          t.tra_status,
          t.acc_id_origin,
          t.acc_id_destination,
          u_origin.use_names || ' ' || u_origin.use_last_names as origin_user_name,
          u_dest.use_names || ' ' || u_dest.use_last_names as destination_user_name
        FROM transaction t
        JOIN account aid ON aid.acc_id = t.acc_id_destination
        JOIN account aio ON aio.acc_id = t.acc_id_origin
        LEFT JOIN app_user u_origin ON aio.use_id = u_origin.use_id
        LEFT JOIN app_user u_dest ON aid.use_id = u_dest.use_id
        WHERE 
          (CAST(:userId AS BIGINT) IS NULL OR aio.use_id = :userId OR aid.use_id = :userId)
          AND (CAST(:startDate AS TIMESTAMP) IS NULL OR t.tra_date_time >= :startDate)
          AND (CAST(:endDate AS TIMESTAMP) IS NULL OR t.tra_date_time <= :endDate)
          AND (CAST(:status AS VARCHAR) IS NULL OR t.tra_status = CAST(:status AS VARCHAR))
          AND (CAST(:commerce AS VARCHAR) IS NULL OR LOWER(t.tra_commerce_name) = LOWER(:commerce))
          AND (CAST(:category AS VARCHAR) IS NULL OR LOWER(t.tra_payment_category) LIKE LOWER('%' || :category || '%'))
          AND (CAST(:minAmount AS NUMERIC) IS NULL OR t.tra_amount >= :minAmount)
          AND (CAST(:maxAmount AS NUMERIC) IS NULL OR t.tra_amount <= :maxAmount)
        ORDER BY t.tra_date_time DESC
    """)
    List<TransactionDTO> findFilteredTransactions(
            @Param("userId") Long userId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("status") String status,
            @Param("commerce") String commerce,
            @Param("category") String category,
            @Param("minAmount") BigDecimal minAmount,
            @Param("maxAmount") BigDecimal maxAmount
    );
}
