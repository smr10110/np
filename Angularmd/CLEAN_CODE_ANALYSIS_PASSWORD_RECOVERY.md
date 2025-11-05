# 🎯 Análisis Clean Code & SOLID - Password Recovery System

**Fecha:** 2025-11-02
**Alcance:** Módulo de Recuperación de Contraseña + Arquitectura de Seguridad
**Framework:** Spring Boot 3.5.6 + Java 21

---

## 📚 Principios Evaluados

| Principio | Definición | Peso |
|-----------|------------|------|
| **SOLID** | Single Responsibility, Open/Closed, Liskov Substitution, Interface Segregation, Dependency Inversion | 30% |
| **Clean Code** | Nombres descriptivos, funciones pequeñas, comentarios mínimos | 25% |
| **DRY** | Don't Repeat Yourself | 15% |
| **KISS** | Keep It Simple, Stupid | 10% |
| **YAGNI** | You Aren't Gonna Need It | 5% |
| **Seguridad** | OWASP, validaciones, manejo de errores | 15% |

---

## 🔍 ANÁLISIS POR PRINCIPIOS SOLID

### 1️⃣ **Single Responsibility Principle (SRP)**

> "Una clase debe tener una única razón para cambiar"

#### ✅ **CUMPLE: PasswordRecoveryService**

**Archivo:** `PasswordRecoveryService.java`

**Responsabilidad:** Gestionar lógica de recuperación de contraseña

```java
@Service
public class PasswordRecoveryService {
    // ✅ Solo se encarga de recuperación de contraseña
    public void sendRecoveryCode(String email) { }
    public void verifyCode(String email, String code) { }
    public void resetPassword(String email, String code, String newPassword) { }
    private PasswordRecovery validateRecoveryCode(String email, String code) { }
    private String generateCode() { }
}
```

**Análisis:**
- ✅ **Cohesión alta:** Todos los métodos relacionados con recovery
- ✅ **Acoplamiento bajo:** Usa inyección de dependencias
- ✅ **Una razón para cambiar:** Si cambia la lógica de recovery

**Puntuación SRP:** 9/10

---

#### ⚠️ **VIOLACIÓN PARCIAL: EmailService**

**Archivo:** `EmailService.java`

**Problema:**
```java
@Service
public class EmailService {
    public void sendVerificationEmail(String to, String code) { }     // ← Registro
    public void sendDeviceRecoveryEmail(String to, String code) { }   // ← Dispositivos
    public void sendPasswordRecoveryEmail(String to, String code) { } // ← Contraseña
}
```

**Violación:**
- ❌ **3 responsabilidades diferentes** (verificación, dispositivos, password)
- ❌ **Cambiará por 3 razones distintas** (cambio en cualquier módulo)
- ❌ **No sigue el módulo al que pertenece** (está en `registro` pero se usa en `autentificación`)

**Refactorización Recomendada:**

**Opción A: Extracción por contexto**
```java
// Módulo: registro/service/
@Service
public class RegistrationEmailService {
    public void sendVerificationEmail(String to, String code) { }
}

// Módulo: autentificacion/service/
@Service
public class AuthenticationEmailService {
    public void sendPasswordRecoveryEmail(String to, String code) { }
    public void sendPasswordChangedNotification(String to) { }
    public void sendDeviceRecoveryEmail(String to, String code) { }
}
```

**Opción B: Template genérico (mejor)**
```java
@Service
public class EmailService {
    private final JavaMailSender mailSender;

    // Método genérico
    public void sendEmail(EmailTemplate template, String to, Map<String, String> params) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject(template.getSubject());
        message.setText(template.format(params));
        mailSender.send(message);
    }
}

// Enum con templates
public enum EmailTemplate {
    PASSWORD_RECOVERY(
        "Recuperación de Contraseña - NaivePay",
        "Hola,\n\nRecibimos una solicitud para restablecer tu contraseña.\n" +
        "Usa el siguiente código: {code}\n\n" +
        "Este código expira en {expirationMinutes} minutos."
    ),
    VERIFICATION(
        "Código de Verificación",
        "Tu código de verificación es: {code}"
    );

    private final String subject;
    private final String template;

    public String format(Map<String, String> params) {
        String result = template;
        for (Map.Entry<String, String> entry : params.entrySet()) {
            result = result.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return result;
    }
}

// Uso:
emailService.sendEmail(
    EmailTemplate.PASSWORD_RECOVERY,
    email,
    Map.of("code", code, "expirationMinutes", "15")
);
```

**Impacto:** 🟡 MEDIO
**Esfuerzo:** 🟡 Medio (2-3 horas)
**Puntuación SRP:** 5/10

---

#### ❌ **VIOLACIÓN GRAVE: AuthController**

**Archivo:** `AuthController.java`

**Problema:**
```java
@RestController
@RequestMapping("/auth")
public class AuthController {
    @PostMapping("/login") { }           // ← Autenticación
    @PostMapping("/logout") { }          // ← Sesiones
    @PostMapping("/password/request") { } // ← Recuperación de contraseña
    @PostMapping("/password/verify") { }
    @PostMapping("/password/reset") { }
}
```

**Violación:**
- ❌ **2 responsabilidades:** Autenticación + Recuperación de contraseña
- ❌ **Cambiará por múltiples razones**

**Refactorización Recomendada:**

```java
// Archivo: AuthController.java
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest req,
                                   @RequestHeader(value = "X-Device-Fingerprint", required = false) String deviceFingerprint) {
        return authService.login(req, deviceFingerprint);
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout(@RequestHeader("Authorization") String authHeader) {
        return authService.logout(authHeader);
    }
}

// Archivo NUEVO: PasswordRecoveryController.java
@RestController
@RequestMapping("/auth/password")
@RequiredArgsConstructor
public class PasswordRecoveryController {
    private final PasswordRecoveryService passwordRecoveryService;

    @PostMapping("/request")
    public ResponseEntity<ApiResponse<Void>> requestPasswordReset(@Valid @RequestBody ForgotPasswordRequest request) {
        passwordRecoveryService.sendRecoveryCode(request.getEmail());
        return ResponseEntity.ok(ApiResponse.success(
            "Si el email existe, recibirás un código de recuperación"
        ));
    }

    @PostMapping("/verify")
    public ResponseEntity<ApiResponse<Void>> verifyCode(@Valid @RequestBody ResetPasswordRequest request) {
        passwordRecoveryService.verifyCode(request.getEmail(), request.getCode());
        return ResponseEntity.ok(ApiResponse.success(
            "Código verificado correctamente"
        ));
    }

    @PostMapping("/reset")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        passwordRecoveryService.resetPassword(
            request.getEmail(),
            request.getCode(),
            request.getNewPassword()
        );
        return ResponseEntity.ok(ApiResponse.success(
            "Contraseña actualizada exitosamente"
        ));
    }
}
```

