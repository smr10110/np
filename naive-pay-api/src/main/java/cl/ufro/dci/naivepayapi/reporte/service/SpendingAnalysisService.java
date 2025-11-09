package cl.ufro.dci.naivepayapi.reporte.service;

import cl.ufro.dci.naivepayapi.reporte.dto.ReportFilterDTO;
import org.springframework.stereotype.Service;
import cl.ufro.dci.naivepayapi.pagos.domain.PaymentTransaction;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

/**
 * User spending analysis service.
 *
 * Given a set of filtered transactions, produces a report with:
 * <ul>
 *   <li>Total spending.</li>
 *   <li>Distribution by category (or commerce), sorted from highest to lowest amount.</li>
 *   <li>Time series aggregated by day or by month.</li>
 * </ul>
 *
 * <h2>Notes</h2>
 * <ul>
 *   <li>Summations are computed with {@link BigDecimal} and the final result is exposed as a {@code double}.</li>
 *   <li>Empty or null keys are normalized to {@code "N/A"} via {@link #orNA(String)}.</li>
 *   <li>The {@code groupBy} parameter is uppercased via {@link #safe(String)} before evaluation.</li>
 * </ul>
 *
 * @since 1.0
 * @see ReportService
 * @see PaymentTransaction
 */
@Service
public class SpendingAnalysisService {

    /**
     * Underlying service to retrieve transactions based on filters.
     */
    private final ReportService reportService;

    /**
     * Creates an instance of the spending analysis service.
     *
     * @param reportService required dependency to fetch filtered transactions; must not be {@code null}.
     */
    public SpendingAnalysisService(ReportService reportService) {
        this.reportService = reportService;
    }

    /**
     * Builds a consolidated spending report for a given user.
     *
     * <h3>Processing flow</h3>
     * <ol>
     *   <li>Fetch filtered transactions by {@code userId} and {@code filter}.</li>
     *   <li>Compute total spending (sum of {@link PaymentTransaction#getAmount()}).</li>
     *   <li>Group by key (category or commerce) according to {@code groupBy} and sort by amount descending.</li>
     *   <li>Group by date according to {@code granularity} ("DAY" or "MONTH").</li>
     * </ol>
     *
     * <h3>Grouping parameters</h3>
     * <ul>
     *   <li>{@code groupBy}:
     *     <ul>
     *       <li>"TRANSACTION_TYPE" &rarr; groups by transaction {@code category}.</li>
     *       <li>Any other value (or empty) &rarr; groups by {@code commerce}.</li>
     *     </ul>
     *   </li>
     *   <li>{@code granularity}:
     *     <ul>
     *       <li>"MONTH" &rarr; monthly series ({@link YearMonth}).</li>
     *       <li>Any other value (or empty) &rarr; daily series ({@link LocalDate}).</li>
     *     </ul>
     *   </li>
     * </ul>
     *
     * <h3>Edge cases</h3>
     * <ul>
     *   <li>If {@code getAmount()} is {@code null} in any transaction, it is ignored in summations.</li>
     *   <li>Null or blank keys are transformed to {@code "N/A"}.</li>
     * </ul>
     *
     * <h3>Thread-safety</h3>
     * This method is not thread-safe if external mutable state is shared; internally it operates on local collections.
     *
     * @param userId      identifier of the user whose transactions are analyzed; must not be {@code null}.
     * @param filter      filtering criteria to apply; may be {@code null} if the implementation allows it.
     * @param groupBy     key grouping strategy (see section above).
     * @param granularity time series granularity ("DAY" or "MONTH").
     * @return immutable {@link SpendingReport} instance with total, per-key breakdown, and time series.
     */

    public SpendingReport build(Long userId, ReportFilterDTO filter, String groupBy, String granularity) {
        List<PaymentTransaction> tx = reportService.getFilteredTransactions(filter, userId);

        double total = tx.stream()
                .map(PaymentTransaction::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .doubleValue();

        LinkedHashMap<String, Double> byCategory = switch (safe(groupBy)) {
            case "TRANSACTION_TYPE" -> groupByKey(tx, t -> orNA(t.getCategory()));
            default -> groupByKey(tx, t -> orNA(t.getCommerce()));
        };

        List<DateSummary> byDate = switch (safe(granularity)) {
            case "MONTH" -> groupByMonth(tx);
            default -> groupByDay(tx);
        };

        return new SpendingReport(total, byCategory, byDate);
    }

    /**
     * Returns {@code null} if the string is {@code null} or blank; otherwise returns the same string.
     *
     * @param s input string; may be {@code null}.
     * @return {@code null} if {@code s} is {@code null} or blank; otherwise {@code s}.
     */
    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }

    /**
     * Returns {@code "N/A"} if the string is {@code null} or blank; otherwise returns the same string.
     *
     * @param s input string; may be {@code null}.
     * @return {@code "N/A"} if blank or null; otherwise {@code s}.
     */
    private static String orNA(String s) {
        return (s == null || s.isBlank()) ? "N/A" : s;
    }

    /**
     * Normalizes a string for safe evaluation in {@code switch} or comparison operations.
     * If the input is {@code null}, returns an empty string. Otherwise applies {@code trim()} and {@code toUpperCase()}.
     *
     * @param s input string; may be {@code null}.
     * @return normalized (non-null) string.
     */
    private static String safe(String s) {
        return s == null ? "" : s.trim().toUpperCase();
    }

    /**
     * Groups and sums amounts by an arbitrary key defined by {@code classifier}, and sorts by amount descending.
     *
     * @param tx         list of transactions to group; must not be {@code null}.
     * @param classifier function extracting the grouping key from each transaction.
     * @return {@link LinkedHashMap} with keys and totals as {@code double}, sorted from highest to lowest.
     */
    private static LinkedHashMap<String, Double> groupByKey(
            List<PaymentTransaction> tx,
            java.util.function.Function<PaymentTransaction, String> classifier
    ) {
        Map<String, BigDecimal> sums = tx.stream().collect(Collectors.groupingBy(
                classifier,
                Collectors.reducing(BigDecimal.ZERO, PaymentTransaction::getAmount, BigDecimal::add)
        ));
        return sums.entrySet().stream()
                .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getValue().doubleValue(),
                        (a, b) -> a, LinkedHashMap::new));
    }

    /**
     * Produces a time series aggregated by day (YYYY-MM-DD) with the daily total.
     *
     * @param tx list of transactions; must not be {@code null}.
     * @return immutable list of {@link DateSummary} sorted by ascending date.
     */
    private static List<DateSummary> groupByDay(List<PaymentTransaction> tx) {
        Map<LocalDate, BigDecimal> sums = tx.stream().collect(Collectors.groupingBy(
                t -> t.getCreatedAt().toLocalDate(),
                Collectors.reducing(BigDecimal.ZERO, PaymentTransaction::getAmount, BigDecimal::add)
        ));
        return sums.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> new DateSummary(e.getKey().toString(), e.getValue().doubleValue()))
                .toList();
    }

    /**
     * Produces a time series aggregated by month ({@link YearMonth#toString()}, format {@code yyyy-MM}).
     *
     * @param tx list of transactions; must not be {@code null}.
     * @return immutable list of {@link DateSummary} sorted by ascending month.
     */
    private static List<DateSummary> groupByMonth(List<PaymentTransaction> tx) {
        Map<YearMonth, BigDecimal> sums = tx.stream().collect(Collectors.groupingBy(
                t -> YearMonth.from(t.getCreatedAt()),
                Collectors.reducing(BigDecimal.ZERO, PaymentTransaction::getAmount, BigDecimal::add)
        ));
        return sums.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> new DateSummary(e.getKey().toString(), e.getValue().doubleValue()))
                .toList();
    }

    /**
     * Immutable report with spending metrics.
     *
     * @param totalSpent total spending in the filtered period (sum of amounts).
     * @param byCategory totals per key (category or commerce), sorted descending by amount.
     * @param byDate     time series (by day or month) with totals for each period.
     */
    public record SpendingReport(
            double totalSpent,
            Map<String, Double> byCategory,
            List<DateSummary> byDate
    ) {}

    /**
     * Summary for a time period (day or month) with its associated total.
     *
     * @param period label of the period (e.g., "2025-10-01" or "2025-10").
     * @param total  total of the period as {@code Double}.
     */
    public record DateSummary(String period, Double total) {}
}