# Recomendación: Estrategia de Tiempo de Vida de Sesión para Naive-Pay

## Análisis del Contexto

### Características Críticas de Naive-Pay

1. **Transacciones con timeout de 3 minutos**: Pagos tienen límite temporal estricto
2. **Doble sistema de autenticación**:
   - Clave de acceso web (login)
   - Clave privada (aprobación de transacciones)
3. **Seguridad crítica**: Manejo de dinero real, pagos, fondos
4. **Dispositivo único vinculado**: Control estricto por dispositivo
5. **Operaciones diversas**: Aprobación rápida de pagos + navegación prolongada de historial

### Escenarios de Uso Real

#### Escenario A: Aprobación Rápida de Pago
```
Tiempo estimado: 2-5 minutos
0:00 - Usuario compra en comercio externo
0:30 - Login a Naive-Pay app web
1:00 - Revisa solicitud de pago pendiente
2:00 - Ingresa clave privada
2:30 - Pago aprobado → Cierra app
```

#### Escenario B: Gestión de Cuenta
```
Tiempo estimado: 10-20 minutos
0:00 - Login
1:00 - Revisa historial de transacciones (últimos 3 meses)
5:00 - Consulta saldo y fondos
8:00 - Revisa puntos de recompensas
12:00 - Genera reporte de gastos mensuales
15:00 - Canjea puntos por descuento
```

#### Escenario C: Múltiples Pagos Consecutivos
```
Tiempo estimado: 8-15 minutos
0:00 - Login
1:00 - Aprueba pago #1 (Netflix)
4:00 - Aprueba pago #2 (Steam)
7:00 - Aprueba pago #3 (Amazon)
10:00 - Revisa saldo restante → Cierra app
```

---

## ❌ Por Qué 10 Minutos Fijos NO es Óptimo

### Problemas Identificados

1. **Demasiado corto para navegación prolongada**
   - Usuario revisando historial de 3 meses → popup interrumpe cada 9 minutos
   - Genera frustración en tareas legítimas de gestión

2. **No se alinea con patrones de uso**
   - Aprobación rápida (2-5 min): Sobra tiempo
   - Gestión completa (15-20 min): Falta tiempo, requiere múltiples extensiones

3. **Extensiones infinitas sin límite máximo**
   - Usuario puede mantener sesión abierta indefinidamente haciendo clic cada 9 min
   - Riesgo de seguridad: sesión abierta por horas si usuario olvida cerrar

4. **Interrumpe transacciones activas**
   - Usuario en medio de aprobar 3 pagos consecutivos → popup aparece
   - Rompe flujo de trabajo natural

---

## ✅ Estrategia Recomendada: Sliding Window Híbrido

### Concepto: "15 minutos absolutos + 10 minutos de inactividad"

```
Reglas:
1. Token JWT válido por 15 minutos desde emisión (límite absoluto mínimo)
2. Cada request del usuario resetea contador de inactividad a 10 minutos
3. Si 9 minutos sin actividad → Popup "¿Sigues conectado?"
4. Si 10 minutos sin actividad → Logout automático
5. Límite máximo absoluto: 30 minutos desde login inicial
```

### Configuración

```properties
# Backend: application.properties

# Duración base del JWT (no renovable automáticamente)
security.jwt.ttl-minutes=15

# Tiempo de inactividad permitido antes de expirar sesión
security.session.inactivity-timeout-minutes=10

# Advertencia antes de expirar por inactividad
security.session.warning-before-expiry-minutes=1

# Tiempo máximo absoluto de sesión (desde login inicial)
security.session.max-lifetime-minutes=30
```

---

## Comparación: 10 min Fijos vs Sliding Window

