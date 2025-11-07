package cl.ufro.dci.naivepayapi.registro.dto;

import lombok.Data;

@Data
public class RegistrationCompleteDTO {
    private String email; // Para vincular con el registro iniciado
    private String names;
    private String lastNames;
    private Long rutGeneral;
    private char verificationDigit;
    private Long phoneNumber;
    private String profession;
    private String adress;

    private String fingerprint;
    private String os;
    private String type;
    private String browser;
}