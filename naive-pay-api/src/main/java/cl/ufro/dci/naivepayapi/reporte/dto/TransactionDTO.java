package cl.ufro.dci.naivepayapi.reporte.dto;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;

/**
 * DTO for Transaction data in reports.
 * Excludes relationship data to prevent circular references during serialization.
 * 
 * @since 2.0
 */
public class TransactionDTO {
    public final Long traId;
    public final BigDecimal traAmount;
    public final LocalDateTime traDateTime;
    public final String traDescription;
    public final String traType;
    public final String traCustomerName;
    public final String traCommerceName;
    public final String traPaymentCategory;
    public final String traStatus;
    public final Long accIdOriginId;
    public final Long accIdDestinationId;
    public final String originUserName;
    public final String destinationUserName;

    /**
     * Constructor that accepts raw JDBC types and converts them appropriately.
     * This is used by Hibernate's native query result mapping.
     */
    public TransactionDTO(
            Long traId,
            BigDecimal traAmount,
            Object traDateTime,  // Can be Timestamp or LocalDateTime
            String traDescription,
            String traType,
            String traCustomerName,
            String traCommerceName,
            String traPaymentCategory,
            String traStatus,
            Long accIdOriginId,
            Long accIdDestinationId,
            String originUserName,
            String destinationUserName
    ) {
        this.traId = traId;
        this.traAmount = traAmount;
        // Handle conversion from java.sql.Timestamp to java.time.LocalDateTime
        this.traDateTime = traDateTime instanceof Timestamp
            ? ((Timestamp) traDateTime).toLocalDateTime()
            : (LocalDateTime) traDateTime;
        this.traDescription = traDescription;
        this.traType = traType;
        this.traCustomerName = traCustomerName;
        this.traCommerceName = traCommerceName;
        this.traPaymentCategory = traPaymentCategory;
        this.traStatus = traStatus;
        this.accIdOriginId = accIdOriginId;
        this.accIdDestinationId = accIdDestinationId;
        this.originUserName = originUserName;
        this.destinationUserName = destinationUserName;
    }
}
