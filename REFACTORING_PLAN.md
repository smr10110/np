# Plan de Refactorización - AuthService

## Análisis del Estado Actual

### Problemas Identificados

#### 1. **Violación del Principio de Responsabilidad Única (SRP)**
- AuthService maneja demasiadas responsabilidades:
  - Validación de credenciales
  - Resolución de usuarios (email/RUT)
  - Gestión de bloqueos de cuenta
  - Cálculo de intentos restantes
  - Creación de sesiones
  - Generación de tokens JWT
  - Validación de dispositivos
  - Logging de intentos
  - Construcción de respuestas HTTP
  - Gestión de MDC para logging
  - Extracción de tokens Bearer

#### 2. **Método `login()` Excesivamente Complejo**
- **132 líneas** de código
- **6 niveles de indentación** en algunos puntos
- Mezcla lógica de negocio con:
  - Logging
  - Gestión de MDC
  - Construcción de respuestas HTTP
  - Manejo de errores

#### 3. **Código Duplicado**
- Múltiples llamadas a `logFailedAttempt()` con diferentes razones
- Construcción repetitiva de `ResponseEntity`:
  ```java
  ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", reason.name()))
  ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", reason.name()))
  ```
- Gestión de MDC duplicada (put/remove) en múltiples lugares

#### 4. **Alto Acoplamiento**
- Depende de **7 servicios diferentes**:
  - JWTService
  - AuthAttemptService
  - UserRepository
  - PasswordEncoder
  - AuthSessionService
  - DeviceService
  - AccountLockService

#### 5. **Gestión de Errores Inconsistente**
- Mezcla de:
  - Retorno de `ResponseEntity<?>` con códigos HTTP
  - Lanzamiento de `ResponseStatusException`
  - Try-catch con manejo genérico

#### 6. **Testing Difícil**
- Métodos largos dificultan el unit testing
- Muchas dependencias requieren mocking extensivo
- Lógica de negocio mezclada con infraestructura

#### 7. **Gestión de MDC Mezclada con Lógica de Negocio**
- MDC (Mapped Diagnostic Context) manejado directamente en el servicio
- Código de infraestructura mezclado con lógica de dominio

---

## Plan de Refactorización

### **FASE 1: Extraer Validadores y Utilities** ⏱️ Estimado: 2-3 horas

#### 1.1. Crear `LoginRequestValidator`
**Objetivo**: Centralizar validación de requests de login

**Archivos a crear**:
- `autentificacion/validation/LoginRequestValidator.java`

**Responsabilidades**:
- Validar que identifier y password no sean vacíos
- Validar formato de email
- Validar formato de RUT
- Retornar un `ValidationResult` con errores específicos

**Beneficios**:
- Reutilizable en otros contextos
- Fácil de testear
- Separa validación de lógica de negocio

---

#### 1.2. Crear `UserIdentifierResolver`
**Objetivo**: Extraer lógica de resolución de usuarios (email/RUT)

**Archivos a crear**:
- `autentificacion/resolver/UserIdentifierResolver.java`

**Responsabilidades**:
- Detectar si el identifier es email o RUT
- Parsear RUT con DV
- Buscar usuario en repositorio
- Retornar `Optional<User>`

**Beneficios**:
- Encapsula lógica compleja de parsing RUT
- Reutilizable en registro y recuperación de contraseña
- Fácil de testear independientemente

---

#### 1.3. Extraer `BearerTokenExtractor`
**Objetivo**: Utility para extraer tokens JWT de headers

**Archivos a crear**:
- `autentificacion/util/BearerTokenExtractor.java`

**Responsabilidades**:
- Validar formato "Bearer {token}"
- Extraer token sin prefijo
- Manejar casos edge (null, vacío, sin Bearer)

**Beneficios**:
- Reutilizable en filtros y otros servicios
- Lógica de infraestructura separada

---

### **FASE 2: Extraer Gestión de Respuestas HTTP** ⏱️ Estimado: 1-2 horas

#### 2.1. Crear `AuthResponseBuilder`
**Objetivo**: Centralizar construcción de respuestas de autenticación

**Archivos a crear**:
- `autentificacion/response/AuthResponseBuilder.java`

**Responsabilidades**:
- Construir respuestas `401 Unauthorized` con razón
- Construir respuestas `403 Forbidden` con razón
- Incluir `remainingAttempts` cuando aplique
- Construir respuestas exitosas de login
- Construir respuestas de logout

**Métodos propuestos**:
```java
ResponseEntity<?> unauthorized(AuthAttemptReason reason)
ResponseEntity<?> unauthorized(AuthAttemptReason reason, int remainingAttempts)
ResponseEntity<?> forbidden(AuthAttemptReason reason)
ResponseEntity<LoginResponse> loginSuccess(LoginResponse response)
ResponseEntity<Map<String, Object>> logoutSuccess(UUID jti)
```