| Criterio | 10 min Fijos | 15 min + Sliding | Ganador |
|----------|--------------|------------------|---------|
| **Seguridad: Auto-logout inactivo** | ✅ 10 min | ✅ 10 min | 🟰 Empate |
| **UX: No interrumpe usuarios activos** | ❌ Popup cada 9 min | ✅ Solo si inactivo | ✅ Sliding |
| **Límite máximo de sesión** | ❌ Infinito (con extensiones) | ✅ 30 min máx | ✅ Sliding |
| **Previene sesiones huérfanas** | ❌ Usuario puede extender indefinidamente | ✅ Hard limit 30 min | ✅ Sliding |
| **Alineado con transacciones (3 min)** | ⚠️ Puede interrumpir múltiples pagos | ✅ Actividad resetea contador | ✅ Sliding |
| **Simplicidad de implementación** | ✅ Más simple | ⚠️ Más complejo | ✅ Fijos |
| **Carga en backend** | ✅ Baja (no actualiza BD constantemente) | ⚠️ Media (actualiza lastActivity) | ✅ Fijos |

### Resultado: Sliding Window gana 5 de 7 criterios

---

## Justificación Detallada

### 1. Seguridad Mejorada

#### Con 10 min fijos + extensión manual:
```
Usuario login → 9 min → Popup → Click "Seguir" → +10 min
→ 9 min → Popup → Click "Seguir" → +10 min
→ 9 min → Popup → Click "Seguir" → +10 min
... (puede continuar indefinidamente si hace clic)
```
**Problema**: Sesión puede durar horas si usuario hace clic mecánicamente cada 9 min.

#### Con Sliding Window + límite 30 min:
```
Usuario login → Navega activamente → Cada click resetea inactividad
→ 30 minutos desde login inicial → Logout forzado (sin excepción)
```
**Ventaja**: Garantiza que NINGUNA sesión dure más de 30 minutos, sin importar actividad.

---

### 2. UX Alineado con Patrones de Uso

#### Caso: Aprobación Rápida de Pago (2-5 min)

**10 min fijos**:
```
0:00 - Login
2:00 - Aprueba pago
2:30 - Cierra app
✅ No hay popup (termina antes de 9 min)
```

**Sliding window**:
```
0:00 - Login
2:00 - Aprueba pago (resetea inactividad)
2:30 - Cierra app
✅ No hay popup (termina antes de 9 min)
```
**Resultado**: 🟰 Empate (ambos funcionan bien)

---

#### Caso: Navegación Prolongada (15-20 min)

**10 min fijos**:
```
0:00 - Login
5:00 - Revisa historial
9:00 - 🔴 POPUP "¿Sigues conectado?" (interrumpe)
9:30 - Click "Seguir Conectado" → Nuevo token
12:00 - Revisa recompensas
18:00 - 🔴 POPUP nuevamente (interrumpe)
18:30 - Click "Seguir Conectado"
20:00 - Termina
```
**Problema**: 2 interrupciones para tarea legítima de gestión.

**Sliding window**:
```
0:00 - Login
5:00 - Revisa historial (resetea inactividad)
8:00 - Consulta saldo (resetea inactividad)
12:00 - Revisa recompensas (resetea inactividad)
15:00 - Genera reporte (resetea inactividad)
20:00 - Termina
✅ Sin interrupciones (cada navegación resetea)
```
**Resultado**: ✅ Sliding window gana (mejor UX)

---

#### Caso: Usuario se Distrae y Deja Tab Abierta

**10 min fijos**:
```
0:00 - Login
2:00 - Aprueba pago
2:01 - Usuario recibe llamada, deja tab abierta
9:00 - 🔴 POPUP aparece (usuario no ve, está en llamada)
10:00 - ⚠️ Auto-logout (sesión cerrada)
✅ Seguridad: sesión no queda abierta
```

**Sliding window**:
```
0:00 - Login
2:00 - Aprueba pago (última actividad)
2:01 - Usuario recibe llamada, deja tab abierta
11:00 - 🔴 POPUP aparece (9 min sin actividad desde 2:00)
12:00 - ⚠️ Auto-logout (10 min sin actividad)
✅ Seguridad: sesión no queda abierta
```
**Resultado**: 🟰 Empate (ambos cierran sesión inactiva)

---

### 3. Prevención de Sesiones Huérfanas

