# 🔐 Plan Simple: Recuperación de Contraseña y Bloqueo de Cuenta

**Proyecto:** NaivePay - Prototipo
**Fecha:** 2025-01-31
**Enfoque:** SIMPLE, PRAGMÁTICO, REUTILIZAR CÓDIGO EXISTENTE

---

## 📋 Contexto Actual

### ✅ Lo que YA TIENES y podemos reutilizar:

1. **Tabla `Register`** con:
   - ✅ `verificationCode` (String)
   - ✅ `verificationCodeExpiration` (Date)
   - ✅ `email` (String)
   - ✅ **PERFECTO para password reset!**

2. **Enum `AccountState`**:
   - `ACTIVE` = Usuario normal ✅
   - `INACTIVE` = Usuario bloqueado ✅
   - **NO necesitas crear más estados**

3. **Tabla `AuthAttempt`**:
   - ✅ Registra todos los intentos
   - ✅ Tiene timestamp
   - ✅ Tiene success/fail

4. **Servicios existentes**:
   - ✅ `AuthAttemptService` (registra intentos)
   - ✅ `PasswordEncoder` (para hashear)
   - ✅ `UserRepository` (acceso a usuarios)

---

## 🎯 Funcionalidad 1: Bloqueo por 5 Intentos Fallidos

### **Estrategia SIMPLE:**

> **Cambiar `AccountState` a `INACTIVE` después de 5 intentos fallidos consecutivos**

### 📝 **Lo que necesitas:**

1. Query para contar intentos fallidos recientes
2. Lógica en `AuthService.login()` para verificar y bloquear
3. Verificar estado `INACTIVE` antes de permitir login

### 🔧 **Implementación (1 hora):**

#### **Paso 1: Agregar query en `AuthAttemptRepository`**

```java
package cl.ufro.dci.naivepayapi.autentificacion.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.Instant;

public interface AuthAttemptRepository extends JpaRepository<AuthAttempt, Long> {

    /**
     * Cuenta intentos fallidos de un usuario desde una fecha.
     */
    @Query("""
        SELECT COUNT(a)
        FROM AuthAttempt a
        WHERE a.user.id = :userId
        AND a.attSuccess = false
        AND a.attOccurred > :since
        """)
    long countFailedAttemptsSince(
        @Param("userId") Long userId,
        @Param("since") Instant since
    );
}
```

---

#### **Paso 2: Actualizar `AuthService.login()`**

```java
// Al inicio del método login(), DESPUÉS de resolver el usuario:

User user = userOpt.get();

// ✅ NUEVO: Verificar si está bloqueado
if (user.getState() == AccountState.INACTIVE) {
    logAttempt(user, deviceFingerprint, null, false, AuthAttemptReason.ACCOUNT_BLOCKED);
    return ResponseEntity.status(HttpStatus.FORBIDDEN)
        .body(Map.of("error", "ACCOUNT_BLOCKED"));
}

// 2) Verificar contraseña
if (!isValidPassword(user, req.getPassword())) {
    logFailedAttempt(user, AuthAttemptReason.BAD_CREDENTIALS);

    // ✅ NUEVO: Verificar intentos fallidos y bloquear si es necesario
    checkAndBlockAccount(user);

    return unauthorized(AuthAttemptReason.BAD_CREDENTIALS);
}
```

---

#### **Paso 3: Agregar método helper `checkAndBlockAccount()`**

```java
// En AuthService, sección Helpers - Validation

/**
 * Verifica intentos fallidos y bloquea la cuenta si hay 5 en los últimos 30 minutos.
 */
private void checkAndBlockAccount(User user) {
    // Contar intentos fallidos en últimos 30 minutos
    Instant thirtyMinutesAgo = Instant.now().minus(30, ChronoUnit.MINUTES);
    long failedAttempts = authAttemptRepository.countFailedAttemptsSince(
        user.getId(),
        thirtyMinutesAgo
    );

    // Si hay 5 o más intentos fallidos, bloquear
    if (failedAttempts >= 5) {
        user.setState(AccountState.INACTIVE);
        userRepo.save(user);

        // Log para auditoría
        log.warn("Cuenta bloqueada por intentos fallidos: userId={}, email={}",
            user.getId(),
            user.getRegister().getEmail()
        );

        // TODO: Aquí podrías enviar email notificando (opcional)
    }
}
```

