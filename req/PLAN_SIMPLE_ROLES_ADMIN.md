# PLAN SIMPLE - PROTECCIÓN DE RUTAS Y ENDPOINTS ADMIN

## OBJETIVO
Proteger endpoints backend y rutas frontend para que SOLO usuarios con rol ADMIN puedan acceder.

---

## FASE 1: BASE DE DATOS Y USUARIO ADMIN

### Opción A: Automático con AdminUserInitializer (RECOMENDADO para desarrollo) ✅

**Archivo:** `AdminUserInitializer.java` (YA CREADO)

- ✅ Crea el admin automáticamente al arrancar la app
- ✅ Idempotente (solo crea si no existe)
- ✅ Hashea la contraseña con BCrypt
- ✅ Genera claves RSA automáticamente
- ✅ Crea Register, User, Credencial y Account

**No requiere configuración manual**

### Opción B: Migración SQL manual (para producción)

**Archivo:** `/home/user/np/db/migrations/001_add_user_roles.sql`

Ver archivo SQL para:
- Agregar columna `use_role` a tabla `app_user`
- Crear usuario admin con credenciales
- Instrucciones para generar hash BCrypt

**Uso:** Ejecutar manualmente en producción antes del deploy

---

## FASE 2: BACKEND (6 archivos)

### 1. Crear Enum `UserRole.java` ✅ (YA CREADO)
```java
package cl.ufro.dci.naivepayapi.registro.domain;

public enum UserRole {
    USER,
    ADMIN
}
```

### 2. Modificar `User.java` - Agregar campo ✅ (YA CREADO)
```java
@Enumerated(EnumType.STRING)
@Column(name = "use_role", nullable = false, length = 20)
private UserRole useRole = UserRole.USER;

// No agregar getters/setters - Lombok @Data los genera automáticamente
```

### 3. Modificar `JWTService.java` - Agregar claim
```java
public String generateToken(User user, String jti) {
    return Jwts.builder()
        .setSubject(user.getUseEmail())
        .claim("useId", user.getUseId())
        .claim("jti", jti)
        .claim("role", user.getUseRole().name()) // ← AGREGAR ESTO
        .setIssuedAt(Date.from(now))
        .setExpiration(Date.from(expiration))
        .signWith(SignatureAlgorithm.HS256, secret)
        .compact();
}
```

### 4. Modificar `JwtAuthFilter.java` - Extraer rol y dar authorities
```java
protected void doFilterInternal(...) {
    String token = BearerTokenUtil.extractToken(request);

    if (token != null && jwtService.validateToken(token)) {
        Claims claims = jwtService.extractAllClaims(token);
        String email = claims.getSubject();
        Long userId = claims.get("useId", Long.class);
        String jti = claims.get("jti", String.class);
        String role = claims.get("role", String.class); // ← EXTRAER ROL

        if (authSessionService.isSessionActive(jti, userId)) {
            // Crear authority con el rol
            List<GrantedAuthority> authorities = new ArrayList<>();
            if (role != null) {
                authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
            }

            UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(email, null, authorities); // ← PASAR AUTHORITIES

            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }
    }

    filterChain.doFilter(request, response);
}
```

### 5. Modificar `SecurityConfig.java` - Habilitar method security
```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true) // ← AGREGAR ESTO
public class SecurityConfig {
    // resto del código igual...
}
```

### 6. Proteger endpoints en `CommerceValidationController.java`
```java
@RestController
@RequestMapping("/api/validation")
public class CommerceValidationController {

    @PostMapping("/approve/{id}")
    @PreAuthorize("hasRole('ADMIN')") // ← AGREGAR
    public ResponseEntity<?> approveCommerce(@PathVariable Long id) {
        // tu código...
    }

    @PostMapping("/reject/{id}")
    @PreAuthorize("hasRole('ADMIN')") // ← AGREGAR
    public ResponseEntity<?> rejectCommerce(@PathVariable Long id) {
        // tu código...
    }

    @GetMapping("/pending")
    @PreAuthorize("hasRole('ADMIN')") // ← AGREGAR
    public ResponseEntity<?> getPendingCommerces() {
        // tu código...
    }
}
```

**LISTO BACKEND** ✅

---

## FASE 3: FRONTEND (3 archivos)

### 1. Modificar `login-response.interface.ts` - Agregar role
```typescript
export interface LoginResponse {
    accessToken: string;
    expiresAt: string;
    jti: string;
    role: 'USER' | 'ADMIN'; // ← AGREGAR
}
```

### 2. Modificar `autentificacion.service.ts` - Guardar rol
```typescript
login(credentials: LoginRequest): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${API}/auth/login`, credentials).pipe(
        tap(response => {
            sessionStorage.setItem('token', response.accessToken);
            sessionStorage.setItem('userRole', response.role); // ← GUARDAR ROL
            this.scheduleAutoLogout(response.expiresAt);
        })
    );
}

// Método helper
isAdmin(): boolean {
    return sessionStorage.getItem('userRole') === 'ADMIN';
}
```

### 3. Crear `admin.guard.ts` - Proteger rutas
```typescript
import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

export const adminGuard: CanActivateFn = () => {
    const router = inject(Router);
    const role = sessionStorage.getItem('userRole');

    if (role === 'ADMIN') {
        return true;
    }

    console.warn('Acceso denegado: Se requiere rol ADMIN');
    return router.createUrlTree(['/']);
};
```

### 4. Modificar `app.routes.ts` - Proteger ruta admin
```typescript
{
    path: '',
    canActivate: [authGuard],
    component: DashboardComponent,
    children: [
        { path: '', component: HomeComponent },
        { path: 'devices', component: DispositivosComponent },
        // ... otras rutas ...

        // PROTEGER RUTA DE VALIDACIÓN
        {
            path: 'validatecommerce',
            component: ValidationProcessComponent,
            canActivate: [adminGuard] // ← AGREGAR GUARD
        }
    ]
}
```

**LISTO FRONTEND** ✅

---

## TESTING

### Backend:
1. Login con admin → JWT debe tener `"role": "ADMIN"`
2. `POST /api/validation/approve/1` con admin → 200 OK
3. `POST /api/validation/approve/1` con user normal → 403 Forbidden

### Frontend:
1. Login con admin → puede acceder a `/validatecommerce`
2. Login con user normal → intenta acceder a `/validatecommerce` → redirect a home

---

## RESUMEN DE ARCHIVOS

**Backend (8 archivos):**
1. ✅ `001_add_user_roles.sql` (NUEVO - opcional, para producción)
2. ✅ `UserRole.java` (NUEVO - enum)
3. ✅ `User.java` (modificado - campo useRole agregado)
4. ✅ `AdminUserInitializer.java` (NUEVO - auto-inicialización)
5. ⏳ `JWTService.java` (modificar - agregar claim "role")
6. ⏳ `JwtAuthFilter.java` (modificar - extraer rol del JWT)
7. ⏳ `SecurityConfig.java` (modificar - habilitar @PreAuthorize)
8. ⏳ `CommerceValidationController.java` (modificar - agregar @PreAuthorize)

**Frontend (4 archivos):**
1. ⏳ `login-response.interface.ts` (modificar)
2. ⏳ `autentificacion.service.ts` (modificar)
3. ⏳ `admin.guard.ts` (NUEVO)
4. ⏳ `app.routes.ts` (modificar)

**Total: 12 archivos (4 completados ✅, 8 pendientes ⏳)**

---

## CREDENCIALES ADMIN

```
Email: admin@naivepay.cl
RUT: 11111111-1
Password: Admin@2025
Rol: ADMIN
```
