package cl.ufro.dci.naivepayapi.reporte.service;

import cl.ufro.dci.naivepayapi.pagos.domain.PaymentTransaction;
import cl.ufro.dci.naivepayapi.pagos.domain.PaymentTransactionStatus;
import cl.ufro.dci.naivepayapi.reporte.dto.ReportFilterDTO;
import cl.ufro.dci.naivepayapi.reporte.repository.ReportRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Reporting service for retrieving and exporting transactions.
 *
 * Exposes utilities to:
 * <ul>
 *   <li>Query transactions filtered by user and search criteria.</li>
 *   <li>Export a collection of transactions to a simple CSV.</li>
 * </ul>
 *
 * <h2>Implementation notes</h2>
 * <ul>
 *   <li>If the search yields no results, a {@link ResponseStatusException}
 *       with {@link HttpStatus#NOT_FOUND} is thrown.</li>
 *   <li>The generated CSV is basic (does not escape commas, quotes, or newlines).
 *       For production, consider using a CSV library (e.g., OpenCSV) and
 *       following RFC 4180.</li>
 *   <li><strong>Potential inconsistency:</strong> the CSV header uses
 *       <em>Description</em> but the code writes {@code getCategory()}. Verify that the field is the expected one.</li>
 * </ul>
 *
 * @since 1.0
 */
@Service
public class ReportService {

    /**
     * Repository to access persisted transactions.
     */
    private final ReportRepository reportRepository;

    /**
     * Creates a new instance of the reporting service.
     *
     * @param reportRepository report repository; must not be {@code null}.
     */
    public ReportService(ReportRepository reportRepository) {
        this.reportRepository = reportRepository;
    }

    /**
     * Retrieves filtered transactions for a given user.
     *
     * Applies the {@code filters} criteria over the set of transactions for {@code userId}.
     * If no results are found, an HTTP 404 exception is thrown.
     *
     * @param filters filtering criteria (date range, status, commerce, description, amounts).
     * @param userId  identifier of the user who owns the transactions.
     * @return list of transactions that match the filter.
     * @throws ResponseStatusException if no transactions are found ({@code 404 NOT_FOUND}).
     */
    public List<PaymentTransaction> getFilteredTransactions(ReportFilterDTO filters, Long userId) {

        LocalDateTime startDate = filters.getStartDate();
        LocalDateTime endDate = filters.getEndDate();
        BigDecimal minAmount = filters.getMinAmount();
        BigDecimal maxAmount = filters.getMaxAmount();

        List<PaymentTransaction> transactions = reportRepository.findFilteredTransactions(
                userId,
                startDate,
                endDate,
                filters.getStatus(),
                filters.getCommerce(),
                filters.getDescription(),
                minAmount,
                maxAmount
        );

        for (PaymentTransaction tx : transactions) {
            if (tx.getOriginAccount().equals(userId)) {
                tx.setAmount(tx.getAmount().negate());
            }
        }

        return transactions;
    }

    /**
     * Exports a list of transactions to a simple CSV (no quoting/escaping).
     *
     * The CSV contains the columns:
     * {@code ID,Date,Amount,State,Commerce,Description}.
     *
     * <h3>Limitations</h3>
     * <ul>
     *   <li>No escaping rules are applied for commas, quotes, or newlines
     *       in text values (e.g., commerce or description).</li>
     *   <li>The date format depends on {@code toString()} of {@code createdAt}.</li>
     *   <li><strong>Verify:</strong> currently {@code getCategory()} is written into the
     *       "Description" column. Adjust to {@code getDescription()} or change the header accordingly.</li>
     * </ul>
     *
     * @param transactions list of transactions to export.
     * @return CSV content as bytes using the JVM default encoding.
     */
    public byte[] exportToCsv(List<PaymentTransaction> transactions) {
        StringBuilder csv = new StringBuilder();
        csv.append("ID,Date,Amount,State,Commerce,Description\n");
        for (PaymentTransaction t : transactions) {
            csv.append(String.format("%d,%s,%.2f,%s,%s,%s\n",
                    t.getId(),
                    t.getCreatedAt(),
                    t.getAmount(),
                    t.getStatus(),
                    t.getCommerce(),
                    t.getCategory() // <-- Check if it should be getDescription()
            ));
        }
        return csv.toString().getBytes();
    }
}
