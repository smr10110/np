package cl.ufro.dci.naivepayapi.registro.domain;

import jakarta.persistence.*;
import lombok.Data;

import java.util.Date;

@Data
@Entity
public class Change {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long chaId;
	private Date chaDateChange;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "credencial_id", referencedColumnName = "creId")
    private Credencial credencial;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "register_id", referencedColumnName = "regId")
    private Register register;
}