---

#### **Paso 4: Agregar nueva razón en `AuthAttemptReason`**

```java
public enum AuthAttemptReason {
    OK,
    BAD_CREDENTIALS,
    USER_NOT_FOUND,
    DEVICE_REQUIRED,
    DEVICE_UNAUTHORIZED,
    ACCOUNT_BLOCKED  // ✅ NUEVO
}
```

---

### ✅ **Resultado:**
- Usuario intenta login 5 veces con contraseña incorrecta
- Automáticamente se pone `state = INACTIVE`
- Ya no puede hacer login (recibe `ACCOUNT_BLOCKED`)
- Admin puede desbloquearlo manualmente cambiando `state = ACTIVE`

---

## 🔑 Funcionalidad 2: Recuperación de Contraseña

### **Estrategia SIMPLE:**

> **Reutilizar `verificationCode` y `verificationCodeExpiration` de la tabla `Register`**

### 📝 **Lo que necesitas:**

1. Endpoint para solicitar código
2. Endpoint para verificar código y cambiar contraseña
3. Lógica para generar código y guardarlo (REUTILIZAR código de registro)

### 🔧 **Implementación (1.5 horas):**

#### **Paso 1: Crear DTOs simples**

```java
// ForgotPasswordRequest.java
package cl.ufro.dci.naivepayapi.autentificacion.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class ForgotPasswordRequest {
    @NotBlank
    @Email
    private String email;

    // getters y setters
}
```

```java
// ResetPasswordRequest.java
package cl.ufro.dci.naivepayapi.autentificacion.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ResetPasswordRequest {
    @NotBlank
    @Email
    private String email;

    @NotBlank
    @Size(min = 6, max = 6)
    private String code;

    @NotBlank
    @Size(min = 8)
    private String newPassword;

    // getters y setters
}
```

---

#### **Paso 2: Crear servicio `PasswordRecoveryService`**

