package cl.ufro.dci.naivepayapi.registro.repository;

import cl.ufro.dci.naivepayapi.registro.domain.Credencial;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CredencialRepository extends JpaRepository<Credencial, Long> {
}