**Beneficios:**
- ✅ **Separación clara de responsabilidades**
- ✅ **Rutas más semánticas:** `/auth/password/request` vs `/auth/password/request`
- ✅ **Más fácil de testear**
- ✅ **Más fácil de mantener**

**Impacto:** 🔴 ALTO
**Esfuerzo:** 🟢 Bajo (30 min)
**Puntuación SRP:** 4/10 → 9/10

---

### 2️⃣ **Open/Closed Principle (OCP)**

> "Las clases deben estar abiertas a extensión pero cerradas a modificación"

#### ❌ **VIOLACIÓN: validateRecoveryCode()**

**Archivo:** `PasswordRecoveryService.java:95-113`

**Problema:**
```java
private PasswordRecovery validateRecoveryCode(String email, String code) {
    User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "INVALID_CODE"));

    PasswordRecovery recovery = passwordRecoveryRepository.findByUser_IdAndPasCode(user.getId(), code)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "INVALID_CODE"));

    if (recovery.getPasStatus() != PasswordRecoveryStatus.PENDING) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "CODE_ALREADY_USED");
    }

    if (recovery.getPasExpired().isBefore(Instant.now())) {
        recovery.setPasStatus(PasswordRecoveryStatus.EXPIRED);
        passwordRecoveryRepository.save(recovery);
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "CODE_EXPIRED");
    }

    return recovery;
}
```

**Violación:**
- ❌ **Si quieres agregar nueva validación** (ej: validar intentos fallidos), hay que **modificar este método**
- ❌ **Validaciones hardcodeadas**

**Refactorización con Chain of Responsibility:**

```java
// Interface de validación
public interface RecoveryCodeValidator {
    void validate(PasswordRecovery recovery, User user);
}

// Validadores concretos
@Component
public class CodeStatusValidator implements RecoveryCodeValidator {
    @Override
    public void validate(PasswordRecovery recovery, User user) {
        if (recovery.getPasStatus() != PasswordRecoveryStatus.PENDING) {
            throw new InvalidRecoveryCodeException("CODE_ALREADY_USED");
        }
    }
}

@Component
public class CodeExpirationValidator implements RecoveryCodeValidator {
    @Override
    public void validate(PasswordRecovery recovery, User user) {
        if (recovery.getPasExpired().isBefore(Instant.now())) {
            recovery.setPasStatus(PasswordRecoveryStatus.EXPIRED);
            throw new InvalidRecoveryCodeException("CODE_EXPIRED");
        }
    }
}

// Nueva validación sin modificar código existente
@Component
public class MaxAttemptsValidator implements RecoveryCodeValidator {
    @Override
    public void validate(PasswordRecovery recovery, User user) {
        if (recovery.getPasResendCount() > 3) {
            throw new InvalidRecoveryCodeException("TOO_MANY_ATTEMPTS");
        }
    }
}

// Servicio refactorizado
@Service
@RequiredArgsConstructor
public class PasswordRecoveryService {
    private final List<RecoveryCodeValidator> validators;

    private PasswordRecovery validateRecoveryCode(String email, String code) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new InvalidRecoveryCodeException("INVALID_CODE"));

        PasswordRecovery recovery = passwordRecoveryRepository.findByUser_IdAndPasCode(user.getId(), code)
                .orElseThrow(() -> new InvalidRecoveryCodeException("INVALID_CODE"));

        // ✅ Extensible sin modificación
        validators.forEach(validator -> validator.validate(recovery, user));

        return recovery;
    }
}
```

**Beneficios:**
- ✅ **Agregar validaciones sin modificar código existente**
- ✅ **Testeable independientemente**
- ✅ **Desacoplado**

**Impacto:** 🟡 MEDIO
**Esfuerzo:** 🟡 Medio (2 horas)
**Puntuación OCP:** 5/10 → 9/10

---

### 3️⃣ **Liskov Substitution Principle (LSP)**

> "Los objetos de una clase derivada deben poder reemplazar a objetos de la clase base sin alterar el comportamiento"

#### ✅ **CUMPLE**

**Análisis:**
- No hay herencia en el código de recuperación de contraseña
- Las interfaces (`JpaRepository`, `OncePerRequestFilter`) se respetan correctamente
- No hay sobreescritura de comportamientos

**Puntuación LSP:** 10/10 (N/A)

---

### 4️⃣ **Interface Segregation Principle (ISP)**

> "Los clientes no deberían depender de interfaces que no usan"

#### ⚠️ **VIOLACIÓN MENOR: ResetPasswordRequest**

**Archivo:** `ResetPasswordRequest.java`, `AuthController.java:42`

**Problema:**
```java
@Data
public class ResetPasswordRequest {
    private String email;
    private String code;
    private String newPassword;
}

// Usado en /password/verify
@PostMapping("/password/verify")
public ResponseEntity<Map<String, String>> verifyCode(@RequestBody ResetPasswordRequest request) {
    passwordRecoveryService.verifyCode(request.getEmail(), request.getCode());
    // ❌ newPassword no se usa aquí, pero el DTO lo requiere
}
```

**Violación:**
- ❌ `/password/verify` no necesita `newPassword` pero usa `ResetPasswordRequest`
- ❌ El frontend debe enviar `newPassword: ""` aunque no se use

**Refactorización:**

```java
// DTO específico para verificar
@Data
public class VerifyCodeRequest {
    @NotBlank(message = "El email es obligatorio")
    @Email(message = "Email inválido")
    private String email;

    @NotBlank(message = "El código es obligatorio")
    @Pattern(regexp = "^\\d{6}$", message = "El código debe tener 6 dígitos")
    private String code;
}

// DTO para reset (hereda de verify)
@Data
@EqualsAndHashCode(callSuper = true)
public class ResetPasswordRequest extends VerifyCodeRequest {
    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres")
    private String newPassword;
}

// Controller
@PostMapping("/password/verify")
public ResponseEntity<ApiResponse<Void>> verifyCode(@Valid @RequestBody VerifyCodeRequest request) {
    passwordRecoveryService.verifyCode(request.getEmail(), request.getCode());
    return ResponseEntity.ok(ApiResponse.success("Código verificado"));
}

@PostMapping("/password/reset")
public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
    passwordRecoveryService.resetPassword(
        request.getEmail(),
        request.getCode(),
        request.getNewPassword()
    );
    return ResponseEntity.ok(ApiResponse.success("Contraseña actualizada"));
}
```