**Beneficios**:
- Elimina código duplicado
- Consistencia en formato de respuestas
- Fácil modificar estructura de respuestas globalmente

---

### **FASE 3: Extraer Orquestador de Flujo de Login** ⏱️ Estimado: 4-5 horas

#### 3.1. Crear `LoginFlowOrchestrator`
**Objetivo**: Coordinar el flujo completo de login paso a paso

**Archivos a crear**:
- `autentificacion/flow/LoginFlowOrchestrator.java`

**Responsabilidades**:
- Coordinar pasos del login en orden:
  1. Validar request
  2. Resolver usuario
  3. Verificar bloqueo de cuenta
  4. Validar contraseña
  5. Manejar intentos fallidos
  6. Crear sesión autenticada
- Delegar cada paso a servicios especializados
- Orquestar respuestas de error en cada paso

**Beneficios**:
- Método `login()` de AuthService se reduce drásticamente
- Flujo de login explícito y legible
- Fácil agregar/modificar pasos
- Facilita testing de flujo completo

---

#### 3.2. Crear `LoginAttemptHandler`
**Objetivo**: Manejar lógica de intentos fallidos y bloqueos

**Archivos a crear**:
- `autentificacion/attempt/LoginAttemptHandler.java`

**Responsabilidades**:
- Calcular intentos restantes
- Determinar si debe bloquear cuenta
- Logging de intentos (delegando a AuthAttemptService)
- Retornar información de intentos para respuesta

**Beneficios**:
- Encapsula lógica compleja de intentos/bloqueos
- Separa de lógica principal de login
- Facilita cambiar políticas de bloqueo

---

#### 3.3. Crear `AuthenticationSessionFactory`
**Objetivo**: Encapsular creación completa de sesiones autenticadas

**Archivos a crear**:
- `autentificacion/session/AuthenticationSessionFactory.java`

**Responsabilidades**:
- Generar JTI único
- Generar token JWT
- Validar/autorizar dispositivo
- Persistir sesión
- Retornar `LoginResponse` completo

**Beneficios**:
- Encapsula creación compleja de sesión
- Método `createAuthenticatedSession()` actual es demasiado largo
- Facilita testing de creación de sesiones
- Single Responsibility: solo crear sesiones

---

### **FASE 4: Extraer Gestión de MDC y Logging** ⏱️ Estimado: 2-3 horas

#### 4.1. Crear `AuthenticationLoggingContext`
**Objetivo**: Encapsular gestión de MDC para logging contextual

**Archivos a crear**:
- `autentificacion/logging/AuthenticationLoggingContext.java`

**Responsabilidades**:
- Inicializar contexto MDC con datos de login
- Actualizar contexto cuando se resuelve usuario
- Limpiar contexto automáticamente (try-with-resources)
- Proporcionar API fluida para logging

**Ejemplo de uso**:
```java
try (AuthenticationLoggingContext ctx =
    AuthenticationLoggingContext.forLogin(request, deviceFingerprint)) {

    // MDC ya configurado automáticamente
    User user = resolveUser(...);
    ctx.withUser(user); // actualiza MDC con userId

    // ... lógica de login ...

} // MDC limpiado automáticamente
```

**Beneficios**:
- Elimina código repetitivo de MDC.put/remove
- Garantiza limpieza de MDC con try-with-resources
- Centraliza gestión de contexto de logging

---

#### 4.2. Crear `AuthenticationAuditLogger`
**Objetivo**: Centralizar logging de auditoría de autenticación

**Archivos a crear**:
- `autentificacion/logging/AuthenticationAuditLogger.java`

**Responsabilidades**:
- Log de eventos de login (éxito/fallo)
- Log de eventos de logout
- Log de eventos de bloqueo de cuenta
- Log de errores de autorización de dispositivo
- Formato consistente de mensajes

**Beneficios**:
- Separa logging de lógica de negocio
- Facilita cambiar estrategia de logging
- Centraliza auditoría

---

### **FASE 5: Refactorizar AuthService** ⏱️ Estimado: 3-4 horas

#### 5.1. Simplificar método `login()`
**Objetivo**: Reducir `login()` a coordinación simple

**Antes (132 líneas)**:
```java
public ResponseEntity<?> login(LoginRequest req, String deviceFingerprint) {
    MDC.put(...);
    try {
        // 100+ líneas de lógica compleja
    } finally {
        MDC.remove(...);
    }
}
```

