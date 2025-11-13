# Análisis de Código Muerto y Duplicado - Módulo de Autenticación

**Proyecto:** NaivePay API
**Fecha:** 2025-11-13
**Alcance:** Módulo de Autenticación

---

## Resumen Ejecutivo

Se identificaron **10 instancias** de código duplicado y código muerto en el módulo de autenticación, con impactos desde críticos hasta bajos. La prioridad más alta es resolver las inconsistencias en las rutas públicas entre `SecurityConfig` y `JwtAuthFilter`.

---

## ❌ CÓDIGO DUPLICADO

### 1. Dos clases RutUtils con implementaciones DIFERENTES

**Severidad:** 🔴 ALTA
**Tipo:** Duplicación de clase completa

#### Ubicaciones:
- `naive-pay-api/src/main/java/cl/ufro/dci/naivepayapi/registro/service/RutUtils.java` (58 líneas)
- `naive-pay-api/src/main/java/cl/ufro/dci/naivepayapi/autentificacion/service/RutUtils.java` (24 líneas)

#### Descripción del problema:
- **Clase en `registro`:** Implementa validación completa del RUT con dígito verificador usando algoritmo de checksum (módulo 11)
- **Clase en `autentificacion`:** Solo parsea el formato RUT sin validar el checksum, además incluye método `isEmail()`
- Ambas clases tienen el mismo nombre pero están en paquetes diferentes

#### Funcionalidades por clase:

**RutUtils en registro:**
```java
public static boolean isValid(String rut)
```
- Limpia formato (puntos, guiones)
- Valida formato con regex
- Calcula y verifica dígito verificador con algoritmo módulo 11

**RutUtils en autentificacion:**
```java
public static boolean isEmail(String s)
public static Optional<Rut> parseRut(String s)
public record Rut(String rut, char dv) {}
```
- Solo parsea formato "12345678-9"
- No valida checksum
- Incluye utilidad para detectar email

#### Uso actual:
- **AuthService.java:275-286** usa la clase de `autentificacion` para resolver usuarios por email o RUT
- La clase de `registro` se usa para validación durante el registro de usuarios

#### Impacto:
- Confusión sobre cuál usar
- Riesgo de usar validación incorrecta
- Mantenimiento duplicado

#### Recomendación:
Consolidar en UNA clase utilitaria ubicada en:
```
cl.ufro.dci.naivepayapi.common.utils.RutUtils
```

Con métodos:
```java
public static boolean isValid(String rut)          // Validación completa con checksum
public static Optional<Rut> parseRut(String s)     // Parsing de formato
public static boolean isEmail(String s)            // Detección de email
public record Rut(String rut, char dv) {}
```

---

### 2. Extracción de Bearer Token duplicada en 3 lugares

**Severidad:** 🟡 MEDIA
**Tipo:** Lógica duplicada

#### Ubicaciones:
1. **AuthService.java:326-331**
   ```java
   private String extractBearer(String authHeader) {
       if (isBlank(authHeader) || !authHeader.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
           return null;
       }
       return authHeader.substring(BEARER_PREFIX.length()).trim();
   }
   ```

2. **DeviceTokenUtil.java:49-57**
   ```java
   public String extractBearerTokenFromHeader(String authorizationHeaderValue) {
       if (authorizationHeaderValue == null || authorizationHeaderValue.isBlank()) {
           throw new IllegalArgumentException("Missing Authorization header");
       }
       if (!authorizationHeaderValue.startsWith(BEARER_PREFIX)) {
           throw new IllegalArgumentException("Invalid Authorization format...");
       }
       return authorizationHeaderValue.substring(BEARER_PREFIX.length()).trim();
   }
   ```

3. **JwtAuthFilter.java:82-89**
   ```java
   final String header = request.getHeader(AUTH_HEADER);
   if (header == null || !header.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
       chain.doFilter(request, response);
       return;
   }
   final String token = header.substring(BEARER_PREFIX.length()).trim();
   ```