**Impacto:** 🟢 BAJO
**Esfuerzo:** 🟢 Bajo (20 min)
**Puntuación ISP:** 6/10 → 10/10

---

### 5️⃣ **Dependency Inversion Principle (DIP)**

> "Depender de abstracciones, no de concreciones"

#### ✅ **CUMPLE: PasswordRecoveryService**

```java
@Service
@RequiredArgsConstructor
public class PasswordRecoveryService {
    private final PasswordRecoveryRepository passwordRecoveryRepository; // ✅ Interface
    private final UserRepository userRepository;                         // ✅ Interface
    private final PasswordEncoder passwordEncoder;                       // ✅ Interface
    private final EmailService emailService;                             // ❌ Clase concreta
}
```

**Análisis:**
- ✅ **3/4 dependencias son interfaces**
- ❌ `EmailService` es clase concreta (debería ser `IEmailService` o `EmailSender`)

**Refactorización:**

```java
// Interface
public interface EmailSender {
    void sendEmail(String to, String subject, String body);
}

// Implementación SMTP
@Service
public class SmtpEmailSender implements EmailSender {
    private final JavaMailSender mailSender;

    @Override
    public void sendEmail(String to, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);
        mailSender.send(message);
    }
}

// Implementación Mock (para testing)
@Profile("test")
@Service
public class MockEmailSender implements EmailSender {
    @Override
    public void sendEmail(String to, String subject, String body) {
        System.out.println("Mock email sent to: " + to);
    }
}

// Service refactorizado
@Service
@RequiredArgsConstructor
public class PasswordRecoveryService {
    private final PasswordRecoveryRepository passwordRecoveryRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailSender emailSender; // ✅ Interface
}
```

**Beneficios:**
- ✅ **Fácil cambiar de proveedor de email** (SendGrid, AWS SES, etc.)
- ✅ **Testeable con mocks**
- ✅ **Desacoplado**

**Impacto:** 🟡 MEDIO
**Esfuerzo:** 🟢 Bajo (1 hora)
**Puntuación DIP:** 7/10 → 10/10

---

## 📖 ANÁLISIS CLEAN CODE

### 1. **Nombres Descriptivos**

#### ✅ **BIEN: Métodos del servicio**

```java
public void sendRecoveryCode(String email)              // ✅ Claro
public void verifyCode(String email, String code)       // ✅ Claro
public void resetPassword(String email, String code, String newPassword) // ✅ Claro
private PasswordRecovery validateRecoveryCode(String email, String code) // ✅ Claro
private String generateCode()                           // ✅ Claro
```

**Puntuación:** 10/10

---

#### ❌ **MAL: Nombres de campos en PasswordRecovery**

```java
private String pasCode;        // ❌ Prefijo "pas" innecesario
private Instant pasCreated;    // ❌ Hungarian notation
private Instant pasExpired;    // ❌ "Expired" debería ser "ExpiresAt"
private Integer pasResendCount;
```

**Refactorización:**
```java
private String code;           // ✅ Contexto obvio (clase PasswordRecovery)
private Instant createdAt;     // ✅ Estándar
private Instant expiresAt;     // ✅ Más claro
private Integer resendCount;   // ✅ Sin prefijo
```

**Puntuación:** 4/10 → 10/10

---

#### ❌ **MAL: Constantes genéricas**

```java
private static final int CODE_EXPIRATION_MINUTES = 15; // ✅ Bueno
return String.format("%06d", SECURE_RANDOM.nextInt(1000000)); // ❌ 1000000 es mágico
```

**Refactorización:**
```java
private static final int CODE_EXPIRATION_MINUTES = 15;
private static final int CODE_LENGTH = 6;
private static final int CODE_MAX_VALUE = 1_000_000; // ✅ 10^6

private String generateCode() {
    return String.format("%0" + CODE_LENGTH + "d", SECURE_RANDOM.nextInt(CODE_MAX_VALUE));
}
```

**Puntuación:** 6/10 → 10/10

---

### 2. **Funciones Pequeñas**

> "Una función debe hacer una cosa, hacerla bien, y solo eso"

#### ✅ **CUMPLE: La mayoría de métodos**

```java
@Transactional
public void verifyCode(String email, String code) {
    validateRecoveryCode(email, code); // ✅ 1 línea, hace 1 cosa
}

private String generateCode() {
    return String.format("%06d", SECURE_RANDOM.nextInt(1000000)); // ✅ 1 línea
}
```

**Puntuación:** 9/10

---

#### ⚠️ **MEJORABLE: sendRecoveryCode()**

**Archivo:** `PasswordRecoveryService.java:37-70`

```java
@Transactional
public void sendRecoveryCode(String email) {
    // 1. Validar email
    var userOpt = userRepository.findByEmail(email);
    if (userOpt.isEmpty()) {
        logger.debug("Intento de recuperación para email no existente: {}", email);
        return;
    }
    User user = userOpt.get();

    // 2. Invalidar códigos anteriores
    passwordRecoveryRepository.findLatestByUserIdAndStatus(user.getId(), PasswordRecoveryStatus.PENDING)
            .ifPresent(oldRecovery -> {
                oldRecovery.setPasStatus(PasswordRecoveryStatus.EXPIRED);
                passwordRecoveryRepository.save(oldRecovery);
            });

    // 3. Generar código
    String code = generateCode();
    Instant now = Instant.now();
    Instant expiration = now.plus(CODE_EXPIRATION_MINUTES, ChronoUnit.MINUTES);

    // 4. Crear recovery
    PasswordRecovery recovery = PasswordRecovery.builder()
            .user(user)
            .pasCode(code)
            .pasCreated(now)
            .pasExpired(expiration)
            .pasLastSent(now)
            .pasResendCount(0)
            .pasStatus(PasswordRecoveryStatus.PENDING)
            .build();

    // 5. Guardar y enviar email
    passwordRecoveryRepository.save(recovery);
    emailService.sendPasswordRecoveryEmail(email, code);
    logger.debug("Código generado: {}", code);
}
```

**Problema:**
- ❌ **Hace 5 cosas** (validar, invalidar, generar, crear, enviar)
- ❌ **33 líneas** (debería ser <20)

**Refactorización:**

