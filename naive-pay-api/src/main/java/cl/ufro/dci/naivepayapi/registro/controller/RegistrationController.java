package cl.ufro.dci.naivepayapi.registro.controller;

import cl.ufro.dci.naivepayapi.registro.dto.RegistrationCompleteDTO;
import cl.ufro.dci.naivepayapi.registro.dto.RegistrationStartDTO;
import cl.ufro.dci.naivepayapi.registro.dto.SetDynamicKeyDTO;
import cl.ufro.dci.naivepayapi.registro.service.RegisterService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/api/register")
public class RegistrationController {

    private final RegisterService registerService;

    public RegistrationController(RegisterService registerService) {
        this.registerService = registerService;
    }

    @PostMapping("/start")
    public ResponseEntity<?> startRegistration(@RequestBody RegistrationStartDTO startDTO) {
        try {
            registerService.startRegistration(startDTO);
            return ResponseEntity.ok().body(Map.of("message", "Código de verificación enviado a " + startDTO.getEmail()));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/verify")
    public ResponseEntity<?> verifyEmail(@RequestParam String email, @RequestParam String code) {
        try {
            registerService.verifyEmail(email, code);
            return ResponseEntity.ok().body(Map.of("message", "Email verificado correctamente."));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/resend-code")
    public ResponseEntity<?> resendVerificationCode(@RequestBody Map<String, String> payload) {
        try {
            String email = payload.get("email");
            registerService.resendVerificationCode(email);
            return ResponseEntity.ok().body(Map.of("message", "Nuevo código de verificación enviado a " + email));
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/complete")
    public ResponseEntity<?> completeRegistration(@RequestBody RegistrationCompleteDTO completeDTO) {
        try {
            registerService.completeRegistration(completeDTO);
            return ResponseEntity.status(201).body(Map.of("message", "Usuario registrado exitosamente."));
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/set-dynamic-key")
    public ResponseEntity<?> setDynamicKey(@RequestBody SetDynamicKeyDTO payload) {
        try {
            registerService.setDynamicKey(payload.getEmail(), payload.getDynamicKey());
            return ResponseEntity.ok(Map.of("message", "Clave dinámica creada exitosamente."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (RuntimeException e) { // Añade este bloque catch
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }
}
