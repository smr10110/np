package cl.ufro.dci.naivepayapi.fondos.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Request DTO for loading balance to an account.
 * <p>
 * Used when a user wants to add funds to their account.
 * In a production environment, this would be integrated with a
 * real payment gateway (credit card, bank transfer, etc.).
 * </p>
 * 
 * <p><b>Required validations:</b></p>
 * <ul>
 *   <li>userId must exist in the system</li>
 *   <li>amount must be greater than zero</li>
 *   <li>The user must have an associated account</li>
 * </ul>
 * 
 * @author NaivePay Development Team
 * @version 1.0
 * @since 2025-10-06
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AddFundsRequest {
    /** ID of the user requesting the fund load */
    private Long userId;
    
    /** Amount to add to the account (must be greater than zero) */
    private BigDecimal amount;
}