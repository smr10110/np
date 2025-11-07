package cl.ufro.dci.naivepayapi.dispositivos.repository;

import cl.ufro.dci.naivepayapi.dispositivos.domain.DeviceRecovery;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface DeviceRecoveryRepository extends JpaRepository<DeviceRecovery, UUID> {
    Optional<DeviceRecovery> findByIdAndStatus(UUID id, String status);
}
