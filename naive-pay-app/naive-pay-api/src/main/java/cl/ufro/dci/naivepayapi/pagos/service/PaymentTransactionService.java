package cl.ufro.dci.naivepayapi.pagos.service;

import cl.ufro.dci.naivepayapi.fondos.dto.TransferRequest;
import cl.ufro.dci.naivepayapi.fondos.dto.TransferResponse;
import cl.ufro.dci.naivepayapi.fondos.service.TransactionService;
import cl.ufro.dci.naivepayapi.pagos.domain.PaymentTransaction;
import cl.ufro.dci.naivepayapi.pagos.domain.PaymentTransactionStatus;
import cl.ufro.dci.naivepayapi.pagos.dto.PendingTransactionDTO;
import cl.ufro.dci.naivepayapi.pagos.repository.PaymentTransactionRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service layer for payment transaction operations.
 * Handles business logic and coordinates with funds module for transfers.
 */
@Service
public class PaymentTransactionService {

    private final PaymentTransactionRepository repository;
    private final TransactionService transactionService;

    public PaymentTransactionService(PaymentTransactionRepository repository, TransactionService transactionService) {
        this.repository = repository;
        this.transactionService = transactionService;
    }

    /**
     * Creates a new payment transaction.
     */
    public PaymentTransaction createTransaction(PaymentTransaction tx) {
        return repository.save(tx);
    }

    /**
     * Retrieves all pending transactions as DTOs.
     */
    public List<PendingTransactionDTO> getPendingTransactions() {
        return repository.findAll().stream()
                .filter(tx -> tx.getStatus() == PaymentTransactionStatus.PENDING)
                .map(tx -> new PendingTransactionDTO(
                        tx.getId(),
                        tx.getAmount(),
                        tx.getCommerce()
                ))
                .collect(Collectors.toList());
    }

    /**
     * Retrieves a transaction by ID.
     * @throws RuntimeException if transaction not found
     */
    public PaymentTransaction getTransactionById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));
    }

    /**
     * Approves a transaction and processes the funds transfer.
     * Updates status based on transfer result.
     */
    public PaymentTransaction approveTransaction(Long id) {
        PaymentTransaction tx = getTransactionById(id);

        TransferRequest request = new TransferRequest(
                tx.getOriginAccount(),
                tx.getDestinationAccount(),
                tx.getAmount(),
                "Payment at commerce " + tx.getCommerce()
        );
        TransferResponse result = transactionService.transfer(request);

        if (result.isSuccess()) {
            tx.setStatus(PaymentTransactionStatus.APPROVED);
        } else {
            tx.setStatus(PaymentTransactionStatus.REJECTED);
        }

        return repository.save(tx);
    }

    /**
     * Cancels a pending transaction.
     */
    public PaymentTransaction cancelTransaction(Long id) {
        PaymentTransaction tx = getTransactionById(id);
        tx.setStatus(PaymentTransactionStatus.CANCELED);
        return repository.save(tx);
    }
}
