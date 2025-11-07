package cl.ufro.dci.naivepayapi.dispositivos.repository;

import cl.ufro.dci.naivepayapi.dispositivos.domain.Device;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

public interface DeviceRepository extends JpaRepository<Device, String> {
    Optional<Device> findByUser_Id(Long userId);
}


