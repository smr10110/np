# Plan de Implementación: Tiempo de Vida de Sesión (10 minutos)

## Resumen
Implementar un sistema de sesiones con duración de 10 minutos que muestre un popup de advertencia 1 minuto antes de expirar, preguntando "¿Sigues conectado?" con opciones "Salir" y "Seguir Conectado".

---

## Análisis de Arquitectura Actual

### Backend
- **JWT TTL actual**: 60 minutos (`security.jwt.ttl-minutes=60`)
- **Entidad Session**: Ya existe con campos `sesCreated`, `sesExpires`, `sesClosed`, `status`
- **Servicios existentes**:
  - `JWTServiceImpl`: Genera tokens con expiración configurable
  - `AuthSessionService`: Persiste y gestiona sesiones en BD
  - `JwtAuthFilter`: Valida tokens en cada request

### Frontend
- **AutentificacionService**:
  - Ya implementa auto-logout con timer basado en JWT exp
  - Método `scheduleAutoLogout(at: Date)` programa logout automático
  - Método `scheduleAutoLogoutFromToken(token)` decodifica JWT y extrae exp
- **AuthInterceptor**: Detecta 401 y redirige a login
- **SessionStorage**: Almacena el token actual

---

## Opción Elegida: Extensión de Sesión con Refresh Token

### Estrategia
1. **Backend**: Crear endpoint `/auth/extend-session` que:
   - Valida sesión activa actual
   - Genera nuevo JWT con nueva expiración (+10 min desde ahora)
   - Actualiza `sesExpires` en BD
   - Mantiene mismo `jti` (no crea nueva sesión, extiende la actual)

2. **Frontend**: Temporizador que:
   - A los 9 minutos: Muestra popup "¿Sigues conectado?"
   - Si "Seguir Conectado": Llama `/auth/extend-session` → obtiene nuevo token
   - Si "Salir": Llama `/auth/logout` → limpia sesión
   - Si no responde (timeout 1 min): Auto-logout

---

## Cambios Necesarios

### 1. Backend: Configuración (application.properties)

```properties
# Cambiar de 60 a 10 minutos
security.jwt.ttl-minutes=10
```

**Archivo**: `naive-pay-api/src/main/resources/application.properties`

---

### 2. Backend: Nuevo Endpoint - SessionExtensionController

**Crear**: `naive-pay-api/src/main/java/cl/ufro/dci/naivepayapi/autentificacion/controller/SessionExtensionController.java`

```java
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class SessionExtensionController {
    private final JWTService jwtService;
    private final AuthSessionService sessionService;
    private final SessionRepository sessionRepository;

    @Value("${security.jwt.ttl-minutes}")
    private long ttlMinutes;

    // POST /auth/extend-session
    // Valida sesión activa y genera nuevo JWT con +10 min
    @PostMapping("/extend-session")
    public ResponseEntity<ExtendSessionResponse> extendSession(
        @RequestHeader("Authorization") String authHeader
    ) {
        String oldToken = authHeader.replace("Bearer ", "");

        // Validar token actual
        if (jwtService.isExpired(oldToken)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "SESSION_EXPIRED");
        }

        String jtiStr = jwtService.getJti(oldToken);
        UUID jti = UUID.fromString(jtiStr);

        // Verificar sesión activa en BD
        Session session = sessionService.findActiveByJti(jti)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "SESSION_NOT_FOUND"));

        // Generar nuevo token con mismos claims pero nueva expiración
        String userId = jwtService.getUserId(oldToken);
        String deviceFp = jwtService.getDeviceFingerprint(oldToken);
        Instant newExpiration = Instant.now().plus(ttlMinutes, ChronoUnit.MINUTES);

        String newToken = jwtService.generate(userId, deviceFp, jtiStr);

        // Actualizar sesExpires en BD
        session.setSesExpires(newExpiration);
        sessionRepository.save(session);

        logger.info("Sesión extendida | userId={} | jti={} | newExpires={}",
            userId, jti, newExpiration);

        return ResponseEntity.ok(new ExtendSessionResponse(newToken, newExpiration));
    }
}
```

