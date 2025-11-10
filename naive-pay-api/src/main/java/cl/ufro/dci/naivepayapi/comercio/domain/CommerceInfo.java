package cl.ufro.dci.naivepayapi.comercio.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Data;

@Data
@Embeddable
public class CommerceInfo {


    private String comName;

	private String comTaxId;

	private String comLocation;

    private String comEmail;

    private String comContactPhone;

    @Column(name = "com_description", columnDefinition = "TEXT")
    private String comDescription;

    public CommerceInfo() {}

    public CommerceInfo(String name, String taxId, String location, String email, String contact, String description) {
        this.comName = name;
        this.comTaxId = taxId;
        this.comLocation = location;
        this.comEmail = email;
        this.comContactPhone = contact;
        this.comDescription = description;
    }
}