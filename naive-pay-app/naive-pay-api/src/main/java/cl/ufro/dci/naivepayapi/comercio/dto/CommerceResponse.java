package cl.ufro.dci.naivepayapi.comercio.dto;

import cl.ufro.dci.naivepayapi.comercio.domain.CommerceInfo;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.Set;

public class CommerceResponse {

    @Getter
    @Setter
    private Long comId;

    @Getter
    @Setter
    private CommerceInfo comInfo;

    @Getter
    @Setter
    private boolean comIsVerified;

    @Getter
    @Setter
    private Date comValidUntil;

    @Getter
    @Setter
    private Set<String> comCategoriesNames;

    public CommerceResponse(){}

    public CommerceResponse(Long comId, CommerceInfo comInfo, boolean comIsVerified, Date comValidUntil, Set<String> comCategoriesNames) {
        this.comId = comId;
        this.comInfo = comInfo;
        this.comIsVerified = comIsVerified;
        this.comValidUntil = comValidUntil;
        this.comCategoriesNames = comCategoriesNames;
    }

}