**Después (~15 líneas)**:
```java
public ResponseEntity<?> login(LoginRequest req, String deviceFingerprint) {
    try (AuthenticationLoggingContext ctx =
        AuthenticationLoggingContext.forLogin(req, deviceFingerprint)) {

        return loginFlowOrchestrator.executeLogin(req, deviceFingerprint);
    }
}
```

**Beneficios**:
- Método extremadamente simple
- Fácil de leer y entender
- Fácil de testear

---

#### 5.2. Simplificar método `logout()`
**Objetivo**: Delegar extracción y validación de token

**Antes (39 líneas con try-catch anidados)**

**Después (~10 líneas)**:
```java
public ResponseEntity<?> logout(String authHeader) {
    try (AuthenticationLoggingContext ctx =
        AuthenticationLoggingContext.forLogout()) {

        return logoutFlowOrchestrator.executeLogout(authHeader);
    }
}
```

---

#### 5.3. Eliminar métodos helper del servicio
**Objetivo**: Mover a clases especializadas

**Métodos a mover**:
- `resolveUser()` → `UserIdentifierResolver`
- `extractBearer()` → `BearerTokenExtractor`
- `calculateRemainingAttempts()` → `LoginAttemptHandler`
- `logAttempt()` / `logFailedAttempt()` → `AuthenticationAuditLogger`
- `isValidPassword()` → `PasswordValidator`
- `isBlank()` → Usar `StringUtils` de Spring
- `unauthorized()` / `forbidden()` → `AuthResponseBuilder`

---

### **FASE 6: Mejorar Manejo de Errores** ⏱️ Estimado: 2-3 horas

#### 6.1. Crear `AuthenticationException` personalizado
**Objetivo**: Excepción específica de dominio para errores de autenticación

**Archivos a crear**:
- `autentificacion/exception/AuthenticationException.java`
- `autentificacion/exception/AccountLockedException.java`
- `autentificacion/exception/InvalidCredentialsException.java`
- `autentificacion/exception/DeviceNotAuthorizedException.java`

**Beneficios**:
- Separar excepciones de dominio de HTTP
- Facilita testing sin depender de Spring
- Permite manejar errores en diferentes capas

---

#### 6.2. Crear `AuthenticationExceptionHandler`
**Objetivo**: Centralizar mapeo de excepciones a respuestas HTTP

**Archivos a crear**:
- `autentificacion/exception/AuthenticationExceptionHandler.java`

**Responsabilidades**:
- Capturar excepciones de autenticación
- Mapear a códigos HTTP apropiados
- Construir respuestas consistentes
- Log de errores

**Beneficios**:
- Separa manejo de errores de lógica de negocio
- Consistencia en respuestas de error
- Facilita agregar nuevos tipos de errores

---

### **FASE 7: Mejorar Testing** ⏱️ Estimado: 4-6 horas

#### 7.1. Tests unitarios por clase
**Crear tests para cada clase nueva**:
- `LoginRequestValidatorTest`
- `UserIdentifierResolverTest`
- `BearerTokenExtractorTest`
- `LoginAttemptHandlerTest`
- `AuthenticationSessionFactoryTest`
- `LoginFlowOrchestratorTest`

**Beneficios**:
- Cobertura alta de código
- Tests rápidos y específicos
- Fácil identificar bugs

---

#### 7.2. Tests de integración
**Crear tests de integración para flujos completos**:
- Login exitoso
- Login con credenciales inválidas
- Login con cuenta bloqueada
- Login con dispositivo no autorizado
- Logout exitoso
- Logout con token inválido

**Beneficios**:
- Validar flujos completos
- Detectar problemas de integración

---

### **FASE 8: Documentación** ⏱️ Estimado: 2 horas

#### 8.1. Documentar arquitectura refactorizada
**Crear documentación**:
- Diagrama de flujo de login
- Diagrama de flujo de logout
- Diagrama de clases
- Explicación de responsabilidades

#### 8.2. JavaDoc
**Agregar JavaDoc completo a**:
- Clases públicas
- Métodos públicos
- Interfaces

---

## Estructura Final Propuesta

