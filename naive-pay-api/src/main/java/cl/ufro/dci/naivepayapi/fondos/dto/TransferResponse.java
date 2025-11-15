package cl.ufro.dci.naivepayapi.fondos.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Response DTO for transfer operations.
 * <p>
 * Provides standardized information about the result of a financial operation,
 * indicating whether it was successful and providing relevant details.
 * </p>
 * 
 * <p><b>Uses the Factory pattern for:</b></p>
 * <ul>
 *   <li>Consistent creation of success and error responses</li>
 *   <li>More readable and maintainable code</li>
 *   <li>Reduce errors in response construction</li>
 * </ul>
 * 
 * @author NaivePay Development Team
 * @version 1.0
 * @since 2025-10-06
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TransferResponse {
    /** Indicates whether the operation was successful */
    private boolean success;
    
    /** Descriptive message of the result */
    private String message;
    
    /** Transaction ID (null if failed) */
    private Long transactionId;

    /**
     * Factory method to create a success response.
     * 
     * @param transactionId the ID of the created transaction
     * @return a success response with the transaction ID
     */
    public static TransferResponse success(Long transactionId) {
        return new TransferResponse(true, "Transfer completed successfully", transactionId);
    }

    /**
     * Factory method to create an error response.
     * 
     * @param message the descriptive error message
     * @return an error response with the provided message
     */
    public static TransferResponse error(String message) {
        return new TransferResponse(false, message, null);
    }
}
