package cl.ufro.dci.naivepayapi.fondos.controller;

import cl.ufro.dci.naivepayapi.fondos.dto.TransactionResponse;
import cl.ufro.dci.naivepayapi.fondos.dto.TransferRequest;
import cl.ufro.dci.naivepayapi.fondos.dto.TransferResponse;
import cl.ufro.dci.naivepayapi.fondos.dto.PendingPaymentRequest;
import cl.ufro.dci.naivepayapi.fondos.dto.PendingPaymentResponse;
import cl.ufro.dci.naivepayapi.fondos.service.TransactionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * REST controller for transaction and transfer management.
 * <p>
 * Defines HTTP endpoints for all operations related to
 * fund movements, including transfers, balance loading,
 * history queries, and validations.
 * </p>
 * 
 * <p><b>Base URL:</b> {@code /api/funds/transactions}</p>
 * 
 * <p><b>Available endpoints:</b></p>
 * <ul>
 *   <li>POST /transfer - Execute transfer between accounts (public for Payments module)</li>
 *   <li>POST /add-funds - Load balance to authenticated user's account</li>
 *   <li>GET /history - Get authenticated user's transaction history</li>
 *   <li>GET /validate-balance/{accountId} - Validate sufficient balance (internal use)</li>
 *   <li>GET /{transactionId} - Query specific transaction</li>
 * </ul>
 * 
 * <p><b>Security:</b> Most endpoints require JWT authentication. The userId is extracted
 * from the authentication token to ensure users can only access their own data.</p>
 * 
 * <p><b>Integration with other modules:</b></p>
 * <ul>
 *   <li><b>Payments Module:</b> Uses /transfer to execute payments</li>
 *   <li><b>Reports Module:</b> Uses /history to generate reports</li>
 * </ul>
 * 
 * <p><b>CORS configuration:</b> Enabled for all origins (*).
 * In production it should be restricted to specific domains.</p>
 * 
 * @author NaivePay Development Team
 * @version 1.0
 * @since 2025-10-06
 * @see TransactionService
 * @see TransferRequest
 * @see TransferResponse
 * @see TransactionResponse
 */
@RestController
@RequestMapping("/api/funds/transactions")
@CrossOrigin(origins = "http://localhost:4200")
public class TransactionController {

    private final TransactionService transactionService;