**DTO Response**:
```java
public record ExtendSessionResponse(String accessToken, Instant expiresAt) {}
```

---

### 3. Backend: Modificar AuthSessionService

**Archivo**: `naive-pay-api/src/main/java/cl/ufro/dci/naivepayapi/autentificacion/service/AuthSessionService.java`

**Agregar método**:
```java
@Transactional
public void extendSession(UUID jti, Instant newExpiresAt) {
    Session session = authRepo.findBySesJtiAndStatus(jti, SessionStatus.ACTIVE)
        .orElseThrow(() -> new IllegalArgumentException("Session not found or inactive"));

    session.setSesExpires(newExpiresAt);
    authRepo.save(session);
}
```

---

### 4. Frontend: Modificar AutentificacionService

**Archivo**: `naive-pay-ui/src/app/modules/autentificacion/service/autentificacion.service.ts`

**Cambios**:

1. **Agregar propiedad para timer de advertencia**:
```typescript
private warningTimer: ReturnType<typeof setTimeout> | null = null;
```

2. **Modificar `scheduleAutoLogout`** para programar advertencia 1 minuto antes:
```typescript
private scheduleAutoLogout(at: Date) {
  const ms = at.getTime() - Date.now();
  const warningMs = ms - 60000; // 1 minuto antes

  this.cleanupTimers();

  if (ms <= 0) {
    this.clearAndRedirect('session_closed');
    return;
  }

  // Programar advertencia 1 minuto antes de expirar
  if (warningMs > 0) {
    this.warningTimer = setTimeout(() => {
      this.showSessionWarning();
    }, warningMs);
  }

  // Programar logout automático si no responde
  this.logoutTimer = setTimeout(() => {
    this.http.post<void>(`${this.base}/logout`, {}).subscribe({
      next: () => this.clearAndRedirect('session_closed'),
      error: () => this.clearAndRedirect('session_closed')
    });
  }, ms);
}
```

3. **Agregar método para mostrar popup de advertencia**:
```typescript
private showSessionWarning(): void {
  // Emitir evento o usar servicio de modal para mostrar popup
  // (Ver sección siguiente sobre componente de popup)
}
```

4. **Agregar método para extender sesión**:
```typescript
extendSession(): Observable<LoginResponse> {
  return this.http.post<LoginResponse>(`${this.base}/extend-session`, {}).pipe(
    tap(res => {
      sessionStorage.setItem('token', res.accessToken);
      this.scheduleAutoLogoutFromToken(res.accessToken);
    })
  );
}
```

5. **Modificar `cleanupTimers`** para limpiar ambos timers:
```typescript
private cleanupTimers(): void {
  if (this.logoutTimer) {
    clearTimeout(this.logoutTimer);
    this.logoutTimer = null;
  }
  if (this.warningTimer) {
    clearTimeout(this.warningTimer);
    this.warningTimer = null;
  }
}
```

---

### 5. Frontend: Servicio de Popup de Sesión

**Crear**: `naive-pay-ui/src/app/modules/autentificacion/service/session-warning.service.ts`

```typescript
import { Injectable } from '@angular/core';
import { Subject, Observable } from 'rxjs';

export interface SessionWarningEvent {
  action: 'continue' | 'logout';
}

@Injectable({ providedIn: 'root' })
export class SessionWarningService {
  private warningSubject = new Subject<SessionWarningEvent>();

  // Observable para que AutentificacionService escuche respuestas del usuario
  warning$ = this.warningSubject.asObservable();

  // Emite evento cuando usuario responde al popup
  emitWarning(event: SessionWarningEvent): void {
    this.warningSubject.next(event);
  }
}
```

---

### 6. Frontend: Componente Popup de Advertencia

