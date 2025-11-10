package cl.ufro.dci.naivepayapi.dispositivos.repository;

import cl.ufro.dci.naivepayapi.dispositivos.domain.Device;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;


/**
 * Repository interface for managing {@link Device} entities
 * <p>
 * Provides data access methods for device persistence and lookup
 * operations associated with specific users
 */
public interface DeviceRepository extends JpaRepository<Device, String> {

    /**
     * Finds a device linked to a specific user ID.
     *
     * @param userId the unique identifier of the user
     * @return an {@link Optional} containing the device if found, or empty otherwise
     */
    Optional<Device> findByUserUseId(Long userId);
}