```java
@Transactional
public void sendRecoveryCode(String email) {
    User user = findUserByEmailOrSkip(email);
    if (user == null) return;

    invalidatePendingRecoveryCodes(user);

    PasswordRecovery recovery = createRecoveryCode(user);
    passwordRecoveryRepository.save(recovery);

    sendRecoveryEmail(email, recovery.getCode());
}

private User findUserByEmailOrSkip(String email) {
    return userRepository.findByEmail(email)
            .orElseGet(() -> {
                logger.debug("Intento de recuperación para email no existente: {}", email);
                return null;
            });
}

private void invalidatePendingRecoveryCodes(User user) {
    passwordRecoveryRepository
            .findLatestByUserIdAndStatus(user.getId(), PasswordRecoveryStatus.PENDING)
            .ifPresent(oldRecovery -> {
                oldRecovery.setPasStatus(PasswordRecoveryStatus.EXPIRED);
                passwordRecoveryRepository.save(oldRecovery);
            });
}

private PasswordRecovery createRecoveryCode(User user) {
    String code = generateCode();
    Instant now = Instant.now();

    return PasswordRecovery.builder()
            .user(user)
            .pasCode(code)
            .pasCreated(now)
            .pasExpired(now.plus(CODE_EXPIRATION_MINUTES, ChronoUnit.MINUTES))
            .pasLastSent(now)
            .pasResendCount(0)
            .pasStatus(PasswordRecoveryStatus.PENDING)
            .build();
}

private void sendRecoveryEmail(String email, String code) {
    emailService.sendPasswordRecoveryEmail(email, code);
    logger.debug("Código de recuperación enviado");
}
```

**Beneficios:**
- ✅ **Método principal:** 7 líneas (vs 33)
- ✅ **Cada método hace 1 cosa**
- ✅ **Más fácil de testear**
- ✅ **Nivel de abstracción consistente**

**Puntuación:** 6/10 → 9/10

---

### 3. **Comentarios Mínimos**

> "El código debe explicarse a sí mismo"

#### ✅ **CUMPLE: Mayoría del código**

```java
// ✅ Sin comentarios innecesarios
@Transactional
public void resetPassword(String email, String code, String newPassword) {
    PasswordRecovery recovery = validateRecoveryCode(email, code);
    User user = recovery.getUser();

    user.getRegister().setHashedLoginPassword(passwordEncoder.encode(newPassword));
    recovery.setPasStatus(PasswordRecoveryStatus.USED);
    recovery.setPasUsed(Instant.now());

    if (user.getState() == AccountState.INACTIVE) {
        user.setState(AccountState.ACTIVE);
        logger.info("Cuenta desbloqueada tras recuperación: userId={}", user.getId());
    }

    logger.info("Contraseña actualizada para usuario {}", user.getId());
}
```

**Análisis:**
- ✅ **Código autodescriptivo**
- ✅ **Nombres claros**
- ✅ **Sin comentarios obvios**

**Puntuación:** 10/10

---

#### ⚠️ **COMENTARIO INNECESARIO**

```java
// Invalidar códigos PENDING anteriores  // ❌ Obvio por el nombre del método
passwordRecoveryRepository.findLatestByUserIdAndStatus(user.getId(), PasswordRecoveryStatus.PENDING)
    .ifPresent(oldRecovery -> {
        oldRecovery.setPasStatus(PasswordRecoveryStatus.EXPIRED);
        passwordRecoveryRepository.save(oldRecovery);
    });
```

**Refactorización:**
```java
// Sin comentario, método privado explica el intent
invalidatePendingRecoveryCodes(user);
```

**Puntuación:** 8/10 → 10/10

---

### 4. **Manejo de Errores**

#### ❌ **VIOLACIÓN: ResponseStatusException genérica**

```java
throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "INVALID_CODE");
throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "CODE_EXPIRED");
throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "CODE_ALREADY_USED");
```

**Problemas:**
- ❌ **No es específica** (RuntimeException genérica)
- ❌ **Dificulta testing** (hay que capturar ResponseStatusException)
- ❌ **No es semántica**

**Refactorización con Excepciones Custom:**

```java
// Excepciones custom
public class InvalidRecoveryCodeException extends RuntimeException {
    private final RecoveryCodeError error;

    public InvalidRecoveryCodeException(RecoveryCodeError error) {
        super(error.getMessage());
        this.error = error;
    }
}

public enum RecoveryCodeError {
    INVALID_CODE("El código ingresado es inválido"),
    CODE_EXPIRED("El código ha expirado. Solicita uno nuevo"),
    CODE_ALREADY_USED("Este código ya fue utilizado"),
    USER_NOT_FOUND("Usuario no encontrado");

    private final String message;

    RecoveryCodeError(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}

// Uso
throw new InvalidRecoveryCodeException(RecoveryCodeError.INVALID_CODE);

// GlobalExceptionHandler
@ExceptionHandler(InvalidRecoveryCodeException.class)
public ResponseEntity<ErrorResponse> handleInvalidRecoveryCode(InvalidRecoveryCodeException ex) {
    return ResponseEntity
            .badRequest()
            .body(new ErrorResponse(
                ex.getError().name(),
                ex.getMessage(),
                Instant.now()
            ));
}
```

**Beneficios:**
- ✅ **Type-safe**
- ✅ **Fácil de testear**
- ✅ **Mensajes centralizados**
- ✅ **Semántico**

**Puntuación:** 5/10 → 10/10

---

## 🔁 ANÁLISIS DRY (Don't Repeat Yourself)

### ✅ **CUMPLE: validateRecoveryCode() extraído**

**Antes (código duplicado):**
```java
// En verifyCode():
User user = userRepository.findByEmail(email).orElseThrow(...);
PasswordRecovery recovery = passwordRecoveryRepository.findByUser_IdAndPasCode(...).orElseThrow(...);
if (recovery.getPasStatus() != PasswordRecoveryStatus.PENDING) { throw ... }
if (recovery.getPasExpired().isBefore(Instant.now())) { throw ... }

// En resetPassword():
User user = userRepository.findByEmail(email).orElseThrow(...);  // ← DUPLICADO
PasswordRecovery recovery = passwordRecoveryRepository.findByUser_IdAndPasCode(...).orElseThrow(...); // ← DUPLICADO
if (recovery.getPasStatus() != PasswordRecoveryStatus.PENDING) { throw ... } // ← DUPLICADO
if (recovery.getPasExpired().isBefore(Instant.now())) { throw ... } // ← DUPLICADO
```

**Después (refactorizado):**
```java
@Transactional
public void verifyCode(String email, String code) {
    validateRecoveryCode(email, code); // ✅ Reutiliza
}

@Transactional
public void resetPassword(String email, String code, String newPassword) {
    PasswordRecovery recovery = validateRecoveryCode(email, code); // ✅ Reutiliza
    // ...
}

private PasswordRecovery validateRecoveryCode(String email, String code) {
    // Lógica centralizada
}
```

**Puntuación DRY:** 10/10 ✅

---

### ❌ **VIOLACIÓN: Duplicación en PUBLIC_PATHS y PUBLIC_ENDPOINTS**

