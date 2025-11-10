# Pasos Completos: Implementación Opción B (Sliding Window Simplificado)

## Resumen
- **Tiempo estimado**: 2-3 horas
- **Archivos backend**: 5
- **Archivos frontend**: 1 (mínimo con alert) o 6 (con popup bonito)
- **Campos nuevos BD**: 1 (`sesLastActivity`)

---

## BACKEND (5 archivos)

### Paso 1: Modificar Session.java
**Archivo**: `naive-pay-api/src/main/java/cl/ufro/dci/naivepayapi/autentificacion/domain/Session.java`

**Cambio**: Agregar 1 campo nuevo antes de `status`

```java
@Column(name = "ses_last_activity", nullable = false)
private Instant sesLastActivity;
```

**Ubicación**: Línea 53-54 (después de `sesClosed`, antes de `status`)

---

### Paso 2: Modificar application.properties
**Archivo**: `naive-pay-api/src/main/resources/application.properties`

**Cambio**: Actualizar/agregar propiedades de configuración

```properties
# Cambiar de 60 a 15 minutos
security.jwt.ttl-minutes=15

# Agregar nueva propiedad
security.session.inactivity-timeout-minutes=10
```

**Ubicación**: Líneas 3-4 (sección Autentificacion)

---

### Paso 3: Modificar AuthSessionService.java (Parte 1 - Inicialización)
**Archivo**: `naive-pay-api/src/main/java/cl/ufro/dci/naivepayapi/autentificacion/service/AuthSessionService.java`

**Cambio 1**: Agregar property para configuración

```java
@Value("${security.session.inactivity-timeout-minutes:10}")
private long inactivityTimeoutMinutes;
```

**Ubicación**: Después de las inyecciones de dependencias (línea ~25)

**Cambio 2**: Modificar método `saveActiveSession()` para inicializar `sesLastActivity`

```java
// ANTES:
Session auth = Session.builder()
        .sesJti(jti)
        .user(user)
        .device(device)
        .sesDeviceFingerprint(device != null ? device.getFingerprint() : null)
        .sesCreated(Instant.now())
        .sesExpires(expiresAt)
        .status(SessionStatus.ACTIVE)
        .build();

// DESPUÉS:
Instant now = Instant.now();
Session auth = Session.builder()
        .sesJti(jti)
        .user(user)
        .device(device)
        .sesDeviceFingerprint(device != null ? device.getFingerprint() : null)
        .sesCreated(now)
        .sesExpires(expiresAt)
        .sesLastActivity(now)  // ✅ AGREGAR ESTA LÍNEA
        .status(SessionStatus.ACTIVE)
        .build();
```

**Ubicación**: Método `saveActiveSession()` línea ~30-40

---

### Paso 4: Agregar método updateLastActivity a AuthSessionService.java (Parte 2)
**Archivo**: `naive-pay-api/src/main/java/cl/ufro/dci/naivepayapi/autentificacion/service/AuthSessionService.java`

**Cambio**: Agregar método nuevo al final de la clase (antes del `}` final)

```java
@Transactional
public void updateLastActivity(UUID jti) {
    Session session = authRepo.findBySesJtiAndStatus(jti, SessionStatus.ACTIVE)
            .orElseThrow(() -> new IllegalArgumentException("Session not found"));

    Instant now = Instant.now();
    Instant lastUpdate = session.getSesLastActivity();

    // Optimización: solo actualizar si pasó más de 1 minuto desde última actualización
    if (ChronoUnit.MINUTES.between(lastUpdate, now) < 1) {
        return;
    }

    // Validar que no haya superado tiempo de inactividad (10 min)
    Instant inactivityLimit = lastUpdate.plus(inactivityTimeoutMinutes, ChronoUnit.MINUTES);
    if (now.isAfter(inactivityLimit)) {
        session.setStatus(SessionStatus.CLOSED);
        session.setSesClosed(inactivityLimit);
        authRepo.save(session);
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "SESSION_INACTIVE");
    }

    // Actualizar última actividad
    session.setSesLastActivity(now);
    authRepo.save(session);
}
```

