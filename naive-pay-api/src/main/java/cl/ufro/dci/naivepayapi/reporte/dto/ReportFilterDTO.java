package cl.ufro.dci.naivepayapi.reporte.dto;

import cl.ufro.dci.naivepayapi.fondos.domain.TransactionStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Filters for transaction report queries.
 *
 * All fields are optional; {@code null} values disable the corresponding filter.
 *
 * @since 1.0
 */
public class ReportFilterDTO {

    /** Minimum (inclusive) date/time of the queried range. */
    private LocalDateTime startDate;

    /** Maximum (inclusive) date/time of the queried range. */
    private LocalDateTime endDate;

    /** Transaction status to filter by. */
    private TransactionStatus status;

    /** Exact commerce name to filter by. */
    private String commerce;

    /** Text to search in description/category (depending on the query). */
    private String description;

    /** Minimum amount (inclusive). */
    private BigDecimal minAmount;

    /** Maximum amount (inclusive). */
    private BigDecimal maxAmount;

    /** User ID for admin queries (optional). */
    private Long userId;

    /** @return minimum (inclusive) date/time. */
    public LocalDateTime getStartDate() {
        return startDate;
    }

    /** @param startDate minimum (inclusive) date/time. */
    public void setStartDate(LocalDateTime startDate) {
        this.startDate = startDate;
    }

    /** @return maximum (inclusive) date/time. */
    public LocalDateTime getEndDate() {
        return endDate;
    }

    /** @param endDate maximum (inclusive) date/time. */
    public void setEndDate(LocalDateTime endDate) {
        this.endDate = endDate;
    }

    /** @return transaction status. */
    public TransactionStatus getStatus() {
        return status;
    }

    /** @param status transaction status. */
    public void setStatus(TransactionStatus status) {
        this.status = status;
    }

    /** @return exact commerce name. */
    public String getCommerce() {
        return commerce;
    }

    /** @param commerce exact commerce name. */
    public void setCommerce(String commerce) {
        this.commerce = commerce;
    }

    /** @return description/category text. */
    public String getDescription() {
        return description;
    }

    /** @param description description/category text. */
    public void setDescription(String description) {
        this.description = description;
    }

    /** @return minimum amount (inclusive). */
    public BigDecimal getMinAmount() {
        return minAmount;
    }

    /** @param minAmount minimum amount (inclusive). */
    public void setMinAmount(BigDecimal minAmount) {
        this.minAmount = minAmount;
    }

    /** @return maximum amount (inclusive). */
    public BigDecimal getMaxAmount() {
        return maxAmount;
    }

    /** @param maxAmount maximum amount (inclusive). */
    public void setMaxAmount(BigDecimal maxAmount) {
        this.maxAmount = maxAmount;
    }

    /** @return user ID. */
    public Long getUserId() {
        return userId;
    }

    /** @param userId user ID. */
    public void setUserId(Long userId) {
        this.userId = userId;
    }
}
