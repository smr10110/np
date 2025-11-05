# Plan de Mejoramiento y Refactorización - Módulo de Autenticación
## NaivePay Application

---

## Tabla de Contenidos

1. [Resumen Ejecutivo](#resumen-ejecutivo)
2. [Análisis del Estado Actual](#análisis-del-estado-actual)
3. [Principios y Estándares](#principios-y-estándares)
4. [Problemas Identificados](#problemas-identificados)
5. [Plan de Refactorización](#plan-de-refactorización)
6. [Roadmap de Implementación](#roadmap-de-implementación)

---

## Resumen Ejecutivo

Este documento presenta un análisis exhaustivo del módulo de autenticación de NaivePay (backend Spring Boot y frontend Angular) y propone un plan de refactorización basado en las mejores prácticas de desarrollo, principios SOLID, seguridad y mantenibilidad.

### Alcance del Análisis
- **Backend**: `cl.ufro.dci.naivepayapi.autentificacion.*`
- **Frontend**: `app/modules/autentificacion/*`
- **Módulos relacionados**: `registro.*`, `dispositivos.*`

### Hallazgos Clave
- ✅ **Fortalezas**: Implementación funcional de JWT, manejo de sesiones, protección contra fuerza bruta
- ⚠️ **Áreas críticas**: Seguridad de secretos, validaciones, testing, observabilidad, arquitectura de errores

---

## Análisis del Estado Actual

### Arquitectura Backend

```
autentificacion/
├── configuration/security/
│   ├── SecurityConfig.java          ✅ Configuración centralizada
│   ├── JwtAuthFilter.java           ⚠️ Lógica compleja, hardcoded
│   ├── GlobalExceptionHandler.java  ⚠️ Manejo básico de errores
│   └── RestAuthenticationEntryPoint.java
├── controller/
│   ├── AuthController.java          ✅ Endpoint REST simple
│   └── PasswordRecoveryController.java
├── service/
│   ├── AuthService.java             ⚠️ Múltiples responsabilidades
│   ├── JWTServiceImpl.java          🔴 SECRETO EN PLAINTEXT
│   ├── PasswordRecoveryService.java ✅ Bien estructurado
│   ├── AccountLockService.java      ✅ Single responsibility
│   └── AuthSessionService.java      ✅ Bien encapsulado
├── domain/
│   ├── Session.java                 ✅ Entidad JPA correcta
│   ├── AuthAttempt.java             ✅ Auditoría adecuada
│   └── PasswordRecovery.java
└── dto/
    └── LoginRequest.java            ⚠️ Sin validaciones Jakarta
```

### Arquitectura Frontend

```
autentificacion/
├── component/
│   ├── login/                       ✅ Reactive Forms
│   ├── password-recovery/
│   └── recuperar-acceso/
├── service/
│   └── autentificacion.service.ts   ⚠️ Token en sessionStorage
├── guards/
│   ├── auth.guard.ts                ⚠️ Lógica mínima
│   └── auth-entry.guard.ts
└── interceptors/
    └── auth.interceptor.ts          ⚠️ Paths hardcoded
```

---

## Principios y Estándares

### 1. SOLID Principles

#### S - Single Responsibility Principle (SRP)
> Cada clase debe tener una única razón para cambiar

**Aplicación en Autenticación**:
- Separar lógica de validación de credenciales de la lógica de generación de tokens
- Extraer lógica de bloqueo de cuentas a servicio dedicado ✅ (ya implementado)
- Separar logging de auditoría de la lógica de negocio

#### O - Open/Closed Principle (OCP)
> Las entidades deben estar abiertas para extensión pero cerradas para modificación

**Aplicación en Autenticación**:
- Usar estrategias para diferentes métodos de autenticación (email, RUT, OAuth)
- Permitir múltiples proveedores de tokens sin modificar código existente

#### L - Liskov Substitution Principle (LSP)
> Los subtipos deben ser sustituibles por sus tipos base

**Aplicación en Autenticación**:
- Interfaces para servicios de autenticación (JWTService ya usa interface ✅)
- Implementaciones intercambiables de PasswordEncoder

#### I - Interface Segregation Principle (ISP)
> Los clientes no deben depender de interfaces que no usan

**Aplicación en Autenticación**:
- Separar `JWTService` en `JWTGenerator` y `JWTValidator`
- Crear interfaces específicas por responsabilidad

#### D - Dependency Inversion Principle (DIP)
> Depender de abstracciones, no de concreciones

**Aplicación en Autenticación**:
- Inyección de dependencias via constructores ✅ (implementado)
- Usar interfaces para todos los servicios críticos

---

### 2. Clean Code Practices

#### Nombres Descriptivos
```java
// ❌ MAL
private boolean isBlank(String s)

// ✅ BIEN
private boolean isNullOrEmpty(String value)
private boolean isCredentialValid(String credential)
```

#### Funciones Pequeñas y Enfocadas
- Máximo 20 líneas por método
- Una sola acción por función
- Nivel de abstracción consistente

#### Código Auto-Documentado
```java
// ❌ MAL - Comentarios innecesarios
// Verifica si la cuenta está bloqueada
if (user.getState() == AccountState.INACTIVE) { ... }

// ✅ BIEN - Código expresivo
if (isAccountLocked(user)) { ... }
```

---

### 3. DRY / KISS / YAGNI

#### DRY (Don't Repeat Yourself)
**Problema identificado**: Duplicación de lógica de paths públicos
```java
// SecurityConfig.java - línea 26
private static final String[] PUBLIC_ENDPOINTS = { ... }

// JwtAuthFilter.java - línea 38
private static final String[] PUBLIC_PATHS = { ... }
```

#### KISS (Keep It Simple, Stupid)
**Principio**: Soluciones simples sobre complejas

**Problema identificado**: Lógica compleja en `AuthService.login()` (110 líneas)

#### YAGNI (You Aren't Gonna Need It)
**Principio**: No implementar funcionalidad hasta que sea necesaria

---

### 4. Dependency Injection

✅ **Buenas prácticas actuales**:
```java
@Service
public class AuthService {
    private final JWTService jwtService;
    private final AuthAttemptService authAttemptService;

    public AuthService(JWTService jwtService, AuthAttemptService authAttemptService) {
        this.jwtService = jwtService;
        this.authAttemptService = authAttemptService;
    }
}
```

⚠️ **Mejoras necesarias**:
- Usar `@RequiredArgsConstructor` de Lombok para reducir boilerplate
- Marcar dependencias como `final` para inmutabilidad

---

### 5. Centralized Error Handling

#### Estado Actual
```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> handleResponseStatusException(...) {
        // Manejo básico
    }
}
```

#### Problemas Identificados
1. Solo maneja `ResponseStatusException`
2. No registra logs estructurados
3. No captura excepciones de validación (Jakarta Validation)
4. Respuestas inconsistentes entre controladores

---

### 6. Security

#### Fortalezas Actuales ✅
- Spring Security con JWT implementado
- CSRF deshabilitado para API REST (correcto)
- Stateless session management
- BCrypt para hashing de passwords
- Protección contra fuerza bruta (5 intentos, ventana 30min)
- Validación de sesiones activas en base de datos

#### Vulnerabilidades Críticas 🔴

##### 6.1 SECRET JWT EN PLAINTEXT
**Ubicación**: `application.properties:2`
```properties
security.jwt.secret=NaivePay_2025_SecureKey_8aF9vX3nZ2tP5kL1rT6jW0yE4qM7hU9s
```

**Riesgo**:
- Exposición del secreto en repositorio Git
- Acceso no autorizado a tokens JWT
- Compromiso total del sistema de autenticación

**Solución requerida**: Variables de entorno + Secrets Manager

---

##### 6.2 CREDENCIALES DE EMAIL EN PLAINTEXT
**Ubicación**: `application.properties:6-7`
```properties
spring.mail.username=naivepay.registro@gmail.com
spring.mail.password=lkmr jkem mvsv cxxm
```

**Riesgo**: Compromiso de cuenta de email

---

##### 6.3 TTL DE TOKEN MUY CORTO
**Ubicación**: `application.properties:3`
```properties
security.jwt.ttl-minutes=1
```

**Problema**:
- Token expira en 1 minuto (probablemente para testing)
- Mala experiencia de usuario en producción
- Logs excesivos de sesiones expiradas

**Recomendación**:
- Desarrollo: 60 minutos
- Producción: 15-30 minutos
- Implementar refresh tokens

---

##### 6.4 CORS PERMISIVO
**Ubicación**: `SecurityConfig.java:46`
```java
cfg.setAllowedOrigins(List.of("http://localhost:4200"));
```

**Problema**: Hardcoded, solo desarrollo

**Solución**: Configurar desde properties por ambiente

---

##### 6.5 FALTA VALIDACIÓN DE INPUT EN DTOS
**Ubicación**: `LoginRequest.java`
```java
@Data
public class LoginRequest {
    private String identifier;   // SIN @NotBlank, @Email, etc.
    private String password;     // SIN @NotBlank, @Size
}
```

**Riesgo**:
- Inyección de código
- Datos malformados en BD
- Errores inesperados

---

##### 6.6 LOGGING DE INFORMACIÓN SENSIBLE
**Ubicación**: `JwtAuthFilter.java:127`
```java
System.out.println("Ruta pública detectada: " + uri);
```

**Problemas**:
- Uso de `System.out.println` en lugar de logger
- Potencial logging de información sensible en URIs

---

##### 6.7 TOKEN EN sessionStorage (Frontend)
**Ubicación**: `autentificacion.service.ts:86`
```typescript
sessionStorage.setItem('token', res.accessToken);
```

**Riesgo**:
- Vulnerable a XSS
- Token accesible desde JavaScript

**Alternativa más segura**: HttpOnly cookies

---

### 7. Testing

#### Estado Actual
❌ **No se encontraron tests unitarios ni de integración**

#### Impacto
- Alta probabilidad de regresiones
- Dificulta refactorización segura
- No hay garantía de comportamiento correcto

#### Tests Requeridos

##### Backend
```java
// Unit Tests
- AuthServiceTest
  ├── login_withValidCredentials_shouldReturnToken()
  ├── login_withInvalidPassword_shouldReturnUnauthorized()
  ├── login_withBlockedAccount_shouldReturnForbidden()
  ├── login_afterFiveFailedAttempts_shouldBlockAccount()
  └── logout_withValidToken_shouldInvalidateSession()

- JWTServiceTest
  ├── generate_shouldCreateValidToken()
  ├── parse_withExpiredToken_shouldThrowException()
  └── parse_withInvalidSignature_shouldThrowException()

- AccountLockServiceTest
  ├── checkAndBlock_withFiveFailures_shouldLockAccount()
  └── checkAndBlock_withOldAttempts_shouldNotLock()

// Integration Tests
- AuthControllerIT
  ├── loginFlow_endToEnd()
  ├── passwordRecoveryFlow_endToEnd()
  └── sessionExpiration_shouldDenyAccess()
```

##### Frontend
```typescript
// Unit Tests
- AutentificacionService
  ├── login() success path
  ├── login() error handling
  ├── logout() session cleanup
  └── auto-logout on token expiration

// E2E Tests
- Login flow con Cypress/Playwright
- Password recovery flow
- Session timeout behavior
```

---

### 8. Observability

#### Estado Actual
⚠️ **Logging básico sin estructura**

```java
// AccountLockService.java:138
logger.warn("Cuenta bloqueada por seguridad. Usuario: {}, RUT: {}-{}, intentos: {}, ventana_min: {}, ts: {}",
    user.getId(), user.getRutGeneral(), user.getVerificationDigit(),
    MAX_FAILED_ATTEMPTS, LOCKOUT_DURATION_MINUTES, Instant.now());
```

✅ **Aspectos positivos**:
- Uso de SLF4J
- Logs de eventos críticos (bloqueos, errores)

#### Mejoras Necesarias

##### 8.1 Structured Logging
```java
// ❌ Actual
logger.info("Código de recuperación enviado exitosamente");

// ✅ Propuesto
logger.info("Password recovery code sent",
    kv("userId", user.getId()),
    kv("email", maskEmail(email)),
    kv("codeExpiration", expiration),
    kv("eventType", "PASSWORD_RECOVERY_INITIATED")
);
```

##### 8.2 Métricas de Negocio
- Tasa de éxito/fallo de login
- Tiempo promedio de autenticación
- Número de cuentas bloqueadas por día
- Distribución de métodos de recuperación de contraseña

##### 8.3 Distributed Tracing
- Implementar Micrometer + Spring Actuator
- Correlacionar requests entre frontend y backend
- Trace IDs en headers HTTP

##### 8.4 Health Checks
```java
@Component
public class AuthHealthIndicator implements HealthIndicator {
    @Override
    public Health health() {
        // Verificar conectividad con BD
        // Verificar generación de tokens
        // Verificar servicio de email
        return Health.up().build();
    }
}
```

---

## Problemas Identificados

### Críticos (P0) 🔴

| ID | Problema | Ubicación | Impacto | Principio Violado |
|----|----------|-----------|---------|-------------------|
| P0-1 | **Secreto JWT en plaintext** | `application.properties:2` | Compromiso total de seguridad | Security |
| P0-2 | **Credenciales email en plaintext** | `application.properties:6-7` | Compromiso de cuenta de correo | Security |
| P0-3 | **Sin validación de DTOs** | `LoginRequest.java` | Inyección, errores inesperados | Security, Clean Code |
| P0-4 | **Token en sessionStorage (XSS)** | Frontend `service.ts:86` | Robo de sesiones | Security |
| P0-5 | **Ausencia de tests** | Todo el módulo | Regresiones, bugs en producción | Testing |

---

### Altos (P1) ⚠️

| ID | Problema | Ubicación | Impacto | Principio Violado |
|----|----------|-----------|---------|-------------------|
| P1-1 | **Duplicación de PUBLIC_PATHS** | `SecurityConfig.java`, `JwtAuthFilter.java` | Inconsistencias, mantenimiento | DRY |
| P1-2 | **AuthService con múltiples responsabilidades** | `AuthService.java:110 líneas` | Difícil testing, acoplamiento | SRP, Clean Code |
| P1-3 | **System.out.println en producción** | `JwtAuthFilter.java:127` | Logs perdidos, no estructurados | Observability |
| P1-4 | **CORS hardcoded** | `SecurityConfig.java:46` | No funciona en otros ambientes | Configuration |
| P1-5 | **GlobalExceptionHandler incompleto** | `GlobalExceptionHandler.java` | Errores no capturados | Centralized Error Handling |
| P1-6 | **Sin logs estructurados** | Todo el módulo | Dificulta debugging en producción | Observability |
| P1-7 | **Sin métricas de negocio** | Todo el módulo | No hay visibilidad de rendimiento | Observability |

---

### Medios (P2) 📋

| ID | Problema | Ubicación | Impacto | Principio Violado |
|----|----------|-----------|---------|-------------------|
| P2-1 | **Nombres poco descriptivos** | `isBlank()`, `write401()` | Dificulta lectura | Clean Code |
| P2-2 | **Métodos largos** | `AuthService.login()` (110 líneas) | Dificulta mantenimiento | Clean Code |
| P2-3 | **Comentarios obvios** | Varios archivos | Ruido innecesario | Clean Code |
| P2-4 | **Falta documentación JavaDoc** | Servicios públicos | Dificulta onboarding | Documentation |
| P2-5 | **TTL de 1 minuto en token** | `application.properties:3` | Mala UX, logs excesivos | Configuration |
| P2-6 | **Base de datos H2 en memoria** | `application.properties` | Pérdida de datos al reiniciar | Configuration |
| P2-7 | **Sin refresh tokens** | Todo el módulo | UX subóptima | Feature Gap |

---

### Bajos (P3) 💡

| ID | Problema | Ubicación | Impacto | Principio Violado |
|----|----------|-----------|---------|-------------------|
| P3-1 | **Uso de @Data en entidades JPA** | Todas las entidades | Puede causar lazy loading issues | Best Practices |
| P3-2 | **Falta @Transactional(readOnly)** | Algunos métodos de lectura | Performance subóptima | Performance |
| P3-3 | **Sin paginación en consultas** | `AuthAttemptRepository` | Riesgo con gran volumen de datos | Scalability |
| P3-4 | **Hardcoded strings de errores** | Frontend `login.component.ts` | Dificulta i18n | Clean Code |
| P3-5 | **Magic numbers** | `MAX_FAILED_ATTEMPTS=5` | Dificulta ajustes | Configuration |

---

## Plan de Refactorización

### Fase 0: Preparación (Crítica) 🔴
**Duración estimada**: 1 semana
**Objetivo**: Resolver vulnerabilidades de seguridad críticas

#### 0.1 Externalizar Secretos
```yaml
# application-dev.yml
security:
  jwt:
    secret: ${JWT_SECRET}
    ttl-minutes: ${JWT_TTL_MINUTES:60}

spring:
  mail:
    username: ${MAIL_USERNAME}
    password: ${MAIL_PASSWORD}
```

**Implementación**:
```bash
# .env (NO commitear)
JWT_SECRET=generate-secure-random-key-here
JWT_TTL_MINUTES=60
MAIL_USERNAME=naivepay.registro@gmail.com
MAIL_PASSWORD=secure-app-password
```

**Checklist**:
- [ ] Crear `application-dev.yml` y `application-prod.yml`
- [ ] Mover secretos a variables de entorno
- [ ] Actualizar `.gitignore` para excluir `.env`
- [ ] Documentar variables requeridas en `README.md`
- [ ] Configurar secrets en CI/CD (GitHub Actions/Azure/AWS)

---

#### 0.2 Implementar Validación de DTOs
```java
// LoginRequest.java
@Data
public class LoginRequest {
    @NotBlank(message = "Identifier is required")
    @Size(min = 3, max = 100, message = "Identifier must be between 3 and 100 characters")
    private String identifier;

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 128, message = "Password must be between 8 and 128 characters")
    private String password;
}

// AuthController.java
@PostMapping("/login")
public ResponseEntity<?> login(@Valid @RequestBody LoginRequest req, ...) {
    // Spring validará automáticamente
}

// GlobalExceptionHandler.java
@ExceptionHandler(MethodArgumentNotValidException.class)
public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex) {
    // Retornar errores de validación estructurados
}
```

**Checklist**:
- [ ] Agregar `spring-boot-starter-validation` al `pom.xml`
- [ ] Anotar todos los DTOs con validaciones Jakarta
- [ ] Agregar handler para `MethodArgumentNotValidException`
- [ ] Crear tests de validación

---

#### 0.3 Mejorar Almacenamiento de Tokens (Frontend)
```typescript
// Opción 1: HttpOnly Cookies (RECOMENDADO)
// Backend: Enviar token en Set-Cookie header
response.addHeader("Set-Cookie",
    "token=" + jwt + "; HttpOnly; Secure; SameSite=Strict; Max-Age=3600");

// Frontend: No almacenar, el browser lo maneja automáticamente

// Opción 2: Si se mantiene sessionStorage, mitigar XSS
// - Implementar Content Security Policy (CSP)
// - Sanitizar todos los inputs
// - Usar DomSanitizer de Angular
```

**Checklist**:
- [ ] Evaluar cambio a HttpOnly cookies vs. mantener sessionStorage
- [ ] Si se usa cookies: configurar CORS con `credentials: true`
- [ ] Implementar CSP headers en backend
- [ ] Auditar frontend para XSS vulnerabilities

---

### Fase 1: Refactorización de Arquitectura (Alta Prioridad) ⚠️
**Duración estimada**: 2 semanas
**Objetivo**: Mejorar mantenibilidad y aplicar SOLID

#### 1.1 Centralizar Configuración de Rutas Públicas
```java
// SecurityConstants.java (NUEVO)
@Component
public class SecurityConstants {
    public static final String[] PUBLIC_ENDPOINTS = {
        "/h2-console/**",
        "/api/register/**",
        "/auth/login",
        "/auth/recovery/**",
        "/auth/password/**",
        "/api/dispositivos/recover/**",
        "/api/devices/recover/**"
    };
}

// SecurityConfig.java
@Configuration
public class SecurityConfig {
    @Autowired
    private SecurityConstants securityConstants;

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(auth -> auth
            .requestMatchers(securityConstants.PUBLIC_ENDPOINTS).permitAll()
            // ...
        );
    }
}

// JwtAuthFilter.java
@Component
public class JwtAuthFilter extends OncePerRequestFilter {
    private final SecurityConstants securityConstants;

    private boolean isPublic(String uri) {
        for (String pattern : securityConstants.PUBLIC_ENDPOINTS) {
            if (PATH_MATCHER.match(pattern, uri)) {
                return true;
            }
        }
        return false;
    }
}
```

**Checklist**:
- [ ] Crear clase `SecurityConstants`
- [ ] Refactorizar `SecurityConfig` y `JwtAuthFilter`
- [ ] Agregar tests para verificar consistencia

---

#### 1.2 Dividir AuthService (Aplicar SRP)
```java
// CredentialValidator.java (NUEVO)
@Service
public class CredentialValidator {
    private final UserRepository userRepo;
    private final PasswordEncoder passwordEncoder;

    public Optional<User> validateAndResolveUser(String identifier, String password) {
        Optional<User> userOpt = resolveUser(identifier);
        if (userOpt.isEmpty()) return Optional.empty();

        User user = userOpt.get();
        if (!isValidPassword(user, password)) return Optional.empty();

        return Optional.of(user);
    }

    private Optional<User> resolveUser(String identifier) { /* ... */ }
    private boolean isValidPassword(User user, String rawPassword) { /* ... */ }
}

// SessionManager.java (NUEVO)
@Service
public class SessionManager {
    private final JWTService jwtService;
    private final AuthSessionService authSessionService;
    private final DeviceService deviceService;

    public LoginResponse createSession(User user, String deviceFingerprint) {
        UUID jti = UUID.randomUUID();
        String token = jwtService.generate(/* ... */);
        Instant exp = jwtService.getExpiration(token);

        Device device = deviceService.ensureAuthorizedDevice(/* ... */);
        Session session = authSessionService.saveActiveSession(jti, user, device, exp);

        return new LoginResponse(token, exp.toString(), session.getSesId().toString());
    }

    public void closeSession(UUID jti) {
        authSessionService.closeByJti(jti);
    }
}

// AuthService.java (REFACTORIZADO)
@Service
@RequiredArgsConstructor
public class AuthService {
    private final CredentialValidator credentialValidator;
    private final SessionManager sessionManager;
    private final AccountLockService accountLockService;
    private final AuthAttemptService authAttemptService;

    public ResponseEntity<?> login(LoginRequest req, String deviceFingerprint) {
        // Validación básica
        if (isBlank(req.getIdentifier()) || isBlank(req.getPassword())) {
            return unauthorized(AuthAttemptReason.BAD_CREDENTIALS);
        }

        // Resolver y validar credenciales
        Optional<User> userOpt = credentialValidator.validateAndResolveUser(
            req.getIdentifier(),
            req.getPassword()
        );

        if (userOpt.isEmpty()) {
            return handleFailedLogin(null, AuthAttemptReason.BAD_CREDENTIALS);
        }

        User user = userOpt.get();

        // Verificar bloqueo
        if (accountLockService.isAccountLocked(user)) {
            return forbidden(AuthAttemptReason.ACCOUNT_BLOCKED);
        }

        // Crear sesión
        try {
            LoginResponse response = sessionManager.createSession(user, deviceFingerprint);
            authAttemptService.logSuccess(user, deviceFingerprint);
            return ResponseEntity.ok(response);
        } catch (ResponseStatusException ex) {
            return handleDeviceError(user, ex);
        }
    }

    public ResponseEntity<Map<String, Object>> logout(String authHeader) {
        String token = extractBearer(authHeader);
        if (token == null) return unauthorizedLogout();

        try {
            UUID jti = UUID.fromString(jwtService.getJti(token));
            sessionManager.closeSession(jti);
            return ResponseEntity.ok(Map.of("message", "Sesión cerrada"));
        } catch (JwtException | IllegalArgumentException ex) {
            return unauthorizedLogout();
        }
    }

    private ResponseEntity<?> handleFailedLogin(User user, AuthAttemptReason reason) {
        // Lógica de manejo de fallos, intentos restantes, bloqueos
    }
}
```

**Checklist**:
- [ ] Crear `CredentialValidator` service
- [ ] Crear `SessionManager` service
- [ ] Refactorizar `AuthService` para delegar responsabilidades
- [ ] Actualizar tests (crear nuevos tests unitarios por servicio)

---

#### 1.3 Mejorar GlobalExceptionHandler
```java
// ErrorResponse.java (NUEVO)
@Getter
@Builder
public class ErrorResponse {
    private String error;
    private String message;
    private String path;
    private Instant timestamp;
    private Map<String, String> validationErrors;
    private String traceId;  // Para distributed tracing
}

// GlobalExceptionHandler.java (REFACTORIZADO)
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatusException(
        ResponseStatusException ex,
        HttpServletRequest request
    ) {
        log.error("ResponseStatusException: {}", ex.getReason(), ex);

        ErrorResponse error = ErrorResponse.builder()
            .error(ex.getStatusCode().toString())
            .message(ex.getReason() != null ? ex.getReason() : "Error")
            .path(request.getRequestURI())
            .timestamp(Instant.now())
            .traceId(MDC.get("traceId"))
            .build();

        return ResponseEntity.status(ex.getStatusCode()).body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
        MethodArgumentNotValidException ex,
        HttpServletRequest request
    ) {
        log.warn("Validation error on {}: {}", request.getRequestURI(), ex.getMessage());

        Map<String, String> validationErrors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
            validationErrors.put(error.getField(), error.getDefaultMessage())
        );

        ErrorResponse error = ErrorResponse.builder()
            .error("VALIDATION_ERROR")
            .message("Input validation failed")
            .path(request.getRequestURI())
            .timestamp(Instant.now())
            .validationErrors(validationErrors)
            .traceId(MDC.get("traceId"))
            .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(JwtException.class)
    public ResponseEntity<ErrorResponse> handleJwtException(
        JwtException ex,
        HttpServletRequest request
    ) {
        log.error("JWT validation failed: {}", ex.getMessage());

        ErrorResponse error = ErrorResponse.builder()
            .error("TOKEN_INVALID")
            .message("Invalid or expired token")
            .path(request.getRequestURI())
            .timestamp(Instant.now())
            .traceId(MDC.get("traceId"))
            .build();

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
        Exception ex,
        HttpServletRequest request
    ) {
        log.error("Unhandled exception on {}", request.getRequestURI(), ex);

        ErrorResponse error = ErrorResponse.builder()
            .error("INTERNAL_SERVER_ERROR")
            .message("An unexpected error occurred")
            .path(request.getRequestURI())
            .timestamp(Instant.now())
            .traceId(MDC.get("traceId"))
            .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
```

**Checklist**:
- [ ] Crear clase `ErrorResponse`
- [ ] Expandir `GlobalExceptionHandler` con todos los casos
- [ ] Agregar logging estructurado con SLF4J + MDC
- [ ] Configurar trace IDs (Spring Cloud Sleuth o Micrometer Tracing)

---

### Fase 2: Testing (Alta Prioridad) ⚠️
**Duración estimada**: 2 semanas
**Objetivo**: Alcanzar 80% cobertura de código

#### 2.1 Tests Unitarios Backend
```java
// AuthServiceTest.java
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    @Mock private CredentialValidator credentialValidator;
    @Mock private SessionManager sessionManager;
    @Mock private AccountLockService accountLockService;
    @Mock private AuthAttemptService authAttemptService;

    @InjectMocks
    private AuthService authService;

    @Test
    void login_withValidCredentials_shouldReturnToken() {
        // Given
        LoginRequest request = new LoginRequest("test@example.com", "password123");
        User user = createTestUser();
        LoginResponse expectedResponse = new LoginResponse("token", "2025-01-01", "session-id");

        when(credentialValidator.validateAndResolveUser(anyString(), anyString()))
            .thenReturn(Optional.of(user));
        when(accountLockService.isAccountLocked(user)).thenReturn(false);
        when(sessionManager.createSession(user, null)).thenReturn(expectedResponse);

        // When
        ResponseEntity<?> response = authService.login(request, null);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isInstanceOf(LoginResponse.class);
        verify(authAttemptService).logSuccess(user, null);
    }

    @Test
    void login_withInvalidCredentials_shouldReturnUnauthorized() {
        // Given
        LoginRequest request = new LoginRequest("test@example.com", "wrongpassword");
        when(credentialValidator.validateAndResolveUser(anyString(), anyString()))
            .thenReturn(Optional.empty());

        // When
        ResponseEntity<?> response = authService.login(request, null);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(sessionManager, never()).createSession(any(), any());
    }

    @Test
    void login_withBlockedAccount_shouldReturnForbidden() {
        // Given
        LoginRequest request = new LoginRequest("test@example.com", "password123");
        User blockedUser = createBlockedUser();

        when(credentialValidator.validateAndResolveUser(anyString(), anyString()))
            .thenReturn(Optional.of(blockedUser));
        when(accountLockService.isAccountLocked(blockedUser)).thenReturn(true);

        // When
        ResponseEntity<?> response = authService.login(request, null);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        verify(sessionManager, never()).createSession(any(), any());
    }

    @Test
    void login_afterFiveFailedAttempts_shouldBlockAccount() {
        // Test lógica de bloqueo automático
    }

    @Test
    void logout_withValidToken_shouldCloseSession() {
        // Test cierre de sesión
    }

    private User createTestUser() { /* ... */ }
    private User createBlockedUser() { /* ... */ }
}

// JWTServiceTest.java
@SpringBootTest
class JWTServiceTest {
    @Autowired
    private JWTService jwtService;

    @Test
    void generate_shouldCreateValidToken() {
        String token = jwtService.generate("user123", "fingerprint", "jti");
        assertThat(token).isNotBlank();
        assertThat(jwtService.getUserId(token)).isEqualTo("user123");
    }

    @Test
    void parse_withExpiredToken_shouldThrowExpiredJwtException() {
        // Mock time para simular token expirado
        assertThatThrownBy(() -> jwtService.parse(expiredToken))
            .isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    void parse_withInvalidSignature_shouldThrowJwtException() {
        String tampered = validToken + "tampered";
        assertThatThrownBy(() -> jwtService.parse(tampered))
            .isInstanceOf(JwtException.class);
    }
}

// AccountLockServiceTest.java
@ExtendWith(MockitoExtension.class)
class AccountLockServiceTest {
    @Mock private AuthAttemptRepository attemptRepo;
    @Mock private UserRepository userRepo;
    @Mock private EmailService emailService;

    @InjectMocks
    private AccountLockService lockService;

    @Test
    void checkAndBlock_withFiveConsecutiveFailures_shouldLockAccount() {
        // Given
        User user = createTestUser();
        List<AuthAttempt> fiveFailures = createFailedAttempts(5, user);

        when(attemptRepo.findLatestAttemptsByUser(eq(user.getId()), any()))
            .thenReturn(fiveFailures);

        // When
        boolean locked = lockService.checkAndBlockIfNeeded(user);

        // Then
        assertThat(locked).isTrue();
        assertThat(user.getState()).isEqualTo(AccountState.INACTIVE);
        verify(userRepo).save(user);
        verify(emailService).sendAccountBlockedNotice(anyString());
    }

    @Test
    void checkAndBlock_withOldFailures_shouldNotLock() {
        // Test ventana de 30 minutos
    }
}
```

**Checklist**:
- [ ] Configurar JUnit 5 + Mockito + AssertJ
- [ ] Escribir tests para todos los servicios de autenticación
- [ ] Alcanzar >80% cobertura en servicios críticos
- [ ] Configurar Jacoco para reportes de cobertura

---

#### 2.2 Tests de Integración Backend
```java
// AuthControllerIT.java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class AuthControllerIT {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepo;

    @Autowired
    private SessionRepository sessionRepo;

    @BeforeEach
    void setUp() {
        // Limpiar BD y crear datos de prueba
    }

    @Test
    void loginFlow_endToEnd_shouldSucceed() throws Exception {
        // Given: Usuario registrado
        createTestUser("test@example.com", "password123");

        // When: Login con credenciales válidas
        MvcResult result = mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "identifier": "test@example.com",
                        "password": "password123"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").isNotEmpty())
            .andExpect(jsonPath("$.expiresAt").isNotEmpty())
            .andReturn();

        String token = JsonPath.read(result.getResponse().getContentAsString(), "$.accessToken");

        // Then: Token debe permitir acceso a endpoints protegidos
        mockMvc.perform(get("/api/protected-resource")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk());

        // Then: Sesión debe estar registrada en BD
        assertThat(sessionRepo.count()).isEqualTo(1);
    }

    @Test
    void loginFlow_withInvalidPassword_shouldFail() throws Exception {
        createTestUser("test@example.com", "password123");

        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "identifier": "test@example.com",
                        "password": "wrongpassword"
                    }
                    """))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error").value("BAD_CREDENTIALS"));
    }

    @Test
    void loginFlow_afterFiveFailedAttempts_shouldBlockAccount() throws Exception {
        // Test completo de protección contra fuerza bruta
    }

    @Test
    void passwordRecoveryFlow_endToEnd_shouldSucceed() throws Exception {
        // Test flujo completo de recuperación de contraseña
    }
}
```

**Checklist**:
- [ ] Configurar base de datos de test (H2 o Testcontainers)
- [ ] Escribir tests end-to-end para flujos críticos
- [ ] Configurar perfiles de test (`application-test.yml`)

---

#### 2.3 Tests Frontend
```typescript
// autentificacion.service.spec.ts
describe('AutentificacionService', () => {
  let service: AutentificacionService;
  let httpMock: HttpTestingController;
  let router: Router;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule, RouterTestingModule],
      providers: [AutentificacionService, DeviceFingerprintService]
    });
    service = TestBed.inject(AutentificacionService);
    httpMock = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router);
  });

  it('login() should store token and schedule auto-logout on success', () => {
    const mockResponse: LoginResponse = {
      accessToken: 'mock-jwt-token',
      expiresAt: new Date(Date.now() + 3600000).toISOString(),
      jti: 'session-id'
    };

    service.login({ identifier: 'test@example.com', password: 'password' }).subscribe(res => {
      expect(sessionStorage.getItem('token')).toBe('mock-jwt-token');
      expect(res).toEqual(mockResponse);
    });

    const req = httpMock.expectOne('http://localhost:8080/auth/login');
    expect(req.request.method).toBe('POST');
    req.flush(mockResponse);
  });

  it('login() should handle 401 error correctly', () => {
    service.login({ identifier: 'test@example.com', password: 'wrong' }).subscribe({
      next: () => fail('should have failed'),
      error: (error) => {
        expect(error.status).toBe(401);
      }
    });

    const req = httpMock.expectOne('http://localhost:8080/auth/login');
    req.flush({ error: 'BAD_CREDENTIALS' }, { status: 401, statusText: 'Unauthorized' });
  });

  it('logout() should clear token and redirect', fakeAsync(() => {
    sessionStorage.setItem('token', 'mock-token');
    spyOn(router, 'navigate');

    service.logout().subscribe();

    const req = httpMock.expectOne('http://localhost:8080/auth/logout');
    req.flush({});
    tick();

    expect(sessionStorage.getItem('token')).toBeNull();
    expect(router.navigate).toHaveBeenCalledWith(['/auth/login'], { queryParams: { reason: 'logout_ok' } });
  }));
});

// login.component.spec.ts
describe('LoginComponent', () => {
  let component: LoginComponent;
  let fixture: ComponentFixture<LoginComponent>;
  let authService: jasmine.SpyObj<AutentificacionService>;

  beforeEach(() => {
    const authSpy = jasmine.createSpyObj('AutentificacionService', ['login']);

    TestBed.configureTestingModule({
      imports: [LoginComponent, ReactiveFormsModule],
      providers: [{ provide: AutentificacionService, useValue: authSpy }]
    });

    fixture = TestBed.createComponent(LoginComponent);
    component = fixture.componentInstance;
    authService = TestBed.inject(AutentificacionService) as jasmine.SpyObj<AutentificacionService>;
  });

  it('should create form with required validators', () => {
    expect(component.loginForm.get('identifier')?.hasError('required')).toBeTrue();
    expect(component.loginForm.get('password')?.hasError('required')).toBeTrue();
  });

  it('submit() should call authService.login() with form data', () => {
    authService.login.and.returnValue(of({ accessToken: 'token', expiresAt: '', jti: '' }));

    component.loginForm.patchValue({
      identifier: 'test@example.com',
      password: 'password123'
    });

    component.submit();

    expect(authService.login).toHaveBeenCalledWith({
      identifier: 'test@example.com',
      password: 'password123'
    });
  });

  it('submit() should show error message on BAD_CREDENTIALS', () => {
    authService.login.and.returnValue(throwError(() => ({
      error: { error: 'BAD_CREDENTIALS', remainingAttempts: 4 }
    })));

    component.loginForm.patchValue({
      identifier: 'test@example.com',
      password: 'wrongpassword'
    });

    component.submit();

    expect(component.message()).toContain('CREDENCIALES INVALIDAS');
    expect(component.remainingAttempts()).toBe(4);
  });
});
```

**Checklist**:
- [ ] Configurar Karma/Jasmine para tests unitarios
- [ ] Escribir tests para servicios y componentes
- [ ] Configurar Cypress o Playwright para E2E
- [ ] Agregar tests E2E para flujos críticos

---

### Fase 3: Observabilidad y Métricas (Prioridad Media) 📋
**Duración estimada**: 1 semana
**Objetivo**: Visibilidad completa en producción

#### 3.1 Structured Logging
```java
// Agregar dependencia
// pom.xml
<dependency>
    <groupId>net.logstash.logback</groupId>
    <artifactId>logstash-logback-encoder</artifactId>
    <version>7.4</version>
</dependency>

// logback-spring.xml
<configuration>
    <appender name="CONSOLE_JSON" class="ch.qos.logback.core.ConsoleAppender">
        <encoder class="net.logstash.logback.encoder.LogstashEncoder">
            <includeMdcKeyName>traceId</includeMdcKeyName>
            <includeMdcKeyName>userId</includeMdcKeyName>
            <fieldNames>
                <timestamp>@timestamp</timestamp>
                <message>message</message>
                <level>level</level>
                <logger>logger</logger>
            </fieldNames>
        </encoder>
    </appender>

    <root level="INFO">
        <appender-ref ref="CONSOLE_JSON" />
    </root>
</configuration>

// AuthService.java (refactorizado)
@Slf4j
@Service
public class AuthService {
    public ResponseEntity<?> login(LoginRequest req, String deviceFingerprint) {
        MDC.put("traceId", UUID.randomUUID().toString());
        MDC.put("identifier", maskSensitiveData(req.getIdentifier()));

        try {
            log.info("Login attempt started",
                kv("eventType", "LOGIN_ATTEMPT"),
                kv("deviceFingerprint", deviceFingerprint)
            );

            // Lógica de login

            log.info("Login successful",
                kv("eventType", "LOGIN_SUCCESS"),
                kv("userId", user.getId()),
                kv("sessionId", session.getSesId())
            );

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Login failed",
                kv("eventType", "LOGIN_FAILED"),
                kv("reason", e.getMessage()),
                e
            );
            throw e;
        } finally {
            MDC.clear();
        }
    }

    private String maskSensitiveData(String data) {
        // Ocultar parcialmente emails y RUTs
        if (data.contains("@")) {
            String[] parts = data.split("@");
            return parts[0].substring(0, 2) + "***@" + parts[1];
        }
        return data.substring(0, 3) + "***";
    }
}
```

**Checklist**:
- [ ] Agregar Logstash encoder
- [ ] Configurar logback con formato JSON
- [ ] Refactorizar todos los logs para usar MDC
- [ ] Implementar masking de datos sensibles

---

#### 3.2 Métricas con Micrometer
```java
// pom.xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>

// application.yml
management:
  endpoints:
    web:
      exposure:
        include: health,metrics,prometheus
  metrics:
    tags:
      application: naive-pay-api
      environment: ${ENVIRONMENT:dev}

// AuthMetricsService.java (NUEVO)
@Service
public class AuthMetricsService {
    private final MeterRegistry registry;

    private final Counter loginAttempts;
    private final Counter loginSuccesses;
    private final Counter loginFailures;
    private final Counter accountLocks;
    private final Timer loginDuration;

    public AuthMetricsService(MeterRegistry registry) {
        this.registry = registry;

        this.loginAttempts = Counter.builder("auth.login.attempts")
            .description("Total number of login attempts")
            .tag("type", "all")
            .register(registry);

        this.loginSuccesses = Counter.builder("auth.login.successes")
            .description("Successful login attempts")
            .register(registry);

        this.loginFailures = Counter.builder("auth.login.failures")
            .description("Failed login attempts")
            .register(registry);

        this.accountLocks = Counter.builder("auth.account.locks")
            .description("Number of accounts locked")
            .register(registry);

        this.loginDuration = Timer.builder("auth.login.duration")
            .description("Time taken to process login")
            .register(registry);
    }

    public void recordLoginAttempt() { loginAttempts.increment(); }
    public void recordLoginSuccess() { loginSuccesses.increment(); }
    public void recordLoginFailure(String reason) {
        loginFailures.increment();
        Counter.builder("auth.login.failures.by_reason")
            .tag("reason", reason)
            .register(registry)
            .increment();
    }
    public void recordAccountLock() { accountLocks.increment(); }

    public <T> T recordLoginDuration(Supplier<T> operation) {
        return loginDuration.record(operation);
    }
}

// AuthService.java (con métricas)
@Service
@RequiredArgsConstructor
public class AuthService {
    private final AuthMetricsService metrics;

    public ResponseEntity<?> login(LoginRequest req, String deviceFingerprint) {
        return metrics.recordLoginDuration(() -> {
            metrics.recordLoginAttempt();

            // Lógica de login

            if (loginSuccessful) {
                metrics.recordLoginSuccess();
            } else {
                metrics.recordLoginFailure(reason.name());
            }

            return response;
        });
    }
}

// Grafana Dashboard (ejemplo de consulta Prometheus)
# Tasa de éxito de login (últimos 5 minutos)
sum(rate(auth_login_successes_total[5m])) /
sum(rate(auth_login_attempts_total[5m])) * 100

# Top 5 razones de fallo
topk(5, sum by (reason) (rate(auth_login_failures_by_reason_total[1h])))

# Tiempo promedio de login (p95)
histogram_quantile(0.95,
  rate(auth_login_duration_seconds_bucket[5m]))
```

**Checklist**:
- [ ] Agregar Spring Actuator + Micrometer
- [ ] Crear servicio de métricas de autenticación
- [ ] Exponer métricas en `/actuator/prometheus`
- [ ] Configurar Grafana dashboard (opcional, si hay infraestructura)

---

#### 3.3 Health Checks
```java
// AuthHealthIndicator.java
@Component
public class AuthHealthIndicator implements HealthIndicator {
    private final JWTService jwtService;
    private final UserRepository userRepo;
    private final SessionRepository sessionRepo;

    @Override
    public Health health() {
        try {
            // Verificar capacidad de generar tokens
            String testToken = jwtService.generate("health-check", "test", UUID.randomUUID().toString());
            jwtService.parse(testToken);

            // Verificar conectividad con BD
            long userCount = userRepo.count();
            long sessionCount = sessionRepo.count();

            return Health.up()
                .withDetail("jwt", "operational")
                .withDetail("database", "connected")
                .withDetail("userCount", userCount)
                .withDetail("activeSessionCount", sessionCount)
                .build();

        } catch (Exception e) {
            return Health.down()
                .withDetail("error", e.getMessage())
                .build();
        }
    }
}

// application.yml
management:
  endpoint:
    health:
      show-details: always
  health:
    defaults:
      enabled: true
```

**Checklist**:
- [ ] Crear health indicator para autenticación
- [ ] Configurar endpoint `/actuator/health`
- [ ] Integrar con monitoring externo (Kubernetes liveness probe, etc.)

---

### Fase 4: Mejoras de Código y Documentación (Prioridad Media) 📋
**Duración estimada**: 1 semana
**Objetivo**: Código limpio y bien documentado

#### 4.1 Refactoring de Nombres y Métodos
```java
// ❌ ANTES
private boolean isBlank(String s) { return s == null || s.trim().isEmpty(); }
private void write401(HttpServletResponse response, String code, String msg) { ... }
private int calculateRemainingAttempts(User user) { ... }

// ✅ DESPUÉS
private boolean isNullOrEmpty(String value) {
    return value == null || value.trim().isEmpty();
}

private void writeUnauthorizedResponse(HttpServletResponse response, String errorCode, String message) {
    ...
}

private int getRemainingLoginAttempts(User user) {
    ...
}

// ❌ ANTES - Método largo (110 líneas)
public ResponseEntity<?> login(LoginRequest req, String deviceFingerprint) {
    // Validación
    // Resolver usuario
    // Verificar bloqueo
    // Verificar contraseña
    // Crear sesión
    // Manejar errores
}

// ✅ DESPUÉS - Métodos pequeños y enfocados
public ResponseEntity<?> login(LoginRequest req, String deviceFingerprint) {
    validateLoginRequest(req);
    User user = resolveAndAuthenticateUser(req);
    ensureAccountNotLocked(user);
    return createAuthenticatedSession(user, deviceFingerprint);
}
```

**Checklist**:
- [ ] Renombrar métodos con nombres poco descriptivos
- [ ] Dividir métodos largos (>20 líneas) en funciones más pequeñas
- [ ] Eliminar comentarios obvios
- [ ] Usar nombres de variables más expresivos

---

#### 4.2 Documentación JavaDoc
```java
/**
 * Service responsible for user authentication and session management.
 *
 * <p>This service handles the complete authentication flow including:
 * <ul>
 *   <li>Credential validation (email/RUT + password)</li>
 *   <li>Account lockout after failed attempts</li>
 *   <li>JWT token generation and validation</li>
 *   <li>Session lifecycle management</li>
 * </ul>
 *
 * <h2>Security Features</h2>
 * <ul>
 *   <li>Automatic account lock after 5 failed attempts within 30 minutes</li>
 *   <li>Device fingerprinting for enhanced security</li>
 *   <li>Audit trail of all authentication attempts</li>
 * </ul>
 *
 * @author NaivePay Team
 * @version 1.0
 * @since 2025-01-01
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    /**
     * Authenticates a user with the provided credentials.
     *
     * <p>This method performs the following operations:
     * <ol>
     *   <li>Validates input credentials</li>
     *   <li>Resolves user by email or RUT</li>
     *   <li>Checks if account is locked</li>
     *   <li>Verifies password</li>
     *   <li>Creates JWT token and session</li>
     *   <li>Logs authentication attempt</li>
     * </ol>
     *
     * @param req the login request containing identifier (email/RUT) and password
     * @param deviceFingerprint the unique identifier of the device making the request
     * @return ResponseEntity containing LoginResponse with JWT token on success,
     *         or error response with appropriate HTTP status code on failure
     *
     * @throws ResponseStatusException if device is not authorized
     *
     * @see LoginRequest
     * @see LoginResponse
     * @see AccountLockService#isAccountLocked(User)
     */
    public ResponseEntity<?> login(LoginRequest req, String deviceFingerprint) {
        // Implementation
    }

    /**
     * Invalidates the current user session and JWT token.
     *
     * <p>This method extracts the JWT token from the Authorization header,
     * retrieves the JTI (JWT ID) from the token, and marks the associated
     * session as closed in the database.
     *
     * @param authHeader the Authorization header containing "Bearer {token}"
     * @return ResponseEntity with success message if logout succeeds,
     *         or 401 Unauthorized if token is invalid or missing
     *
     * @see AuthSessionService#closeByJti(UUID)
     */
    public ResponseEntity<Map<String, Object>> logout(String authHeader) {
        // Implementation
    }
}
```

**Checklist**:
- [ ] Agregar JavaDoc a todas las clases públicas
- [ ] Documentar todos los métodos públicos con @param y @return
- [ ] Crear README.md específico para el módulo de autenticación
- [ ] Documentar configuraciones y variables de entorno

---

#### 4.3 Configuración Externalizada
```yaml
# application.yml (base)
naivepay:
  auth:
    max-failed-attempts: ${AUTH_MAX_FAILED_ATTEMPTS:5}
    lockout-window-minutes: ${AUTH_LOCKOUT_WINDOW_MINUTES:30}
    password-recovery-ttl-minutes: ${PASSWORD_RECOVERY_TTL:10}
  jwt:
    secret: ${JWT_SECRET:changeme}
    ttl-minutes: ${JWT_TTL_MINUTES:60}
  cors:
    allowed-origins: ${CORS_ALLOWED_ORIGINS:http://localhost:4200}

# application-dev.yml
naivepay:
  jwt:
    ttl-minutes: 60
  cors:
    allowed-origins:
      - http://localhost:4200
      - http://localhost:4201

# application-prod.yml
naivepay:
  jwt:
    ttl-minutes: 30
  cors:
    allowed-origins: ${CORS_ALLOWED_ORIGINS}  # Debe configurarse en variables de entorno

# AuthConfig.java (NUEVO)
@Configuration
@ConfigurationProperties(prefix = "naivepay.auth")
@Validated
@Data
public class AuthConfig {
    @Min(3)
    @Max(10)
    private int maxFailedAttempts = 5;

    @Min(5)
    @Max(120)
    private int lockoutWindowMinutes = 30;

    @Min(5)
    @Max(60)
    private int passwordRecoveryTtlMinutes = 10;
}

// Uso en servicios
@Service
@RequiredArgsConstructor
public class AccountLockService {
    private final AuthConfig authConfig;

    public boolean checkAndBlockIfNeeded(User user) {
        int maxAttempts = authConfig.getMaxFailedAttempts();
        int windowMinutes = authConfig.getLockoutWindowMinutes();
        // ...
    }
}
```

**Checklist**:
- [ ] Crear clases `@ConfigurationProperties` para grupos de configuración
- [ ] Externalizar todos los magic numbers a configuración
- [ ] Documentar todas las variables de entorno en README
- [ ] Validar configuraciones con Jakarta Validation

---

### Fase 5: Features Adicionales (Prioridad Baja) 💡
**Duración estimada**: 2 semanas (opcional)
**Objetivo**: Mejorar experiencia de usuario y seguridad

#### 5.1 Refresh Tokens
```java
// RefreshToken.java (NUEVA ENTIDAD)
@Entity
@Data
public class RefreshToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String token;

    @ManyToOne
    private User user;

    private Instant expiresAt;
    private boolean revoked;
}

// RefreshTokenService.java (NUEVO)
@Service
public class RefreshTokenService {
    public RefreshToken createRefreshToken(User user) {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken(UUID.randomUUID().toString());
        refreshToken.setUser(user);
        refreshToken.setExpiresAt(Instant.now().plus(7, ChronoUnit.DAYS));
        return refreshTokenRepository.save(refreshToken);
    }

    public Optional<RefreshToken> validateRefreshToken(String token) {
        return refreshTokenRepository.findByToken(token)
            .filter(rt -> !rt.isRevoked())
            .filter(rt -> rt.getExpiresAt().isAfter(Instant.now()));
    }
}

// AuthController.java (nuevo endpoint)
@PostMapping("/refresh")
public ResponseEntity<?> refreshToken(@RequestBody RefreshTokenRequest request) {
    return authService.refreshAccessToken(request.getRefreshToken());
}
```

**Checklist**:
- [ ] Crear entidad y repositorio para RefreshToken
- [ ] Implementar servicio de refresh tokens
- [ ] Agregar endpoint `/auth/refresh`
- [ ] Actualizar frontend para manejar refresh automático

---

#### 5.2 Multi-Factor Authentication (MFA)
```java
// MfaToken.java
@Entity
public class MfaToken {
    @Id
    @GeneratedValue
    private Long id;

    @ManyToOne
    private User user;

    private String code;
    private Instant expiresAt;
    private boolean verified;
}

// MfaService.java
@Service
public class MfaService {
    public String generateMfaCode(User user) {
        String code = String.format("%06d", new SecureRandom().nextInt(1000000));
        // Enviar código por email o SMS
        return code;
    }

    public boolean verifyMfaCode(User user, String code) {
        // Verificar código y marcar como verificado
    }
}

// Flujo de login con MFA
// 1. Usuario envía credenciales -> retorna { mfaRequired: true }
// 2. Backend envía código por email
// 3. Usuario envía código MFA
// 4. Backend valida y retorna JWT
```

---

#### 5.3 Rate Limiting
```java
// pom.xml
<dependency>
    <groupId>com.github.vladimir-bukhtoyarov</groupId>
    <artifactId>bucket4j-core</artifactId>
    <version>8.1.0</version>
</dependency>

// RateLimitingFilter.java
@Component
public class RateLimitingFilter extends OncePerRequestFilter {
    private final Map<String, Bucket> cache = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String ip = request.getRemoteAddr();
        Bucket bucket = cache.computeIfAbsent(ip, k -> createBucket());

        if (bucket.tryConsume(1)) {
            chain.doFilter(request, response);
        } else {
            response.setStatus(429);
            response.getWriter().write("{\"error\":\"TOO_MANY_REQUESTS\"}");
        }
    }

    private Bucket createBucket() {
        return Bucket.builder()
            .addLimit(Bandwidth.classic(100, Refill.intervally(100, Duration.ofMinutes(1))))
            .build();
    }
}
```

---

## Roadmap de Implementación

### Sprint 1 (Semana 1) - Seguridad Crítica 🔴
**Prioridad**: P0
**Objetivo**: Eliminar vulnerabilidades críticas

- [ ] Día 1-2: Externalizar secretos (JWT, email) a variables de entorno
- [ ] Día 3: Implementar validación de DTOs con Jakarta Validation
- [ ] Día 4: Expandir GlobalExceptionHandler
- [ ] Día 5: Auditoría de seguridad y penetration testing básico

**Criterios de aceptación**:
- ✅ No hay secretos en `application.properties`
- ✅ Todos los DTOs tienen validaciones
- ✅ Todas las excepciones se manejan correctamente
- ✅ Tests de seguridad pasan

---

### Sprint 2 (Semana 2) - Refactorización Arquitectónica ⚠️
**Prioridad**: P1
**Objetivo**: Aplicar principios SOLID y mejorar mantenibilidad

- [ ] Día 1-2: Centralizar PUBLIC_PATHS en SecurityConstants
- [ ] Día 3-4: Dividir AuthService en CredentialValidator + SessionManager
- [ ] Día 5: Refactorizar JwtAuthFilter (eliminar System.out.println, simplificar)

**Criterios de aceptación**:
- ✅ No hay duplicación de código
- ✅ Cada servicio tiene una sola responsabilidad
- ✅ Todos los logs usan SLF4J

---

### Sprint 3 (Semana 3) - Testing Backend ⚠️
**Prioridad**: P1
**Objetivo**: Cobertura >80% en servicios críticos

- [ ] Día 1-2: Tests unitarios de AuthService
- [ ] Día 3: Tests unitarios de JWTService, AccountLockService
- [ ] Día 4: Tests de integración (AuthControllerIT)
- [ ] Día 5: Configurar Jacoco y revisar cobertura

**Criterios de aceptación**:
- ✅ Cobertura >80% en servicios de autenticación
- ✅ Todos los tests pasan
- ✅ CI/CD ejecuta tests automáticamente

---

### Sprint 4 (Semana 4) - Testing Frontend 📋
**Prioridad**: P1
**Objetivo**: Tests unitarios y E2E completos

- [ ] Día 1-2: Tests unitarios de AutentificacionService y LoginComponent
- [ ] Día 3-4: Tests E2E con Cypress (login, recovery, logout)
- [ ] Día 5: Revisión y debugging de tests

**Criterios de aceptación**:
- ✅ Todos los servicios y componentes tienen tests
- ✅ Tests E2E cubren flujos críticos
- ✅ Tests corren en CI/CD

---

### Sprint 5 (Semana 5) - Observabilidad 📋
**Prioridad**: P2
**Objetivo**: Visibilidad completa en producción

- [ ] Día 1-2: Implementar structured logging con Logstash encoder
- [ ] Día 3: Agregar métricas con Micrometer
- [ ] Día 4: Crear health checks
- [ ] Día 5: Configurar dashboard de Grafana (opcional)

**Criterios de aceptación**:
- ✅ Logs en formato JSON con MDC
- ✅ Métricas expuestas en `/actuator/prometheus`
- ✅ Health check funcional

---

### Sprint 6 (Semana 6) - Clean Code y Documentación 📋
**Prioridad**: P2-P3
**Objetivo**: Código limpio y bien documentado

- [ ] Día 1-2: Refactorizar nombres de métodos y variables
- [ ] Día 3: Agregar JavaDoc a todas las clases públicas
- [ ] Día 4: Externalizar configuraciones
- [ ] Día 5: Crear README del módulo de autenticación

**Criterios de aceptación**:
- ✅ No hay métodos >20 líneas
- ✅ Todas las clases públicas tienen JavaDoc
- ✅ Magic numbers externalizados

---

### Sprint 7 (Semana 7-8) - Features Adicionales (Opcional) 💡
**Prioridad**: P3
**Objetivo**: Mejorar UX y seguridad

- [ ] Semana 7: Implementar refresh tokens
- [ ] Semana 8: Implementar rate limiting y considerar MFA

**Criterios de aceptación**:
- ✅ Refresh tokens funcionales
- ✅ Rate limiting protege endpoints públicos
- ✅ MFA opcional disponible

---

## Métricas de Éxito

### Seguridad
- ✅ 0 secretos en código fuente
- ✅ 0 vulnerabilidades críticas en escaneo de seguridad
- ✅ 100% de DTOs con validación

### Calidad de Código
- ✅ Cobertura de tests >80%
- ✅ 0 violaciones de SonarQube críticas
- ✅ Complejidad ciclomática <10 en todos los métodos

### Observabilidad
- ✅ 100% de endpoints con logs estructurados
- ✅ Métricas de negocio expuestas
- ✅ Health checks implementados

### Mantenibilidad
- ✅ 100% de servicios públicos documentados
- ✅ 0 métodos >20 líneas
- ✅ 0 duplicación de código

---

## Conclusión

Este plan de refactorización transforma el módulo de autenticación de NaivePay en un sistema robusto, seguro y mantenible que sigue las mejores prácticas de la industria. La implementación gradual en sprints permite mitigar riesgos y mantener la funcionalidad durante todo el proceso.

### Próximos Pasos Inmediatos
1. **Semana 1**: Ejecutar Sprint 1 (Seguridad Crítica)
2. **Presentar plan**: Revisar con equipo de desarrollo
3. **Priorizar**: Ajustar prioridades según necesidades de negocio
4. **Ejecutar**: Comenzar implementación iterativa

### Recursos Recomendados
- [Spring Security Reference](https://docs.spring.io/spring-security/reference/)
- [OWASP Top 10](https://owasp.org/www-project-top-ten/)
- [Clean Code by Robert Martin](https://www.oreilly.com/library/view/clean-code-a/9780136083238/)
- [Spring Boot Testing Best Practices](https://spring.io/guides/gs/testing-web/)

---

**Documento creado**: 2025-11-05
**Autor**: Claude AI
**Versión**: 1.0
**Estado**: Propuesta inicial
