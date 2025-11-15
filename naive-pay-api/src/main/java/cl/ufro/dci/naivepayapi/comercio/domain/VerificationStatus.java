package cl.ufro.dci.naivepayapi.comercio.domain;

import jakarta.persistence.Embeddable;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Data
@Embeddable
public class VerificationStatus {

	private boolean comIsVerified;
    private Date comVerificationDate;
	private Date comVerificationValidUntil;
	private int comCommerceId;

    public VerificationStatus() {
        this.comIsVerified = false;
    }

    public  VerificationStatus(boolean comIsVerified, Date comVerificationDate, Date comVerificationValidUntil) { // -> (..., String comLastValidatedBy)
        this.comIsVerified = false;
        this.comVerificationDate = comVerificationDate;
        this.comVerificationValidUntil = comVerificationValidUntil;
    }


    public void approve(Date validUntil ) {
        this.comIsVerified = true;
        this.comVerificationDate = new Date();
        this.comVerificationValidUntil = validUntil;
    }
}

