package cl.ufro.dci.naivepayapi.autentificacion.dto;

import lombok.Data;

@Data
public class LoginRequest {
    private String identifier;
    private String password;
}
