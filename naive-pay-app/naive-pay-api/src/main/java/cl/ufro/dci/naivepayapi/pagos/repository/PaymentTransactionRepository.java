package cl.ufro.dci.naivepayapi.pagos.repository;

import cl.ufro.dci.naivepayapi.pagos.domain.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository interface for PaymentTransaction entity.
 * Provides CRUD operations and data access methods for payment transactions.
 * Extends JpaRepository to inherit common database operations.
 */
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {

}