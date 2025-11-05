package cl.ufro.dci.naivepayapi.recompensas.service;

import cl.ufro.dci.naivepayapi.recompensas.domain.RewardAccount;
import cl.ufro.dci.naivepayapi.recompensas.dto.RewardAccountDTO;
import cl.ufro.dci.naivepayapi.recompensas.repository.RewardAccountRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class RewardAccountService {

    private final RewardAccountRepository repository;

    public RewardAccountService(RewardAccountRepository repository) {
        this.repository = repository;
    }

    public RewardAccountDTO getAccountDTOByUserId(Long userId) {
        RewardAccount account = repository.findByUserId(userId)
                .orElseGet(() -> repository.save(
                        new RewardAccount(userId, 0, "Cuenta inicial", LocalDateTime.now())
                ));

        return new RewardAccountDTO(
                account.getId(),
                account.getUserId(),
                account.getPoints(),
                account.getDescription(),
                account.getLastUpdate()
        );
    }

    public RewardAccount createIfNotExists(Long userId) {
        return repository.findByUserId(userId)
                .orElseGet(() -> repository.save(new RewardAccount(userId, 0, "Cuenta inicial", LocalDateTime.now())));
    }
}