#### Diferencias entre implementaciones:
- **AuthService:** Retorna `null` si es inválido
- **DeviceTokenUtil:** Lanza `IllegalArgumentException` si es inválido
- **JwtAuthFilter:** Continúa con el filtro si es inválido

#### Impacto:
- Mantenimiento triplicado
- Comportamientos inconsistentes ante errores
- Duplicación de lógica de validación

#### Recomendación:
Crear método estático centralizado en `DeviceTokenUtil` o nueva clase `JwtTokenExtractor`:
```java
public static String extractBearerToken(String authHeader) throws IllegalArgumentException
public static String extractBearerTokenOrNull(String authHeader)  // Variante que retorna null
```

Reutilizar en todas las ubicaciones.

---

### 3. PUBLIC_ENDPOINTS duplicado con INCONSISTENCIAS

**Severidad:** ⚠️ CRÍTICA
**Tipo:** Configuración duplicada con diferencias

#### Ubicaciones:

**SecurityConfig.java:28-36**
```java
private static final String[] PUBLIC_ENDPOINTS = {
    "/h2-console/**",
    "/api/register/**",
    "/auth/login",
    "/auth/recovery/**",        // ⚠️ Solo en SecurityConfig
    "/auth/password/**",
    "/api/dispositivos/recover/**",
    "/api/devices/recover/**"
};
```

**JwtAuthFilter.java:44-51**
```java
private static final String[] PUBLIC_PATHS = {
    "/h2-console/**",
    "/api/register/**",
    "/auth/password/**",
    "/auth/login",
    "/api/devices/recover/**",
    "/api/dispositivos/recover/**"
    // ❌ Falta "/auth/recovery/**"
};
```

#### Problema CRÍTICO:
La ruta `/auth/recovery/**` está permitida en `SecurityConfig` pero NO en `JwtAuthFilter`, lo que puede causar:
1. La ruta pasa la configuración de Spring Security
2. El filtro JWT la rechaza por falta de token
3. Comportamiento inconsistente e inesperado

#### Impacto:
- Funcionalidad de recuperación de contraseña potencialmente rota
- Debugging difícil por inconsistencia entre capas
- Mantenimiento propenso a errores

#### Recomendación:
Crear constante compartida:
```java
// AuthConstants.java
public class AuthConstants {
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
```

Usar en ambas ubicaciones:
```java
// SecurityConfig.java
.requestMatchers(AuthConstants.PUBLIC_ENDPOINTS).permitAll()

// JwtAuthFilter.java
private boolean isPublic(String uri) {
    for (String pattern : AuthConstants.PUBLIC_ENDPOINTS) {
        if (PATH_MATCHER.match(pattern, uri)) {
            return true;
        }
    }
    return false;
}
```

---

### 4. Constante BEARER_PREFIX duplicada

**Severidad:** 🟢 BAJA
**Tipo:** Constante mágica duplicada

#### Ubicaciones:
- `AuthService.java:33`
  ```java
  private static final String BEARER_PREFIX = "Bearer ";
  ```

- `DeviceTokenUtil.java:23`
  ```java
  private static final String BEARER_PREFIX = "Bearer ";
  ```

- `JwtAuthFilter.java:37`
  ```java
  private static final String BEARER_PREFIX = "Bearer ";
  ```

