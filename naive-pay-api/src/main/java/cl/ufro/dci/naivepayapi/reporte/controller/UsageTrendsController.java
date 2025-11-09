package cl.ufro.dci.naivepayapi.reporte.controller;

import org.springframework.security.core.Authentication;
import cl.ufro.dci.naivepayapi.reporte.dto.ReportFilterDTO;
import cl.ufro.dci.naivepayapi.reporte.service.UsageTrends;
import org.springframework.web.bind.annotation.*;
import cl.ufro.dci.naivepayapi.reporte.util.AuthUtils;

/**
 * REST controller for usage/spending metrics.
 *
 * Exposes endpoints under <code>/api/reports/usage</code> to query trends,
 * such as the authenticated user's monthly average spending.
 *
 * <h2>CORS</h2>
 * Enables origins from <code>http://localhost:4200</code> (Angular).
 *
 * @since 1.0
 */
@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/api/reports/usage")
public class UsageTrendsController {

    /**
     * Domain service for usage trend calculations.
     */
    private final UsageTrends service;

    /**
     * Creates the usage trends controller.
     *
     * @param service service that performs average spending calculations.
     */
    public UsageTrendsController(UsageTrends service) {
        this.service = service;
    }

    /**
     * Calculates the authenticated user's monthly average spending,
     * considering the filters provided in the request body.
     *
     * <h3>Security</h3>
     * <ul>
     *   <li>The <code>userId</code> is obtained from {@link Authentication#getName()} and parsed to {@link Long}.</li>
     *   <li>It is assumed that <code>auth.getName()</code> contains the user's numeric identifier.</li>
     * </ul>
     *
     * <h3>Input</h3>
     * {@link ReportFilterDTO} with date ranges, status, commerce, description, and amounts.
     *
     * <h3>Output</h3>
     * A {@code double} with the monthly average (two decimals in the service's internal calculation).
     *
     * <h3>Possible errors</h3>
     * <ul>
     *   <li>{@link NumberFormatException} if the principal name cannot be converted to {@code Long}.</li>
     *   <li>Authentication/authorization errors if no valid principal exists.</li>
     * </ul>
     *
     * @param filter report filters sent in the body (JSON).
     * @param auth   current authentication context.
     * @return monthly average spending for the authenticated user.
     */
    @PostMapping("/avg-monthly")
    public double averageMonthly(@RequestBody ReportFilterDTO filter,
                                 Authentication auth) {

        Long currentUserId = AuthUtils.getUserId(auth);
        return service.averageMonthlySpent(currentUserId, filter);
    }
}
