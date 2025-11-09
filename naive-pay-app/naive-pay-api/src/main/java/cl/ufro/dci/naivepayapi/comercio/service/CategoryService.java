package cl.ufro.dci.naivepayapi.comercio.service;


import cl.ufro.dci.naivepayapi.comercio.domain.Commerce;
import cl.ufro.dci.naivepayapi.comercio.domain.CommerceCategory;
import cl.ufro.dci.naivepayapi.comercio.repository.CommerceCategoryRepository;
import lombok.Getter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class CategoryService {

    private final CommerceManagementService commerceManagementService;
    @Getter
    private CommerceCategoryRepository commerceCategoryRepository;

    public CategoryService(CommerceManagementService commerceManagementService) {
        this.commerceManagementService = commerceManagementService;
    }

    public List<Commerce> findCommerceByCategory(List<String> selectedCategoryNames) {
        List<Commerce> allCommerces = commerceManagementService.getAllCommerces();

        if (selectedCategoryNames == null || selectedCategoryNames.isEmpty()) {
            return allCommerces;
        }
        return allCommerces.stream().filter(commerce -> {
                    List<String> commerceCategoryNames = commerce.getComCategories().stream().map(CommerceCategory::getCatName).collect(Collectors.toList());
                    return commerceCategoryNames.containsAll(selectedCategoryNames);
        }).collect(Collectors.toList());
    }
    public List<CommerceCategory> findAll() {
        return commerceCategoryRepository.findAll();
    }





}