**Crear**: `naive-pay-ui/src/app/modules/autentificacion/components/session-warning-popup/session-warning-popup.component.ts`

```typescript
import { Component, inject } from '@angular/core';
import { AutentificacionService } from '../../service/autentificacion.service';
import { SessionWarningService } from '../../service/session-warning.service';

@Component({
  selector: 'app-session-warning-popup',
  templateUrl: './session-warning-popup.component.html',
  styleUrls: ['./session-warning-popup.component.css']
})
export class SessionWarningPopupComponent {
  private readonly authService = inject(AutentificacionService);
  private readonly warningService = inject(SessionWarningService);

  visible = false;

  ngOnInit() {
    // Escuchar eventos de advertencia desde el servicio
    this.warningService.warning$.subscribe(() => {
      this.visible = true;
    });
  }

  onContinue(): void {
    this.visible = false;
    this.authService.extendSession().subscribe({
      next: () => console.log('Sesión extendida'),
      error: () => this.authService.logout()
    });
  }

  onLogout(): void {
    this.visible = false;
    this.authService.logout();
  }
}
```

**Template**: `session-warning-popup.component.html`
```html
<div *ngIf="visible" class="overlay">
  <div class="popup-card">
    <h3>¿Sigues conectado?</h3>
    <p>Tu sesión expirará en 1 minuto</p>
    <div class="buttons">
      <button (click)="onLogout()" class="btn-secondary">Salir</button>
      <button (click)="onContinue()" class="btn-primary">Seguir Conectado</button>
    </div>
  </div>
</div>
```

**Styles**: `session-warning-popup.component.css`
```css
.overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 9999;
}

.popup-card {
  background: white;
  padding: 2rem;
  border-radius: 8px;
  box-shadow: 0 4px 6px rgba(0,0,0,0.1);
  max-width: 400px;
  text-align: center;
}

.buttons {
  display: flex;
  gap: 1rem;
  justify-content: center;
  margin-top: 1.5rem;
}
```

---

### 7. Frontend: Integrar Popup en AppComponent

**Modificar**: `naive-pay-ui/src/app/app.component.html`

Agregar al final del template:
```html
<app-session-warning-popup></app-session-warning-popup>
```

**Modificar**: `naive-pay-ui/src/app/app.module.ts`

Declarar el componente:
```typescript
import { SessionWarningPopupComponent } from './modules/autentificacion/components/session-warning-popup/session-warning-popup.component';

@NgModule({
  declarations: [
    // ...
    SessionWarningPopupComponent
  ],
  // ...
})
export class AppModule { }
```

---

### 8. Frontend: Conectar AutentificacionService con SessionWarningService

**Modificar**: `naive-pay-ui/src/app/modules/autentificacion/service/autentificacion.service.ts`

**Inyectar servicio**:
```typescript
private readonly sessionWarning = inject(SessionWarningService);
```

**Modificar método `showSessionWarning`**:
```typescript
private showSessionWarning(): void {
  this.sessionWarning.emitWarning({ action: 'continue' });
}
```

---

## Flujo Completo

### Caso 1: Usuario Activo (hace clic en "Seguir Conectado")

1. Usuario hace login → recibe JWT con exp = now + 10 min
2. Frontend programa timer de advertencia (9 min) y logout (10 min)
3. A los 9 minutos → Popup aparece: "¿Sigues conectado?"
4. Usuario hace clic en "Seguir Conectado"
5. Frontend llama POST `/auth/extend-session`
6. Backend valida sesión activa, genera nuevo JWT (exp = now + 10 min)
7. Backend actualiza `sesExpires` en BD
8. Frontend guarda nuevo token y reprograma timers (9 min advertencia, 10 min logout)
9. Ciclo se repite indefinidamente mientras usuario siga activo

### Caso 2: Usuario Inactivo (no responde al popup)

