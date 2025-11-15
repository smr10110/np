package cl.ufro.dci.naivepayapi.recompensas.repository;

import cl.ufro.dci.naivepayapi.recompensas.domain.RewardTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RewardTransactionRepository extends JpaRepository<RewardTransaction, Long> {
    List<RewardTransaction> findByUser_UseIdOrderByRewtrnCreatedAtDesc(Long userId);
}