**Archivos:**
- `JwtAuthFilter.java:38-45`
- `SecurityConfig.java:26-34`

```java
// JwtAuthFilter.java
private static final String[] PUBLIC_PATHS = {
    "/h2-console/**",
    "/api/register/**",
    "/auth/password/**",
    "/auth/login",
    "/api/devices/recover/**",
    "/api/dispositivos/recover/**"
};

// SecurityConfig.java (DUPLICADO)
private static final String[] PUBLIC_ENDPOINTS = {
    "/h2-console/**",
    "/api/register/**",
    "/auth/login",
    "/auth/recovery/**",
    "/auth/password/**",
    "/api/dispositivos/recover/**",
    "/api/devices/recover/**"
};
```

**Refactorización (ver SOLID - SRP):**
```java
@Component
public class SecurityConstants {
    public static final String[] PUBLIC_ENDPOINTS = { /* ... */ };
}
```

**Puntuación:** 3/10 → 10/10

---

## 💡 ANÁLISIS KISS (Keep It Simple, Stupid)

### ✅ **CUMPLE: generateCode()**

```java
private String generateCode() {
    return String.format("%06d", SECURE_RANDOM.nextInt(1000000)); // ✅ Simple y efectivo
}
```

**Puntuación:** 10/10

---

### ❌ **VIOLACIÓN: Endpoint /password/verify redundante**

```java
@PostMapping("/password/verify")
public ResponseEntity<Map<String, String>> verifyCode(@RequestBody ResetPasswordRequest request) {
    passwordRecoveryService.verifyCode(request.getEmail(), request.getCode());
    return ResponseEntity.ok(Map.of("message", "Código verificado correctamente"));
}
```

**Análisis:**
- ❌ **Agrega complejidad innecesaria**
- ❌ **Petición HTTP extra**
- ❌ `/password/reset` ya valida el código

**Opción 1: Eliminar** (más simple)
```java
// Solo tener:
// POST /password/request → Envía código
// POST /password/reset → Valida código + cambia contraseña
```

**Opción 2: Mantener si UI lo necesita** (justificado)
- Pantalla 2 del PDF solo valida código
- Usuario sabe si código es válido antes de crear password

**Puntuación:** 7/10 (depende de requisitos UI)

---

## 🚫 ANÁLISIS YAGNI (You Aren't Gonna Need It)

### ✅ **CUMPLE: Sin código innecesario**

**Análisis:**
- ✅ No hay métodos sin usar
- ✅ No hay campos sin usar
- ✅ No hay abstracciones prematuras

**Puntuación:** 10/10

---

## 🔐 ANÁLISIS SEGURIDAD (OWASP)

### 🔴 **CRÍTICO: Sin validaciones en DTOs** (ya mencionado)

**Ver:** Problema #1 en análisis anterior

**Puntuación:** 2/10 → 9/10 (con validaciones)

---

### 🔴 **CRÍTICO: Sin rate limiting** (ya mencionado)

**Ver:** Problema #4 en análisis anterior

**Puntuación:** 0/10 → 9/10 (con rate limiting)

---

### 🟡 **MEDIO: Timing attack en findByEmail**

**Archivo:** `PasswordRecoveryService.java:38-42`

```java
var userOpt = userRepository.findByEmail(email);
if (userOpt.isEmpty()) {
    logger.debug("Intento de recuperación para email no existente: {}", email);
    return; // ✅ No revela existencia
}
```

**Análisis:**
- ✅ **No revela si email existe** (buena práctica)
- ⚠️ **Posible timing attack:** Query a BD tarda más si email existe

**Mitigación (opcional):**
```java
@Transactional
public void sendRecoveryCode(String email) {
    User user = userRepository.findByEmail(email).orElse(null);

    String code = generateCode();
    Instant now = Instant.now();

    // Siempre ejecuta mismas operaciones (timing constante)
    if (user != null) {
        invalidatePendingRecoveryCodes(user);
        PasswordRecovery recovery = createRecoveryCode(user);
        passwordRecoveryRepository.save(recovery);
        emailService.sendPasswordRecoveryEmail(email, code);
    } else {
        // Simular trabajo para timing constante
        Thread.sleep(50); // Tiempo promedio de BD + email
    }

    logger.debug("Solicitud de recuperación procesada");
}
```

**Nota:** Esto es **paranoia de seguridad**. Para la mayoría de aplicaciones, el enfoque actual es suficiente.

**Puntuación:** 8/10

---

## 📊 RESUMEN DE PUNTUACIONES

| Categoría | Puntuación Actual | Puntuación Ideal | Diferencia |
|-----------|-------------------|------------------|------------|
| **SRP** | 6.0/10 | 9.5/10 | -3.5 |
| **OCP** | 5.0/10 | 9.0/10 | -4.0 |
| **LSP** | 10.0/10 | 10.0/10 | ✅ |
| **ISP** | 6.0/10 | 10.0/10 | -4.0 |
| **DIP** | 7.0/10 | 10.0/10 | -3.0 |
| **Nombres** | 6.7/10 | 10.0/10 | -3.3 |
| **Funciones** | 7.5/10 | 9.5/10 | -2.0 |
| **Comentarios** | 9.0/10 | 10.0/10 | -1.0 |
| **Errores** | 5.0/10 | 10.0/10 | -5.0 |
| **DRY** | 6.5/10 | 10.0/10 | -3.5 |
| **KISS** | 7.0/10 | 10.0/10 | -3.0 |
| **YAGNI** | 10.0/10 | 10.0/10 | ✅ |
| **Seguridad** | 3.3/10 | 9.0/10 | -5.7 |

**Puntuación General:** **6.8/10**

---

## 📋 PLAN DE REFACTORIZACIÓN (Clean Code + SOLID)

### 🔴 FASE 1: Seguridad Crítica (Día 1 - 4 horas)

#### **1.1 Validaciones en DTOs** (30 min)
```java
// ForgotPasswordRequest.java
@Data
public class ForgotPasswordRequest {
    @NotBlank(message = "El email es obligatorio")
    @Email(message = "Email inválido")
    private String email;
}

// VerifyCodeRequest.java (nuevo)
@Data
public class VerifyCodeRequest {
    @NotBlank @Email
    private String email;

    @NotBlank
    @Pattern(regexp = "^\\d{6}$", message = "El código debe tener 6 dígitos")
    private String code;
}

// ResetPasswordRequest.java
@Data
@EqualsAndHashCode(callSuper = true)
public class ResetPasswordRequest extends VerifyCodeRequest {
    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 8, max = 100, message = "La contraseña debe tener entre 8 y 100 caracteres")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).*$",
             message = "La contraseña debe contener mayúsculas, minúsculas y números")
    private String newPassword;
}

// AuthController - Agregar @Valid
@PostMapping("/password/request")
public ResponseEntity<?> requestPasswordReset(@Valid @RequestBody ForgotPasswordRequest request) {
    // ...
}
```

