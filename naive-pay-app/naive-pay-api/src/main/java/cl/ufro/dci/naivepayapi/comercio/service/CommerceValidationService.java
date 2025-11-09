package cl.ufro.dci.naivepayapi.comercio.service;


import cl.ufro.dci.naivepayapi.comercio.domain.*;
import cl.ufro.dci.naivepayapi.comercio.repository.CommerceRepository;
import cl.ufro.dci.naivepayapi.comercio.repository.CommerceValidationRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Set;

@Service
@Transactional
public class CommerceValidationService {

    private final CommerceRepository commerceRepository;
    private final CommerceValidationRepository validationRepository;

    public CommerceValidationService(CommerceRepository commerceRepository, CommerceValidationRepository validationRepository) {
        this.commerceRepository = commerceRepository;
        this.validationRepository = validationRepository;
    }

    public List<CommerceValidation> getPendingRequests() {
        return validationRepository.findAllByComStatus(ValidationStatus.PENDING);
    }

    public List<CommerceValidation> getExpiredRequests() {
        return validationRepository.findAllByComStatus(ValidationStatus.EXPIRED);
    }

    public CommerceValidation submitNewRequest(CommerceInfo info) {
        if (commerceRepository.findByComInfoComTaxId(info.getComTaxId()) != null) {
            throw new IllegalStateException("Tax already exists");
        }

        CommerceValidation newRequest = new CommerceValidation(info);
        return validationRepository.save(newRequest);
    }

    public Commerce approveRequest(Long validationId, Set<CommerceCategory> categories){
        CommerceValidation validationRequest = validationRepository.findById(validationId).orElseThrow(() -> new RuntimeException("request not found"));

        validationRequest.approve();

        String taxId = validationRequest.getComInfo().getComTaxId();
        Commerce commerce = commerceRepository.findByComInfoComTaxId(taxId);

        if (commerce == null) {
            commerce = new Commerce(validationRequest.getComInfo(), categories);
        } else {
            commerce.setComInfo(validationRequest.getComInfo());
            commerce.setComCategories(categories);
        }

        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.YEAR, 1);
        Date validUntil = calendar.getTime();

        commerce.getComVerificationStatus().approve(validUntil);

        validationRepository.save(validationRequest);
        return commerceRepository.save(commerce);

    }

    public void rejectRequest(Long validationId) {
        CommerceValidation validationRequest = validationRepository.findById(validationId).orElseThrow(() -> new RuntimeException("request not found"));

        validationRequest.reject();
        validationRepository.save(validationRequest);
    }

    public List<CommerceValidation> getRejectedRequests() {
        return validationRepository.findAllByComStatus(ValidationStatus.INVALID);
    }
}
