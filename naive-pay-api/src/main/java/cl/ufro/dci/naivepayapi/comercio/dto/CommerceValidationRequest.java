package cl.ufro.dci.naivepayapi.comercio.dto;

import cl.ufro.dci.naivepayapi.comercio.domain.CommerceInfo;
import lombok.Getter;
import lombok.Setter;

public class CommerceValidationRequest {

    @Getter
    @Setter
    private String comName;

    @Getter
    @Setter
    private String comTaxId;

    @Getter
    @Setter
    private String comLocation;

    @Getter
    @Setter
    private String comEmail;

    @Getter
    @Setter
    private String comContactPhone;

    @Getter
    @Setter
    private String comDescription;


    public CommerceValidationRequest() {}

    public CommerceValidationRequest(String comName, String comTaxId, String comLocation, String comEmail, String comContactPhone, String comDescription) {
        this.comName = comName;
        this.comTaxId = comTaxId;
        this.comLocation = comLocation;
        this.comEmail = comEmail;
        this.comContactPhone = comContactPhone;
        this.comDescription = comDescription;
    }

    public CommerceInfo toCommerceInfo() {
        return new CommerceInfo(this.comName, this.comTaxId, this.comLocation, this.comEmail, this.comContactPhone, this.comDescription);
    }

}
