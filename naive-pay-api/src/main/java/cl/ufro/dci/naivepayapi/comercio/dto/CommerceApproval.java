package cl.ufro.dci.naivepayapi.comercio.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.Set;

public class CommerceApproval {

    @Getter
    @Setter
    private Set<Long> categoryIds;

    public CommerceApproval() {}

    public CommerceApproval(Set<Long> categoryIds) {
        this.categoryIds = categoryIds;
    }

}
