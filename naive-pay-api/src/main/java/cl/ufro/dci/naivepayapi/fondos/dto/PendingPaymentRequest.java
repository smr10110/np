package cl.ufro.dci.naivepayapi.fondos.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Request DTO for creating a payment transaction pending approval.
 * <p>
 * Used when a payment requires manual validation before executing
 * the actual fund transfer.
 * </p>
 * 
 * @author NaivePay Development Team
 * @version 1.0
 * @since 2025-11-03
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PendingPaymentRequest {
    
    /** ID of the account from which payment will be made */
    private Long originAccountId;
    
    /** ID of the commerce account receiving the payment */
    private Long destinationAccountId;
    
    /** Payment amount */
    private BigDecimal amount;
    
    /** Payment description */
    private String description;
    
    /** Name of the customer making the payment */
    private String customerName;
    
    /** Name of the commerce receiving the payment */
    private String commerceName;
    
    /** Payment category (e.g., "Food", "Transport") */
    private String category;
}
