package cl.ufro.dci.naivepayapi.registro.dto;

import lombok.Data;

@Data
public class SetDynamicKeyDTO {
    private String email;
    private String dynamicKey;
}