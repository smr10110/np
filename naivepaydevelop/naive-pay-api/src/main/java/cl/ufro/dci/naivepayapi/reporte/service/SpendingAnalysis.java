package cl.ufro.dci.naivepayapi.reporte.service;

import cl.ufro.dci.naivepayapi.pagos.domain.PaymentTransaction;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Spending analysis utilities over an immutable collection of transactions.
 *
 * This class provides common aggregations:
 * <ul>
 *   <li>Total spending.</li>
 *   <li>Spending by category/commerce, sorted in descending order by amount.</li>
 *   <li>Time series by day and by month.</li>
 * </ul>
 *
 * <h2>Design</h2>
 * <ul>
 *   <li>The internal transaction list is copied immutably in the constructor.</li>
 *   <li>Sums are computed with {@link BigDecimal} and exposed as {@code double}
 *       for consumer convenience.</li>
 * </ul>
 *
 * <h2>Assumptions and notes</h2>
 * <ul>
 *   <li>It is assumed that {@code PaymentTransaction#getAmount()} is not {@code null} except where explicitly filtered.</li>
 *   <li>In the per-category breakdown, {@code commerce} null values are normalized to {@code "N/A"}.</li>
 *   <li>Result map keys preserve the sort order (using {@link LinkedHashMap}).</li>
 * </ul>
 *
 * @see PaymentTransaction
 * @since 1.0
 */
public class SpendingAnalysis {

    /**
     * Immutable set of source transactions for the analysis.
     */
    private final List<PaymentTransaction> tx;

    /**
     * Creates an analysis instance over a list of transactions.
     *
     * If {@code transactions} is {@code null}, an empty list is used.
     * Otherwise, an immutable copy is created via {@link List#copyOf(Collection)}.
     *
     * @param transactions list of transactions to analyze; may be {@code null}.
     */
    public SpendingAnalysis(List<PaymentTransaction> transactions) {
        this.tx = (transactions == null) ? List.of() : List.copyOf(transactions);
    }

    /**
     * Computes total spending by summing the amounts of all transactions.
     *
     * Transactions with {@code amount} equal to {@code null} are ignored.
     *
     * @return total spent as a {@code double}.
     */
    public double totalSpent() {
        return tx.stream()
                .map(PaymentTransaction::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .doubleValue();
    }

    /**
     * Computes total spending grouped by category/commerce.
     *
     * The grouping key is {@code commerce}; if it is {@code null}, {@code "N/A"} is used.
     * The result is returned sorted in descending order by amount.
     *
     * <h3>Implementation note</h3>
     * This method assumes {@code getAmount()} is not {@code null}. If null amounts exist,
     * consider filtering them out or mapping to {@code BigDecimal.ZERO}.
     *
     * @return a map (desc) from category/commerce to total spent.
     */
    public LinkedHashMap<String, Double> byCategory() {
        Map<String, BigDecimal> sums = tx.stream().collect(Collectors.groupingBy(
                t -> Optional.ofNullable(t.getCommerce()).orElse("N/A"),
                Collectors.reducing(BigDecimal.ZERO, PaymentTransaction::getAmount, BigDecimal::add)
        ));
        // sort desc
        return sums.entrySet().stream()
                .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getValue().doubleValue(),
                        (a,b) -> a,
                        LinkedHashMap::new
                ));
    }

    /**
     * Produces a daily time series with the total per day.
     *
     * The key is {@link LocalDate} derived from each transaction's {@code createdAt}.
     * The result is sorted in ascending date order.
     *
     * <h3>Implementation note</h3>
     * This method assumes {@code getAmount()} is not {@code null}.
     *
     * @return a map (asc) from date to total spent that day.
     */
    public LinkedHashMap<LocalDate, Double> byDay() {
        Map<LocalDate, BigDecimal> sums = tx.stream().collect(Collectors.groupingBy(
                t -> t.getCreatedAt().toLocalDate(),
                Collectors.reducing(BigDecimal.ZERO, PaymentTransaction::getAmount, BigDecimal::add)
        ));
        return sums.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getValue().doubleValue(),
                        (a,b) -> a,
                        LinkedHashMap::new
                ));
    }

    /**
     * Produces a monthly time series with the total per month.
     *
     * The key is {@link YearMonth} derived from {@code createdAt}.
     * The result is sorted in ascending month order.
     *
     * <h3>Implementation note</h3>
     * This method assumes {@code getAmount()} is not {@code null}.
     *
     * @return a map (asc) from {@link YearMonth} to total spent in that month.
     */
    public LinkedHashMap<YearMonth, Double> byMonth() {
        Map<YearMonth, BigDecimal> sums = tx.stream().collect(Collectors.groupingBy(
                t -> YearMonth.from(t.getCreatedAt()),
                Collectors.reducing(BigDecimal.ZERO, PaymentTransaction::getAmount, BigDecimal::add)
        ));
        return sums.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getValue().doubleValue(),
                        (a,b) -> a,
                        LinkedHashMap::new
                ));
    }
}
