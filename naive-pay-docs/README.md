# Documentación de Esquemas - NaivePay

Este directorio contiene diagramas UML y ER del sistema NaivePay en formato PlantUML.

## Archivos Disponibles

### 1. `database-schema.puml`
Diagrama Entidad-Relación (ER) que muestra:
- Todas las tablas de la base de datos
- Claves primarias y foráneas
- Tipos de datos
- Relaciones entre entidades
- Notas explicativas sobre restricciones y comportamientos especiales

### 2. `class-diagram.puml`
Diagrama de Clases UML que muestra:
- Entidades Java del dominio
- Atributos y métodos
- Relaciones JPA (@OneToOne, @ManyToOne, etc.)
- Enums y tipos embebidos
- Organización por paquetes

## Cómo Visualizar en Visual Paradigm

### Opción 1: Importar PlantUML directamente

1. Abre **Visual Paradigm**
2. Ve a **Tools** → **Code Engineering** → **Instant Reverse**
3. Selecciona **PlantUML** como formato
4. Navega a este directorio y selecciona los archivos `.puml`
5. Haz clic en **OK** para generar el diagrama

### Opción 2: Usar el plugin de PlantUML

1. Instala el plugin de PlantUML en Visual Paradigm:
   - **Tools** → **Plugins** → busca "PlantUML"
2. Abre los archivos `.puml` directamente
3. Visual Paradigm los renderizará automáticamente

### Opción 3: Ingeniería Inversa desde Código Java

1. En Visual Paradigm: **Tools** → **Code** → **Instant Reverse**
2. Selecciona **Java** como lenguaje
3. Navega a: `/home/user/np/naive-pay-api/src/main/java/cl/ufro/dci/naivepayapi`
4. Selecciona los paquetes que quieres incluir:
   - `autentificacion.domain`
   - `registro.domain`
   - `dispositivos.domain`
   - `fondos.domain`
   - `pagos.domain`
   - `comercio.domain`
   - `recompensas.domain`
5. Visual Paradigm generará el diagrama automáticamente

### Opción 4: Ingeniería Inversa desde Base de Datos

1. En Visual Paradigm: **Tools** → **Database** → **Instant Reverse**
2. Configura la conexión a tu base de datos PostgreSQL
3. Selecciona las tablas del esquema
4. Genera el diagrama ER

## Visualización Online (Sin Visual Paradigm)

Si no tienes Visual Paradigm instalado, puedes visualizar los diagramas en:

### PlantUML Online Editor
1. Ve a: https://www.plantuml.com/plantuml/uml/
2. Copia el contenido de cualquier archivo `.puml`
3. Pégalo en el editor
4. El diagrama se generará automáticamente

### VS Code con Extension PlantUML
1. Instala la extensión "PlantUML" en VS Code
2. Abre los archivos `.puml`
3. Presiona `Alt+D` para ver el preview

## Estructura del Sistema

### Módulos Principales

1. **Registro y Autenticación**
   - `User`: Usuario de la aplicación
   - `Credencial`: Credenciales y claves de usuario
   - `Device`: Dispositivo registrado
   - `AuthAttempt`: Intentos de autenticación
   - `Session`: Sesiones activas

2. **Fondos**
   - `Account`: Cuenta bancaria del usuario
   - `FundTransaction`: Transacciones de fondos

3. **Pagos**
   - `PaymentTransaction`: Transacciones de pago a comercios

4. **Comercio**
   - `Commerce`: Información de comercios
   - `CommerceCategory`: Categorías de comercios

5. **Recompensas**
   - `RewardAccount`: Cuenta de recompensas
   - `RewardTransaction`: Transacciones de puntos/recompensas

### Relaciones Clave

- **User ↔ Device**: 1:1 - Un usuario tiene exactamente un dispositivo registrado
- **User ↔ Account**: 1:1 - Un usuario tiene exactamente una cuenta de fondos
- **Session → AuthAttempt → Device → User**: Cadena de navegación para sesiones
- **Account ↔ FundTransaction**: 1:N - Una cuenta puede tener múltiples transacciones

## Actualizar Diagramas

Para regenerar estos diagramas después de cambios en el código:

```bash
# Desde el directorio del proyecto
cd /home/user/np/naive-pay-docs

# Los archivos .puml se pueden editar manualmente
# o regenerar usando herramientas de ingeniería inversa
```

## Notas Importantes

1. **Integridad Referencial**: Las relaciones Session → AuthAttempt → Device → User fueron diseñadas para mantener integridad referencial sin relaciones bidireccionales innecesarias.

2. **Sesiones Huérfanas**: `Session.initialAuthAttempt` es nullable para soportar sesiones de migraciones anteriores.

3. **Transacciones LOAD**: En `FundTransaction`, el campo `originAccount` puede ser NULL para transacciones tipo LOAD (carga de fondos desde el sistema).

4. **Device Único**: Cada usuario solo puede tener un dispositivo registrado (relación 1:1 única).

## Herramientas Recomendadas

- **Visual Paradigm**: Para edición profesional y colaboración
- **PlantUML**: Para versionado y integración CI/CD
- **dbdiagram.io**: Para diagramas ER interactivos online
- **draw.io**: Para edición visual gratuita

## Contacto

Para preguntas sobre la arquitectura del sistema, consulta con el equipo de desarrollo de NaivePay.
