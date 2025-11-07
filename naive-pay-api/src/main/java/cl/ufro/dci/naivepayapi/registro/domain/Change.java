package cl.ufro.dci.naivepayapi.registro.domain;

import jakarta.persistence.*;
import lombok.Data;

import java.util.Date;

@Data
@Entity
public class Change {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	private Date dateChange;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
}