package cl.ufro.dci.naivepayapi.comercio.controller;


import cl.ufro.dci.naivepayapi.comercio.domain.Commerce;
import cl.ufro.dci.naivepayapi.comercio.domain.CommerceCategory;
import cl.ufro.dci.naivepayapi.comercio.dto.CommerceCreation;
import cl.ufro.dci.naivepayapi.comercio.dto.CommerceResponse;
import cl.ufro.dci.naivepayapi.comercio.dto.CommerceUpdate;
import cl.ufro.dci.naivepayapi.comercio.repository.CommerceCategoryRepository;
import cl.ufro.dci.naivepayapi.comercio.service.CommerceManagementService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/commerce")
public class CommerceController {

    private final CommerceManagementService managementService;
    private final CommerceCategoryRepository categoryRepository;

    public CommerceController(CommerceManagementService managementService, CommerceCategoryRepository categoryRepository) {

        this.managementService = managementService;
        this.categoryRepository = categoryRepository;
    }

    private CommerceResponse toCommerceResponse(Commerce commerce) {
        if (commerce == null) return null;

        Set<String> categoryNames = commerce.getComCategories().stream().map(c -> c.getCatName()).collect(Collectors.toSet());

        return new CommerceResponse(
                commerce.getComId(),
                commerce.getComInfo(),
                commerce.getComVerificationStatus().isComIsVerified(),
                commerce.getComVerificationStatus().getComVerificationValidUntil(),
                categoryNames
        );
    }

    @GetMapping
    public ResponseEntity<String> commerceRoot() {
        String message = "Commerce service is running. Use /commerce/all -> list all commerces, or /commerce/{taxId} to fetch a specific commerce.";
        return ResponseEntity.ok(message);
    }

    @PostMapping
    public ResponseEntity<CommerceResponse> createCommerce(@RequestBody CommerceCreation dto) {
        Commerce newCommerce = managementService.createCommerce(dto);
        CommerceResponse commerceResponse = toCommerceResponse(newCommerce);
        return ResponseEntity.status(HttpStatus.CREATED).body(commerceResponse);
    }

    @GetMapping("/all")
    public ResponseEntity<List<CommerceResponse>> getAllCommerces() {
        List<Commerce> commerces = managementService.getAllCommerces();

        List<CommerceResponse> dtos = commerces.stream().map(this :: toCommerceResponse).collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/{taxId}")
    public ResponseEntity<CommerceResponse> getCommerce(@PathVariable("taxId") String taxId) {
        Commerce commerce = managementService.getCommerceByTaxId(taxId);
        return ResponseEntity.ok(toCommerceResponse(commerce));
    }

    @PutMapping("/{taxId}")
    public ResponseEntity<CommerceResponse> updateCommerce(@PathVariable String taxId, @RequestBody CommerceUpdate dto) {

        List<CommerceCategory> categoriesList = categoryRepository.findAllById(dto.getCategoryIds());

        Set<CommerceCategory> categories = categoriesList.stream().collect(Collectors.toSet());

        Commerce updatedCommerce = managementService.updateCommerce(
                taxId,
                dto.toCommerceInfo(),
                categories
        );

        return ResponseEntity.ok(toCommerceResponse(updatedCommerce));
    }

    @DeleteMapping("/{taxId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCommerce(@PathVariable String taxId) {
        managementService.deleteCommerce(taxId);
    }


}