1. Usuario hace login → recibe JWT con exp = now + 10 min
2. A los 9 minutos → Popup aparece: "¿Sigues conectado?"
3. Usuario NO responde (1 minuto pasa)
4. A los 10 minutos → Timer de logout se ejecuta
5. Frontend llama POST `/auth/logout`
6. Backend marca sesión como CLOSED en BD
7. Frontend limpia token y redirige a login con reason=session_closed

### Caso 3: Usuario Cierra Sesión Manualmente

1. Usuario hace clic en "Salir" en el popup (o botón de logout en UI)
2. Frontend llama POST `/auth/logout`
3. Backend marca sesión como CLOSED
4. Frontend limpia token y redirige a login con reason=logout_ok

---

## Orden de Implementación (Pasos)

### Fase 1: Backend (session extension)
1. **Cambiar configuración**: `security.jwt.ttl-minutes=10` en application.properties
2. **Crear DTO**: `ExtendSessionResponse.java`
3. **Crear endpoint**: `SessionExtensionController.java` con método `extendSession()`
4. **Agregar método a servicio**: `AuthSessionService.extendSession()`
5. **Probar con Postman/curl**: Verificar que endpoint extiende sesión correctamente

### Fase 2: Frontend (warning timer + service)
6. **Crear servicio de popup**: `SessionWarningService.ts`
7. **Modificar AutentificacionService**:
   - Agregar `warningTimer` property
   - Modificar `scheduleAutoLogout()` para programar advertencia 1 min antes
   - Agregar método `extendSession()`
   - Modificar `cleanupTimers()` para limpiar ambos timers
   - Inyectar `SessionWarningService` y emitir evento en `showSessionWarning()`

### Fase 3: Frontend (popup component)
8. **Crear componente**: `SessionWarningPopupComponent`
   - Template con overlay + card + 2 botones
   - Lógica para mostrar/ocultar basado en evento del servicio
   - Métodos `onContinue()` y `onLogout()`
9. **Integrar en AppComponent**: Agregar selector del popup
10. **Declarar en AppModule**: Agregar componente a declarations

### Fase 4: Testing
11. **Test end-to-end**:
    - Login → esperar 9 min → verificar popup aparece
    - Click "Seguir Conectado" → verificar nuevo token recibido
    - Esperar otros 9 min → verificar popup aparece nuevamente
    - NO responder → esperar 1 min → verificar logout automático
    - Login → click "Salir" → verificar logout inmediato

---

## Consideraciones de Seguridad

### ✅ Ventajas
- **Timeout automático**: Sesiones inactivas se cierran después de 10 minutos
- **Extensión controlada**: Solo usuarios autenticados pueden extender (requiere token válido)
- **Auditoría**: Cada extensión se registra en logs con MDC (userId, jti, newExpires)
- **No crea sesiones huérfanas**: Reutiliza mismo jti, solo actualiza expiración

### ⚠️ Puntos a considerar
- **Refresh infinito**: Usuario puede mantener sesión indefinidamente haciendo clic cada 9 minutos
  - **Solución**: Agregar límite máximo de extensiones (ej: 6 extensiones = 1 hora máx)
  - **Implementación**: Agregar campo `sesExtensionCount` en Session entity, validar en endpoint
- **Concurrencia**: Si usuario tiene múltiples pestañas, cada una programa su propio timer
  - **Solución**: Usar BroadcastChannel API para sincronizar timers entre pestañas
- **Robo de token**: Si token es robado, atacante puede extender indefinidamente
  - **Mitigación actual**: Device fingerprint validation en cada request (ya implementado)

---

## Alternativas Consideradas (No elegidas)

### Opción A: Refresh Token Separado (OAuth 2.0 style)
- **Descripción**: Access token (corta duración) + Refresh token (larga duración)
- **Rechazada**: Más complejo, requiere 2 tokens, no necesario para app interna

### Opción B: Sliding Window Automático
- **Descripción**: Cada request válido extiende automáticamente la sesión +10 min
- **Rechazada**: No alerta al usuario, sesiones pueden quedar abiertas indefinidamente sin interacción

