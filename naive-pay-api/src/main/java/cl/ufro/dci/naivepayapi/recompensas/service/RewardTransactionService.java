package cl.ufro.dci.naivepayapi.recompensas.service;

import cl.ufro.dci.naivepayapi.recompensas.domain.*;
import cl.ufro.dci.naivepayapi.recompensas.dto.RewardTransactionDTO;
import cl.ufro.dci.naivepayapi.recompensas.repository.RewardAccountRepository;
import cl.ufro.dci.naivepayapi.recompensas.repository.RewardTransactionRepository;
import cl.ufro.dci.naivepayapi.registro.domain.User;
import cl.ufro.dci.naivepayapi.registro.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class RewardTransactionService {

    private final RewardAccountRepository accountRepository;
    private final RewardTransactionRepository transactionRepository;
    private final RewardAccountService accountService;
    private final UserRepository userRepository;

    public RewardTransactionService(RewardAccountRepository accountRepository,
                                    RewardTransactionRepository transactionRepository,
                                    RewardAccountService accountService,
                                    UserRepository userRepository) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.accountService = accountService;
        this.userRepository = userRepository;
    }

    // metodo para buscar el objeto User
    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
    }

    public RewardTransactionDTO processTransaction(Long userId, int points, String description, RewardTransactionType type) {
        RewardAccount account = accountService.createIfNotExists(userId);
        User user = findUserById(userId);

        RewardTransaction transaction = new RewardTransaction();
        transaction.setUser(user);
        transaction.setRewtrnPoints(points);
        transaction.setRewtrnDescription(description);
        transaction.setRewtrnType(type);
        transaction.setRewtrnStatus(RewardTransactionStatus.COMPLETED);
        //transaction.setRewtrnCreatedAt(LocalDateTime.now());

        if (type == RewardTransactionType.ACCUMULATE) {
            account.addPoints(points);
        } else if (type == RewardTransactionType.REDEEM) {
            account.redeemPoints(points);
        }

        accountRepository.save(account);
        transactionRepository.save(transaction);

        return new RewardTransactionDTO(transaction);
    }

    public RewardTransactionDTO redeemPoints(Long userId, int points, String description) {
        return processTransaction(userId, points, description, RewardTransactionType.REDEEM);
    }

    public RewardTransactionDTO accumulatePoints(Long userId, int points, String description) {
        return processTransaction(userId, points, description, RewardTransactionType.ACCUMULATE);
    }

    public List<RewardTransactionDTO> getTransactionsByUser(Long userId) {
        return transactionRepository.findByUser_UseIdOrderByRewtrnCreatedAtDesc(userId)
                .stream()
                .map(RewardTransactionDTO::new)
                .collect(Collectors.toList());
    }

    public List<RewardTransactionDTO> getAllTransactions() {
        return transactionRepository.findAll()
                .stream()
                .map(RewardTransactionDTO::new)
                .collect(Collectors.toList());
    }
}