**Archivos:**
- `ForgotPasswordRequest.java` - MODIFICAR
- `VerifyCodeRequest.java` - CREAR
- `ResetPasswordRequest.java` - MODIFICAR
- `AuthController.java` - MODIFICAR (agregar @Valid)

**Tests:**
```java
@Test
void shouldRejectInvalidEmail() {
    // email vacío, null, formato inválido
}

@Test
void shouldRejectInvalidCode() {
    // código con letras, menos de 6 dígitos, más de 6
}

@Test
void shouldRejectWeakPassword() {
    // < 8 chars, sin mayúsculas, sin números
}
```

---

#### **1.2 Rate Limiting** (2 horas)
```java
// Dependencia (pom.xml)
<dependency>
    <groupId>com.bucket4j</groupId>
    <artifactId>bucket4j-core</artifactId>
    <version>8.5.0</version>
</dependency>

// RateLimitService.java (nuevo)
@Service
public class RateLimitService {
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    public boolean allowRequest(String key, int maxRequests, int minutes) {
        Bucket bucket = buckets.computeIfAbsent(key, k ->
            Bucket.builder()
                .addLimit(Bandwidth.simple(maxRequests, Duration.ofMinutes(minutes)))
                .build()
        );
        return bucket.tryConsume(1);
    }
}

// AuthController - Aplicar rate limit
@PostMapping("/password/request")
public ResponseEntity<?> requestPasswordReset(@Valid @RequestBody ForgotPasswordRequest request) {
    if (!rateLimitService.allowRequest(request.getEmail(), 3, 15)) {
        throw new TooManyRequestsException("Demasiadas solicitudes. Intenta en 15 minutos.");
    }
    passwordRecoveryService.sendRecoveryCode(request.getEmail());
    return ResponseEntity.ok(ApiResponse.success("Si el email existe, recibirás un código"));
}

// TooManyRequestsException.java (nuevo)
@ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
public class TooManyRequestsException extends RuntimeException {
    public TooManyRequestsException(String message) {
        super(message);
    }
}
```

**Archivos:**
- `pom.xml` - MODIFICAR
- `RateLimitService.java` - CREAR
- `TooManyRequestsException.java` - CREAR
- `AuthController.java` - MODIFICAR

---

#### **1.3 Excepciones Custom** (1 hora)
```java
// InvalidRecoveryCodeException.java (nuevo)
public class InvalidRecoveryCodeException extends RuntimeException {
    private final RecoveryCodeError error;

    public InvalidRecoveryCodeException(RecoveryCodeError error) {
        super(error.getMessage());
        this.error = error;
    }

    public RecoveryCodeError getError() {
        return error;
    }
}

// RecoveryCodeError.java (nuevo - enum)
public enum RecoveryCodeError {
    INVALID_CODE("El código ingresado es inválido"),
    CODE_EXPIRED("El código ha expirado. Solicita uno nuevo"),
    CODE_ALREADY_USED("Este código ya fue utilizado"),
    USER_NOT_FOUND("Usuario no encontrado");

    private final String message;

    RecoveryCodeError(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}

// PasswordRecoveryService - Reemplazar ResponseStatusException
throw new InvalidRecoveryCodeException(RecoveryCodeError.INVALID_CODE);

// GlobalExceptionHandler - Agregar handler
@ExceptionHandler(InvalidRecoveryCodeException.class)
public ResponseEntity<ErrorResponse> handleInvalidRecoveryCode(
        InvalidRecoveryCodeException ex,
        HttpServletRequest request) {
    return ResponseEntity.badRequest().body(
        new ErrorResponse(
            ex.getError().name(),
            ex.getMessage(),
            request.getRequestURI(),
            Instant.now()
        )
    );
}
```

**Archivos:**
- `InvalidRecoveryCodeException.java` - CREAR
- `RecoveryCodeError.java` - CREAR
- `PasswordRecoveryService.java` - MODIFICAR (reemplazar throws)
- `GlobalExceptionHandler.java` - MODIFICAR
- `ErrorResponse.java` - CREAR

---

#### **1.4 Eliminar Logging de Código Sensible** (5 min)
```java
// PasswordRecoveryService.java
passwordRecoveryRepository.save(recovery);
emailService.sendPasswordRecoveryEmail(email, code);
// ❌ ELIMINAR: logger.debug("Código generado: {}", code);
logger.info("Código de recuperación enviado para usuario {}", user.getId()); // ✅ Agregar
```

---

### 🟡 FASE 2: SOLID (Día 2 - 5 horas)

#### **2.1 Separar AuthController (SRP)** (30 min)
```java
// PasswordRecoveryController.java (nuevo)
@RestController
@RequestMapping("/auth/password")
@RequiredArgsConstructor
public class PasswordRecoveryController {
    private final PasswordRecoveryService passwordRecoveryService;
    private final RateLimitService rateLimitService;

    @PostMapping("/request")
    public ResponseEntity<ApiResponse<Void>> requestPasswordReset(
            @Valid @RequestBody ForgotPasswordRequest request) {
        if (!rateLimitService.allowRequest(request.getEmail(), 3, 15)) {
            throw new TooManyRequestsException("Demasiadas solicitudes");
        }
        passwordRecoveryService.sendRecoveryCode(request.getEmail());
        return ResponseEntity.ok(ApiResponse.success(
            "Si el email existe, recibirás un código"
        ));
    }

    @PostMapping("/verify")
    public ResponseEntity<ApiResponse<Void>> verifyCode(
            @Valid @RequestBody VerifyCodeRequest request) {
        passwordRecoveryService.verifyCode(request.getEmail(), request.getCode());
        return ResponseEntity.ok(ApiResponse.success("Código verificado"));
    }

    @PostMapping("/reset")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {
        passwordRecoveryService.resetPassword(
            request.getEmail(),
            request.getCode(),
            request.getNewPassword()
        );
        return ResponseEntity.ok(ApiResponse.success("Contraseña actualizada"));
    }
}

// AuthController.java - ELIMINAR endpoints /password/*
```

**Archivos:**
- `PasswordRecoveryController.java` - CREAR
- `AuthController.java` - MODIFICAR (eliminar endpoints)
- `ApiResponse.java` - CREAR

---

