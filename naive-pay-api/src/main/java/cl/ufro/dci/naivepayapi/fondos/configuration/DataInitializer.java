package cl.ufro.dci.naivepayapi.fondos.configuration;

import cl.ufro.dci.naivepayapi.fondos.domain.Account;
import cl.ufro.dci.naivepayapi.fondos.repository.AccountRepository;
import cl.ufro.dci.naivepayapi.fondos.service.AccountService;
import cl.ufro.dci.naivepayapi.pagos.domain.PaymentTransaction;
import cl.ufro.dci.naivepayapi.pagos.domain.PaymentTransactionStatus;
import cl.ufro.dci.naivepayapi.pagos.repository.PaymentTransactionRepository;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Initializes test data in the database on application startup.
 * Only for development/testing purposes.
 */
@Configuration
public class DataInitializer {

    @Bean
    public CommandLineRunner initData(AccountRepository accountRepository, PaymentTransactionRepository paymentTransactionRepository) {
        return args -> {

            System.out.println("Inicializando datos de prueba en la tabla de pagos...");

            paymentTransactionRepository.deleteAll(); // Limpiar la tabla antes de insertar

            long userAId = 1L;
            long userBId = 2L;

            List<PaymentTransaction> transactions = List.of(
                    // 1. Pagos salientes del Usuario 1 (APPROVED)
                    createTx(userAId, userBId, "Juan Pérez", "MercadoX",
                            BigDecimal.valueOf(50.00), "Grocery shopping",
                            PaymentTransactionStatus.APPROVED, LocalDateTime.now().minusDays(5)),

                    createTx(userAId, userBId, "Juan Pérez", "Servicios",
                            BigDecimal.valueOf(200.00), "Monthly utility",
                            PaymentTransactionStatus.APPROVED, LocalDateTime.now().minusDays(35)),

                    createTx(userAId, userBId, "Juan Pérez", "Supermercado",
                            BigDecimal.valueOf(5.99), "Quick stop",
                            PaymentTransactionStatus.APPROVED, LocalDateTime.now()),

                    // 2. Pago entrante al Usuario 1 (PENDING)
                    createTx(userBId, userAId, "María González", "ComercioZ",
                            BigDecimal.valueOf(12.50), "Refund",
                            PaymentTransactionStatus.PENDING, LocalDateTime.now().minusHours(10)),

                    // 3. Transacción para el Usuario 2 (Saliente)
                    createTx(userBId, userAId, "María González", "TiendaB",
                            BigDecimal.valueOf(88.00), "Online purchase",
                            PaymentTransactionStatus.APPROVED, LocalDateTime.now().minusDays(1))
            );

            paymentTransactionRepository.saveAll(transactions);

            System.out.println("DataInitializer: Insertados " + transactions.size() + " registros en la tabla de pagos.");


            // Create system account (userId = 0) with high balance for loading funds
            if (!accountRepository.existsByUserId(AccountService.SYSTEM_ACCOUNT_USER_ID)) {
                Account systemAccount = new Account(AccountService.SYSTEM_ACCOUNT_USER_ID);
                systemAccount.updateBalance(new BigDecimal("999999.00"));
                accountRepository.save(systemAccount);
                System.out.println("System account created (userId=0) with balance: 999999.00");
            }

            System.out.println("Database fondos initialized");
        };
    }

    private PaymentTransaction createTx(Long origin, Long destination, String customer, String commerce,
                                        BigDecimal amount, String category, PaymentTransactionStatus status,
                                        LocalDateTime createdAt) {
        PaymentTransaction tx = new PaymentTransaction();
        tx.setOriginAccount(origin);
        tx.setDestinationAccount(destination);
        tx.setCustomer(customer);
        tx.setCommerce(commerce);
        tx.setAmount(amount);
        tx.setCategory(category);
        tx.setStatus(status);
        tx.setCreatedAt(createdAt);
        return tx;
    }
}