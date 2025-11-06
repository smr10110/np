package cl.ufro.dci.naivepayapi.autentificacion.repository;

import cl.ufro.dci.naivepayapi.autentificacion.domain.SessionHistory;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositorio simple para guardar historial de sesiones cerradas.
 */
public interface SessionHistoryRepository extends JpaRepository<SessionHistory, Long> {
    // Solo necesitamos save(), no queries complejas
}
