package cl.ufro.dci.naivepayapi.pagos.controller;

import cl.ufro.dci.naivepayapi.reporte.util.AuthUtils;
import cl.ufro.dci.naivepayapi.pagos.domain.PaymentTransaction;
import cl.ufro.dci.naivepayapi.pagos.dto.PendingTransactionDTO;
import cl.ufro.dci.naivepayapi.pagos.service.PaymentTransactionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller for managing payment transactions.
 * Provides endpoints for creating, retrieving, approving, and canceling payment transactions.
 */
@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/api/payments")
public class PaymentTransactionController {

    private final PaymentTransactionService service;

    /**
     * Constructs a new PaymentTransactionController with the specified service.
     *
     * @param service the payment transaction service to handle business logic
     */
    public PaymentTransactionController(PaymentTransactionService service) {
        this.service = service;
    }

    /**
     * Creates a new payment transaction.
     * NOW uses the unified Funds module internally while maintaining API contract.
     *
     * @param tx the payment transaction to create
     * @return the created payment transaction with generated ID and timestamps
     * @deprecated The internal implementation now uses FundTransaction.
     *             Consider migrating to use /api/funds/transactions/payments/pending directly.
     */
    @PostMapping
    @Deprecated
    public PaymentTransaction create(@RequestBody PaymentTransaction tx) {
        return service.createTransaction(tx);
    }

    /**
     * Retrieves all pending payment transactions FOR THE AUTHENTICATED USER.
     * NOW queries from the unified Funds module.
     * 
     * **OWNERSHIP MODEL**: Returns only payments where the authenticated user
     * is the DESTINATION (commerce who must approve/reject).
     * 
     * @param auth Spring Security authentication with user ID
     * @return a list of pending transaction DTOs containing basic transaction information
     */
    @GetMapping("/pending")
    public List<PendingTransactionDTO> listPending(Authentication auth) {
        Long userId = AuthUtils.getUserId(auth);
        return service.getPendingTransactionsByUser(userId);
    }

    /**
     * Retrieves a specific payment transaction by its ID.
     *
     * @param id the ID of the transaction to retrieve
     * @return the payment transaction with the specified ID
     * @throws RuntimeException if no transaction is found with the given ID
     */
    @GetMapping("/{id}")
    public PaymentTransaction getById(@PathVariable Long id) {
        return service.getTransactionById(id);
    }

    /**
     * Approves a pending payment transaction.
     * NOW uses the unified approval workflow from the Funds module.
     * Returns transaction ID instead of full PaymentTransaction object.
     *
     * @param id the ID of the transaction to approve
     * @return response with the transaction ID
     */
    @PutMapping("/{id}/approve")
    public ResponseEntity<?> approve(@PathVariable Long id) {
        Long transactionId = service.approveTransaction(id);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "transactionId", transactionId,
                "message", "Payment approved successfully"
        ));
    }

    /**
     * Cancels a pending payment transaction.
     * NOW uses the unified cancellation workflow from the Funds module.
     * Returns transaction ID instead of full PaymentTransaction object.
     *
     * @param id the ID of the transaction to cancel
     * @return response with the transaction ID
     */
    @PutMapping("/{id}/cancel")
    public ResponseEntity<?> cancel(@PathVariable Long id) {
        Long transactionId = service.cancelTransaction(id);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "transactionId", transactionId,
                "message", "Payment canceled successfully"
        ));
    }
}