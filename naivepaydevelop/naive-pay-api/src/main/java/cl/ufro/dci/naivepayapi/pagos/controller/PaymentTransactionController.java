package cl.ufro.dci.naivepayapi.pagos.controller;

import cl.ufro.dci.naivepayapi.pagos.domain.PaymentTransaction;
import cl.ufro.dci.naivepayapi.pagos.dto.PendingTransactionDTO;
import cl.ufro.dci.naivepayapi.pagos.service.PaymentTransactionService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for managing payment transactions.
 * Provides endpoints for creating, retrieving, approving, and canceling payment transactions.
 */
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
     *
     * @param tx the payment transaction to create
     * @return the created payment transaction with generated ID and timestamps
     */
    @PostMapping
    public PaymentTransaction create(@RequestBody PaymentTransaction tx) {
        return service.createTransaction(tx);
    }

    /**
     * Retrieves all pending payment transactions.
     *
     * @return a list of pending transaction DTOs containing basic transaction information
     */
    @GetMapping("/pending")
    public List<PendingTransactionDTO> listPending() {
        return service.getPendingTransactions();
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
     * Processes the payment transfer and updates the transaction status accordingly.
     *
     * @param id the ID of the transaction to approve
     * @return the updated payment transaction with new status (APPROVED or REJECTED)
     */
    @PutMapping("/{id}/approve")
    public PaymentTransaction approve(@PathVariable Long id) {
        return service.approveTransaction(id);
    }

    /**
     * Cancels a pending payment transaction.
     * Updates the transaction status to CANCELED without processing the payment.
     *
     * @param id the ID of the transaction to cancel
     * @return the updated payment transaction with CANCELED status
     */
    @PutMapping("/{id}/cancel")
    public PaymentTransaction cancel(@PathVariable Long id) {
        return service.cancelTransaction(id);
    }
}