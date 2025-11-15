package cl.ufro.dci.naivepayapi.registro.domain;

import jakarta.persistence.*;
import lombok.Data;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
@Entity
public class Register {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long regId;
	private Date regRegisterDate;
	private boolean regVerified;
    private String regVerificationCode;
    private Date regVerificationCodeExpiration; // Se agrega fecha de vencimiento

    private String regEmail;
    private String regHashedLoginPassword;

    @OneToOne(mappedBy = "register")
    private User user;

    @OneToMany(mappedBy = "register", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Change> regChanges = new ArrayList<>();

}