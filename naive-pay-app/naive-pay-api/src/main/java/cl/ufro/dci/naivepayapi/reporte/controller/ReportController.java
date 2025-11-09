package cl.ufro.dci.naivepayapi.reporte.controller;

import org.springframework.security.core.Authentication;
import cl.ufro.dci.naivepayapi.pagos.domain.PaymentTransaction;
import cl.ufro.dci.naivepayapi.reporte.dto.ReportFilterDTO;
import cl.ufro.dci.naivepayapi.reporte.service.ReportService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import cl.ufro.dci.naivepayapi.reporte.util.AuthUtils;

import java.util.List;

/**
 * REST controller for querying and exporting reports.
 *
 * Endpoints under <code>/api/reports</code> to:
 * <ul>
 *   <li>List filtered transactions of the authenticated user.</li>
 *   <li>Export those transactions to CSV.</li>
 * </ul>
 *
 * <h2>CORS</h2>
 * Allows requests from <code>http://localhost:4200</code>.
 *
 * <h2>Notes</h2>
 * <ul>
 *   <li>It is assumed that {@link Authentication#getName()} contains the numeric {@code userId}.</li>
 *   <li>For logging, consider using a logger (SLF4J) instead of {@code System.out.println()}.</li>
 * </ul>
 *
 * @since 1.0
 */
@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/api/reports")
public class ReportController {

    /**
     * Reporting service for retrieval and export.
     */
    private final ReportService reportService;

    /**
     * Creates the report controller.
     *
     * @param reportService reporting service; must not be {@code null}.
     */
    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    /**
     * Returns the list of the authenticated user's transactions that match
     * the specified filters.
     *
     * <h3>Security</h3>
     * The user ID is obtained from {@link Authentication#getName()} and converted to {@link Long}.
     *
     * <h3>Response</h3>
     * 200 OK with the list of {@link PaymentTransaction}. If there are no results,
     * {@link ReportService} may throw 404 (NOT_FOUND).
     *
     * @param filters search filters (date range, status, commerce, description, amounts).
     * @param auth    current authentication context.
     * @return HTTP 200 response with the filtered transaction list.
     * @throws NumberFormatException if the principal cannot be converted to {@code Long}.
     */
    @PostMapping("/transactions")
    public ResponseEntity<List<PaymentTransaction>> getTransactions(
            @RequestBody ReportFilterDTO filters, Authentication auth) {

        Long currentUserId = filters.getUserId() != null ? filters.getUserId() : AuthUtils.getUserId(auth);

        List<PaymentTransaction> transactions =
                reportService.getFilteredTransactions(filters, currentUserId);
        return ResponseEntity.ok(transactions);
    }

    /**
     * Exports to CSV the authenticated user's transactions that match the filters.
     *
     * <h3>Content</h3>
     * Returns a <code>text/csv</code> file with a download header.
     *
     * <h3>Errors</h3>
     * <ul>
     *   <li>404 (NOT_FOUND) if there are no transactions (thrown by the service).</li>
     *   <li>{@link NumberFormatException} if the principal cannot be converted to {@code Long}.</li>
     * </ul>
     *
     * @param filters search filters applied prior to export.
     * @param auth    authentication context.
     * @return 200 OK with the CSV in the body and download headers.
     */
    @PostMapping("/export/csv")
    public ResponseEntity<byte[]> exportReportToCsv(@RequestBody ReportFilterDTO filters, Authentication auth) {
        Long currentUserId = AuthUtils.getUserId(auth);

        List<PaymentTransaction> transactions =
                reportService.getFilteredTransactions(filters, currentUserId);

        byte[] csvBytes = reportService.exportToCsv(transactions);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"report.csv\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csvBytes);
    }
}