    /**
     * Constructor with dependency injection.
     * 
     * @param transactionService the transaction management service
     */
    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    /**
     * Perform transfer between accounts.
     * Route: POST /api/funds/transactions/transfer
     * Used by Payment module to execute transfers.
     * 
     * @param request Transfer data (origin, destination, amount)
     * @return Transfer result (success or error)
     */
    @PostMapping("/transfer")
    public ResponseEntity<TransferResponse> transfer(@RequestBody TransferRequest request) {
        TransferResponse response = transactionService.transfer(request);
        
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Add funds to the authenticated user's account (from system account).
     * <p>
     * <b>Endpoint:</b> POST /api/funds/transactions/add-funds
     * </p>
     * 
     * <p><b>Security:</b> Requires JWT authentication. The userId is extracted from the token.</p>
     * 
     * <p><b>Example usage:</b></p>
     * <pre>
     * POST /api/funds/transactions/add-funds?amount=100.00
     * Headers: Authorization: Bearer {jwt-token}
     * </pre>
     * 
     * @param auth Spring Security authentication object containing the authenticated user's ID
     * @param amount Amount to add (must be greater than 0)
     * @return Result of fund addition with success status and message
     */
    @PostMapping("/add-funds")
    public ResponseEntity<TransferResponse> addFunds(
            Authentication auth,
            @RequestParam BigDecimal amount) {
        Long userId = Long.parseLong(auth.getName());
        TransferResponse response = transactionService.addFunds(userId, amount);
        
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Get the authenticated user's transaction history.
     * <p>
     * <b>Endpoint:</b> GET /api/funds/transactions/history
     * </p>
     * 
     * <p><b>Security:</b> Requires JWT authentication. The userId is extracted from the token.</p>
     * 
     * <p><b>Example usage:</b></p>
     * <pre>
     * GET /api/funds/transactions/history
     * Headers: Authorization: Bearer {jwt-token}
     * </pre>
     * 
     * @param auth Spring Security authentication object containing the authenticated user's ID
     * @return List of user transactions ordered by date (most recent first)
     */
    @GetMapping("/history")
    public ResponseEntity<List<TransactionResponse>> getTransactionHistory(Authentication auth) {
        try {
            Long userId = Long.parseLong(auth.getName());
            List<TransactionResponse> transactions = transactionService.getTransactionHistory(userId);
            return ResponseEntity.ok(transactions);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    /**
     * Validate if an account has sufficient balance for a transfer.
     * Route: GET /api/funds/transactions/validate-balance/{accountId}
     * Used by other modules to verify balance before creating a payment request.
     * 
     * @param accountId Account ID
     * @param amount Amount to validate
     * @return true if sufficient balance, false otherwise
     */
    @GetMapping("/validate-balance/{accountId}")
    public ResponseEntity<Boolean> validateBalance(
            @PathVariable Long accountId,
            @RequestParam BigDecimal amount) {
        boolean hasBalance = transactionService.validateBalance(accountId, amount);
        return ResponseEntity.ok(hasBalance);
    }

    /**
     * Get specific transaction details.
     * Route: GET /api/funds/transactions/{transactionId}
     * 
     * @param transactionId Transaction ID
     * @return Transaction details
     */
    @GetMapping("/{transactionId}")
    public ResponseEntity<TransactionResponse> getTransaction(@PathVariable Long transactionId) {
        try {
            TransactionResponse response = transactionService.getTransactionById(transactionId);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    /**
     * Create a payment transaction pending approval.
     * <p>
     * <b>Endpoint:</b> POST /api/funds/transactions/payments/pending
     * </p>
     * 
     * <p>Creates a transaction that requires manual approval before
     * executing the actual fund transfer. Useful for fraud prevention
     * and payment validation workflows.</p>
     * 
     * <p><b>Request body example:</b></p>
     * <pre>
     * {
     *   "originAccountId": 1,
     *   "destinationAccountId": 2,
     *   "amount": 100.00,
     *   "description": "Payment for groceries",
     *   "customerName": "John Doe",
     *   "commerceName": "SuperMarket XYZ",
     *   "category": "Food"
     * }
     * </pre>
     * 
     * @param request Pending payment data
     * @return Response with transaction ID if successful
     * @since 2.0
     */
    @PostMapping("/payments/pending")
    public ResponseEntity<TransferResponse> createPendingPayment(
            @RequestBody PendingPaymentRequest request) {
        TransferResponse response = transactionService.createPendingPayment(
                request.getOriginAccountId(),
                request.getDestinationAccountId(),
                request.getAmount(),
                request.getDescription(),
                request.getCustomerName(),
                request.getCommerceName(),
                request.getCategory()
        );

        if (response.isSuccess()) {
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Get all pending payment transactions.
     * <p>
     * <b>Endpoint:</b> GET /api/funds/transactions/payments/pending
     * </p>
     * 
     * <p>Returns all transactions with PENDING status awaiting approval.
     * Useful for admin dashboards and approval workflows.</p>
     * 
     * @return List of pending payments with extended information
     * @since 2.0
     */
    @GetMapping("/payments/pending")
    public ResponseEntity<List<PendingPaymentResponse>> getPendingPayments() {
        List<PendingPaymentResponse> pending = transactionService.getPendingPayments();
        return ResponseEntity.ok(pending);
    }

    /**
     * Get pending payments for a specific account.
     * <p>
     * <b>Endpoint:</b> GET /api/funds/transactions/payments/pending/account/{accountId}
     * </p>
     * 
     * <p>Returns pending payments from a specific origin account.
     * Useful for showing users their outgoing pending payments.</p>
     * 
     * @param accountId ID of the origin account
     * @return List of pending payments from that account
     * @since 2.0
     */
    @GetMapping("/payments/pending/account/{accountId}")
    public ResponseEntity<List<PendingPaymentResponse>> getPendingPaymentsByAccount(
            @PathVariable Long accountId) {
        try {
            List<PendingPaymentResponse> pending = transactionService.getPendingPaymentsByAccount(accountId);
            return ResponseEntity.ok(pending);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    /**
     * Approve a pending payment.
     * <p>
     * <b>Endpoint:</b> PUT /api/funds/transactions/payments/{id}/approve
     * </p>
     * 
     * <p>Approves a pending payment and executes the fund transfer.
     * Validates sufficient balance before execution. If insufficient,
     * marks the payment as rejected.</p>
     * 
     * @param id Transaction ID to approve
     * @return Response with success status and message
     * @since 2.0
     */
    @PutMapping("/payments/{id}/approve")
    public ResponseEntity<TransferResponse> approvePayment(@PathVariable Long id) {
        TransferResponse response = transactionService.approvePendingPayment(id);

        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Cancel a pending payment.
     * <p>
     * <b>Endpoint:</b> PUT /api/funds/transactions/payments/{id}/cancel
     * </p>
     * 
     * <p>Cancels a pending payment before approval. No fund transfer occurs.
     * The transaction remains in history for audit purposes.</p>
     * 
     * @param id Transaction ID to cancel
     * @return Response with success status and message
     * @since 2.0
     */
    @PutMapping("/payments/{id}/cancel")
    public ResponseEntity<TransferResponse> cancelPayment(@PathVariable Long id) {
        TransferResponse response = transactionService.cancelPendingPayment(id);

        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Reject a pending payment.
     * <p>
     * <b>Endpoint:</b> PUT /api/funds/transactions/payments/{id}/reject
     * </p>
     * 
     * <p>Rejects a pending payment (administrative action). Used for
     * fraud detection, policy violations, etc. Includes a reason for audit.</p>
     * 
     * @param id Transaction ID to reject
     * @param reason Rejection reason (optional)
     * @return Response with success status and message
     * @since 2.0
     */
    @PutMapping("/payments/{id}/reject")
    public ResponseEntity<TransferResponse> rejectPayment(
            @PathVariable Long id,
            @RequestParam(required = false) String reason) {
        TransferResponse response = transactionService.rejectPendingPayment(id, reason);

        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Get extended transaction history including all states.
     * <p>
     * <b>Endpoint:</b> GET /api/funds/transactions/history/extended
     * </p>
     * 
     * <p>Returns complete transaction history including pending, approved,
     * rejected, and canceled transactions with extended information.</p>
     * 
     * @param auth Spring Security authentication object
     * @return List of all transactions with extended fields
     * @since 2.0
     */
    @GetMapping("/history/extended")
    public ResponseEntity<List<TransactionResponse>> getExtendedTransactionHistory(Authentication auth) {
        try {
            Long userId = Long.parseLong(auth.getName());
            List<TransactionResponse> transactions = transactionService.getExtendedTransactionHistory(userId);
            return ResponseEntity.ok(transactions);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }
}
