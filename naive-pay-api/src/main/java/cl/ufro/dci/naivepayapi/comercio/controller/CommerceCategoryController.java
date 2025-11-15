package cl.ufro.dci.naivepayapi.comercio.controller;


import cl.ufro.dci.naivepayapi.comercio.domain.Commerce;
import cl.ufro.dci.naivepayapi.comercio.domain.CommerceCategory;
import cl.ufro.dci.naivepayapi.comercio.repository.CommerceCategoryRepository;
import cl.ufro.dci.naivepayapi.comercio.service.CategoryService;
import cl.ufro.dci.naivepayapi.comercio.service.CommerceManagementService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/categories")
public class CommerceCategoryController {

    private final CommerceCategoryRepository categoryRepository;
    private final CommerceManagementService commerceManagementService;
    private final CategoryService categoryService;

    public CommerceCategoryController(CommerceCategoryRepository categoryRepository, CommerceManagementService commerceManagementService, CategoryService categoryService) {
        this.categoryRepository = categoryRepository;
        this.commerceManagementService = commerceManagementService;
        this.categoryService = categoryService;
    }

    @GetMapping
    public ResponseEntity<List<CommerceCategory>> getAllCategories(){
        List<CommerceCategory> categories = categoryRepository.findAll();

        return ResponseEntity.ok(categories);
    }

    @GetMapping("/search")
    public ResponseEntity<List<Commerce>> findCommercesByCategory(@RequestParam(name = "category", required = false) List<String> categories){
        List<Commerce> response;

        if(categories == null || categories.isEmpty()){
            response = commerceManagementService.getAllCommerces();
        } else {
            response = categoryService.findCommerceByCategory(categories);
        }

        return ResponseEntity.ok(response);


    }

}
