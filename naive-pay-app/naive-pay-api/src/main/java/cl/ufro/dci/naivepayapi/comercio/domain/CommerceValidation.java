package cl.ufro.dci.naivepayapi.comercio.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Entity
public class CommerceValidation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long comValId;

    @Embedded
	private CommerceInfo comInfo;

    @Enumerated(EnumType.STRING)
    private ValidationStatus comStatus;


    public CommerceValidation(CommerceInfo comInfo) {
        this.comInfo = comInfo;
        this.comStatus = ValidationStatus.PENDING;
    }


    public CommerceValidation() {}

	public void approve() {
        this.comStatus = ValidationStatus.VALIDATED;
	}

	public void reject() {
        this.comStatus = ValidationStatus.INVALID;
	}

}