package cl.ufro.dci.naivepayapi.registro.domain;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.util.Date;

@Data
@Entity
@Table(name = "app_user")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long useId;
	private String useNames;
	private String useLastNames;
	private Long useRutGeneral;
    private char useVerificationDigit;
    private LocalDate useBirthDate; // Se agrega la fecha de nacimiento
    private Long usePhoneNumber;
    private String useProfession;
    @Enumerated(EnumType.STRING)
	private AccountState useState;
	private String useAdress;


    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "credencial_id", referencedColumnName = "creId")
	public Credencial credencial;

    @OneToOne
    @JoinColumn(name = "register_id", referencedColumnName = "regId")
	public Register register;

}