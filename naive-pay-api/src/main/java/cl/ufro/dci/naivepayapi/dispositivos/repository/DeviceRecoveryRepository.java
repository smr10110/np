package cl.ufro.dci.naivepayapi.dispositivos.repository;

import cl.ufro.dci.naivepayapi.dispositivos.domain.DeviceRecovery;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;


/**
 * Repository interface for managing {@link DeviceRecovery} entities.
 * <p>
 * Handles persistence and lookup operations related to device
 * recovery requests, including filtering by status and identifier.
 */
@Repository
public interface DeviceRecoveryRepository extends JpaRepository<DeviceRecovery, UUID> {

    /**
     * Finds a recovery request by its ID and current status.
     *
     * @param id     the recovery request unique identifier
     * @param status the expected recovery status (e.g. PENDING, VERIFIED)
     * @return an {@link Optional} containing the matching recovery record, or empty if not found
     */
    Optional<DeviceRecovery> findByIdAndStatus(UUID id, String status);
}
