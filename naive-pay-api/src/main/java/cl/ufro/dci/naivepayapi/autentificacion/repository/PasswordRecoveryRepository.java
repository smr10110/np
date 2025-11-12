package cl.ufro.dci.naivepayapi.autentificacion.repository;

import cl.ufro.dci.naivepayapi.autentificacion.domain.PasswordRecovery;
import cl.ufro.dci.naivepayapi.autentificacion.domain.enums.PasswordRecoveryStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PasswordRecoveryRepository extends JpaRepository<PasswordRecovery, Long> {

    @Query("""
        SELECT pr FROM PasswordRecovery pr
        WHERE pr.user.useId = :userId
        AND pr.pasStatus = :status
        ORDER BY pr.pasCreated DESC
        LIMIT 1
        """)
    Optional<PasswordRecovery> findLatestByUserIdAndStatus(
            @Param("userId") Long userId,
            @Param("status") PasswordRecoveryStatus status
    );

    Optional<PasswordRecovery> findByUser_UseIdAndPasCode(Long userId, String code);

}