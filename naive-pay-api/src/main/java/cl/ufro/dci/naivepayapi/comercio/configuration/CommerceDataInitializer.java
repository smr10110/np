package cl.ufro.dci.naivepayapi.comercio.configuration;

import cl.ufro.dci.naivepayapi.comercio.domain.CommerceCategory;
import cl.ufro.dci.naivepayapi.comercio.repository.CommerceCategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component("commerceDataInitializer")
public class CommerceDataInitializer implements CommandLineRunner {

    @Autowired
    private CommerceCategoryRepository categoryRepository;

    @Override
    public void run(String... args) throws Exception {
        if (categoryRepository.count() == 0) {
            CommerceCategory restaurant = new CommerceCategory("RESTAURANTE", "Restaurantes y establecimientos de comida");
            CommerceCategory retail = new CommerceCategory("TIENDA MINORISTA", "Tiendas minoristas y comercio al por menor");
            
            categoryRepository.save(restaurant);
            categoryRepository.save(retail);
            
            System.out.println("Categorías iniciales creadas exitosamente");
        }
    }
}
