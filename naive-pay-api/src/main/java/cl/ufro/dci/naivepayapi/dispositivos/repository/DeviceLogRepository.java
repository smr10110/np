package cl.ufro.dci.naivepayapi.dispositivos.repository;

import cl.ufro.dci.naivepayapi.dispositivos.domain.Device;
import cl.ufro.dci.naivepayapi.dispositivos.domain.DeviceLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Repository for accessing and updating device log records.
 * Permite consultar logs por usuario o dispositivo y
 * “desasociar” un device de sus logs cuando el device se elimina.
 */
@Repository
public interface DeviceLogRepository extends JpaRepository<DeviceLog, Long> {

    List<DeviceLog> findByUserId(Long userId);

    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update DeviceLog dl set dl.device = null where dl.device = :device")
    int detachDeviceFromLogs(@Param("device") Device device);

    @Query("select dl from DeviceLog dl where dl.userId = :userId order by dl.createdAt desc")
    List<DeviceLog> findAllByUserIdDesc(@Param("userId") Long userId);
}