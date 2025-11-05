package cl.ufro.dci.naivepayapi.fondos.repository;

import cl.ufro.dci.naivepayapi.fondos.domain.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for account management in the NaivePay system.
 * Provides data access methods for the Account entity.
 */
@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {
    
    /**
     * Finds an account by associated user ID.
     * Each user must have a unique account in the system.
     * 
     * @param userId User ID
     * @return Optional with account if exists, empty otherwise
     */
    Optional<Account> findByUserId(Long userId);
    
    /**
     * Checks if an account exists for a specific user.
     * 
     * @param userId User ID
     * @return true if exists, false otherwise
     */
    boolean existsByUserId(Long userId);
}