**Ubicación**: Al final de la clase, antes del `}` final

**Imports necesarios**: Agregar al inicio del archivo si no existen:
```java
import java.time.temporal.ChronoUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
```

---

### Paso 5: Modificar JwtAuthFilter.java
**Archivo**: `naive-pay-api/src/main/java/cl/ufro/dci/naivepayapi/autentificacion/configuration/security/JwtAuthFilter.java`

**Cambio 1**: Inyectar AuthSessionService (si no está ya)

Buscar el constructor o las inyecciones de dependencias y agregar:
```java
private final AuthSessionService sessionService;
```

**Cambio 2**: Actualizar actividad en cada request autenticado

Buscar el método `doFilterInternal()` y después de validar el JWT, agregar:

```java
// BUSCAR esta sección (después de extraer y validar el token):
String jtiStr = jwtService.getJti(token);
UUID jti = UUID.fromString(jtiStr);

// ✅ AGREGAR estas líneas:
try {
    sessionService.updateLastActivity(jti);
} catch (ResponseStatusException e) {
    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    return;
} catch (IllegalArgumentException e) {
    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    return;
}
```

**Ubicación**: Dentro del método `doFilterInternal()`, después de extraer `jti` del token

**Import necesario**:
```java
import org.springframework.web.server.ResponseStatusException;
```

---

### Paso 6: Crear SessionStatusController.java
**Archivo**: `naive-pay-api/src/main/java/cl/ufro/dci/naivepayapi/autentificacion/controller/SessionStatusController.java`

**Acción**: Crear archivo nuevo

**Contenido completo**:
```java
package cl.ufro.dci.naivepayapi.autentificacion.controller;

import cl.ufro.dci.naivepayapi.autentificacion.domain.Session;
import cl.ufro.dci.naivepayapi.autentificacion.domain.enums.SessionStatus;
import cl.ufro.dci.naivepayapi.autentificacion.repository.SessionRepository;
import cl.ufro.dci.naivepayapi.autentificacion.service.JWTService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class SessionStatusController {
    private final JWTService jwtService;
    private final SessionRepository sessionRepository;

    @GetMapping("/session-status")
    public ResponseEntity<SessionStatusResponse> getSessionStatus(
            @RequestHeader("Authorization") String authHeader
    ) {
        String token = authHeader.replace("Bearer ", "");
        String jtiStr = jwtService.getJti(token);
        UUID jti = UUID.fromString(jtiStr);

        Session session = sessionRepository.findBySesJtiAndStatus(jti, SessionStatus.ACTIVE)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "SESSION_NOT_FOUND"));

        Instant now = Instant.now();
        long minutesSinceActivity = ChronoUnit.MINUTES.between(session.getSesLastActivity(), now);
        long minutesRemaining = 10 - minutesSinceActivity;

        return ResponseEntity.ok(new SessionStatusResponse(Math.max(0, minutesRemaining)));
    }
}

record SessionStatusResponse(long minutesUntilInactivity) {}
```

**Ubicación**: Crear en `naive-pay-api/src/main/java/cl/ufro/dci/naivepayapi/autentificacion/controller/`

---

## FRONTEND (Opción Mínima - 1 archivo)

### Paso 7: Modificar AutentificacionService.ts
**Archivo**: `naive-pay-ui/src/app/modules/autentificacion/service/autentificacion.service.ts`

**Cambio 1**: Agregar properties para polling

```typescript
private inactivityCheckTimer: ReturnType<typeof setInterval> | null = null;
private warningShown = false;
```

**Ubicación**: Después de `private logoutTimer` (línea ~46)

---

**Cambio 2**: Agregar método de polling de inactividad

