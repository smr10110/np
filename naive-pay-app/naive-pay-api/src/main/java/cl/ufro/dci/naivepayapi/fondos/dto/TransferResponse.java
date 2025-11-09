package cl.ufro.dci.naivepayapi.fondos.dto;

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
public class TransferResponse {
    /** Indicates whether the operation was successful */
    private boolean success;
    
    /** Descriptive message of the result */
    private String message;
    
    /** Transaction ID (null if failed) */
    private Long transactionId;

    /**
     * Default constructor.
     */
    public TransferResponse() {
    }

    /**
     * Complete constructor with all fields.
     * 
     * @param success indicates whether the operation was successful
     * @param message descriptive message of the result
     * @param transactionId ID of the created transaction (null if failed)
     */
    public TransferResponse(boolean success, String message, Long transactionId) {
        this.success = success;
        this.message = message;
        this.transactionId = transactionId;
    }

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

    // Getters and Setters
    
    /**
     * Indicates whether the operation was successful.
     * 
     * @return {@code true} if successful, {@code false} otherwise
     */
    public boolean isSuccess() {
        return success;
    }

    /**
     * Sets the success status of the operation.
     * 
     * @param success the success status
     */
    public void setSuccess(boolean success) {
        this.success = success;
    }

    /**
     * Gets the descriptive message.
     * 
     * @return the result message
     */
    public String getMessage() {
        return message;
    }

    /**
     * Sets the descriptive message.
     * 
     * @param message the message to set
     */
    public void setMessage(String message) {
        this.message = message;
    }

    /**
     * Gets the ID of the created transaction.
     * 
     * @return the transaction ID, or null if the operation failed
     */
    public Long getTransactionId() {
        return transactionId;
    }

    /**
     * Sets the transaction ID.
     * 
     * @param transactionId the transaction ID
     */
    public void setTransactionId(Long transactionId) {
        this.transactionId = transactionId;
    }

    /**
     * Generates a String representation of the object.
     * 
     * @return a string with the response data
     */
    @Override
    public String toString() {
        return "TransferResponse{" +
                "success=" + success +
                ", message='" + message + '\'' +
                ", transactionId=" + transactionId +
                '}';
    }
}
