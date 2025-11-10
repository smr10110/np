# Implementación Simplificada: Sliding Window para Sesiones

## Estrategia Más Simple

### Opción A: Sliding Window Completo (Recomendado)
**Complejidad**: Media | **Beneficios**: Máximos

- Agrega 2 campos a Session entity
- Actualiza `sesLastActivity` en cada request (JwtAuthFilter)
- Frontend hace polling cada minuto para detectar inactividad
- **Resultado**: UX perfecta + seguridad robusta

**Tiempo estimado**: 3-4 horas

---

### Opción B: Sliding Window Simplificado (Más Rápido)
**Complejidad**: Baja | **Beneficios**: Buenos

- Agrega SOLO 1 campo: `sesLastActivity`
- NO agrega `sesMaxExpiration` (usa exp del JWT como límite)
- Actualiza `sesLastActivity` en cada request
- Frontend hace polling simple

**Tiempo estimado**: 2-3 horas

---

### Opción C: 10 Minutos Fijos con Límite Máximo (Más Simple)
**Complejidad**: Muy Baja | **Beneficios**: Aceptables

- Agrega SOLO 1 campo: `sesMaxExpiration`
- Endpoint `/auth/extend-session` genera nuevo token
- Frontend muestra popup a los 9 min
- Hard limit de 30 min desde login inicial

**Tiempo estimado**: 1-2 horas

---

## Comparación de Opciones

| Aspecto | Opción A (Completo) | Opción B (Simplificado) | Opción C (Fijos + Límite) |
|---------|---------------------|-------------------------|---------------------------|
| **Campos nuevos** | 2 | 1 | 1 |
| **Archivos backend** | 7 | 5 | 4 |
| **Archivos frontend** | 7 | 6 | 5 |
| **Complejidad código** | Media | Baja | Muy Baja |
| **UX usuarios activos** | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐ |
| **Seguridad** | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ |
| **Tiempo implementación** | 3-4h | 2-3h | 1-2h |

---

## Recomendación: Opción B (Sliding Window Simplificado)

**Por qué**: Balance perfecto entre simplicidad y beneficios.

### Diferencia con Opción A:
- **Elimina**: Campo `sesMaxExpiration` y validación de límite absoluto
- **Simplifica**: Usa `exp` del JWT como límite máximo natural
- **Mantiene**: Sliding window (cada request resetea inactividad)

### Cómo funciona:

```
1. Usuario hace login
   - JWT válido por 15 minutos (exp = now + 15 min)
   - sesLastActivity = now

2. Usuario navega activamente
   - Cada request actualiza sesLastActivity = now
   - Mientras esté activo, no hay popup

3. Usuario se queda inactivo
   - Frontend polling detecta: now - sesLastActivity > 9 min
   - Muestra popup "¿Sigues conectado?"
   - Si no responde en 1 min → logout

4. Límite natural del JWT
   - Después de 15 min desde login inicial
   - JWT expira automáticamente (no renovable)
   - Backend rechaza requests con 401
   - Frontend detecta y hace logout
```

**Ventaja**: JWT exp actúa como límite máximo automático (15 min), no necesitas campo adicional.

---

## Implementación Opción B: Paso a Paso

### Backend (5 archivos)

#### 1. Session.java - Agregar 1 campo
```java
@Column(name = "ses_last_activity", nullable = false)
private Instant sesLastActivity;
```

#### 2. application.properties - Configuración
```properties
security.jwt.ttl-minutes=15
security.session.inactivity-timeout-minutes=10
```

#### 3. AuthSessionService.java - Inicializar y actualizar

**Agregar en `saveActiveSession()`**:
```java
.sesLastActivity(now)  // Inicializar con now
```

**Agregar método nuevo**:
```java
@Transactional
public void updateLastActivity(UUID jti) {
    Session session = authRepo.findBySesJtiAndStatus(jti, SessionStatus.ACTIVE)
            .orElseThrow(() -> new IllegalArgumentException("Session not found"));

    Instant now = Instant.now();
    Instant lastUpdate = session.getSesLastActivity();

    // Optimización: solo actualizar si pasó más de 1 minuto
    if (ChronoUnit.MINUTES.between(lastUpdate, now) < 1) {
        return;
    }

    // Validar inactividad (10 min)
    Instant inactivityLimit = lastUpdate.plus(10, ChronoUnit.MINUTES);
    if (now.isAfter(inactivityLimit)) {
        session.setStatus(SessionStatus.CLOSED);
        session.setSesClosed(inactivityLimit);
        authRepo.save(session);
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "SESSION_INACTIVE");
    }

    session.setSesLastActivity(now);
    authRepo.save(session);
}
```

#### 4. JwtAuthFilter.java - Actualizar en cada request

**Agregar después de validar JWT**:
```java
String jtiStr = jwtService.getJti(token);
UUID jti = UUID.fromString(jtiStr);

// Actualizar última actividad
try {
    sessionService.updateLastActivity(jti);
} catch (ResponseStatusException e) {
    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    return;
}
```

#### 5. SessionStatusController.java - Endpoint para frontend

```java
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class SessionStatusController {
    private final JWTService jwtService;
    private final SessionRepository sessionRepository;

    @GetMapping("/session-status")
    public ResponseEntity<SessionStatusResponse> getStatus(
        @RequestHeader("Authorization") String authHeader
    ) {
        String token = authHeader.replace("Bearer ", "");
        String jtiStr = jwtService.getJti(token);
        UUID jti = UUID.fromString(jtiStr);

        Session session = sessionRepository.findBySesJtiAndStatus(jti, SessionStatus.ACTIVE)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        Instant now = Instant.now();
        long minutesSinceActivity = ChronoUnit.MINUTES.between(session.getSesLastActivity(), now);
        long minutesRemaining = 10 - minutesSinceActivity;

        return ResponseEntity.ok(new SessionStatusResponse(Math.max(0, minutesRemaining)));
    }
}

record SessionStatusResponse(long minutesUntilInactivity) {}
```