**Problema con 10 min fijos sin límite máximo**:
```
Usuario A (malintencionado o despistado):
0:00 - Login
9:00 - Click "Seguir"
18:00 - Click "Seguir"
27:00 - Click "Seguir"
...
3 horas después - Sesión sigue activa
```

**Solución con Sliding Window + límite 30 min**:
```
Usuario A:
0:00 - Login
9:00 - Click "Seguir" (extiende inactividad, pero no extiende límite absoluto)
18:00 - Click "Seguir"
27:00 - Click "Seguir"
30:00 - ⚠️ LOGOUT FORZADO (límite absoluto alcanzado)
```
**Resultado**: ✅ Sliding window gana (garantiza cierre después de 30 min)

---

## Implementación Técnica: Sliding Window

### Cambios en Backend

#### 1. Agregar Campos a Session Entity

```java
@Entity
@Table(name = "session")
public class Session {
    // ... campos existentes ...

    @Column(name = "ses_last_activity")
    private Instant sesLastActivity;  // ✅ NUEVO: última actividad del usuario

    @Column(name = "ses_max_expiration")
    private Instant sesMaxExpiration;  // ✅ NUEVO: límite absoluto (created + 30 min)
}
```

#### 2. Modificar AuthSessionService

```java
@Service
@RequiredArgsConstructor
public class AuthSessionService {
    private final SessionRepository authRepo;

    @Value("${security.jwt.ttl-minutes}")
    private long jwtTtlMinutes;  // 15 min

    @Value("${security.session.inactivity-timeout-minutes}")
    private long inactivityTimeoutMinutes;  // 10 min

    @Value("${security.session.max-lifetime-minutes}")
    private long maxLifetimeMinutes;  // 30 min

    @Transactional
    public Session saveActiveSession(UUID jti, User user, Device device) {
        Instant now = Instant.now();
        Instant jwtExpiration = now.plus(jwtTtlMinutes, ChronoUnit.MINUTES);
        Instant maxExpiration = now.plus(maxLifetimeMinutes, ChronoUnit.MINUTES);

        Session session = Session.builder()
                .sesJti(jti)
                .user(user)
                .device(device)
                .sesDeviceFingerprint(device != null ? device.getFingerprint() : null)
                .sesCreated(now)
                .sesExpires(jwtExpiration)  // JWT expira en 15 min
                .sesLastActivity(now)       // ✅ Última actividad = ahora
                .sesMaxExpiration(maxExpiration)  // ✅ Límite absoluto = 30 min
                .status(SessionStatus.ACTIVE)
                .build();

        return authRepo.save(session);
    }

    // ✅ NUEVO: Actualizar última actividad en cada request
    @Transactional
    public void updateLastActivity(UUID jti) {
        Session session = authRepo.findBySesJtiAndStatus(jti, SessionStatus.ACTIVE)
                .orElseThrow(() -> new IllegalArgumentException("Session not found"));

        Instant now = Instant.now();

        // Validar que no haya superado límite absoluto
        if (now.isAfter(session.getSesMaxExpiration())) {
            session.setStatus(SessionStatus.CLOSED);
            session.setSesClosed(session.getSesMaxExpiration());
            authRepo.save(session);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "MAX_SESSION_TIME_EXCEEDED");
        }

        // Validar que no haya superado inactividad
        Instant inactivityLimit = session.getSesLastActivity()
                .plus(inactivityTimeoutMinutes, ChronoUnit.MINUTES);
        if (now.isAfter(inactivityLimit)) {
            session.setStatus(SessionStatus.CLOSED);
            session.setSesClosed(inactivityLimit);
            authRepo.save(session);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "SESSION_INACTIVE_TIMEOUT");
        }

        // Actualizar última actividad
        session.setSesLastActivity(now);
        authRepo.save(session);
    }

    // ✅ NUEVO: Obtener tiempo restante hasta inactividad
    @Transactional(readOnly = true)
    public long getMinutesUntilInactivity(UUID jti) {
        Session session = authRepo.findBySesJtiAndStatus(jti, SessionStatus.ACTIVE)
                .orElseThrow(() -> new IllegalArgumentException("Session not found"));

        Instant now = Instant.now();
        Instant inactivityLimit = session.getSesLastActivity()
                .plus(inactivityTimeoutMinutes, ChronoUnit.MINUTES);

        long secondsRemaining = ChronoUnit.SECONDS.between(now, inactivityLimit);
        return secondsRemaining / 60;
    }
}
```

