package cl.ufro.dci.naivepayapi.autentificacion.CU1.CP3;

import cl.ufro.dci.naivepayapi.autentificacion.dto.LoginRequest;
import cl.ufro.dci.naivepayapi.autentificacion.repository.AuthAttemptRepository;
import cl.ufro.dci.naivepayapi.autentificacion.service.AccountLockService;
import cl.ufro.dci.naivepayapi.autentificacion.service.AuthAttemptService;
import cl.ufro.dci.naivepayapi.autentificacion.service.AuthService;
import cl.ufro.dci.naivepayapi.autentificacion.service.AuthSessionService;
import cl.ufro.dci.naivepayapi.autentificacion.service.JWTService;
import cl.ufro.dci.naivepayapi.dispositivos.service.DeviceService;
import cl.ufro.dci.naivepayapi.registro.domain.AccountState;
import cl.ufro.dci.naivepayapi.registro.domain.Register;
import cl.ufro.dci.naivepayapi.registro.domain.User;
import cl.ufro.dci.naivepayapi.registro.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Test para verificar login fallido con contraseña incorrecta.
 * CU1-CP3: Login con contraseña incorrecta
 */
@ExtendWith(MockitoExtension.class)
class LoginBadPasswordTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private DeviceService deviceService;

    @Mock
    private JWTService jwtService;

    @Mock
    private AccountLockService accountLockService;

    @Mock
    private AuthAttemptRepository authAttemptRepository;

    @Mock
    private AuthSessionService authSessionService;

    @Mock
    private AuthAttemptService authAttemptService;

    @InjectMocks
    private AuthService authService;

    private User testUser;
    private static final String TEST_EMAIL = "usuario@test.com";
    private static final String WRONG_PASSWORD = "ContraseñaMala123";
    private static final String TEST_FINGERPRINT = "device-fingerprint-123";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "maxFailedAttempts", 5);
        ReflectionTestUtils.setField(authService, "lockoutWindowMinutes", 30);

        testUser = new User();
        testUser.setUseId(1L);
        testUser.setUseState(AccountState.ACTIVE);

        Register register = new Register();
        register.setRegEmail(TEST_EMAIL);
        register.setRegHashedLoginPassword("$2a$10$hashedPassword");
        testUser.setRegister(register);
    }

    @Test
    void deberiaRechazarLoginConPasswordIncorrecta() {
        // ARRANGE
        LoginRequest request = new LoginRequest();
        request.setIdentifier(TEST_EMAIL);
        request.setPassword(WRONG_PASSWORD);

        // Simular que el usuario existe
        when(userRepository.findByRegisterRegEmail(TEST_EMAIL))
            .thenReturn(Optional.of(testUser));

        // Simular que la cuenta NO está bloqueada
        when(accountLockService.isAccountLocked(testUser))
            .thenReturn(false);

        // Simular que la contraseña NO coincide
        when(passwordEncoder.matches(WRONG_PASSWORD, testUser.getRegister().getRegHashedLoginPassword()))
            .thenReturn(false);

        // Simular que aún no se bloquea (menos de 5 intentos)
        when(accountLockService.checkAndBlockIfNeeded(testUser))
            .thenReturn(false);

        // ACT
        ResponseEntity<?> response = authService.login(request, TEST_FINGERPRINT);

        // ASSERT
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isInstanceOf(Map.class);

        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertThat(body.get("error")).isEqualTo("BAD_CREDENTIALS");
        assertThat(body).containsKey("remainingAttempts");

        // Verificar que se buscó el usuario
        verify(userRepository).findByRegisterRegEmail(TEST_EMAIL);

        // Verificar que se validó la contraseña
        verify(passwordEncoder).matches(WRONG_PASSWORD, testUser.getRegister().getRegHashedLoginPassword());

        // Verificar que se verificó si bloquear la cuenta
        verify(accountLockService).checkAndBlockIfNeeded(testUser);

        // Verificar que NO se generó token JWT
        verify(jwtService, never()).generate(any(), any(), any());

        // Verificar que NO se guardó sesión
        verify(authSessionService, never()).saveActiveSession(any(), any(), any(), any());
    }
}
