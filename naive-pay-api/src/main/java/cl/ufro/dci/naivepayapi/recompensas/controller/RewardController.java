package cl.ufro.dci.naivepayapi.recompensas.controller;

import cl.ufro.dci.naivepayapi.recompensas.domain.RewardTransactionType;
import cl.ufro.dci.naivepayapi.recompensas.dto.RewardAccountDTO;
import cl.ufro.dci.naivepayapi.recompensas.dto.RewardTransactionDTO;
import cl.ufro.dci.naivepayapi.recompensas.service.RewardAccountService;
import cl.ufro.dci.naivepayapi.recompensas.service.RewardTransactionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;

import java.util.List;

@RestController
@RequestMapping("/api/rewards")
@CrossOrigin(origins = "http://localhost:4200")
public class RewardController {
    private final RewardTransactionService transactionService;
    private final RewardAccountService accountService;

    public RewardController(RewardTransactionService transactionService, RewardAccountService accountService) {
        this.transactionService = transactionService;
        this.accountService = accountService;
    }

    // aux acumula puntos desde pagos
    @PostMapping("/accumulate")
    public ResponseEntity<RewardTransactionDTO> accumulate(
            @RequestParam Long userId,
            @RequestParam int points,
            @RequestParam(required = false, defaultValue = "Compra aprobada") String description) {
        return ResponseEntity.ok(
                transactionService.processTransaction(userId, points, description, RewardTransactionType.ACCUMULATE)
        );
    }

    //canjea puntos
    @PostMapping("/redeem")
    public ResponseEntity<RewardTransactionDTO> redeem(
            Authentication auth,
            @RequestParam int points,
            @RequestParam(required = false, defaultValue = "Canje manual") String description) {
        Long userId = Long.parseLong(auth.getName());
        return ResponseEntity.ok(transactionService.redeemPoints(userId, points, description));
    }

    //revisa movimientos
    @GetMapping("/history")
    public ResponseEntity<List<RewardTransactionDTO>> getUserHistory(Authentication auth) { // <-- Corregido
        Long userId = Long.parseLong(auth.getName());
        return ResponseEntity.ok(transactionService.getTransactionsByUser(userId));
    }

    @GetMapping("/history-all")
    public ResponseEntity<List<RewardTransactionDTO>> getAllHistory() {
        return ResponseEntity.ok(transactionService.getAllTransactions());
    }

    //administrar reglas
    @PutMapping("/config")
    public ResponseEntity<String> updateRewardRules(
            @RequestParam double multiplier,
            @RequestParam int monthsExpiration) {
        String msg = "Configuración actualizada: multiplicador=" + multiplier +
                ", vencimiento=" + monthsExpiration + " meses";
        return ResponseEntity.ok(msg);
    }

    // consultar promos
    @GetMapping("/promotions")
    public ResponseEntity<List<String>> getPromotions() {
        return ResponseEntity.ok(List.of(
                "Doble puntos en compras de electrónica",
                "Puntos x3 en supermercados este mes",
                "Descuento de $1000 por cada 1000 puntos canjeados"
        ));
    }

    //consultar puntos
    @GetMapping("/account")
    public ResponseEntity<RewardAccountDTO> getAccount(Authentication auth) {
        Long userId = Long.parseLong(auth.getName());
        return ResponseEntity.ok(accountService.getAccountDTOByUserId(userId));
    }
}