package cl.ufro.dci.naivepayapi.comercio.repository;

import cl.ufro.dci.naivepayapi.comercio.domain.Commerce;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CommerceRepository extends JpaRepository<Commerce,Long> {

    Commerce findByComInfoComTaxId(String taxId);

}

