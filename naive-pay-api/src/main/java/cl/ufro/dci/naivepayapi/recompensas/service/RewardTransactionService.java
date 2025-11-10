package cl.ufro.dci.naivepayapi.recompensas.service;

import cl.ufro.dci.naivepayapi.recompensas.domain.*;
import cl.ufro.dci.naivepayapi.recompensas.dto.RewardTransactionDTO;
import cl.ufro.dci.naivepayapi.recompensas.repository.RewardAccountRepository;
import cl.ufro.dci.naivepayapi.recompensas.repository.RewardTransactionRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class RewardTransactionService {

    private final RewardAccountRepository accountRepository;
    private final RewardTransactionRepository transactionRepository;
    private final RewardAccountService accountService;

    public RewardTransactionService(RewardAccountRepository accountRepository,
                                    RewardTransactionRepository transactionRepository,
                                    RewardAccountService accountService) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.accountService = accountService;
    }

    public RewardTransactionDTO processTransaction(Long userId, int points, String description, RewardTransactionType type) {
        RewardAccount account = accountService.createIfNotExists(userId);

        RewardTransaction transaction = new RewardTransaction();
        transaction.setUserId(userId);
        transaction.setPoints(points);
        transaction.setDescription(description);
        transaction.setType(type);
        transaction.setStatus(RewardTransactionStatus.COMPLETED);
        transaction.setCreatedAt(LocalDateTime.now());

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
        RewardAccount account = accountRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Cuenta de recompensas no encontrada"));

        return transactionRepository.findByUserIdOrderByCreatedAtDesc(userId)
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