---

### Frontend (6 archivos)

#### 6. autentificacion.service.ts - Polling de inactividad

**Agregar properties**:
```typescript
private inactivityCheckTimer: any = null;
private warningShown = false;
```

**Agregar método de polling**:
```typescript
private startInactivityMonitoring(): void {
  this.stopInactivityMonitoring();

  this.inactivityCheckTimer = setInterval(() => {
    this.http.get<{ minutesUntilInactivity: number }>(`${this.base}/session-status`)
      .subscribe({
        next: (res) => {
          // Si queda 1 minuto o menos y no hemos mostrado popup
          if (res.minutesUntilInactivity <= 1 && !this.warningShown) {
            this.warningShown = true;
            this.showInactivityWarning();
          }
        },
        error: (err) => {
          if (err.status === 401) {
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
  // Mostrar popup (implementar en siguiente paso)
  alert('Tu sesión expirará en 1 minuto por inactividad. ¿Deseas continuar?');

  // Si usuario hace click OK, cualquier request resetea actividad
  // Si no responde en 1 min, backend hará logout automático
}
```

**Modificar `login()`**:
```typescript
login(req: LoginRequest): Observable<LoginResponse> {
  const headers = new HttpHeaders().set('X-Device-Fingerprint', this.deviceFp.get());
  return this.http.post<LoginResponse>(`${this.base}/login`, req, { headers }).pipe(
    tap(res => {
      sessionStorage.setItem('token', res.accessToken);
      this.startInactivityMonitoring();  // ✅ Iniciar polling
    })
  );
}
```

**Modificar `logout()`**:
```typescript
logout(redirect: boolean = true): Observable<void> {
  this.stopInactivityMonitoring();  // ✅ Detener polling
  return this.http.post<void>(`${this.base}/logout`, {}).pipe(
    // ... resto del código ...
  );
}
```

**Modificar `ngOnDestroy()`**:
```typescript
ngOnDestroy(): void {
  this.stopInactivityMonitoring();
}
```

#### 7-11. Popup Component (Opcional - puede usar alert() temporalmente)

Si quieres popup bonito, reutiliza los componentes del plan anterior.
Si quieres implementación rápida, usa `alert()` o `confirm()` temporalmente.

---

## Resumen de Cambios (Opción B)

### Backend
1. ✏️ Session.java → Agregar `sesLastActivity` (1 campo)
2. ✏️ application.properties → Agregar configuraciones
3. ✏️ AuthSessionService.java → Agregar método `updateLastActivity()`
4. ✏️ JwtAuthFilter.java → Llamar `updateLastActivity()`
5. ➕ SessionStatusController.java → Endpoint GET /session-status

**Total backend**: 5 archivos

### Frontend
6. ✏️ autentificacion.service.ts → Agregar polling + detección

**Total frontend**: 1 archivo (mínimo) o 6 con popup component

---

## Ventajas de Opción B vs C

| Aspecto | Opción B (Sliding) | Opción C (Fijos) |
|---------|-------------------|------------------|
| Usuario navegando 15 min activamente | ✅ Sin interrupciones | ❌ Popup a los 9 min |
| Usuario inactivo 10 min | ✅ Logout automático | ✅ Logout automático |
| Complejidad backend | Media (5 archivos) | Baja (4 archivos) |
| Complejidad frontend | Baja (polling simple) | Media (manejo de extensión) |
| Requests adicionales | 1 por minuto (polling) | 1 cada 9 min (extensión) |

**Diferencia clave**: Opción B NO requiere endpoint de extensión porque la actividad normal ya resetea el contador.

---

## Mi Recomendación Final

### Para implementar HOY: Opción B (Sliding Simplificado)

**Por qué**:
1. ✅ Solo 1 campo nuevo en BD
2. ✅ No necesita endpoint de extensión (usa actividad natural)
3. ✅ Frontend simple (solo polling)
4. ✅ UX excelente (no interrumpe usuarios activos)
5. ✅ Límite natural de 15 min (exp del JWT)

**Tiempo**: 2-3 horas total

---

## ¿Por Dónde Empezar?

### Orden de Implementación (Opción B)

**Paso 1**: Backend - Agregar campo
- Modificar Session.java (1 campo)
- Modificar application.properties (2 propiedades)

**Paso 2**: Backend - Lógica de actualización
- Modificar AuthSessionService.java (1 método)
- Inyectar en JwtAuthFilter.java (3 líneas)

**Paso 3**: Backend - Endpoint de status
- Crear SessionStatusController.java (nuevo archivo)

**Paso 4**: Frontend - Polling
- Modificar autentificacion.service.ts (1 método)
- Usar alert() temporal o crear popup component

**Paso 5**: Testing
- Probar usuario activo (navegando)
- Probar usuario inactivo (dejar tab abierta)
- Probar límite 15 min (navegación prolongada)

---

## Pregunta para Ti

¿Quieres que implemente la **Opción B (Sliding Simplificado)** ahora?

Ventajas:
- ✅ 2-3 horas de trabajo
- ✅ Solo 1 campo en BD
- ✅ UX excelente
- ✅ Seguridad robusta

Alternativa si tienes prisa:
- Opción C (1-2 horas, UX aceptable)

**¿Cuál prefieres?**
