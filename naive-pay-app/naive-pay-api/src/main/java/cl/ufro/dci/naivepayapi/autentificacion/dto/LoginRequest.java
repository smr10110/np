package cl.ufro.dci.naivepayapi.autentificacion.dto;

import lombok.Data;

@Data
public class LoginRequest {
    private String identifier;   // Rut
    private String password;
}
