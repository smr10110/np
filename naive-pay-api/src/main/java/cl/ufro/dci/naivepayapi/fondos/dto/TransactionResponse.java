package cl.ufro.dci.naivepayapi.fondos.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response DTO for detailed transaction information.
 * <p>
 * Provides transaction data while avoiding lazy loading issues
 * by exposing only account IDs instead of complete entities.
 * </p>
 * 
 * <p><b>Design advantages:</b></p>
 * <ul>
 *   <li>Avoids LazyInitializationException during JSON serialization</li>
 *   <li>Reduces HTTP response size</li>
 *   <li>Allows client to request account details separately if needed</li>
 *   <li>Improves performance by not loading unnecessary relationships</li>
 * </ul>
 * 
 * @author NaivePay Development Team
 * @version 1.0
 * @since 2025-10-06
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransactionResponse {
    /** Unique transaction identifier */
    private Long id;
    
    /** Transaction amount */
    private BigDecimal amount;
    
    /** Transaction date and time */
    private LocalDateTime dateTime;
    
    /** Transaction description or concept */
    private String description;
    
    /** Origin account ID (can be null for LOAD operations) */
    private Long originAccountId;
    
    /** Destination account ID */
    private Long destinationAccountId;

    /** Transaction type (LOAD, TRANSFER, PAYMENT, etc.) */
    private String type;
    
    /** Transaction status (PENDING, COMPLETED, REJECTED, CANCELED) */
    private String status;
    
    /** Customer name (for payment transactions) */
    private String customerName;
    
    /** Commerce name (for payment transactions) */
    private String commerceName;
    
    /** Payment category (for payment transactions) */
    private String paymentCategory;
}
