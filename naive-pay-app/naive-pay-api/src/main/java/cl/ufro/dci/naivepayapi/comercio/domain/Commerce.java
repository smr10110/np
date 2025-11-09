package cl.ufro.dci.naivepayapi.comercio.domain;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Data;

import java.util.*;

@Data
@Entity
public class Commerce {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long comId;

    @Embedded
	private CommerceInfo comInfo;

    @Embedded
    private VerificationStatus comVerificationStatus;

    @ManyToMany
    @JoinTable(
            name = "commerce_category_join",
            joinColumns = @JoinColumn(name = "commerce_id"),
            inverseJoinColumns = @JoinColumn(name = "category_id")
    )
    @JsonBackReference
	private Set<CommerceCategory> comCategories =  new HashSet<>();

    public Commerce() {
        this.comInfo = new CommerceInfo();
        this.comVerificationStatus = new VerificationStatus();
        this.comCategories = new HashSet<>();
    }

    public Commerce(CommerceInfo info, VerificationStatus status, Set<CommerceCategory> categories){
        this.comInfo = info;
        this.comVerificationStatus = status;
        this.comCategories = (categories != null) ? new HashSet<>(comCategories) : new HashSet<>();
    }

    public Commerce(CommerceInfo info, Set<CommerceCategory> categories){
        this.comInfo = info;
        this.comVerificationStatus = new VerificationStatus();
        this.comCategories = (categories != null) ? new HashSet<>(comCategories) : new  HashSet<>();
    }


}