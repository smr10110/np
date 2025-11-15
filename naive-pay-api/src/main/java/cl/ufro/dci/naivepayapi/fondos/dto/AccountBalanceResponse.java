package cl.ufro.dci.naivepayapi.fondos.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response DTO for account balance queries.
 * <p>
 * Provides relevant account information without exposing the complete entity,
 * following the DTO (Data Transfer Object) design pattern.
 * </p>
 * 
 * <p><b>Advantages of using this DTO:</b></p>
 * <ul>
 *   <li>Decouples presentation layer from domain model</li>
 *   <li>Avoids lazy loading issues in JSON serialization</li>
 *   <li>Controls exactly what information is exposed to the client</li>
 *   <li>Facilitates entity changes without affecting the API</li>
 * </ul>
 * 
 * @author NaivePay Development Team
 * @version 1.0
 * @since 2025-10-06
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AccountBalanceResponse {
    /** Unique identifier of the account */
    private Long accountId;
    
    /** Identifier of the owner user */
    private Long userId;
    
    /** Available balance in the account */
    private BigDecimal availableBalance;
    
    /** Date and time of the last balance update */
    private LocalDateTime lastUpdate;
}
