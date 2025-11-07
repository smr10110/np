### 6. **Falta de Auditoría en Cambios de Contraseña**

**Archivo:** `PasswordRecoveryService.java:78-93`

**Problema:**
```java
@Transactional
public void resetPassword(String email, String code, String newPassword) {
    PasswordRecovery recovery = validateRecoveryCode(email, code);
    User user = recovery.getUser();

    user.getRegister().setHashedLoginPassword(passwordEncoder.encode(newPassword));
    recovery.setPasStatus(PasswordRecoveryStatus.USED);
    recovery.setPasUsed(Instant.now());

    // ❌ No registra IP, User-Agent, timestamp en tabla de auditoría
}
```

**Mejora:**
```java
@Transactional
public void resetPassword(String email, String code, String newPassword, String ipAddress, String userAgent) {
    PasswordRecovery recovery = validateRecoveryCode(email, code);
    User user = recovery.getUser();

    user.getRegister().setHashedLoginPassword(passwordEncoder.encode(newPassword));
    recovery.setPasStatus(PasswordRecoveryStatus.USED);
    recovery.setPasUsed(Instant.now());

    if (user.getState() == AccountState.INACTIVE) {
        user.setState(AccountState.ACTIVE);
        logger.info("Cuenta desbloqueada tras recuperación: userId={}", user.getId());
    }

    // Auditoría
    auditService.logPasswordChange(user.getId(), ipAddress, userAgent, "PASSWORD_RECOVERY");

    logger.info("Contraseña actualizada para usuario {} desde IP {}", user.getId(), ipAddress);
}
```

**Impacto:** 🟡 IMPORTANTE (Seguridad + Compliance)
**Esfuerzo:** 🟡 Medio (3 horas)

---

### 8. **Mensajes de Error Genéricos Poco Útiles**

**Archivo:** `GlobalExceptionHandler.java:18`

**Problema:**
```java
@ExceptionHandler(ResponseStatusException.class)
public ResponseEntity<Map<String, String>> handleResponseStatusException(ResponseStatusException ex) {
    return ResponseEntity
            .status(ex.getStatusCode())
            .body(Map.of(
                    "error", ex.getStatusCode().toString(),  // ❌ "400 BAD_REQUEST" (técnico)
                    "message", ex.getReason() != null ? ex.getReason() : "Error"
            ));
}
```

**Problemas:**
1. `"error": "400 BAD_REQUEST"` es muy técnico para usuarios
2. `"message": "INVALID_CODE"` tampoco es user-friendly

**Solución:**
```java
@ExceptionHandler(ResponseStatusException.class)
public ResponseEntity<Map<String, Object>> handleResponseStatusException(
        ResponseStatusException ex, HttpServletRequest request) {

    String userFriendlyMessage = getUserFriendlyMessage(ex.getReason());

    return ResponseEntity
            .status(ex.getStatusCode())
            .body(Map.of(
                    "error", ex.getReason() != null ? ex.getReason() : "ERROR",
                    "message", userFriendlyMessage,
                    "timestamp", Instant.now().toString(),
                    "path", request.getRequestURI()
            ));
}

private String getUserFriendlyMessage(String errorCode) {
    return switch (errorCode) {
        case "INVALID_CODE" -> "El código ingresado es inválido";
        case "CODE_EXPIRED" -> "El código ha expirado. Solicita uno nuevo";
        case "CODE_ALREADY_USED" -> "Este código ya fue utilizado";
        default -> "Ha ocurrido un error";
    };
}
```

**Impacto:** 🟡 IMPORTANTE (UX)
**Esfuerzo:** 🟢 Bajo (1 hora)

---


### 11. **Constante Hardcodeada en `generateCode()`**

**Archivo:** `PasswordRecoveryService.java:116`

**Problema:**
```java
private String generateCode() {
    return String.format("%06d", SECURE_RANDOM.nextInt(1000000)); // ❌ 1000000 hardcoded
}
```

