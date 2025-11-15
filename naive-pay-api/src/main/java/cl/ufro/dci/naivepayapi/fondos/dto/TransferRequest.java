package cl.ufro.dci.naivepayapi.fondos.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Request DTO for transfers between accounts.
 * <p>
 * Used primarily by the Payments module to request
 * fund transfers between users and merchants.
 * </p>
 * 
 * <p><b>Required validations:</b></p>
 * <ul>
 *   <li>originAccountId and destinationAccountId must exist</li>
 *   <li>amount must be greater than zero</li>
 *   <li>originAccountId must have sufficient balance</li>
 *   <li>originAccountId and destinationAccountId must be different</li>
 * </ul>
 * 
 * @author NaivePay Development Team
 * @version 1.0
 * @since 2025-10-06
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransferRequest {
    /** ID of the origin account of the funds */
    private Long originAccountId;
    
    /** ID of the destination account of the funds */
    private Long destinationAccountId;
    
    /** Amount to transfer (must be greater than zero) */
    private BigDecimal amount;
    
    /** Description or concept of the transfer */
    private String description;
}
