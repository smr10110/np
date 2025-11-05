package cl.ufro.dci.naivepayapi.registro.domain;
import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "app_users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	private String names;
	private String lastNames;
	private Long rutGeneral;
    private Long phoneNumber;
    private String profession;
    @Enumerated(EnumType.STRING)
	private AccountState state;
	private String adress;
    private char verificationDigit;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "credencial_id", referencedColumnName = "id")
	public Credencial credencial;

    @OneToOne
    @JoinColumn(name = "register_id", referencedColumnName = "id")
	public Register register;

}