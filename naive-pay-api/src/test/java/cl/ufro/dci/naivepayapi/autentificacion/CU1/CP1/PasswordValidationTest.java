package cl.ufro.dci.naivepayapi.autentificacion.CU1.CP1;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test para verificar que BCrypt valida correctamente contraseñas en texto plano
 * contra sus versiones hasheadas.
 */
class PasswordValidationTest {

    @Test
    void deberiaValidarPasswordCorrectaContraHash() {
        // ARRANGE: Crear encoder y hashear una contraseña
        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        String passwordTextoPlano = "MiPassword123!";
        String passwordHasheada = passwordEncoder.encode(passwordTextoPlano);

        // ACT: Validar que el texto plano coincide con el hash
        boolean esValida = passwordEncoder.matches(passwordTextoPlano, passwordHasheada);

        // ASSERT: Debe retornar true
        assertThat(esValida).isTrue();
    }
}
