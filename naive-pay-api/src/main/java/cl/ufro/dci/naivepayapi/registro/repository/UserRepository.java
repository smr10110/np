package cl.ufro.dci.naivepayapi.registro.repository;

import cl.ufro.dci.naivepayapi.registro.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByRutGeneral(Long rutGeneral);

    @Query("SELECT u FROM User u JOIN u.register r WHERE r.email = :email")
    Optional<User> findByEmail(@Param("email") String email);
}
