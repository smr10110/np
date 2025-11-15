package cl.ufro.dci.naivepayapi.fondos.repository;

import cl.ufro.dci.naivepayapi.fondos.domain.Account;
import cl.ufro.dci.naivepayapi.registro.domain.User;
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
     * @param useId User ID (foreign key from app_user table)
     * @return Optional with account if exists, empty otherwise
     */
    Optional<Account> findByUserUseId(Long useId);
    
    /**
     * Finds an account by User entity.
     * 
     * @param user User entity
     * @return Optional with account if exists, empty otherwise
     */
    Optional<Account> findByUser(User user);
    
    /**
     * Checks if an account exists for a specific user.
     * 
     * @param useId User ID (foreign key from app_user table)
     * @return true if exists, false otherwise
     */
    boolean existsByUserUseId(Long useId);
}
