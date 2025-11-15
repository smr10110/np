package cl.ufro.dci.naivepayapi.autentificacion.repository;

import cl.ufro.dci.naivepayapi.autentificacion.domain.Session;
import cl.ufro.dci.naivepayapi.autentificacion.domain.enums.SessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SessionRepository extends JpaRepository<Session, Long> {

    @Query("SELECT s FROM Session s " +
           "LEFT JOIN FETCH s.initialAuthAttempt a " +
           "LEFT JOIN FETCH a.device d " +
           "LEFT JOIN FETCH d.user " +
           "WHERE s.sesJti = :sesJti AND s.status = :status")
    Optional<Session> findBySesJtiAndStatus(@Param("sesJti") UUID sesJti, @Param("status") SessionStatus status);

    @Query("SELECT s FROM Session s " +
           "LEFT JOIN FETCH s.initialAuthAttempt a " +
           "LEFT JOIN FETCH a.device d " +
           "LEFT JOIN FETCH d.user " +
           "WHERE s.sesJti = :sesJti")
    Optional<Session> findBySesJti(@Param("sesJti") UUID sesJti);

}