#### 3. Modificar JwtAuthFilter

```java
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {
    private final JWTService jwtService;
    private final AuthSessionService sessionService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, ...) {
        // ... validación de token existente ...

        String jtiStr = jwtService.getJti(token);
        UUID jti = UUID.fromString(jtiStr);

        // ✅ ACTUALIZAR última actividad en cada request autenticado
        try {
            sessionService.updateLastActivity(jti);
        } catch (ResponseStatusException e) {
            // Sesión expiró por inactividad o límite máximo
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        // ... continuar con autenticación ...
    }
}
```

#### 4. Nuevo Endpoint: Obtener Estado de Sesión

```java
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class SessionStatusController {
    private final JWTService jwtService;
    private final AuthSessionService sessionService;

    // GET /auth/session-status
    // Retorna minutos restantes hasta inactividad
    @GetMapping("/session-status")
    public ResponseEntity<SessionStatusResponse> getSessionStatus(
        @RequestHeader("Authorization") String authHeader
    ) {
        String token = authHeader.replace("Bearer ", "");
        String jtiStr = jwtService.getJti(token);
        UUID jti = UUID.fromString(jtiStr);

        long minutesUntilInactivity = sessionService.getMinutesUntilInactivity(jti);

        return ResponseEntity.ok(new SessionStatusResponse(minutesUntilInactivity));
    }
}
```

---

### Cambios en Frontend

#### 1. Modificar AutentificacionService

```typescript
@Injectable({ providedIn: 'root' })
export class AutentificacionService implements OnDestroy {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  private readonly deviceFp = inject(DeviceFingerprintService);
  private readonly base = 'http://localhost:8080/auth';

  private inactivityCheckTimer: ReturnType<typeof setTimeout> | null = null;
  private warningTimer: ReturnType<typeof setTimeout> | null = null;

  constructor() {
    const token = sessionStorage.getItem('token');
    if (token) this.startInactivityMonitoring();
  }

  ngOnDestroy(): void {
    this.cleanupTimers();
  }

  // ✅ NUEVO: Monitorear inactividad cada minuto
  private startInactivityMonitoring(): void {
    this.cleanupTimers();

    this.inactivityCheckTimer = setInterval(() => {
      this.http.get<{ minutesUntilInactivity: number }>(`${this.base}/session-status`)
        .subscribe({
          next: (res) => {
            // Si quedan 1 minuto o menos → mostrar popup
            if (res.minutesUntilInactivity <= 1 && !this.warningTimer) {
              this.showInactivityWarning();
            }
          },
          error: (err) => {
            if (err.status === 401) {
              // Sesión expirada
              this.clearAndRedirect('session_closed');
            }
          }
        });
    }, 60000);  // Cada 1 minuto
  }

  private showInactivityWarning(): void {
    // Emitir evento para mostrar popup
    this.sessionWarning.emitWarning({ action: 'continue' });

    // Si no responde en 1 minuto → logout
    this.warningTimer = setTimeout(() => {
      this.logout(false).subscribe();
    }, 60000);
  }

  // Cuando usuario hace clic en "Seguir Conectado"
  onUserInteraction(): void {
    // Cancelar timer de advertencia si está activo
    if (this.warningTimer) {
      clearTimeout(this.warningTimer);
      this.warningTimer = null;
    }

    // Cualquier request HTTP actualiza sesLastActivity en backend automáticamente
    // No necesita endpoint dedicado de "extend-session"
  }

  private cleanupTimers(): void {
    if (this.inactivityCheckTimer) {
      clearInterval(this.inactivityCheckTimer);
      this.inactivityCheckTimer = null;
    }
    if (this.warningTimer) {
      clearTimeout(this.warningTimer);
      this.warningTimer = null;
    }
  }

  login(req: LoginRequest): Observable<LoginResponse> {
    const headers = new HttpHeaders().set('X-Device-Fingerprint', this.deviceFp.get());
    return this.http.post<LoginResponse>(`${this.base}/login`, req, { headers }).pipe(
      tap(res => {
        sessionStorage.setItem('token', res.accessToken);
        this.startInactivityMonitoring();  // ✅ Iniciar monitoreo
      })
    );
  }

  logout(redirect: boolean = true): Observable<void> {
    this.cleanupTimers();
    return this.http.post<void>(`${this.base}/logout`, {}).pipe(
      tap(() => {
        this.clear();
        if (redirect) {
          void this.router.navigate(['/auth/login'], { queryParams: { reason: 'logout_ok' } });
        }
      }),
      catchError(() => {
        this.clear();
        if (redirect) {
          void this.router.navigate(['/auth/login'], { queryParams: { reason: 'logout_ok' } });
        }
        return of(void 0);
      })
    );
  }
}
```