```java
package cl.ufro.dci.naivepayapi.autentificacion.service;

import cl.ufro.dci.naivepayapi.registro.domain.User;
import cl.ufro.dci.naivepayapi.registro.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

@Service
public class PasswordRecoveryService {

    private final UserRepository userRepo;
    private final PasswordEncoder passwordEncoder;
    // TODO: Agregar EmailService cuando esté disponible

    public PasswordRecoveryService(UserRepository userRepo, PasswordEncoder passwordEncoder) {
        this.userRepo = userRepo;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Genera código de 6 dígitos y lo guarda en Register.verificationCode
     */
    @Transactional
    public void sendRecoveryCode(String email) {
        // Buscar usuario por email (sin revelar si existe)
        var userOpt = userRepo.findByRegisterEmail(email);
        if (userOpt.isEmpty()) {
            // Por seguridad, no revelar que el email no existe
            return;
        }

        User user = userOpt.get();

        // Generar código de 6 dígitos
        String code = generateCode();

        // Guardar código hasheado en Register (REUTILIZAR campo existente)
        user.getRegister().setVerificationCode(passwordEncoder.encode(code));

        // Expiración: 15 minutos
        Date expiration = Date.from(Instant.now().plus(15, ChronoUnit.MINUTES));
        user.getRegister().setVerificationCodeExpiration(expiration);

        userRepo.save(user);

        // TODO: Enviar email con código
        // Por ahora, loggear en consola (SOLO PARA DESARROLLO)
        System.out.println("==================================");
        System.out.println("📧 CÓDIGO DE RECUPERACIÓN");
        System.out.println("Email: " + email);
        System.out.println("Código: " + code);
        System.out.println("==================================");
    }

    /**
     * Verifica código y actualiza contraseña
     */
    @Transactional
    public void resetPassword(String email, String code, String newPassword) {
        // Buscar usuario
        User user = userRepo.findByRegisterEmail(email)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "INVALID_CODE"
            ));

        var register = user.getRegister();

        // Verificar que hay un código
        if (register.getVerificationCode() == null) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "NO_CODE_REQUESTED"
            );
        }

        // Verificar expiración
        if (register.getVerificationCodeExpiration() == null ||
            new Date().after(register.getVerificationCodeExpiration())) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "CODE_EXPIRED"
            );
        }

        // Verificar código (comparar hasheado)
        if (!passwordEncoder.matches(code, register.getVerificationCode())) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "INVALID_CODE"
            );
        }

        // Validar nueva contraseña (mínimo 8 caracteres)
        if (newPassword.length() < 8) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "PASSWORD_TOO_SHORT"
            );
        }

        // Actualizar contraseña
        register.setHashedLoginPassword(passwordEncoder.encode(newPassword));

        // Limpiar código usado (seguridad)
        register.setVerificationCode(null);
        register.setVerificationCodeExpiration(null);

        userRepo.save(user);

        // Si la cuenta estaba bloqueada, desbloquearla
        if (user.getState() == AccountState.INACTIVE) {
            user.setState(AccountState.ACTIVE);
            userRepo.save(user);
        }
    }

    private String generateCode() {
        SecureRandom random = new SecureRandom();
        return String.format("%06d", random.nextInt(1000000));
    }
}
```

---

#### **Paso 3: Agregar endpoints en `AuthController`**

```java
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final PasswordRecoveryService passwordRecoveryService;

    // ... constructor con nueva dependencia ...

    /**
     * Solicita código de recuperación.
     * POST /auth/forgot-password
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(
            @RequestBody @Valid ForgotPasswordRequest request) {

        passwordRecoveryService.sendRecoveryCode(request.getEmail());

        // Siempre retorna mismo mensaje (seguridad)
        return ResponseEntity.ok(Map.of(
            "message", "Si el email existe, recibirás un código"
        ));
    }

    /**
     * Verifica código y cambia contraseña.
     * POST /auth/reset-password
     */
    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(
            @RequestBody @Valid ResetPasswordRequest request) {

        passwordRecoveryService.resetPassword(
            request.getEmail(),
            request.getCode(),
            request.getNewPassword()
        );

        return ResponseEntity.ok(Map.of(
            "message", "Contraseña actualizada exitosamente"
        ));
    }
}
```

---

### ✅ **Resultado:**
- Usuario solicita reset → Recibe código de 6 dígitos (por ahora en logs)
- Usuario ingresa código + nueva contraseña → Se actualiza
- Si estaba bloqueado (`INACTIVE`), se desbloquea automáticamente
- Código expira en 15 minutos

---

## 📊 Resumen de Cambios

### **Archivos a CREAR:**

| Archivo | Descripción | Líneas |
|---------|-------------|--------|
| `PasswordRecoveryService.java` | Lógica de recovery | ~100 |
| `ForgotPasswordRequest.java` | DTO request | ~10 |
| `ResetPasswordRequest.java` | DTO request | ~20 |

**Total:** 3 archivos nuevos, ~130 líneas

### **Archivos a MODIFICAR:**

| Archivo | Qué cambiar | Líneas |
|---------|-------------|--------|
| `AuthAttemptRepository.java` | Agregar query `countFailedAttemptsSince()` | +10 |
| `AuthAttemptReason.java` | Agregar `ACCOUNT_BLOCKED` | +1 |
| `AuthService.java` | Verificar INACTIVE + método `checkAndBlockAccount()` | +30 |
| `AuthController.java` | Agregar 2 endpoints | +25 |
| `UserRepository.java` | Agregar `findByRegisterEmail()` (si no existe) | +5 |

