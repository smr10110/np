package cl.ufro.dci.naivepayapi.pagos.repository;

import cl.ufro.dci.naivepayapi.pagos.domain.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository interface for PaymentTransaction entity.
 * Provides CRUD operations and data access methods for payment transactions.
 * Extends JpaRepository to inherit common database operations.
 * 
 * @deprecated This repository accesses the legacy payment_transaction table.
 *             All payment transaction data is now managed through the unified system
 *             in the Funds module using {@code FundTransactionRepository}.
 *             This repository is kept for backward compatibility during migration
 *             and will be removed in a future version.
 *             
 *             <p><b>Migration path:</b></p>
 *             <ul>
 *               <li>Replace with {@code FundTransactionRepository} from the Funds module</li>
 *               <li>Use {@code TransactionService} methods instead of direct repository access</li>
 *               <li>Query methods should use {@code FundTransaction} entity</li>
 *             </ul>
 * 
 * @see cl.ufro.dci.naivepayapi.fondos.repository.FundTransactionRepository
 * @see cl.ufro.dci.naivepayapi.fondos.domain.FundTransaction
 */
@Deprecated
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {

}