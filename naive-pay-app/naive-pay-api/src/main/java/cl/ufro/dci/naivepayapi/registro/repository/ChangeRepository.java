package cl.ufro.dci.naivepayapi.registro.repository;

import cl.ufro.dci.naivepayapi.registro.domain.Change;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChangeRepository extends JpaRepository<Change, Long> {
}