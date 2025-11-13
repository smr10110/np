# PLAN SIMPLE - PROTECCIÓN DE RUTAS Y ENDPOINTS ADMIN

## OBJETIVO
Proteger endpoints backend y rutas frontend para que SOLO usuarios con rol ADMIN puedan acceder.

---

## FASE 1: BASE DE DATOS (1 archivo)

### Archivo: `/home/user/np/migrations/add_user_roles.sql`

```sql
-- Agregar columna use_role
ALTER TABLE users ADD COLUMN use_role VARCHAR(20) NOT NULL DEFAULT 'USER';

-- Actualizar usuarios existentes
UPDATE users SET use_role = 'USER' WHERE use_role IS NULL;

-- Crear usuario admin por defecto
-- Password: Admin@2025 (debes hashear con BCrypt antes de ejecutar)
INSERT INTO users (use_id, use_rut, use_dv, use_names, use_last_names, use_email, use_phone_number, use_profession, use_address, use_state, use_role)
VALUES (999, '11111111', '1', 'Admin', 'Sistema', 'admin@naivepay.cl', '+56912345678', 'Administrador', 'UFRO', 'ACTIVE', 'ADMIN');

-- Crear credencial para admin (genera el hash antes)
INSERT INTO credenciales (use_id, cre_password, cre_key_status)
VALUES (999, '$2a$10$...TU_HASH_AQUI...', 'ACTIVE');

-- Crear cuenta de fondos
INSERT INTO accounts (use_id, acc_balance, acc_created_at)
VALUES (999, 0.00, CURRENT_TIMESTAMP);
```

**CREDENCIALES ADMIN:**
```
Email: admin@naivepay.cl
RUT: 11111111-1
Password: Admin@2025
```

---

## FASE 2: BACKEND (5 archivos)

### 1. Crear Enum `UserRole.java`
```java
package cl.ufro.dci.naivepayapi.registro.domain;

public enum UserRole {
    USER,
    ADMIN
}
```

### 2. Modificar `User.java` - Agregar campo
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

**Backend (6 archivos):**
1. `add_user_roles.sql` (NUEVO)
2. `UserRole.java` (NUEVO)
3. `User.java` (modificar)
4. `JWTService.java` (modificar)
5. `JwtAuthFilter.java` (modificar)
6. `SecurityConfig.java` (modificar)
7. `CommerceValidationController.java` (modificar)

**Frontend (4 archivos):**
1. `login-response.interface.ts` (modificar)
2. `autentificacion.service.ts` (modificar)
3. `admin.guard.ts` (NUEVO)
4. `app.routes.ts` (modificar)

**Total: 11 archivos**

---

## CREDENCIALES ADMIN

```
Email: admin@naivepay.cl
RUT: 11111111-1
Password: Admin@2025
Rol: ADMIN
```