```typescript
private startInactivityMonitoring(): void {
  this.stopInactivityMonitoring();

  this.inactivityCheckTimer = setInterval(() => {
    this.http.get<{ minutesUntilInactivity: number }>(`${this.base}/session-status`)
      .subscribe({
        next: (res) => {
          // Si queda 1 minuto o menos y no hemos mostrado advertencia
          if (res.minutesUntilInactivity <= 1 && !this.warningShown) {
            this.warningShown = true;
            this.showInactivityWarning();
          }
        },
        error: (err) => {
          if (err.status === 401) {
            this.stopInactivityMonitoring();
            this.clearAndRedirect('session_closed');
          }
        }
      });
  }, 60000);  // Cada 1 minuto
}

private stopInactivityMonitoring(): void {
  if (this.inactivityCheckTimer) {
    clearInterval(this.inactivityCheckTimer);
    this.inactivityCheckTimer = null;
  }
  this.warningShown = false;
}

private showInactivityWarning(): void {
  const userWantsToContinue = confirm(
    'Tu sesión expirará en 1 minuto por inactividad.\n\n' +
    '¿Deseas continuar?\n\n' +
    'Haz clic en "Aceptar" para continuar o "Cancelar" para cerrar sesión.'
  );

  if (userWantsToContinue) {
    // Cualquier request HTTP resetea la actividad automáticamente
    // Hacemos un request simple para resetear
    this.http.get(`${this.base}/session-status`).subscribe();
    this.warningShown = false;  // Resetear para mostrar próxima advertencia si es necesario
  } else {
    // Usuario quiere cerrar sesión
    this.logout(true).subscribe();
  }
}
```

**Ubicación**: Agregar después del método `scheduleAutoLogoutFromToken()` (línea ~85-97)

---

**Cambio 3**: Modificar método `login()` para iniciar polling

```typescript
// BUSCAR método login() existente y modificar el tap():

login(req: LoginRequest): Observable<LoginResponse> {
  const headers = new HttpHeaders().set('X-Device-Fingerprint', this.deviceFp.get());
  return this.http.post<LoginResponse>(`${this.base}/login`, req, { headers }).pipe(
    tap(res => {
      sessionStorage.setItem('token', res.accessToken);
      this.scheduleAutoLogoutFromToken(res.accessToken);
      this.startInactivityMonitoring();  // ✅ AGREGAR ESTA LÍNEA
    })
  );
}
```

**Ubicación**: Método `login()` línea ~122-129

---

**Cambio 4**: Modificar método `logout()` para detener polling

```typescript
// BUSCAR método logout() existente y agregar al inicio:

logout(redirect: boolean = true): Observable<void> {
  this.stopInactivityMonitoring();  // ✅ AGREGAR ESTA LÍNEA AL INICIO

  return this.http.post<void>(`${this.base}/logout`, {}).pipe(
    tap(() => {
      this.clear();
      // ... resto del código ...
    }),
    // ... resto del código ...
  );
}
```

**Ubicación**: Método `logout()` línea ~133, agregar como primera línea

---

**Cambio 5**: Modificar método `ngOnDestroy()` para detener polling

```typescript
// BUSCAR método ngOnDestroy() existente y modificar:

ngOnDestroy(): void {
  this.cleanupTimers();
  this.stopInactivityMonitoring();  // ✅ AGREGAR ESTA LÍNEA
}
```

**Ubicación**: Método `ngOnDestroy()` línea ~55-57

---

**Cambio 6**: Modificar constructor para restaurar polling si hay sesión

```typescript
// BUSCAR constructor existente y modificar:

constructor() {
  const token = sessionStorage.getItem('token');
  if (token) {
    this.scheduleAutoLogoutFromToken(token);
    this.startInactivityMonitoring();  // ✅ AGREGAR ESTA LÍNEA
  }
}
```

**Ubicación**: Constructor línea ~48-52

---

## FRONTEND (Opción Completa - 6 archivos adicionales)

Si quieres un popup bonito en lugar de `confirm()`, sigue estos pasos adicionales:

### Paso 8 (Opcional): Crear SessionWarningService
### Paso 9 (Opcional): Crear SessionWarningPopupComponent
### Paso 10 (Opcional): Modificar AppComponent y AppModule

**(Por ahora, puedes usar la opción mínima con `confirm()` y mejorar después)**

---

## Resumen de Cambios por Archivo

