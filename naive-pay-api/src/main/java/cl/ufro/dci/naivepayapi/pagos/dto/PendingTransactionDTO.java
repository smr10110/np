package cl.ufro.dci.naivepayapi.pagos.dto;

import java.math.BigDecimal;

/**
 * Data Transfer Object for representing pending payment transactions.
 * Contains essential information for displaying pending transactions in lists or overviews.
 */
public class PendingTransactionDTO {
    private Long id;
    private BigDecimal amount;
    private String commerce;

    /**
     * Constructs a new PendingTransactionDTO with the specified details.
     *
     * @param id the unique identifier of the transaction
     * @param amount the transaction amount
     * @param commerce the name of the commerce receiving the payment
     */
    public PendingTransactionDTO(Long id, BigDecimal amount, String commerce) {
        this.id = id;
        this.amount = amount;
        this.commerce = commerce;
    }

    /**
     * Returns the transaction ID.
     *
     * @return the unique identifier of the transaction
     */
    public Long getId() { return id; }

    /**
     * Returns the transaction amount.
     *
     * @return the amount being transferred in the transaction
     */
    public BigDecimal getAmount() { return amount; }

    /**
     * Returns the commerce name.
     *
     * @return the name of the commerce or business receiving the payment
     */
    public String getCommerce() { return commerce; }
}