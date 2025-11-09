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
	private Long creId;
	private Date creCreationDate;
	private Boolean creDenied;
	private Boolean creActiveDinamicKey;

    @Lob
	private String crePrivateKeyRsa;
    @Lob
    private String crePublicKeyRsa;

    @OneToOne(mappedBy = "credencial")
    private User user;

    @OneToMany(mappedBy = "credencial", cascade = CascadeType.ALL, orphanRemoval = true)
	public List<Change> creChanges = new ArrayList<Change>();

}