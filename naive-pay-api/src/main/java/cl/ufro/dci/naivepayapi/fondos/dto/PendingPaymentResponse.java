package cl.ufro.dci.naivepayapi.fondos.dto;

import cl.ufro.dci.naivepayapi.fondos.domain.TransactionStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response DTO for pending payment information.
 * <p>
 * Provides details of a payment awaiting approval, including
 * customer and commerce information.
 * </p>
 * 
 * @author NaivePay Development Team
 * @version 1.0
 * @since 2025-11-03
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PendingPaymentResponse {
    
    private Long id;
    private BigDecimal amount;
    private String commerceName;
    private String customerName;
    private String category;
    private String description;
    private LocalDateTime dateTime;
    private TransactionStatus status;
    private Long originAccountId;
    private Long destinationAccountId;
}
