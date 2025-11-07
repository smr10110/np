package cl.ufro.dci.naivepayapi.registro.repository;

import cl.ufro.dci.naivepayapi.registro.domain.Register;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RegisterRepository extends JpaRepository<Register, Long> {
    Register findByRegEmail(String regEmail);
}
