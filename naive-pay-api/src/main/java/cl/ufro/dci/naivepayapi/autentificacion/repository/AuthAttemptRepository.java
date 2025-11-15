package cl.ufro.dci.naivepayapi.autentificacion.repository;

import cl.ufro.dci.naivepayapi.autentificacion.domain.AuthAttempt;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface AuthAttemptRepository extends JpaRepository<AuthAttempt, Long> {

    /**
     * Obtiene los últimos N intentos de autenticación de un usuario
     * ordenados por fecha descendente (más reciente primero).
     *
     * @param userId ID del usuario
     * @param pageable Paginación (usar PageRequest.of(0, 5) para obtener últimos 5)
     * @return Lista de intentos de autenticación
     */
    @Query("""
        SELECT a FROM AuthAttempt a
        JOIN a.device d
        JOIN d.user u
        WHERE u.useId = :userId
        ORDER BY a.attOccurred DESC
        """)
    List<AuthAttempt> findLatestAttemptsByUser(
        @Param("userId") Long userId,
        Pageable pageable
    );

    /**
     * Cuenta los intentos fallidos de un usuario desde una fecha específica.
     *
     * @param userId ID del usuario
     * @param since Fecha desde la cual contar (ej: hace 30 minutos)
     * @return Cantidad de intentos fallidos
     */
    @Query("""
        SELECT COUNT(a) FROM AuthAttempt a
        JOIN a.device d
        JOIN d.user u
        WHERE u.useId = :userId
        AND a.attSuccess = false
        AND a.attOccurred > :since
        """)
    long countFailedAttemptsSince(
        @Param("userId") Long userId,
        @Param("since") Instant since
    );

  // obtener la fecha/hora del último intento EXITOSO para poder reinicar contador
    @Query("""
        SELECT MAX(a.attOccurred) FROM AuthAttempt a
        JOIN a.device d
        JOIN d.user u
        WHERE u.useId = :userId AND a.attSuccess = true
        """)
    Instant findLastSuccessAt(@Param("userId") Long userId);
}
