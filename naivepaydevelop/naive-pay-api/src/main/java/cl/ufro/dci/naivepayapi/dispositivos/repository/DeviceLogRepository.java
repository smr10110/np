package cl.ufro.dci.naivepayapi.dispositivos.repository;

import cl.ufro.dci.naivepayapi.dispositivos.domain.DeviceLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

import cl.ufro.dci.naivepayapi.dispositivos.domain.Device;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


@Repository
public interface DeviceLogRepository extends JpaRepository<DeviceLog, Long> {

    List<DeviceLog> findByUserUseId(Long userId);

    List<DeviceLog> findByDevice_Fingerprint(String fingerprint);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update DeviceLog l set l.device = null where l.device = :device")
    int detachDeviceFromLogs(@Param("device") Device device);
}