#### **2.2 Chain of Responsibility para Validaciones (OCP)** (2 horas)
```java
// RecoveryCodeValidator.java (interfaz)
public interface RecoveryCodeValidator {
    void validate(PasswordRecovery recovery, User user);
}

// CodeStatusValidator.java
@Component
@Order(1)
public class CodeStatusValidator implements RecoveryCodeValidator {
    @Override
    public void validate(PasswordRecovery recovery, User user) {
        if (recovery.getPasStatus() != PasswordRecoveryStatus.PENDING) {
            throw new InvalidRecoveryCodeException(RecoveryCodeError.CODE_ALREADY_USED);
        }
    }
}

// CodeExpirationValidator.java
@Component
@Order(2)
public class CodeExpirationValidator implements RecoveryCodeValidator {
    @Override
    public void validate(PasswordRecovery recovery, User user) {
        if (recovery.getPasExpired().isBefore(Instant.now())) {
            recovery.setPasStatus(PasswordRecoveryStatus.EXPIRED);
            throw new InvalidRecoveryCodeException(RecoveryCodeError.CODE_EXPIRED);
        }
    }
}

// PasswordRecoveryService - Modificar
@Service
@RequiredArgsConstructor
public class PasswordRecoveryService {
    private final List<RecoveryCodeValidator> validators;

    private PasswordRecovery validateRecoveryCode(String email, String code) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new InvalidRecoveryCodeException(RecoveryCodeError.INVALID_CODE));

        PasswordRecovery recovery = passwordRecoveryRepository.findByUser_IdAndPasCode(user.getId(), code)
                .orElseThrow(() -> new InvalidRecoveryCodeException(RecoveryCodeError.INVALID_CODE));

        validators.forEach(validator -> validator.validate(recovery, user));

        return recovery;
    }
}
```

**Archivos:**
- `RecoveryCodeValidator.java` - CREAR (interface)
- `CodeStatusValidator.java` - CREAR
- `CodeExpirationValidator.java` - CREAR
- `PasswordRecoveryService.java` - MODIFICAR

---

#### **2.3 EmailSender Interface (DIP)** (1 hora)
```java
// EmailSender.java (interfaz)
public interface EmailSender {
    void sendEmail(String to, String subject, String body);
}

// SmtpEmailSender.java
@Service
@Primary
public class SmtpEmailSender implements EmailSender {
    private final JavaMailSender mailSender;

    @Override
    public void sendEmail(String to, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
        } catch (MailException e) {
            throw new EmailSendException("Error enviando email", e);
        }
    }
}

// MockEmailSender.java (para tests)
@Profile("test")
@Service
public class MockEmailSender implements EmailSender {
    @Override
    public void sendEmail(String to, String subject, String body) {
        System.out.println("Mock email to: " + to);
    }
}

// PasswordRecoveryService
@Service
@RequiredArgsConstructor
public class PasswordRecoveryService {
    private final EmailSender emailSender; // ✅ Interface, no clase concreta
}
```

**Archivos:**
- `EmailSender.java` - CREAR (interface)
- `SmtpEmailSender.java` - CREAR
- `MockEmailSender.java` - CREAR
- `PasswordRecoveryService.java` - MODIFICAR
- `EmailService.java` - DEPRECAR

---

#### **2.4 Eliminar Duplicación PUBLIC_ENDPOINTS** (20 min)
```java
// SecurityConstants.java (nuevo)
@Component
public class SecurityConstants {
    public static final String[] PUBLIC_ENDPOINTS = {
        "/h2-console/**",
        "/api/register/**",
        "/auth/login",
        "/auth/password/**",
        "/api/dispositivos/recover/**",
        "/api/devices/recover/**"
    };
}

// JwtAuthFilter - Usar constante
private static final String[] PUBLIC_PATHS = SecurityConstants.PUBLIC_ENDPOINTS;

// SecurityConfig - Usar constante
private static final String[] PUBLIC_ENDPOINTS = SecurityConstants.PUBLIC_ENDPOINTS;
```

**Archivos:**
- `SecurityConstants.java` - CREAR
- `JwtAuthFilter.java` - MODIFICAR
- `SecurityConfig.java` - MODIFICAR

---

### 🟢 FASE 3: Clean Code (Día 3 - 3 horas)

#### **3.1 Refactorizar sendRecoveryCode()** (1 hora)
```java
@Transactional
public void sendRecoveryCode(String email) {
    User user = findUserByEmailOrSkip(email);
    if (user == null) return;

    invalidatePendingRecoveryCodes(user);

    PasswordRecovery recovery = createRecoveryCode(user);
    passwordRecoveryRepository.save(recovery);

    sendRecoveryEmail(email, recovery.getCode());
}

private User findUserByEmailOrSkip(String email) {
    return userRepository.findByEmail(email)
            .orElseGet(() -> {
                logger.debug("Intento de recuperación para email no existente: {}", email);
                return null;
            });
}

private void invalidatePendingRecoveryCodes(User user) {
    passwordRecoveryRepository
            .findLatestByUserIdAndStatus(user.getId(), PasswordRecoveryStatus.PENDING)
            .ifPresent(oldRecovery -> {
                oldRecovery.setPasStatus(PasswordRecoveryStatus.EXPIRED);
                passwordRecoveryRepository.save(oldRecovery);
            });
}

private PasswordRecovery createRecoveryCode(User user) {
    String code = generateCode();
    Instant now = Instant.now();

    return PasswordRecovery.builder()
            .user(user)
            .pasCode(code)
            .pasCreated(now)
            .pasExpired(now.plus(CODE_EXPIRATION_MINUTES, ChronoUnit.MINUTES))
            .pasLastSent(now)
            .pasResendCount(0)
            .pasStatus(PasswordRecoveryStatus.PENDING)
            .build();
}

private void sendRecoveryEmail(String email, String code) {
    emailSender.sendEmail(
        email,
        "Recuperación de Contraseña - NaivePay",
        buildRecoveryEmailBody(code)
    );
    logger.info("Código de recuperación enviado");
}

private String buildRecoveryEmailBody(String code) {
    return String.format(
        "Hola,\n\n" +
        "Recibimos una solicitud para restablecer tu contraseña.\n" +
        "Usa el siguiente código: %s\n\n" +
        "Este código expira en %d minutos.\n\n" +
        "Si no solicitaste este cambio, ignora este mensaje.",
        code,
        CODE_EXPIRATION_MINUTES
    );
}
```

---

#### **3.2 Extraer Constantes Mágicas** (15 min)
```java
private static final int CODE_EXPIRATION_MINUTES = 15;
private static final int CODE_LENGTH = 6;
private static final int CODE_MAX_VALUE = 1_000_000; // 10^CODE_LENGTH

private String generateCode() {
    return String.format("%0" + CODE_LENGTH + "d",
                         SECURE_RANDOM.nextInt(CODE_MAX_VALUE));
}
```