### Backend
1. ✏️ `Session.java` - 1 línea (agregar campo)
2. ✏️ `application.properties` - 2 líneas (configuración)
3. ✏️ `AuthSessionService.java` - 1 property + modificar método + 1 método nuevo
4. ✏️ `JwtAuthFilter.java` - Agregar 8 líneas (try-catch)
5. ➕ `SessionStatusController.java` - Archivo nuevo (45 líneas)

### Frontend
6. ✏️ `autentificacion.service.ts` - 3 métodos nuevos + 4 modificaciones pequeñas

**Total Backend**: 5 archivos
**Total Frontend**: 1 archivo (mínimo)

---

## Orden de Implementación Sugerido

1. ✅ Backend Paso 1: Session.java (campo)
2. ✅ Backend Paso 2: application.properties (config)
3. ✅ Backend Paso 3: AuthSessionService inicialización
4. ✅ Backend Paso 4: AuthSessionService método updateLastActivity
5. ✅ Backend Paso 6: SessionStatusController (crear archivo)
6. ✅ Backend Paso 5: JwtAuthFilter (integrar updateLastActivity)
7. ✅ Frontend Paso 7: AutentificacionService (todos los cambios)
8. 🧪 Testing completo

**Nota**: Paso 5 (JwtAuthFilter) va después de Paso 6 (Controller) porque necesitamos que todo esté listo antes de integrarlo.

---

## Testing a Realizar

### Test 1: Usuario Activo (sin interrupciones)
```
1. Login
2. Navegar entre páginas cada 2-3 minutos
3. Verificar: NO aparece popup durante 15 minutos
4. A los 15 min: JWT expira naturalmente → logout
```

### Test 2: Usuario Inactivo
```
1. Login
2. Dejar tab abierta sin tocar
3. A los 9 min: Debe aparecer confirm() "Tu sesión expirará..."
4. No hacer clic
5. A los 10 min: Logout automático → redirige a login
```

### Test 3: Usuario Responde a Advertencia
```
1. Login
2. Dejar tab abierta 9 minutos
3. Aparece confirm()
4. Click "Aceptar"
5. Verificar: Sesión continúa (no logout)
6. Backend: sesLastActivity se actualiza
```

### Test 4: Límite Natural 15 Minutos
```
1. Login
2. Navegar activamente durante 15 minutos
3. Cada navegación actualiza sesLastActivity
4. A los 15 min: JWT exp alcanzado
5. Próximo request → 401 Unauthorized
6. Frontend detecta y hace logout
```

---

## Comandos Útiles

### Compilar Backend
```bash
cd naive-pay-api
mvn clean compile
```

### Limpiar BD (H2 en memoria)
```bash
# Reiniciar aplicación Spring Boot (Ctrl+C y volver a ejecutar)
```

### Ver Logs Backend
```bash
# Buscar en consola:
# - "Sesión creada" (al login)
# - "Actualizada última actividad" (en cada request)
# - "Sesión inactiva cerrada" (cuando expira por inactividad)
```

### Compilar Frontend
```bash
cd naive-pay-ui
npm run build
```

### Ver Network en Navegador
```
1. F12 → Network tab
2. Filtrar por "session-status"
3. Verificar: 1 request cada 1 minuto
```

---

## Preguntas Frecuentes

### ¿Qué pasa si tengo múltiples pestañas abiertas?
Cada pestaña hace su propio polling independiente. Cualquier request de cualquier pestaña actualiza `sesLastActivity`, beneficiando a todas.

### ¿Qué pasa si el usuario cierra y reabre el navegador?
Si `sessionStorage` se mantiene (no cerró todas las pestañas), el token sigue válido y el polling se reanuda automáticamente en el constructor.

### ¿Puedo aumentar el límite de 15 minutos?
Sí, cambia `security.jwt.ttl-minutes` a 20 o 30 en `application.properties`.

### ¿El polling consume muchos recursos?
No. Es 1 request/minuto (60 requests/hora). El backend optimiza writes a BD (solo si pasó >1 min).

---

## Siguiente Paso

**¿Empiezo con la implementación ahora?**

Si dices "sí", voy a ejecutar los cambios en este orden:
1. Backend (Pasos 1-6)
2. Frontend (Paso 7)
3. Te guío en testing

**¿Listo para empezar?** 🚀