**Total:** 5 archivos modificados, ~70 líneas

---

## ⏱️ Estimación de Tiempo

| Funcionalidad | Tiempo | Prioridad |
|---------------|--------|-----------|
| **Bloqueo por intentos** | 1 hora | 🔥 ALTA |
| **Recovery de contraseña** | 1.5 horas | 🔥 ALTA |
| **Testing manual** | 30 min | MEDIA |
| **TOTAL** | **3 horas** | - |

---

## 🎯 Plan de Implementación

### **Fase 1: Bloqueo de Cuenta (1 hora)**

```bash
1. Agregar query en AuthAttemptRepository (5 min)
2. Agregar ACCOUNT_BLOCKED en AuthAttemptReason (2 min)
3. Agregar checkAndBlockAccount() en AuthService (15 min)
4. Modificar login() para verificar INACTIVE (10 min)
5. Probar manualmente (15 min)
6. Ajustar si es necesario (13 min)
```

### **Fase 2: Password Recovery (1.5 horas)**

```bash
1. Crear DTOs (ForgotPasswordRequest, ResetPasswordRequest) (10 min)
2. Crear PasswordRecoveryService (40 min)
3. Agregar endpoints en AuthController (15 min)
4. Probar manualmente (20 min)
5. Ajustar si es necesario (5 min)
```

### **Fase 3: Testing (30 min)**

```bash
1. Probar bloqueo con 5 intentos fallidos (10 min)
2. Probar recovery completo (15 min)
3. Verificar desbloqueo automático tras recovery (5 min)
```

---

## 🧪 Casos de Prueba

### **Bloqueo:**

1. ✅ Usuario falla 5 veces → `state = INACTIVE`
2. ✅ Usuario con `INACTIVE` intenta login → Error `ACCOUNT_BLOCKED`
3. ✅ Intentos espaciados (>30 min) no bloquean
4. ✅ Intento exitoso resetea contador

### **Recovery:**

1. ✅ Solicitar código → Aparece en logs (desarrollo)
2. ✅ Código correcto → Actualiza contraseña
3. ✅ Código expirado (>15 min) → Error `CODE_EXPIRED`
4. ✅ Código incorrecto → Error `INVALID_CODE`
5. ✅ Recovery desbloquea cuenta si estaba `INACTIVE`

---

## 💡 Ventajas de Este Plan

1. ✅ **Reutiliza tabla `Register`** (no crea nueva entidad)
2. ✅ **Usa `AccountState` existente** (no agrega estados nuevos)
3. ✅ **Simple y directo** (perfecto para prototipo)
4. ✅ **3 horas de implementación** (factible en 1 día)
5. ✅ **Seguro** (códigos hasheados, TTL, rate limiting implícito)
6. ✅ **Email opcional** (logs en desarrollo, fácil migrar a email real)

---

## 🔄 Mejoras Futuras (Opcional)

### **Cuando tengas más tiempo:**

- [ ] Implementar servicio de email real (Spring Mail)
- [ ] Agregar templates HTML para emails
- [ ] Rate limiting explícito (máximo 3 recovery por hora)
- [ ] Panel admin para desbloquear cuentas
- [ ] Notificación por email al bloquear cuenta
- [ ] Endpoint de desbloqueo vía token

**Pero para el prototipo, NO SON NECESARIAS.**

---

## ✅ ¿Listo para Empezar?

**Recomendación:** Implementa primero el **bloqueo** (1 hora) porque:
- Es más simple
- Más crítico para seguridad
- No requiere email
- Puedes probarlo inmediatamente

Luego implementa **recovery** (1.5 horas) y tienes todo funcionando en **menos de 3 horas**.

---

**¿Empezamos con el bloqueo de cuenta?**
