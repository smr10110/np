package cl.ufro.dci.naivepayapi.registro.domain;

import jakarta.persistence.*;
import lombok.Data;

import java.util.Date;

@Data
@Entity
public class Register {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	private Date registerDate;
	private boolean verified;
    private String verificationCode;
    private Date verificationCodeExpiration; // Se agrega fecha de vencimiento

    private String email;
    private String hashedLoginPassword;

    @OneToOne(mappedBy = "register")
    private User user;

}