package cl.ufro.dci.naivepayapi.recompensas.service;

import cl.ufro.dci.naivepayapi.recompensas.domain.RewardAccount;
import cl.ufro.dci.naivepayapi.recompensas.dto.RewardAccountDTO;
import cl.ufro.dci.naivepayapi.recompensas.repository.RewardAccountRepository;
import cl.ufro.dci.naivepayapi.registro.domain.User;
import cl.ufro.dci.naivepayapi.registro.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;

@Service
public class RewardAccountService {

    private final RewardAccountRepository repository;
    private final UserRepository userRepository;

    public RewardAccountService(RewardAccountRepository repository, UserRepository userRepository) {
        this.repository = repository;
        this.userRepository = userRepository;
    }

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
    }

    public RewardAccountDTO getAccountDTOByUserId(Long userId) {
        RewardAccount account = repository.findByUser_UseId(userId)
                .orElseGet(() -> {
                    User user = findUserById(userId);
                    return repository.save(
                            new RewardAccount(user, 0, "Cuenta inicial", LocalDateTime.now())
                    );
                });

        return new RewardAccountDTO(
                account.getRewaccId(),
                account.getUser().getUseId(),
                account.getRewaccPoints(),
                account.getRewaccDescription(),
                account.getRewaccLastUpdate()
        );
    }

    public RewardAccount createIfNotExists(Long userId) {
        return repository.findByUser_UseId(userId)
                .orElseGet(() -> {
                    User user = findUserById(userId);
                    return repository.save(new RewardAccount(user, 0, "Cuenta inicial", LocalDateTime.now()));
                });
    }
}