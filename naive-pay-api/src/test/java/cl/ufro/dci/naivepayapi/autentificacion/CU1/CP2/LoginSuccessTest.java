package cl.ufro.dci.naivepayapi.autentificacion.CU1.CP2;

import cl.ufro.dci.naivepayapi.autentificacion.domain.Session;
import cl.ufro.dci.naivepayapi.autentificacion.dto.LoginRequest;
import cl.ufro.dci.naivepayapi.autentificacion.dto.LoginResponse;
import cl.ufro.dci.naivepayapi.autentificacion.repository.AuthAttemptRepository;
import cl.ufro.dci.naivepayapi.autentificacion.service.AccountLockService;
import cl.ufro.dci.naivepayapi.autentificacion.service.AuthAttemptService;
import cl.ufro.dci.naivepayapi.autentificacion.service.AuthService;
import cl.ufro.dci.naivepayapi.autentificacion.service.AuthSessionService;
import cl.ufro.dci.naivepayapi.autentificacion.service.JWTService;
import cl.ufro.dci.naivepayapi.dispositivos.domain.Device;
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

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Test para verificar login exitoso con credenciales válidas y dispositivo autorizado.
 * CU1-CP2: Login exitoso
 */
@ExtendWith(MockitoExtension.class)
class LoginSuccessTest {

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
    private Device testDevice;
    private static final String TEST_EMAIL = "usuario@test.com";
    private static final String TEST_PASSWORD = "Password123!";
    private static final String TEST_FINGERPRINT = "device-fingerprint-123";

    @BeforeEach
    void setUp() {
        // Configurar valores de configuración
        ReflectionTestUtils.setField(authService, "maxFailedAttempts", 5);
        ReflectionTestUtils.setField(authService, "lockoutWindowMinutes", 30);

        // Crear usuario de prueba
        testUser = new User();
        testUser.setUseId(1L);
        testUser.setUseState(AccountState.ACTIVE);

        Register register = new Register();
        register.setRegEmail(TEST_EMAIL);
        register.setRegHashedLoginPassword("$2a$10$hashedPassword");
        testUser.setRegister(register);

        // Crear dispositivo de prueba
        testDevice = new Device();
        testDevice.setFingerprint("$2a$10$hashedFingerprint");
        testDevice.setUser(testUser);
    }

    @Test
    void deberiaPermitirLoginConCredencialesValidasYDispositivoAutorizado() {
        // ARRANGE
        LoginRequest request = new LoginRequest();
        request.setIdentifier(TEST_EMAIL);
        request.setPassword(TEST_PASSWORD);

        // Simular que el usuario existe
        when(userRepository.findByRegisterRegEmail(TEST_EMAIL))
            .thenReturn(Optional.of(testUser));

        // Simular que la cuenta NO está bloqueada
        when(accountLockService.isAccountLocked(testUser))
            .thenReturn(false);

        // Simular que la contraseña es correcta
        when(passwordEncoder.matches(TEST_PASSWORD, testUser.getRegister().getRegHashedLoginPassword()))
            .thenReturn(true);

        // Simular generación de JWT
        when(jwtService.generate(anyString(), anyString(), anyString()))
            .thenReturn("jwt-token-123");

        when(jwtService.getUserId(anyString()))
            .thenReturn("1");

        when(jwtService.getExpiration(anyString()))
            .thenReturn(Instant.now().plusSeconds(900));

        // Simular que el dispositivo está autorizado
        when(deviceService.ensureAuthorizedDevice(1L, TEST_FINGERPRINT))
            .thenReturn(testDevice);

        // Simular guardado de sesión
        Session mockSession = new Session();
        mockSession.setSesId(1L);
        when(authSessionService.saveActiveSession(any(UUID.class), any(User.class), any(Device.class), any(Instant.class)))
            .thenReturn(mockSession);

        // ACT
        ResponseEntity<?> response = authService.login(request, TEST_FINGERPRINT);

        // ASSERT
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}