---

## Flujos Completos con Sliding Window

### Flujo A: Usuario Activo Navegando (15 min)

```
0:00 - Login → sesCreated, sesLastActivity = 0:00, sesMaxExpiration = 0:30
1:00 - GET /api/fondos/saldo → sesLastActivity = 1:00
3:00 - GET /api/transacciones/historial → sesLastActivity = 3:00
6:00 - GET /api/recompensas/puntos → sesLastActivity = 6:00
9:00 - POST /api/pagos/aprobar → sesLastActivity = 9:00
12:00 - GET /api/reportes/mensual → sesLastActivity = 12:00
15:00 - Cierra app

✅ Sin interrupciones (cada request resetea inactividad)
✅ No alcanza límite de 10 min inactividad
✅ No alcanza límite de 30 min absoluto
```

---

### Flujo B: Usuario Inactivo (deja tab abierta)

```
0:00 - Login → sesCreated, sesLastActivity = 0:00
2:00 - GET /api/fondos/saldo → sesLastActivity = 2:00
2:01 - Usuario se distrae (recibe llamada)
... sin actividad ...
11:00 - Frontend polling detecta minutesUntilInactivity = 1
11:00 - 🔴 Popup aparece: "¿Sigues conectado?"
12:00 - Usuario no responde (sigue en llamada)
12:00 - ⚠️ Auto-logout (10 min desde última actividad en 2:00)

✅ Seguridad: sesión no queda abierta indefinidamente
```

---

### Flujo C: Usuario Alcanza Límite Máximo 30 min

```
0:00 - Login → sesMaxExpiration = 0:30
5:00 - GET /transacciones → sesLastActivity = 5:00
10:00 - GET /recompensas → sesLastActivity = 10:00
15:00 - GET /reportes → sesLastActivity = 15:00
20:00 - GET /fondos → sesLastActivity = 20:00
25:00 - GET /pagos → sesLastActivity = 25:00
29:00 - GET /historial → sesLastActivity = 29:00
30:00 - GET /saldo → ⚠️ Rechazado (MAX_SESSION_TIME_EXCEEDED)
30:00 - Frontend detecta 401 → clearAndRedirect('session_closed')

✅ Garantiza que ninguna sesión dure más de 30 minutos
✅ Previene sesiones huérfanas
```

---

## Ventajas del Sliding Window para Naive-Pay

### 1. Alineado con Transacciones de 3 Minutos

```
Usuario aprobando múltiples pagos:
0:00 - Login
1:00 - Aprueba pago Netflix (sesLastActivity = 1:00)
4:00 - Aprueba pago Steam (sesLastActivity = 4:00)
7:00 - Aprueba pago Amazon (sesLastActivity = 7:00)

✅ Cada aprobación resetea inactividad
✅ No hay popup interrumpiendo el flujo
```