---

#### **3.3 Crear DTOs de Respuesta** (1 hora)
```java
// ApiResponse.java
@Data
@AllArgsConstructor
public class ApiResponse<T> {
    private boolean success;
    private String message;
    private T data;
    private String timestamp;

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(true, message, data, Instant.now().toString());
    }

    public static ApiResponse<Void> success(String message) {
        return success(message, null);
    }

    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(false, message, null, Instant.now().toString());
    }
}

// ErrorResponse.java
@Data
@AllArgsConstructor
public class ErrorResponse {
    private String error;
    private String message;
    private String path;
    private String timestamp;
}
```

**Usar en todos los controllers:**
```java
return ResponseEntity.ok(ApiResponse.success("Código enviado"));
```

---

### 📈 FASE 4: Mejoras Adicionales (Día 4 - 3 horas)

#### **4.1 Auditoría** (2 horas)
```java
// PasswordChangeAudit.java (entidad)
@Entity
@Data
@Builder
public class PasswordChangeAudit {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;
    private String ipAddress;
    private String userAgent;
    private String reason; // PASSWORD_RECOVERY, MANUAL_CHANGE, etc.
    private Instant timestamp;
}

// AuditService.java
@Service
@RequiredArgsConstructor
public class AuditService {
    private final PasswordChangeAuditRepository repository;

    public void logPasswordChange(Long userId, String ipAddress, String userAgent, String reason) {
        PasswordChangeAudit audit = PasswordChangeAudit.builder()
                .userId(userId)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .reason(reason)
                .timestamp(Instant.now())
                .build();
        repository.save(audit);
    }
}

// PasswordRecoveryService - Agregar auditoría
@Transactional
public void resetPassword(String email, String code, String newPassword,
                          String ipAddress, String userAgent) {
    // ... código existente ...

    auditService.logPasswordChange(
        user.getId(),
        ipAddress,
        userAgent,
        "PASSWORD_RECOVERY"
    );
}

// Controller - Capturar IP y UserAgent
@PostMapping("/reset")
public ResponseEntity<ApiResponse<Void>> resetPassword(
        @Valid @RequestBody ResetPasswordRequest request,
        HttpServletRequest httpRequest) {

    String ipAddress = httpRequest.getRemoteAddr();
    String userAgent = httpRequest.getHeader("User-Agent");

    passwordRecoveryService.resetPassword(
        request.getEmail(),
        request.getCode(),
        request.getNewPassword(),
        ipAddress,
        userAgent
    );
    return ResponseEntity.ok(ApiResponse.success("Contraseña actualizada"));
}
```

---

#### **4.2 Notificación de Cambio** (30 min)
```java
// EmailTemplates con CHANGE_NOTIFICATION
public enum EmailTemplate {
    PASSWORD_CHANGED(
        "Contraseña Cambiada - NaivePay",
        "Hola,\n\nTu contraseña ha sido cambiada exitosamente.\n\n" +
        "Si no fuiste tú, contacta inmediatamente a soporte.\n\n" +
        "Fecha: {timestamp}\nIP: {ipAddress}"
    );
}

// PasswordRecoveryService
emailSender.sendEmail(
    email,
    EmailTemplate.PASSWORD_CHANGED,
    Map.of(
        "timestamp", Instant.now().toString(),
        "ipAddress", ipAddress
    )
);
```

---

#### **4.3 Tests Unitarios** (1 hora)
```java
@SpringBootTest
class PasswordRecoveryServiceTest {

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private PasswordRecoveryRepository passwordRecoveryRepository;

    @MockBean
    private EmailSender emailSender;

    @Autowired
    private PasswordRecoveryService service;

    @Test
    void shouldSendRecoveryCodeForExistingUser() {
        // Given
        User user = createTestUser();
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));

        // When
        service.sendRecoveryCode("test@test.com");

        // Then
        verify(emailSender, times(1)).sendEmail(anyString(), anyString(), anyString());
        verify(passwordRecoveryRepository, times(1)).save(any());
    }

    @Test
    void shouldNotRevealNonExistentEmail() {
        // Given
        when(userRepository.findByEmail("nonexistent@test.com")).thenReturn(Optional.empty());

        // When
        service.sendRecoveryCode("nonexistent@test.com");

        // Then
        verify(emailSender, never()).sendEmail(anyString(), anyString(), anyString());
    }

    @Test
    void shouldThrowExceptionForExpiredCode() {
        // Given
        User user = createTestUser();
        PasswordRecovery expiredRecovery = createExpiredRecovery();

        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));
        when(passwordRecoveryRepository.findByUser_IdAndPasCode(1L, "123456"))
                .thenReturn(Optional.of(expiredRecovery));

        // When & Then
        assertThrows(InvalidRecoveryCodeException.class, () ->
            service.verifyCode("test@test.com", "123456")
        );
    }
}
```

---

## 📊 COMPARACIÓN ANTES/DESPUÉS

| Métrica | Antes | Después | Mejora |
|---------|-------|---------|--------|
| **SOLID Compliance** | 6.0/10 | 9.5/10 | +58% |
| **Clean Code** | 7.0/10 | 9.7/10 | +39% |
| **DRY** | 6.5/10 | 10.0/10 | +54% |
| **Seguridad** | 3.3/10 | 9.0/10 | +173% |
| **Testabilidad** | 4.0/10 | 9.5/10 | +138% |
| **Mantenibilidad** | 5.5/10 | 9.5/10 | +73% |
| **Número de clases** | 8 | 18 | +10 clases |
| **LOC (total)** | ~350 | ~600 | +71% |
| **LOC (por clase)** | ~44 | ~33 | -25% |
| **Complejidad ciclomática** | ~25 | ~12 | -52% |
| **Cobertura de tests** | 0% | 80%+ | +∞ |

---

## 🎯 CONCLUSIÓN

El código actual es **funcional** pero necesita **refactorización significativa** para cumplir con Clean Code y SOLID.

**Esfuerzo total:** 15 horas (3-4 días)
**Beneficio:** Código mantenible, testeable, seguro y escalable

**Prioridades:**
1. 🔴 **Fase 1 (Seguridad):** IMPRESCINDIBLE antes de producción
2. 🟡 **Fase 2 (SOLID):** Altamente recomendado
3. 🟢 **Fase 3 (Clean Code):** Recomendado
4. 📈 **Fase 4 (Mejoras):** Nice to have

**Siguiente paso recomendado:** Comenzar Fase 1 (Seguridad Crítica)