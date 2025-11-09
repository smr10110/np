package cl.ufro.dci.naivepayapi.recompensas.domain;

/**estados de una transaccion de recompensa.*/

public enum RewardTransactionStatus {
    PENDING,
    APPROVED,
    REJECTED,
    CANCELED,
    COMPLETED,
}