### 2. Seguridad Robusta

- **Inactividad**: Logout después de 10 min sin requests
- **Límite absoluto**: Ninguna sesión dura más de 30 min
- **Device fingerprint**: Validado en cada request (ya implementado)
- **Auditoría**: sesLastActivity permite tracking preciso en logs

### 3. UX Superior

- **Usuarios rápidos** (2-5 min): No ven popup
- **Usuarios navegando** (15-20 min): No son interrumpidos si están activos
- **Usuarios inactivos**: Protegidos con auto-logout

---

## Desventajas y Mitigaciones

### Desventaja 1: Mayor Carga en Backend

**Problema**: Cada request actualiza `sesLastActivity` en BD

**Mitigación**:
```java
// Optimización: Solo actualizar si pasó más de 1 minuto desde última actualización
@Transactional
public void updateLastActivity(UUID jti) {
    Session session = authRepo.findBySesJtiAndStatus(jti, SessionStatus.ACTIVE)
            .orElseThrow(() -> new IllegalArgumentException("Session not found"));

    Instant now = Instant.now();
    Instant lastUpdate = session.getSesLastActivity();

    // Solo actualizar si pasó más de 1 minuto
    if (ChronoUnit.MINUTES.between(lastUpdate, now) < 1) {
        return;  // ✅ Evita writes innecesarios
    }

    session.setSesLastActivity(now);
    authRepo.save(session);
}
```

**Resultado**: Máximo 10 writes por sesión (en sesión de 10 min con requests cada minuto)

---

### Desventaja 2: Complejidad de Implementación

**Problema**: Más código que estrategia de 10 min fijos

**Mitigación**:
- Código bien documentado y modular
- Beneficios en UX y seguridad justifican complejidad adicional
- Una vez implementado, no requiere mantenimiento adicional

---

## Decisión Final: ¿Cuál Elegir?

### Para Naive-Pay, Recomiendo: **Sliding Window Híbrido**

### Justificación

| Criterio | Peso | 10 min Fijos | Sliding Window |
|----------|------|--------------|----------------|
| Seguridad | 🔥🔥🔥 Alta | 7/10 | 9/10 |
| UX para usuarios activos | 🔥🔥 Media | 6/10 | 10/10 |
| Prevención sesiones huérfanas | 🔥🔥🔥 Alta | 4/10 | 10/10 |
| Alineado con transacciones 3 min | 🔥🔥 Media | 6/10 | 9/10 |
| Simplicidad implementación | 🔥 Baja | 9/10 | 6/10 |
| Carga backend | 🔥 Baja | 9/10 | 7/10 |

**Puntaje Ponderado**:
- 10 min Fijos: **6.7/10**
- Sliding Window: **8.9/10**

### ✅ Ganador: Sliding Window Híbrido

---

## Plan de Implementación Modificado

### Configuración Final Recomendada

```properties
# application.properties

# JWT válido por 15 minutos
security.jwt.ttl-minutes=15

# Logout automático después de 10 min sin actividad
security.session.inactivity-timeout-minutes=10

# Popup de advertencia 1 minuto antes de expirar por inactividad
security.session.warning-before-expiry-minutes=1

# Límite absoluto: ninguna sesión dura más de 30 minutos
security.session.max-lifetime-minutes=30
```

### Orden de Implementación

#### Fase 1: Backend (Base)
1. Agregar campos `sesLastActivity` y `sesMaxExpiration` a Session entity
2. Modificar `AuthSessionService.saveActiveSession()` para inicializar nuevos campos
3. Agregar método `updateLastActivity()` a AuthSessionService
4. Agregar método `getMinutesUntilInactivity()` a AuthSessionService

#### Fase 2: Backend (Integración)
5. Modificar `JwtAuthFilter` para llamar `updateLastActivity()` en cada request
6. Crear `SessionStatusController` con endpoint `GET /auth/session-status`
7. Agregar propiedades de configuración a application.properties

