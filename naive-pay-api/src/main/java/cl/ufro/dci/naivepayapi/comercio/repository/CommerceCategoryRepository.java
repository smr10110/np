package cl.ufro.dci.naivepayapi.comercio.repository;

import cl.ufro.dci.naivepayapi.comercio.domain.CommerceCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CommerceCategoryRepository extends JpaRepository<CommerceCategory, Long> {}
