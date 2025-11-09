package cl.ufro.dci.naivepayapi.comercio.dto;

import cl.ufro.dci.naivepayapi.comercio.domain.CommerceInfo;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;

public class CommerceUpdate {

    @Getter
    @Setter
    private String comTaxId;

    @Getter
    @Setter
    private String comName;

    @Getter
    @Setter
    private String comLocation;

    @Getter
    @Setter
    private String comEmail;

    @Getter
    @Setter
    private String comContact;

    @Getter
    @Setter
    private String comDescription;

    @Getter
    @Setter
    private Set<Long> categoryIds;

    public CommerceUpdate(){}

    public CommerceUpdate(String comTaxId, String comName, String comLocation, String comEmail, String comContact, String comDescription, Set<Long> categoryIds) {
        this.comTaxId = comTaxId;
        this.comName = comName;
        this.comLocation = comLocation;
        this.comEmail = comEmail;
        this.comContact = comContact;
        this.comDescription = comDescription;
        this.categoryIds = categoryIds;
    }

    public CommerceInfo toCommerceInfo() {
        return new CommerceInfo(this.comName, this.comTaxId, this.comLocation, this.comEmail, this.comContact, this.comDescription);
    }
}
