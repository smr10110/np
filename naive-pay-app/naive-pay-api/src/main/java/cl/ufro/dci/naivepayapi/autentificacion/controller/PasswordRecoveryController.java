package cl.ufro.dci.naivepayapi.autentificacion.controller;

import cl.ufro.dci.naivepayapi.autentificacion.dto.ForgotPasswordRequest;
import cl.ufro.dci.naivepayapi.autentificacion.dto.ResetPasswordRequest;
import cl.ufro.dci.naivepayapi.autentificacion.service.PasswordRecoveryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth/password")
@RequiredArgsConstructor
public class PasswordRecoveryController {

    private final PasswordRecoveryService passwordRecoveryService;

    @PostMapping("/request")
    public ResponseEntity<Map<String, String>> requestPasswordReset(@RequestBody ForgotPasswordRequest request) {
        passwordRecoveryService.sendRecoveryCode(request.getEmail());
        return ResponseEntity.ok(Map.of(
            "message", "Si el email existe, recibirás un código de recuperación"
        ));
    }

    @PostMapping("/reset")
    public ResponseEntity<Map<String, String>> resetPassword(@RequestBody ResetPasswordRequest request) {
        passwordRecoveryService.resetPassword(request.getEmail(), request.getCode(), request.getNewPassword());
        return ResponseEntity.ok(Map.of("message", "Contraseña actualizada exitosamente"));
    }
}