#### Fase 3: Frontend (Monitoreo)
8. Modificar `AutentificacionService` para polling de `/session-status` cada 1 min
9. Agregar lógica de detección de inactividad (minutesUntilInactivity <= 1)
10. Implementar `showInactivityWarning()` para mostrar popup

#### Fase 4: Frontend (UI)
11. Crear `SessionWarningPopupComponent` (reutilizar del plan anterior)
12. Integrar popup en AppComponent
13. Conectar botón "Seguir Conectado" (cualquier request resetea inactividad)

#### Fase 5: Testing
14. Test: Usuario activo por 20 min → sin interrupciones
15. Test: Usuario inactivo 10 min → logout automático
16. Test: Usuario alcanza 30 min límite → logout forzado
17. Test: Usuario responde a popup → sesión continúa

---

## Archivos Modificados/Creados

### Backend (7 archivos)
1. ✏️ `Session.java` - Agregar campos sesLastActivity, sesMaxExpiration
2. ✏️ `AuthSessionService.java` - Métodos updateLastActivity(), getMinutesUntilInactivity()
3. ✏️ `JwtAuthFilter.java` - Llamar updateLastActivity() en cada request
4. ➕ `SessionStatusController.java` - Endpoint GET /session-status
5. ➕ `SessionStatusResponse.java` - DTO para respuesta
6. ✏️ `application.properties` - Agregar propiedades de configuración
7. ✏️ `SessionRepository.java` - (Opcional) Query personalizada si es necesario

### Frontend (5 archivos)
8. ✏️ `autentificacion.service.ts` - Polling + detección inactividad
9. ➕ `session-warning.service.ts` - Servicio de eventos (reutilizar plan anterior)
10. ➕ `session-warning-popup.component.ts` - Componente popup (reutilizar)
11. ➕ `session-warning-popup.component.html` - Template (reutilizar)
12. ➕ `session-warning-popup.component.css` - Estilos (reutilizar)
13. ✏️ `app.component.html` - Agregar selector popup
14. ✏️ `app.module.ts` - Declarar componente

**Total: 14 archivos (7 backend + 7 frontend)**

---

## Mensajes de Commit

```
feat(backend): Implementar sliding window para sesiones

- Agregar campos sesLastActivity y sesMaxExpiration a Session entity
- Crear método updateLastActivity() en AuthSessionService
- Modificar JwtAuthFilter para actualizar actividad en cada request
- Configurar límites: 15 min JWT, 10 min inactividad, 30 min absoluto
- Optimizar updates BD: solo actualizar si pasó >1 minuto
```

```
feat(backend): Agregar endpoint de estado de sesión

- Crear SessionStatusController con GET /auth/session-status
- Crear DTO SessionStatusResponse con minutesUntilInactivity
- Agregar método getMinutesUntilInactivity() a AuthSessionService
- Permitir frontend monitorear tiempo restante hasta expiración
```

```
feat(frontend): Implementar monitoreo de inactividad con sliding window

- Modificar AutentificacionService para polling de /session-status cada 1 min
- Agregar detección automática de inactividad (minutesUntilInactivity <= 1)
- Implementar popup de advertencia cuando queda 1 minuto
- Agregar auto-logout si usuario no responde en 1 minuto
- Reutilizar SessionWarningPopupComponent del plan anterior
```

---

## Conclusión

**Para Naive-Pay, el Sliding Window Híbrido es la mejor opción** porque:

✅ **Seguridad robusta**: Auto-logout después de 10 min inactividad + límite 30 min absoluto
✅ **UX superior**: No interrumpe usuarios activos, solo usuarios inactivos
✅ **Alineado con casos de uso**: Funciona bien tanto para pagos rápidos como navegación prolongada
✅ **Previene sesiones huérfanas**: Límite absoluto garantiza cierre después de 30 min
✅ **Auditoría mejorada**: Campo sesLastActivity permite tracking preciso

**Sacrificio aceptable**: Complejidad de implementación y carga backend ligeramente mayor, pero los beneficios justifican el esfuerzo.
