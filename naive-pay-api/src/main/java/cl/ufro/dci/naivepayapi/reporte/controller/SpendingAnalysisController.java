package cl.ufro.dci.naivepayapi.reporte.controller;

import org.springframework.security.core.Authentication;
import cl.ufro.dci.naivepayapi.reporte.dto.ReportFilterDTO;
import cl.ufro.dci.naivepayapi.reporte.service.SpendingAnalysisService;
import org.springframework.web.bind.annotation.*;
import cl.ufro.dci.naivepayapi.reporte.util.AuthUtils;

/**
 * REST controller to build spending reports.
 *
 * Exposes a single endpoint under <code>/api/reports/spending</code> that returns
 * a consolidated report (total, per-key breakdown, and time series) for
 * the authenticated user.
 *
 * <h2>CORS</h2>
 * Allows requests from <code>http://localhost:4200</code> (Angular client).
 *
 * @since 1.0
 */
@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/api/reports/spending")
public class SpendingAnalysisController {

    /**
     * Domain service for spending analysis.
     */
    private final SpendingAnalysisService service;

    /**
     * Creates the spending analysis controller.
     *
     * @param service service that builds the report.
     */
    public SpendingAnalysisController(SpendingAnalysisService service) {
        this.service = service;
    }

    /**
     * Builds the authenticated user's spending report by applying the provided
     * filters and grouping/granularity parameters.
     *
     * <h3>Parameters</h3>
     * <ul>
     *   <li><b>filter</b>: date ranges, status, commerce, description, and amounts.</li>
     *   <li><b>groupBy</b> (query param): grouping key (default <code>COMMERCE</code>).
     *       Values supported by the service: <code>TRANSACTION_TYPE</code> (category) or other values &rarr; commerce.</li>
     *   <li><b>granularity</b> (query param): time granularity (default <code>DAY</code>).
     *       Supported values: <code>MONTH</code> or, otherwise, day.</li>
     * </ul>
     *
     * <h3>Security</h3>
     * <ul>
     *   <li>The <code>userId</code> is obtained from {@link Authentication#getName()} and parsed to {@link Long}.</li>
     *   <li>It is assumed that the principal is the user's numeric ID.</li>
     * </ul>
     *
     * <h3>Response</h3>
     * {@link SpendingAnalysisService.SpendingReport} with total spent, key-based map, and time series.
     *
     * <h3>Common errors</h3>
     * <ul>
     *   <li>{@link NumberFormatException} if <code>auth.getName()</code> cannot be converted to <code>Long</code>.</li>
     *   <li>401/403 errors if no valid principal exists.</li>
     * </ul>
     *
     * @param filter       report filters (JSON body).
     * @param groupBy      grouping key (query param), default <code>COMMERCE</code>.
     * @param granularity  time granularity (query param), default <code>DAY</code>.
     * @param auth         authentication context.
     * @return consolidated spending report.
     */
    @PostMapping
    public SpendingAnalysisService.SpendingReport build(
            @RequestBody ReportFilterDTO filter,
            @RequestParam(defaultValue = "COMMERCE") String groupBy,
            @RequestParam(defaultValue = "DAY") String granularity,
            Authentication auth) {

        Long currentUserId = AuthUtils.getUserId(auth);
        return service.build(currentUserId, filter, groupBy, granularity);
    }
}
