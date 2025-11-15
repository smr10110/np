package cl.ufro.dci.naivepayapi.comercio.controller;


import cl.ufro.dci.naivepayapi.comercio.domain.Commerce;
import cl.ufro.dci.naivepayapi.comercio.domain.CommerceCategory;
import cl.ufro.dci.naivepayapi.comercio.domain.CommerceValidation;
import cl.ufro.dci.naivepayapi.comercio.dto.CommerceApproval;
import cl.ufro.dci.naivepayapi.comercio.dto.CommerceValidationRequest;
import cl.ufro.dci.naivepayapi.comercio.repository.CommerceCategoryRepository;
import cl.ufro.dci.naivepayapi.comercio.service.CommerceValidationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/validation")
public class CommerceValidationController {

    private final CommerceCategoryRepository categoryRepository;
    private final CommerceValidationService validationService;

    public CommerceValidationController(CommerceCategoryRepository categoryRepository, CommerceValidationService validationService) {
        this.categoryRepository = categoryRepository;
        this.validationService = validationService;
    }

    @GetMapping
    public ResponseEntity<String> validationRoot(){
        String message = "Endpoint working.";
        return ResponseEntity.ok(message);
    }

    @PostMapping("/submit")
    public ResponseEntity<CommerceValidation> submitRequest(@RequestBody CommerceValidationRequest dto) {
        CommerceValidation newRequest = validationService.submitNewRequest(
                dto.toCommerceInfo()
        );
        return new ResponseEntity<>(newRequest, HttpStatus.CREATED);
    }

    @GetMapping("/pending")
    public ResponseEntity<List<CommerceValidation>> getPendingRequests() {
        List<CommerceValidation> pendingRequests = validationService.getPendingRequests();
        return ResponseEntity.ok(pendingRequests);
    }

    @GetMapping("/expired")
    public ResponseEntity<List<CommerceValidation>> getExpiredRequests() {
        List<CommerceValidation> expiredRequestsRequests = validationService.getExpiredRequests();
        return ResponseEntity.ok(expiredRequestsRequests);
    }

    @PutMapping("/approve/{id}")
    public ResponseEntity<Commerce> approveRequest(
            @PathVariable Long id,
            @RequestBody CommerceApproval dto
    ) {
        List<CommerceCategory> categoriesList = categoryRepository.findAllById(dto.getCategoryIds());
        Set<CommerceCategory> categories = categoriesList.stream().collect(Collectors.toSet());

        Commerce approvedCommerce = validationService.approveRequest(id, categories);

        return ResponseEntity.ok(approvedCommerce);
    }

    @PutMapping("/reject/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void rejectRequest(@PathVariable Long id) {
        validationService.rejectRequest(id);
    }

    @GetMapping("/rejected")
    public List<CommerceValidation> getRejected() {
        return validationService.getRejectedRequests();
    }

}
