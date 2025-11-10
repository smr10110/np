package cl.ufro.dci.naivepayapi.recompensas.repository;

import cl.ufro.dci.naivepayapi.recompensas.domain.RewardAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RewardAccountRepository extends JpaRepository<RewardAccount, Long> {
    Optional<RewardAccount> findByUserId(Long userId);
    boolean existsByUserId(Long userId);
}