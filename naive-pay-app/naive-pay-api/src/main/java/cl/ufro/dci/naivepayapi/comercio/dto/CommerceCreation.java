package cl.ufro.dci.naivepayapi.comercio.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.Set;

public class CommerceCreation {

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
    private String comContact;

    @Getter
    @Setter
    private String comDescription;

    @Getter
    @Setter
    private Set<Long> categoryIds;

}