### Opción C: Heartbeat Polling
- **Descripción**: Frontend envía "ping" cada X minutos para mantener sesión viva
- **Rechazada**: Genera tráfico innecesario, consume recursos backend

---

## Testing Manual (Checklist)

### Backend
- [ ] Configuración cambiada a 10 min
- [ ] Endpoint `/auth/extend-session` responde 200 con nuevo token
- [ ] Endpoint valida sesión activa (rechaza si sesión cerrada)
- [ ] Campo `sesExpires` se actualiza en BD después de extensión
- [ ] Logs muestran "Sesión extendida" con userId y jti

### Frontend
- [ ] Después de login, timer de advertencia se programa (9 min)
- [ ] Popup aparece exactamente a los 9 minutos
- [ ] Click "Seguir Conectado" → nuevo token guardado en sessionStorage
- [ ] Después de extensión, timer se reprograma (otros 9 min)
- [ ] Click "Salir" → logout ejecutado, redirige a login
- [ ] Si no responde popup → logout automático a los 10 min
- [ ] Logs en consola muestran "Sesión extendida" o "Auto-logout"

---

## Archivos Modificados/Creados

### Backend (6 archivos)
1. ✏️ `application.properties` - Cambiar ttl de 60 a 10
2. ➕ `ExtendSessionResponse.java` - DTO para respuesta
3. ➕ `SessionExtensionController.java` - Endpoint de extensión
4. ✏️ `AuthSessionService.java` - Agregar método extendSession()
5. ✏️ `JWTService.java` - (No requiere cambios, ya genera tokens con ttl configurable)
6. ✏️ `JWTServiceImpl.java` - (No requiere cambios)

### Frontend (5 archivos)
7. ✏️ `autentificacion.service.ts` - Timers + método extendSession()
8. ➕ `session-warning.service.ts` - Servicio de eventos de popup
9. ➕ `session-warning-popup.component.ts` - Componente de popup
10. ➕ `session-warning-popup.component.html` - Template del popup
11. ➕ `session-warning-popup.component.css` - Estilos del popup
12. ✏️ `app.component.html` - Agregar selector del popup
13. ✏️ `app.module.ts` - Declarar componente

**Total: 13 archivos (6 backend + 7 frontend)**

---

## Mensajes de Commit (Conventional Commits)

```
feat(backend): Reducir tiempo de sesión JWT a 10 minutos

- Cambiar security.jwt.ttl-minutes de 60 a 10 en application.properties
```

```
feat(backend): Agregar endpoint de extensión de sesión

- Crear SessionExtensionController con POST /auth/extend-session
- Crear DTO ExtendSessionResponse
- Agregar método extendSession() a AuthSessionService
- Validar sesión activa antes de extender
- Actualizar sesExpires en BD al extender
- Agregar logs con MDC para auditoría
```

```
feat(frontend): Implementar popup de advertencia de sesión

- Crear SessionWarningService para eventos de popup
- Crear SessionWarningPopupComponent con overlay y botones
- Modificar AutentificacionService para programar advertencia 1 min antes
- Agregar método extendSession() al servicio
- Agregar timer de advertencia (warningTimer)
- Modificar cleanupTimers() para limpiar ambos timers
- Integrar popup en AppComponent
```

---

## Estimación de Tiempo

- **Backend**: 1-2 horas (endpoint + tests)
- **Frontend**: 2-3 horas (servicio + componente + estilos)
- **Testing manual**: 1 hora (validar todos los flujos)
- **Total**: 4-6 horas

---

## Próximos Pasos Sugeridos

1. **Implementar límite de extensiones**: Evitar sesiones infinitas
2. **Agregar analytics**: Registrar cuántas veces usuario extiende sesión
3. **Sincronizar entre pestañas**: Usar BroadcastChannel API
4. **Notificaciones del sistema**: Mostrar notificación nativa del navegador cuando sesión expira
