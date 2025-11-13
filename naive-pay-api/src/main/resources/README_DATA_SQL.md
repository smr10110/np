# data.sql - Inicialización de Datos

## ¿Qué hace este archivo?

`data.sql` se ejecuta **automáticamente** al arrancar Spring Boot y crea el usuario administrador en la base de datos H2.

## Configuración necesaria

En `application-dev.properties`:
```properties
spring.jpa.defer-datasource-initialization=true
```

Esto hace que Spring Boot ejecute `data.sql` **después** de que Hibernate cree las tablas.

## Usuario Admin creado

```
Email: admin@naivepay.cl
RUT: 11111111-1
Password: Admin@2025
Rol: ADMIN
```

## ¿data.sql vs AdminUserInitializer.java?

Actualmente se usa **data.sql** (más simple).

### Comparación:

| Característica | data.sql | AdminUserInitializer.java |
|----------------|----------|---------------------------|
| Auto-ejecuta | ✅ Sí | ✅ Sí |
| Genera hash BCrypt | ⚠️ Hardcodeado | ✅ Dinámico |
| Genera claves RSA | ❌ No | ✅ Sí |
| Idempotente | ✅ WHERE NOT EXISTS | ✅ if (!exists) |
| Simplicidad | ✅ Más simple | ⚠️ Más código |

## Cambiar a AdminUserInitializer

Si prefieres usar `AdminUserInitializer.java` en lugar de `data.sql`:

1. **Borrar o renombrar** `data.sql` a `data.sql.disabled`
2. **Descomentar** en `AdminUserInitializer.java`:
   ```java
   @Component("adminUserInitializer")
   ```
3. **Opcional**: Quitar de `application-dev.properties`:
   ```properties
   # spring.jpa.defer-datasource-initialization=true
   ```

## Limitación actual de data.sql

⚠️ **El script actual NO genera claves RSA** para el usuario admin.

Esto significa que:
- El login funcionará correctamente
- Algunas funcionalidades que requieran claves RSA podrían fallar

**Soluciones:**
1. Usar `AdminUserInitializer.java` (genera RSA automáticamente)
2. Modificar el sistema para que claves RSA sean opcionales para admin
3. Agregar las claves RSA manualmente después

## Verificar que funcionó

1. Arranca la aplicación
2. Revisa los logs (debe aparecer el INSERT ejecutándose)
3. Accede a H2 Console: http://localhost:8080/h2-console
4. Ejecuta:
   ```sql
   SELECT * FROM app_user WHERE use_role = 'ADMIN';
   ```
5. Intenta login con: `admin@naivepay.cl` / `Admin@2025`