#### Impacto:
- Bajo, pero viola DRY (Don't Repeat Yourself)
- Mantenimiento triplicado si el formato cambia

#### Recomendación:
Mover a clase de constantes:
```java
// AuthConstants.java
public class AuthConstants {
    public static final String BEARER_PREFIX = "Bearer ";
    public static final String AUTH_HEADER = "Authorization";
    public static final String DEVICE_FINGERPRINT_HEADER = "X-Device-Fingerprint";
}
```

---

## 🗑️ CÓDIGO MUERTO

### 5. System.out.println en código de producción

**Severidad:** 🟢 BAJA
**Tipo:** Código de debugging

#### Ubicación:
**JwtAuthFilter.java:157**
```java
private boolean isPublic(String uri) {
    for (String pattern : PUBLIC_PATHS) {
        if (PATH_MATCHER.match(pattern, uri)) {
            System.out.println("Ruta pública detectada: " + uri);  // ❌
            return true;
        }
    }
    return false;
}
```

#### Problema:
- No usar logger profesional (SLF4J ya está en el proyecto)
- `System.out` no es apropiado para producción
- No se puede controlar el nivel de log

#### Impacto:
- Contaminación de logs de aplicación
- No se puede desactivar sin modificar código

#### Recomendación:
Usar logger:
```java
private static final Logger logger = LoggerFactory.getLogger(JwtAuthFilter.class);

private boolean isPublic(String uri) {
    for (String pattern : PUBLIC_PATHS) {
        if (PATH_MATCHER.match(pattern, uri)) {
            logger.debug("Ruta pública detectada: {}", uri);
            return true;
        }
    }
    return false;
}
```

O simplemente eliminar si no es necesario.

---

### 6. Dependencias inyectadas pero NO USADAS

**Severidad:** 🟢 BAJA
**Tipo:** Dead code (campos no utilizados)

#### Ubicación:
**DeviceTokenUtil.java:27-28**
```java
@Component
@RequiredArgsConstructor
public class DeviceTokenUtil {
    private final JWTService jwtService;
    private final DeviceRepository deviceRepository;      // ❌ NUNCA USADO
    private final PasswordEncoder passwordEncoder;         // ❌ NUNCA USADO
}
```

#### Análisis:
- `deviceRepository` se inyecta pero no hay ningún método que lo use
- `passwordEncoder` se inyecta pero no hay ningún método que lo use
- Solo `jwtService` se usa realmente

#### Búsqueda en código:
```bash
# Búsqueda de uso de deviceRepository en DeviceTokenUtil
grep -n "deviceRepository" DeviceTokenUtil.java
# Resultado: Solo línea 27 (declaración)

# Búsqueda de uso de passwordEncoder en DeviceTokenUtil
grep -n "passwordEncoder" DeviceTokenUtil.java
# Resultado: Solo línea 28 (declaración)
```

#### Impacto:
- Dependencias innecesarias inyectadas
- Confusión sobre el propósito de la clase
- Leve overhead en construcción del bean

#### Recomendación:
Eliminar del constructor y declaraciones:
```java
@Component
@RequiredArgsConstructor
public class DeviceTokenUtil {
    private final JWTService jwtService;  // ✅ Solo este se usa realmente
}
```

---

## 📊 RESUMEN ESTADÍSTICO

| Categoría | Cantidad | Líneas afectadas | Archivos |
|-----------|----------|------------------|----------|
| **Código duplicado** | 4 problemas | ~150 líneas | 6 archivos |
| **Código muerto** | 2 problemas | ~3 líneas | 2 archivos |
| **Total** | 6 problemas | ~153 líneas | 7 archivos únicos |

### Distribución por severidad:

| Severidad | Cantidad | Problemas |
|-----------|----------|-----------|
| ⚠️ **CRÍTICA** | 1 | PUBLIC_ENDPOINTS inconsistente |
| 🔴 **ALTA** | 1 | RutUtils duplicado |
| 🟡 **MEDIA** | 1 | extractBearer duplicado |
| 🟢 **BAJA** | 3 | BEARER_PREFIX, System.out, dependencias no usadas |

---

## 📋 PLAN DE ACCIÓN RECOMENDADO

### Fase 1: Correcciones Críticas (Prioridad Inmediata)

1. **Resolver inconsistencia en PUBLIC_ENDPOINTS**
   - Crear `AuthConstants.java` con endpoints públicos
   - Actualizar `SecurityConfig.java` y `JwtAuthFilter.java`
   - Probar rutas de recuperación de contraseña

### Fase 2: Consolidación de Código (Alta Prioridad)

2. **Consolidar RutUtils**
   - Crear `cl.ufro.dci.naivepayapi.common.utils.RutUtils`
   - Combinar funcionalidades de ambas clases
   - Actualizar imports en `AuthService` y módulo de registro
   - Eliminar clases duplicadas

3. **Centralizar extracción de Bearer token**
   - Crear método utilitario en `AuthConstants` o `JwtTokenExtractor`
   - Refactorizar `AuthService`, `DeviceTokenUtil` y `JwtAuthFilter`

### Fase 3: Limpieza (Baja Prioridad)

4. **Eliminar código muerto**
   - Remover `System.out.println` de `JwtAuthFilter`
   - Eliminar dependencias no usadas de `DeviceTokenUtil`

5. **Centralizar constantes**
   - Mover `BEARER_PREFIX` a `AuthConstants`
   - Actualizar referencias

---

## 🧪 TESTING REQUERIDO

Después de cada fase, ejecutar:

1. **Tests unitarios:**
   - `AuthServiceTest`
   - `JwtAuthFilterTest`
   - `RutUtilsTest`

2. **Tests de integración:**
   - Login flow completo
   - Recuperación de contraseña
   - Validación de dispositivos

3. **Tests de endpoints públicos:**
   ```bash
   curl -X POST http://localhost:8080/auth/login -d '{"identifier":"...","password":"..."}'
   curl -X POST http://localhost:8080/auth/recovery/request -d '{"email":"..."}'
   curl -X GET http://localhost:8080/api/register/...
   ```

---

## 📁 ARCHIVOS AFECTADOS

### Archivos con código duplicado:
1. `naive-pay-api/src/main/java/cl/ufro/dci/naivepayapi/autentificacion/service/AuthService.java`
2. `naive-pay-api/src/main/java/cl/ufro/dci/naivepayapi/autentificacion/service/RutUtils.java`
3. `naive-pay-api/src/main/java/cl/ufro/dci/naivepayapi/autentificacion/configuration/security/JwtAuthFilter.java`
4. `naive-pay-api/src/main/java/cl/ufro/dci/naivepayapi/autentificacion/configuration/security/SecurityConfig.java`
5. `naive-pay-api/src/main/java/cl/ufro/dci/naivepayapi/dispositivos/configuration/DeviceTokenUtil.java`
6. `naive-pay-api/src/main/java/cl/ufro/dci/naivepayapi/registro/service/RutUtils.java`

### Archivos con código muerto:
1. `naive-pay-api/src/main/java/cl/ufro/dci/naivepayapi/autentificacion/configuration/security/JwtAuthFilter.java:157`
2. `naive-pay-api/src/main/java/cl/ufro/dci/naivepayapi/dispositivos/configuration/DeviceTokenUtil.java:27-28`

---

## 💡 BENEFICIOS ESPERADOS

### Después de implementar correcciones:

**Mantenibilidad:**
- ✅ Código DRY (Don't Repeat Yourself)
- ✅ Fuente única de verdad para configuraciones
- ✅ Menos lugares donde hacer cambios

**Confiabilidad:**
- ✅ Sin inconsistencias entre capas de seguridad
- ✅ Comportamiento predecible
- ✅ Validación consistente de RUT

**Calidad de código:**
- ✅ Logging profesional
- ✅ Sin dependencias innecesarias
- ✅ Código más limpio y enfocado

**Métricas estimadas:**
- 📉 Reducción de ~100 líneas de código duplicado
- 📉 Reducción de 7 archivos a 4 archivos principales
- 📈 Aumento de cohesión y reducción de acoplamiento

---

## 🔗 REFERENCIAS

- [OWASP Secure Coding Practices](https://owasp.org/www-project-secure-coding-practices-quick-reference-guide/)
- [Spring Security Best Practices](https://docs.spring.io/spring-security/reference/features/index.html)
- [Clean Code Principles](https://www.oreilly.com/library/view/clean-code-a/9780136083238/)

---

**Fin del análisis**
