package cl.ufro.dci.naivepayapi.comercio.domain;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Data;

import java.util.List;

@Data
@Entity
public class CommerceCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long catId;

    private String catName;

    private String catDescription;

    @ManyToMany(mappedBy = "comCategories", cascade = CascadeType.ALL)
    @JsonManagedReference
    private List<Commerce> catCommerces;



    public CommerceCategory(String name, String description) {
        this.catName = name;
        this.catDescription = description;
    }

    public CommerceCategory() {

    }




}