package cl.ufro.dci.naivepayapi.fondos.controller;

import cl.ufro.dci.naivepayapi.fondos.dto.AccountBalanceResponse;
import cl.ufro.dci.naivepayapi.fondos.service.AccountService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for account management and balance queries.
 * <p>
 * Defines HTTP endpoints for operations related to user accounts,
 * including creation, balance querying, and existence verification.
 * </p>
 * 
 * <p><b>Base URL:</b> {@code /api/funds/accounts}</p>
 * 
 * <p><b>Available endpoints:</b></p>
 * <ul>
 *   <li>GET /balance - Query authenticated user's account balance</li>
 *   <li>POST /create/{userId} - Create new account (internal use by Registration module)</li>
 *   <li>GET /exists/{userId} - Verify account existence (internal use)</li>
 * </ul>
 * 
 * <p><b>Security:</b> Most endpoints require JWT authentication. The userId is extracted
 * from the authentication token to ensure users can only access their own data.</p>
 * 
 * <p><b>CORS configuration:</b> Enabled for all origins (*).
 * In production it should be restricted to specific domains.</p>
 * 
 * @author NaivePay Development Team
 * @version 1.0
 * @since 2025-10-06
 * @see AccountService
 * @see AccountBalanceResponse
 */
@RestController
@RequestMapping("/api/funds/accounts")
@CrossOrigin(origins = "http://localhost:4200")
public class AccountController {

    private final AccountService accountService;

    /**
     * Constructor with dependency injection.
     * 
     * @param accountService the account management service
     */
    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    /**
     * Gets the balance of the authenticated user's account.
     * <p>
     * <b>Endpoint:</b> GET /api/funds/accounts/balance
     * </p>
     * 
     * <p><b>Security:</b> Requires JWT authentication. The userId is extracted from the token.</p>
     * 
     * <p><b>Example usage:</b></p>
     * <pre>
     * GET /api/funds/accounts/balance
     * Headers: Authorization: Bearer {jwt-token}
     * </pre>
     * 
     * <p><b>Successful response (200 OK):</b></p>
     * <pre>
     * {
     *   "accountId": 1,
     *   "userId": 123,
     *   "availableBalance": 5000.00,
     *   "lastUpdate": "2025-10-06T15:30:00"
     * }
     * </pre>
     * 
     * @param auth Spring Security authentication object containing the authenticated user's ID
     * @return ResponseEntity with the account balance (200 OK) or 404 NOT FOUND if account doesn't exist
     */
    @GetMapping("/balance")
    public ResponseEntity<AccountBalanceResponse> getAccountBalance(Authentication auth) {
        try {
            Long userId = Long.parseLong(auth.getName());
            AccountBalanceResponse response = accountService.getAccountBalance(userId);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    /**
     * Creates a new account for a user.
     * <p>
     * <b>Endpoint:</b> POST /api/funds/accounts/create/{userId}
     * </p>
     * 
     * <p><b>Example usage:</b></p>
     * <pre>
     * POST /api/funds/accounts/create/123
     * </pre>
     * 
     * <p><b>Successful response (201 CREATED):</b></p>
     * <pre>
     * {
     *   "accountId": 1,
     *   "userId": 123,
     *   "availableBalance": 0.00,
     *   "lastUpdate": "2025-10-06T15:30:00"
     * }
     * </pre>
     * 
     * <p><b>Integration:</b> This endpoint must be called by the
     * Registration module immediately after creating a new user.</p>
     * 
     * @param userId the ID of the user for whom the account is created
     * @return ResponseEntity with the created account (201 CREATED) or 409 CONFLICT if it already exists
     */
    @PostMapping("/create/{userId}")
    public ResponseEntity<AccountBalanceResponse> createAccount(@PathVariable Long userId) {
        try {
            AccountBalanceResponse response = accountService.createAccount(userId);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }

    /**
     * Verifies if an account exists for a user.
     * <p>
     * <b>Endpoint:</b> GET /api/funds/accounts/exists/{userId}
     * </p>
     * 
     * <p><b>Example usage:</b></p>
     * <pre>
     * GET /api/funds/accounts/exists/123
     * </pre>
     * 
     * <p><b>Response (200 OK):</b></p>
     * <pre>
     * true
     * </pre>
     * 
     * <p><b>Typical use:</b> Previous validation before operations that
     * require an existing account.</p>
     * 
     * @param userId the ID of the user to verify
     * @return ResponseEntity with true if exists, false otherwise (200 OK)
     */
    @GetMapping("/exists/{userId}")
    public ResponseEntity<Boolean> accountExists(@PathVariable Long userId) {
        boolean exists = accountService.accountExists(userId);
        return ResponseEntity.ok(exists);
    }
}