**Mejora:**
```java
private static final int CODE_LENGTH = 6;
private static final int CODE_MAX_VALUE = (int) Math.pow(10, CODE_LENGTH);

private String generateCode() {
    return String.format("%0" + CODE_LENGTH + "d", SECURE_RANDOM.nextInt(CODE_MAX_VALUE));
}
```

**Impacto:** 🟢 MENOR (Mantenibilidad)
**Esfuerzo:** 🟢 Bajo (5 min)

---

### 12. **Falta Try-Catch en Envío de Email**

**Archivo:** `PasswordRecoveryService.java:68`

**Problema:**
```java
passwordRecoveryRepository.save(recovery);
emailService.sendPasswordRecoveryEmail(email, code); // ❌ Si falla, toda la transacción se revierte
logger.debug("Código generado: {}", code);
```

**Riesgo:**
- Si el servidor SMTP está caído, la transacción falla
- El código no se guarda en BD
- Usuario no puede recuperar contraseña hasta que SMTP funcione

**Solución:**
```java
passwordRecoveryRepository.save(recovery);

try {
    emailService.sendPasswordRecoveryEmail(email, code);
    logger.info("Email de recuperación enviado a usuario {}", user.getId());
} catch (MailException ex) {
    logger.error("Error enviando email a {}: {}", email, ex.getMessage());
    // El código ya está guardado, el usuario puede intentar más tarde
    // O implementar reenvío de código
}
```

**Impacto:** 🟢 MENOR (Resilencia)
**Esfuerzo:** 🟢 Bajo (15 min)

---

## 🔧 PROBLEMAS DE ARQUITECTURA

### 13. **Inconsistencia en Formato de Respuestas**

**Problema:**
```java
// AuthController - Password Recovery
return ResponseEntity.ok(Map.of("message", "Código enviado"));

// AuthController - Login
return authService.login(req, deviceFingerprint); // ← Retorna ResponseEntity<?> custom

// GlobalExceptionHandler
return ResponseEntity.status(...).body(Map.of("error", ..., "message", ...));
```

**Análisis:**
- Login retorna objeto complejo con `token`, `user`, etc.
- Password recovery retorna `Map<String, String>`
- Errores retornan `Map<String, String>`
- **No hay DTOs de respuesta estandarizados**

**Solución:**
```java
// Crear DTOs de respuesta
@Data
@AllArgsConstructor
public class ApiResponse<T> {
    private boolean success;
    private String message;
    private T data;
    private String timestamp;
}

@Data
@AllArgsConstructor
public class ErrorResponse {
    private String error;
    private String message;
    private String path;
    private String timestamp;
}

// Usar en controllers
@PostMapping("/password/request")
public ResponseEntity<ApiResponse<Void>> requestPasswordReset(@Valid @RequestBody ForgotPasswordRequest request) {
    passwordRecoveryService.sendRecoveryCode(request.getEmail());
    return ResponseEntity.ok(new ApiResponse<>(
        true,
        "Si el email existe, recibirás un código",
        null,
        Instant.now().toString()
    ));
}
```

**Impacto:** 🟡 IMPORTANTE (Consistencia)
**Esfuerzo:** 🟡 Medio (3 horas)

---

### 14. **Duplicación de PUBLIC_PATHS y PUBLIC_ENDPOINTS**

**Archivos:**
- `JwtAuthFilter.java:38-45` → `PUBLIC_PATHS`
- `SecurityConfig.java:26-34` → `PUBLIC_ENDPOINTS`

**Problema:**
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

