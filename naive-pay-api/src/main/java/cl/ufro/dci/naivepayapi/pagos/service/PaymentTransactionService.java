package cl.ufro.dci.naivepayapi.pagos.service;

import cl.ufro.dci.naivepayapi.fondos.domain.Account;
import cl.ufro.dci.naivepayapi.fondos.domain.Transaction;
import cl.ufro.dci.naivepayapi.fondos.dto.PendingPaymentResponse;
import cl.ufro.dci.naivepayapi.fondos.dto.TransferResponse;
import cl.ufro.dci.naivepayapi.fondos.repository.AccountRepository;
import cl.ufro.dci.naivepayapi.fondos.repository.TransactionRepository;
import cl.ufro.dci.naivepayapi.fondos.service.TransactionService;
import cl.ufro.dci.naivepayapi.pagos.domain.PaymentTransaction;
import cl.ufro.dci.naivepayapi.pagos.domain.PaymentTransactionStatus;
import cl.ufro.dci.naivepayapi.pagos.dto.PendingTransactionDTO;
import cl.ufro.dci.naivepayapi.pagos.repository.PaymentTransactionRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
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
    private final TransactionRepository fundTransactionRepository;
    private final AccountRepository accountRepository;

    public PaymentTransactionService(
            PaymentTransactionRepository repository, 
            TransactionService transactionService,
            TransactionRepository fundTransactionRepository,
            AccountRepository accountRepository) {
        this.repository = repository;
        this.transactionService = transactionService;
        this.fundTransactionRepository = fundTransactionRepository;
        this.accountRepository = accountRepository;
    }

    /**
     * Creates a new payment transaction.
     * 
     * @deprecated This method saves to the legacy payment_transaction table.
     *             Use {@link #createPendingPaymentRequest(Long, Long, BigDecimal, String, String, String)} instead,
     *             which uses the unified Transaction table from the Funds module.
     */
    @Deprecated
    public PaymentTransaction createTransaction(PaymentTransaction tx) {
        // Legacy implementation - delegates to new unified system
        TransferResponse response = transactionService.createPendingPayment(
                tx.getOriginAccount(),
                tx.getDestinationAccount(),
                tx.getAmount(),
                "Payment to " + tx.getCommerce(),
                tx.getCustomer(),
                tx.getCommerce(),
                tx.getCategory()
        );
        
        if (response.isSuccess()) {
            // Map response to PaymentTransaction for backward compatibility
            tx.setId(response.getTransactionId());
            tx.setStatus(PaymentTransactionStatus.PENDING);
            return tx;
        } else {
            throw new RuntimeException("Error creating payment: " + response.getMessage());
        }
    }

    /**
     * Creates a pending payment request using the unified Funds module.
     * This is the recommended method for creating new payment transactions.
     * 
     * @param originAccountId ID of the account from which payment will be made
     * @param destinationAccountId ID of the commerce account receiving the payment
     * @param amount Payment amount
     * @param customer Name of the customer making the payment
     * @param commerce Name of the commerce receiving the payment
     * @param category Payment category (e.g., "Food", "Transport")
     * @return ID of the created transaction
     * @throws RuntimeException if payment creation fails
     */
    public Long createPendingPaymentRequest(Long originAccountId, Long destinationAccountId,
                                           BigDecimal amount, String customer, 
                                           String commerce, String category) {
        TransferResponse response = transactionService.createPendingPayment(
                originAccountId,
                destinationAccountId,
                amount,
                "Payment to " + commerce,
                customer,
                commerce,
                category
        );
        
        if (response.isSuccess()) {
            return response.getTransactionId();
        } else {
            throw new RuntimeException("Error creating payment: " + response.getMessage());
        }
    }

    /**
     * Retrieves pending transactions FOR A SPECIFIC USER (where user is destination/owner).
     * NOW queries from the unified Funds module with proper ownership filtering.
     * 
     * **OWNERSHIP MODEL**: Returns only payments where the specified user is the DESTINATION,
     * meaning they are the commerce/receiver who must approve or reject the payment.
     * 
     * @param userId ID of the user (must be destination of the payments)
     * @return List of pending transactions that THIS user can approve/reject
     */
    public List<PendingTransactionDTO> getPendingTransactionsByUser(Long userId) {
        // Get user's account
        Account account = accountRepository.findByUserUseId(userId)
                .orElseThrow(() -> new RuntimeException("User " + userId + " has no account"));
        
        // Get pending payments WHERE user is DESTINATION (ownership)
        List<PendingPaymentResponse> pending = transactionService
                .getPendingPaymentsByDestinationAccount(account.getAccId());
        
        // Map to legacy DTO for backward compatibility with frontend
        return pending.stream()
                .map(p -> new PendingTransactionDTO(
                        p.getId(),
                        p.getAmount(),
                        p.getCommerceName()
                ))
                .collect(Collectors.toList());
    }

    /**
     * Retrieves all pending transactions as DTOs.
     * NOW queries from the unified Funds module instead of the legacy payment_transaction table.
     * 
     * @deprecated Use {@link #getPendingTransactionsByUser(Long)} with userId for proper ownership filtering
     */
    @Deprecated
    public List<PendingTransactionDTO> getPendingTransactions() {
        // Get pending payments from the Funds module
        List<PendingPaymentResponse> pending = transactionService.getPendingPayments();
        
        // Map to legacy DTO for backward compatibility with frontend
        return pending.stream()
                .map(p -> new PendingTransactionDTO(
                        p.getId(),
                        p.getAmount(),
                        p.getCommerceName()
                ))
                .collect(Collectors.toList());
    }

    /**
     * Retrieves a transaction by ID.
     * NOW queries the unified transactions table and converts to PaymentTransaction for API compatibility.
     * 
     * @deprecated This method returns PaymentTransaction for backward compatibility.
     *             The underlying data comes from the unified Transaction table.
     * @throws RuntimeException if transaction not found
     */
    @Deprecated
    public PaymentTransaction getTransactionById(Long id) {
        // Query from unified table
        Transaction fundTransaction = fundTransactionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));
        
        // Convert to PaymentTransaction for API compatibility
        PaymentTransaction pt = new PaymentTransaction();
        pt.setId(fundTransaction.getTraId());
        pt.setOriginAccount(fundTransaction.getAccIdOrigin().getUser().getUseId());
        pt.setDestinationAccount(fundTransaction.getAccIdDestination().getUser().getUseId());
        pt.setAmount(fundTransaction.getTraAmount());
        pt.setCreatedAt(fundTransaction.getTraDateTime());
        pt.setCustomer(fundTransaction.getTraCustomerName() != null ? fundTransaction.getTraCustomerName() : "N/A");
        pt.setCommerce(fundTransaction.getTraCommerceName() != null ? fundTransaction.getTraCommerceName() : "N/A");
        pt.setCategory(fundTransaction.getTraPaymentCategory() != null ? fundTransaction.getTraPaymentCategory() : "N/A");
        
        // Map TransactionStatus to PaymentTransactionStatus
        switch (fundTransaction.getTraStatus()) {
            case COMPLETED -> pt.setStatus(PaymentTransactionStatus.APPROVED);
            case PENDING -> pt.setStatus(PaymentTransactionStatus.PENDING);
            case CANCELED -> pt.setStatus(PaymentTransactionStatus.REJECTED);
            default -> pt.setStatus(PaymentTransactionStatus.PENDING);
        }
        
        return pt;
    }

    /**
     * Approves a transaction and processes the funds transfer.
     * NOW uses the unified approval workflow from the Funds module.
     * 
     * @param id Transaction ID to approve
     * @return Transaction ID of the approved payment
     * @throws RuntimeException if approval fails
     */
    public Long approveTransaction(Long id) {
        // Use the unified approval workflow from Funds module
        TransferResponse response = transactionService.approvePendingPayment(id);
        
        if (response.isSuccess()) {
            return response.getTransactionId();
        } else {
            throw new RuntimeException("Error approving payment: " + response.getMessage());
        }
    }

    /**
     * Cancels a pending transaction.
     * NOW uses the unified cancellation workflow from the Funds module.
     * 
     * @param id Transaction ID to cancel
     * @return Transaction ID of the canceled payment
     * @throws RuntimeException if cancellation fails
     */
    public Long cancelTransaction(Long id) {
        TransferResponse response = transactionService.cancelPendingPayment(id);
        
        if (response.isSuccess()) {
            return response.getTransactionId();
        } else {
            throw new RuntimeException("Error canceling payment: " + response.getMessage());
        }
    }
}
