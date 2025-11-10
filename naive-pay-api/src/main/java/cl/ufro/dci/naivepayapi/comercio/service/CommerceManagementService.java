package cl.ufro.dci.naivepayapi.comercio.service;


import cl.ufro.dci.naivepayapi.comercio.domain.Commerce;
import cl.ufro.dci.naivepayapi.comercio.domain.CommerceCategory;
import cl.ufro.dci.naivepayapi.comercio.domain.CommerceInfo;
import cl.ufro.dci.naivepayapi.comercio.domain.VerificationStatus;
import cl.ufro.dci.naivepayapi.comercio.dto.CommerceCreation;
import cl.ufro.dci.naivepayapi.comercio.repository.CommerceCategoryRepository;
import cl.ufro.dci.naivepayapi.comercio.repository.CommerceRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@Transactional
public class CommerceManagementService {

    private final CommerceRepository commerceRepository;
    private final CommerceCategoryRepository categoryRepository;

    public CommerceManagementService(CommerceRepository commerceRepository,  CommerceCategoryRepository categoryRepository) {
        this.commerceRepository = commerceRepository;
        this.categoryRepository = categoryRepository;
    }

    public Commerce createCommerce(CommerceCreation newCommerce) {
        if (commerceRepository.findByComInfoComTaxId(newCommerce.getComTaxId()) != null) {
            throw new RuntimeException("Commerce already exists");
        }

        CommerceInfo info = new CommerceInfo(
                newCommerce.getComName(),
                newCommerce.getComTaxId(),
                newCommerce.getComLocation(),
                newCommerce.getComEmail(),
                newCommerce.getComContact(),
                newCommerce.getComDescription()
                );

        VerificationStatus status = new VerificationStatus(false, new Date(),null);

        List<CommerceCategory> categoriesList = categoryRepository.findAllById(newCommerce.getCategoryIds());
        Set<CommerceCategory> categories = new HashSet<CommerceCategory>(categoriesList);

        if (categories.isEmpty() && !newCommerce.getCategoryIds().isEmpty()) {
            throw new RuntimeException("Category Ids not found");
        }

        Commerce commerceToCreate = new Commerce(info, status, categories);
        return commerceRepository.save(commerceToCreate);
    }

    public Commerce updateCommerce(String currentTaxId, CommerceInfo newInfo, Set<CommerceCategory> newCategories) {
        Commerce existingCommerce = commerceRepository.findByComInfoComTaxId(currentTaxId);

        if (existingCommerce == null) {
            throw new RuntimeException("Commerce not found");
        }

        existingCommerce.setComInfo(newInfo);
        existingCommerce.setComCategories(newCategories);

        return commerceRepository.save(existingCommerce);
    }

    public List<Commerce> getAllCommerces() {
        return commerceRepository.findAll();
    }

    public Commerce getCommerceByTaxId(String taxId) {
        Commerce commerce = commerceRepository.findByComInfoComTaxId(taxId);
        if (commerce == null) {
            throw new RuntimeException("commerce not found");
        }
        return commerce;
    }

    public void deleteCommerce(String taxId) {
        Commerce commerce = commerceRepository.findByComInfoComTaxId(taxId);

        if (commerce == null) {
            throw new RuntimeException("commerce not found");
        }

        commerceRepository.delete(commerce);
    }


}
