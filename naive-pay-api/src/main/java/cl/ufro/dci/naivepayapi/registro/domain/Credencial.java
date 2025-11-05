package cl.ufro.dci.naivepayapi.registro.domain;

import jakarta.persistence.*;
import lombok.Data;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
@Entity
public class Credencial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	private Date creationDate;
	private Boolean denied;
	private Boolean activeDinamicKey;

    @Lob
	private String privateKeyRsa;
    @Lob
    private String publicKeyRsa;

    @OneToOne(mappedBy = "credencial")
    private User user;

    @OneToMany
	public List<Change> changes = new ArrayList<Change>();

}