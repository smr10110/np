package cl.ufro.dci.naivepayapi.comercio.repository;


import cl.ufro.dci.naivepayapi.comercio.domain.CommerceValidation;
import cl.ufro.dci.naivepayapi.comercio.domain.ValidationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommerceValidationRepository extends JpaRepository<CommerceValidation, Long> {

    List<CommerceValidation> findAllByComStatus(ValidationStatus status);

    List<CommerceValidation> findAllByComStatusOrderByComValIdAsc(ValidationStatus status);
}