```
autentificacion/
├── controller/
│   └── AuthController.java (sin cambios)
├── service/
│   ├── AuthService.java (SIMPLIFICADO ~50 líneas)
│   ├── JWTService.java
│   ├── AuthAttemptService.java
│   ├── AuthSessionService.java
│   └── AccountLockService.java
├── flow/
│   ├── LoginFlowOrchestrator.java (NUEVO)
│   └── LogoutFlowOrchestrator.java (NUEVO)
├── session/
│   └── AuthenticationSessionFactory.java (NUEVO)
├── attempt/
│   └── LoginAttemptHandler.java (NUEVO)
├── validation/
│   └── LoginRequestValidator.java (NUEVO)
├── resolver/
│   └── UserIdentifierResolver.java (NUEVO)
├── response/
│   └── AuthResponseBuilder.java (NUEVO)
├── logging/
│   ├── AuthenticationLoggingContext.java (NUEVO)
│   └── AuthenticationAuditLogger.java (NUEVO)
├── util/
│   └── BearerTokenExtractor.java (NUEVO)
├── exception/
│   ├── AuthenticationException.java (NUEVO)
│   ├── AccountLockedException.java (NUEVO)
│   ├── InvalidCredentialsException.java (NUEVO)
│   ├── DeviceNotAuthorizedException.java (NUEVO)
│   └── AuthenticationExceptionHandler.java (NUEVO)
├── domain/
│   └── Session.java
├── dto/
│   ├── LoginRequest.java
│   └── LoginResponse.java
└── repository/
    └── (sin cambios)
```

---

## Métricas de Mejora Esperadas

### Antes de Refactorización
- **AuthService.java**: ~357 líneas
- **Método `login()`**: 132 líneas
- **Complejidad ciclomática**: ~15-20
- **Dependencias directas**: 7 servicios
- **Responsabilidades**: 11+
- **Niveles de indentación máximos**: 6
- **Código duplicado**: Alto

### Después de Refactorización
- **AuthService.java**: ~50-80 líneas
- **Método `login()`**: ~15 líneas
- **Complejidad ciclomática**: ~3-5
- **Dependencias directas**: 3-4 servicios
- **Responsabilidades**: 2 (coordinación)
- **Niveles de indentación máximos**: 2
- **Código duplicado**: Mínimo

---

## Estrategia de Implementación

### Orden Recomendado de Ejecución

1. **FASE 1** (Utilities) - Sin riesgo, fácil rollback
2. **FASE 2** (Responses) - Sin riesgo, mejora inmediata
3. **FASE 4** (Logging) - Mejora calidad de logs
4. **FASE 6** (Excepciones) - Base para manejo de errores
5. **FASE 3** (Orquestadores) - Requiere fases anteriores
6. **FASE 5** (Refactor AuthService) - Requiere todas las anteriores
7. **FASE 7** (Testing) - Validar todo
8. **FASE 8** (Docs) - Documentar resultado

### Consideraciones
- **Cada fase es independiente** (excepto FASE 5)
- **Se puede pausar entre fases**
- **Se puede hacer rollback por fase**
- **Commits pequeños y frecuentes**
- **Tests en cada paso**

---

## Riesgos y Mitigaciones

### Riesgos
1. **Romper funcionalidad existente**
   - Mitigación: Tests de regresión completos

2. **Aumentar complejidad accidentalmente**
   - Mitigación: Revisar métricas después de cada fase

3. **Sobreingeniería**
   - Mitigación: Seguir principio YAGNI, solo extraer lo necesario

4. **Tiempo estimado incorrecto**
   - Mitigación: Ejecutar por fases, validar tiempos

---

## Tiempo Total Estimado

- **FASE 1**: 2-3 horas
- **FASE 2**: 1-2 horas
- **FASE 3**: 4-5 horas
- **FASE 4**: 2-3 horas
- **FASE 5**: 3-4 horas
- **FASE 6**: 2-3 horas
- **FASE 7**: 4-6 horas
- **FASE 8**: 2 horas

**TOTAL**: 20-28 horas de desarrollo

---

## Beneficios Esperados

### Mantenibilidad
- ✅ Código más fácil de leer y entender
- ✅ Clases pequeñas con responsabilidad única
- ✅ Fácil localizar bugs
- ✅ Fácil agregar nuevas funcionalidades

### Testabilidad
- ✅ Tests unitarios simples y rápidos
- ✅ Fácil mockear dependencias
- ✅ Alta cobertura de código
- ✅ Tests de integración claros

### Extensibilidad
- ✅ Fácil agregar nuevos tipos de autenticación
- ✅ Fácil cambiar políticas de bloqueo
- ✅ Fácil agregar logging adicional
- ✅ Fácil modificar respuestas HTTP

### Performance
- ✅ Sin impacto negativo (misma lógica)
- ✅ Potencial mejora por mejor estructura

### Calidad de Código
- ✅ Cumple principios SOLID
- ✅ Bajo acoplamiento
- ✅ Alta cohesión
- ✅ Código limpio y profesional

---

## Notas Finales

Este plan es **exhaustivo pero flexible**. Puedes:
- Ejecutar todas las fases completas
- Ejecutar solo algunas fases según prioridad
- Ajustar el alcance de cada fase
- Agregar/quitar pasos según necesidad

El objetivo principal es **reducir complejidad, mejorar mantenibilidad y facilitar testing**, manteniendo la funcionalidad existente intacta.
