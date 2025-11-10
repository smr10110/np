package cl.ufro.dci.naivepayapi.autentificacion.dto;

import lombok.Data;

@Data
public class ResetPasswordRequest {
    private String email;
    private String code;
    private String newPassword;
}