// SecurityConfig.java
private static final String[] PUBLIC_ENDPOINTS = {
    "/h2-console/**",
    "/api/register/**",
    "/auth/login",
    "/auth/recovery/**",  // ← Diferente!
    "/auth/password/**",
    "/api/dispositivos/recover/**",
    "/api/devices/recover/**"
};
```

**Problemas:**
1. **Duplicación** → Violar DRY
2. **Inconsistencia** → `/auth/recovery/**` solo en uno
3. **Difícil mantenimiento** → Hay que cambiar en 2 lugares

**Solución:**
```java
// Crear clase de constantes
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

// Usar en ambos lugares
@Component
public class JwtAuthFilter extends OncePerRequestFilter {
    private static final String[] PUBLIC_PATHS = SecurityConstants.PUBLIC_ENDPOINTS;
    // ...
}

@Configuration
public class SecurityConfig {
    private static final String[] PUBLIC_ENDPOINTS = SecurityConstants.PUBLIC_ENDPOINTS;
    // ...
}
```

**Impacto:** 🟡 IMPORTANTE (Mantenibilidad)
**Esfuerzo:** 🟢 Bajo (30 min)

---

### 15. **Sin Manejo de Excepciones en `EmailService`**

**Archivo:** `EmailService.java:39-52`

**Problema:**
```java
public void sendPasswordRecoveryEmail(String to, String code) {
    SimpleMailMessage message = new SimpleMailMessage();
    message.setTo(to);
    message.setSubject("Recuperación de Contraseña - NaivePay");
    message.setText(...);
    mailSender.send(message); // ❌ Si falla, lanza MailException sin manejar
}
```

**Riesgo:**
- Si SMTP falla, toda la transacción se revierte
- No hay logs del error específico

**Solución:**
```java
public void sendPasswordRecoveryEmail(String to, String code) {
    try {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("Recuperación de Contraseña - NaivePay");
        message.setText(
            "Hola,\n\n" +
            "Recibimos una solicitud para restablecer tu contraseña.\n" +
            "Usa el siguiente código de verificación:\n\n" +
            code + "\n\n" +
            "Este código expira en 15 minutos.\n\n" +
            "Si no solicitaste este cambio, ignora este mensaje."
        );
        mailSender.send(message);
        logger.info("Email de recuperación enviado a {}", to);
    } catch (MailException e) {
        logger.error("Error enviando email a {}: {}", to, e.getMessage(), e);
        throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
            "Error al enviar email. Intenta más tarde.");
    }
}
```

**Impacto:** 🟡 IMPORTANTE (Resilencia)
**Esfuerzo:** 🟢 Bajo (20 min)

---

## 📊 RESUMEN DE REFACTORIZACIONES RECOMENDADAS

### Prioridad CRÍTICA (Hacer Ahora)

| # | Problema | Archivo | Esfuerzo | Impacto |
|---|----------|---------|----------|---------|
| 1 | Validaciones en DTOs | `*Request.java` | 30 min | 🔴 Seguridad |
| 2 | No devolver email en respuesta | `AuthController.java:38` | 5 min | 🔴 Seguridad |
| 3 | No loggear código | `PasswordRecoveryService.java:69` | 5 min | 🔴 Seguridad |
| 4 | Rate limiting | `AuthController.java` | 2 hrs | 🔴 Seguridad |

**Total Esfuerzo:** ~3 horas
**Beneficio:** Prevenir vulnerabilidades críticas

---

### Prioridad ALTA (Hacer Esta Semana)

| # | Problema | Archivo | Esfuerzo | Impacto |
|---|----------|---------|----------|---------|
| 6 | Auditoría de cambios | `PasswordRecoveryService.java` | 3 hrs | 🟡 Compliance |
| 7 | Notificación de cambio | `PasswordRecoveryService.java` | 30 min | 🟡 Seguridad |
| 8 | Mensajes user-friendly | `GlobalExceptionHandler.java` | 1 hr | 🟡 UX |
| 14 | Eliminar duplicación PUBLIC_* | `SecurityConfig.java`, `JwtAuthFilter.java` | 30 min | 🟡 Mantenibilidad |
| 15 | Try-catch en emails | `EmailService.java` | 20 min | 🟡 Resilencia |

**Total Esfuerzo:** ~5.5 horas
**Beneficio:** Mejor seguridad y experiencia de usuario

---

### Prioridad MEDIA (Backlog)

| # | Problema | Archivo | Esfuerzo | Impacto |
|---|----------|---------|----------|---------|
| 9 | Evaluar si eliminar `/verify` | `AuthController.java` | 10 min | 🟢 Simplicidad |
| 11 | Extraer constantes en `generateCode()` | `PasswordRecoveryService.java` | 5 min | 🟢 Mantenibilidad |
| 12 | Try-catch en envío email (service) | `PasswordRecoveryService.java` | 15 min | 🟢 Resilencia |
| 13 | DTOs de respuesta estandarizados | Todos los controllers | 3 hrs | 🟡 Consistencia |

**Total Esfuerzo:** ~3.5 horas

---

### No Prioritario (Deuda Técnica)

| # | Problema | Esfuerzo | Razón |
|---|----------|----------|-------|
| 10 | Renombrar campos `pas*` | 3 hrs | Requiere migración BD, bajo ROI |

---

## ✅ COSAS QUE ESTÁN BIEN HECHAS

1. ✅ **Uso de `SecureRandom` estático** - Excelente performance
2. ✅ **No revelar si email existe** - Previene enumeración de usuarios
3. ✅ **Invalidación de códigos PENDING anteriores** - Previene múltiples códigos activos
4. ✅ **Uso de `@Transactional`** - Garantiza consistencia de datos
5. ✅ **Método `validateRecoveryCode()` privado** - Elimina duplicación (DRY)
6. ✅ **Auto-desbloqueo de cuentas INACTIVE** - Buena experiencia de usuario
7. ✅ **Separación de responsabilidades** - Service/Controller/Repository claros
8. ✅ **Reutilización de `EmailService`** - No reinventar la rueda
9. ✅ **`GlobalExceptionHandler`** - Centraliza manejo de errores
10. ✅ **Configuración de CORS** - Permite frontend Angular

---

## 📋 PLAN DE ACCIÓN RECOMENDADO

### Sprint 1: Seguridad Crítica (1 día)
```
1. Agregar validaciones @Valid a DTOs
2. Remover email de respuesta en /password/request
3. Eliminar logging de código
4. Implementar rate limiting básico (3 intentos/15min)
```

### Sprint 2: Mejoras de Seguridad (1 día)
```
5. Agregar auditoría de cambios de contraseña
6. Implementar notificación por email de cambio exitoso
7. Mejorar mensajes de error user-friendly
8. Agregar try-catch en EmailService
```

### Sprint 3: Refactorización (1 día)
```
9. Eliminar duplicación PUBLIC_PATHS/PUBLIC_ENDPOINTS
10. Crear DTOs de respuesta estandarizados
11. Evaluar eliminar endpoint /password/verify
12. Extraer constantes mágicas
```

---

## 🎯 MÉTRICAS DE ÉXITO

**Antes de Refactorización:**
- Cobertura de tests: 0%
- Vulnerabilidades: 4 críticas
- Deuda técnica: ~10 horas
- Code smells: 15

**Después de Refactorización:**
- Cobertura de tests: 80%+
- Vulnerabilidades: 0 críticas
- Deuda técnica: ~3 horas
- Code smells: <5

---

## 📚 REFERENCIAS

- [OWASP Password Reset Best Practices](https://cheatsheetseries.owasp.org/cheatsheets/Forgot_Password_Cheat_Sheet.html)
- [Spring Validation Documentation](https://docs.spring.io/spring-framework/reference/core/validation/beanvalidation.html)
- [Bucket4j Rate Limiting](https://github.com/bucket4j/bucket4j)
- [Clean Code - Robert C. Martin](https://www.amazon.com/Clean-Code-Handbook-Software-Craftsmanship/dp/0132350882)

---

**Última Actualización:** 2025-11-02
**Responsable:** Equipo de Desarrollo
**Estado:** Pendiente de Implementación
