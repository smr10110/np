package cl.ufro.dci.naivepayapi.reporte.service;

import cl.ufro.dci.naivepayapi.reporte.dto.ReportFilterDTO;
import cl.ufro.dci.naivepayapi.reporte.dto.TransactionDTO;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Usage and spending trends analysis service.
 *
 * Exposes utilities to compute aggregated metrics from filtered payment
 * transactions (e.g., monthly average spending per user).
 *
 * <h2>Responsibilities</h2>
 * <ul>
 *   <li>Fetch filtered transactions from {@link ReportService}.</li>
 *   <li>Aggregate them by calendar month ({@link YearMonth}).</li>
 *   <li>Compute averages with fixed decimal precision.</li>
 * </ul>
 *
 * <h2>Implementation notes</h2>
 * <ul>
 *   <li>Uses {@link BigDecimal} for summations and division to avoid precision
 *       errors, returning a {@code double} at the end for consumer convenience.</li>
 *   <li>The average is rounded with {@link RoundingMode#HALF_UP} to 2 decimals.</li>
 *   <li>If there are no transactions, the result is {@code 0.0}.</li>
 * </ul>
 *
 * @see ReportService
 * @see TransactionDTO
 * @since 1.0
 */
@Service
public class UsageTrends {

    /**
     * Report service used to retrieve transactions according to filters.
     */
    private final ReportService reportService;

    /**
     * Creates an instance of the usage trends service.
     *
     * @param reportService dependency used to obtain filtered transactions;
     *                      must not be {@code null}.
     */
    public UsageTrends(ReportService reportService) {
        this.reportService = reportService;
    }

    /**
     * Calculates the monthly average spending for a given user, considering
     * the transactions that satisfy the provided filter.
     *
     * <h3>Computation steps</h3>
     * <ol>
     *   <li>Fetch transactions by {@code userId} and {@code filter} from
     *       {@link ReportService}.</li>
     *   <li>Group by month of creation ({@link TransactionDTO#traDateTime()} ).</li>
     *   <li>Sum the amounts per month using {@link BigDecimal}.</li>
     *   <li>Divide the grand total by the number of months that have transactions,
     *       rounding to 2 decimals with {@link RoundingMode#HALF_UP}.</li>
     * </ol>
     *
     * <h3>Conventions and edge cases</h3>
     * <ul>
     *   <li>If no transactions exist, returns {@code 0.0}.</li>
     *   <li>Only months that actually have at least one transaction are counted.</li>
     *   <li>If any transaction has {@code traAmount() == null}, it is implicitly
     *       ignored by using the reduction identity {@code BigDecimal.ZERO}.</li>
     * </ul>
     *
     * <h3>Implementation note</h3>
     * Internal precision is handled with {@link BigDecimal}. Returning a {@code double}
     * is intended for convenience in upper layers (e.g., JSON serialization).
     *
     * <h3>API note</h3>
     * This method does not include months without transactions in the denominator.
     *
     * @param userId the identifier of the user whose transactions are analyzed; must not be {@code null}.
     * @param filter report filtering criteria; may be {@code null} if {@link ReportService}
     *               allows it in its implementation.
     * @return monthly average spending as a {@code double} with 2 decimals; {@code 0.0} if no data.
     */
    public double averageMonthlySpent(Long userId, ReportFilterDTO filter) {

        List<TransactionDTO> tx = reportService.getFilteredTransactions(filter, userId);

        if (tx.isEmpty()) return 0.0;

        Map<YearMonth, BigDecimal> totalsPerMonth = tx.stream()
                .collect(Collectors.groupingBy(
                        t -> YearMonth.from(t.traDateTime),
                        Collectors.reducing(BigDecimal.ZERO, t -> t.traAmount, BigDecimal::add)
                ));

        BigDecimal grandTotal = totalsPerMonth.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long countMonths = totalsPerMonth.size();
        if (countMonths == 0) return 0.0;

        BigDecimal average = grandTotal.divide(
                BigDecimal.valueOf(countMonths),
                2,
                RoundingMode.HALF_UP
        );
        return average.doubleValue();
    }

    /**
     * Converts a string to {@code null} if it is {@code null} or contains only whitespace.
     * Utility method to normalize optional textual inputs.
     *
     * @param s input string; may be {@code null}.
     * @return {@code null} if {@code s} is {@code null} or blank; otherwise the same string.
     */
    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }
}
