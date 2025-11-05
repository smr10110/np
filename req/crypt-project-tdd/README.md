## Documento de especificación: Casos de uso y casos de prueba derivados del código

#### 1) Introducción
Este documento resume los casos de uso del sistema de cifrado “crypt-app” y detalla los casos de prueba derivados directamente del código y pruebas existentes en el proyecto. La aplicación es un servicio Spring Boot que cifra y almacena mensajes, y posteriormente permite su recuperación y descifrado mediante una clave.
Se considera el desarrollo de un servicio de cifrado AES que permite:
- cifrar(Texto Plano, key)
- decifrar(Id mensaje, key)

#### 2) Alcance y contexto
- Tecnología principal: Spring Boot 3.5.x, Java 21, JPA/H2 para persistencia en tests.
- Componentes relevantes:
    - `AESCrypt`: utilidad para cifrar/descifrar con AES.
    - `MessageService`: capa de negocio para guardar y recuperar mensajes.
    - `MessageController`: expone endpoints REST para cifrar/guardar y descifrar/consultar.
    - `Message`: entidad que persiste el texto cifrado; usa un `mesId` autogenerado y un `mesCipherText`.
- Pruebas existentes: unitarias (utilidad), de servicio y de controlador con `MockMvc` y contexto Spring.



### 3) Especificación funcional (Casos de uso)

#### CU-01: Cifrar y guardar mensaje
- Actor: Cliente de la API.
- Objetivo: Enviar un texto plano y una llave para que el sistema lo cifre y lo persista.
- Precondiciones:
    - El servicio está disponible.
    - La llave proporcionada cumple con el tamaño requerido por AES (en el código de pruebas se usa 16 caracteres, p.ej. AES-128).
- Flujo principal:
    1. El cliente invoca `POST /message/cifrar` con cuerpo JSON `{ "msgDtoPlainText": <texto>, "msgDtoKey": <llave> }`.
    2. El sistema cifra el texto usando `AESCrypt` y persiste el resultado como `Message` (`mesCipherText`).
    3. El sistema responde 201 Created con el `Message` creado, encabezados `ETag` y `Location`, y el cuerpo con el id y el texto cifrado.
- Resultado esperado (happy path):
    - Código: 201 Created.
    - Headers: `ETag: "1"`, `Location: /message/1` (según el primer guardado en BD H2 del test).
    - Body (ejemplo):
      ```json
      {
        "mesId": 1,
        "mesCipherText": "vEYs5GMsbyQ34BtYrXwe0Jz26JdWDPKW0s0tvIOcxOU="
      }
      ```
- Posibles excepciones/escenarios alternos:
    - CAU-01.1: Llave con longitud inválida → 400 Bad Request con mensaje de validación.
    - CAU-01.2: Texto vacío o nulo → 400 Bad Request.
    - CAU-01.3: Error interno al cifrar o persistir → 500 Internal Server Error.

#### CU-02: Obtener y descifrar mensaje por id
- Actor: Cliente de la API.
- Objetivo: Entregar un id y una llave para recuperar el mensaje cifrado y obtener el texto en claro.
- Precondiciones:
    - Existe un `Message` persistido con el `id` entregado.
    - La llave usada para descifrar es la correcta (la misma con la que se cifró).
- Flujo principal:
    1. El cliente invoca `POST /message/decifrar` con cuerpo JSON `{ "msgDtoId": <id>, "msgDtoKey": <llave> }`.
    2. El sistema recupera el mensaje por id, descifra `mesCipherText` usando `AESCrypt` y retorna un `MessageDTO` con el texto en claro.
    3. El sistema responde 200 OK con encabezados `ETag` y `Location`, y el cuerpo con el texto plano.
- Resultado esperado (happy path):
    - Código: 200 OK.
    - Headers: `ETag: "1"`, `Location: /message/1` (conforme a los tests).
    - Body (ejemplo):
      ```json
      {
        "msgDtoPlainText": "mi texto sin cifrar"
      }
      ```
- Posibles excepciones/escenarios alternos:
    - CAU-02.1: `id` no encontrado → 404 Not Found.
    - CAU-02.2: Llave incorrecta (no permite descifrar) → 400 Bad Request o 401/403 según política, con mensaje de error.
    - CAU-02.3: `id` inválido o no provisto → 400 Bad Request.

---

### 4) Casos de prueba derivados del código
A continuación, se listan los casos de prueba existentes y propuestos, trazables a los casos de uso.

#### 4.1 Pruebas unitarias (Utilidad)
- TU-AES-01: Cifrado correcto
    - Origen: `AESCryptTest.testEncrypt()`
    - Dado: texto "mi texto sin cifrar" y llave "123456789asdfghj".
    - Cuando: se invoca `encrypt()`.
    - Entonces: retorna `"vEYs5GMsbyQ34BtYrXwe0Jz26JdWDPKW0s0tvIOcxOU="`.
    - Cobertura: CU-01 (lógica de cifrado).

- TU-AES-02: Descifrado correcto
    - Origen: `AESCryptTest.testDecrypt()`
    - Dado: texto cifrado `"vEYs5GMsbyQ34BtYrXwe0Jz26JdWDPKW0s0tvIOcxOU="` y llave "123456789asdfghj".
    - Cuando: se invoca `decrypt()`.
    - Entonces: retorna "mi texto sin cifrar".
    - Cobertura: CU-02 (lógica de descifrado).

- TU-AES-03: Llave inválida (propuesto)
    - Dado: llave con longitud distinta a 16/24/32.
    - Cuando: `encrypt()`/`decrypt()`.
    - Entonces: se lanza excepción específica o se traduce a error controlado.

#### 4.2 Pruebas de servicio
- TS-SVC-01: Guardar mensaje cifrado correctamente
    - Origen: `MessageServiceTest.testSuccessfulMessageSave()`
    - Dado: DTO con texto plano y llave válida.
    - Cuando: `messageService.save(dto)`.
    - Entonces: `mesCipherText` coincide con el esperado y `mesId = 1`.
    - Cobertura: CU-01.

- TS-SVC-02: Recuperar y descifrar por id
    - Origen: `MessageServiceTest.findById()`
    - Dado: `msgDtoId = 1`, llave válida.
    - Cuando: `messageService.findById(dto)`.
    - Entonces: retorna `msgDtoPlainText = "mi texto sin cifrar"` e id = 1.
    - Cobertura: CU-02.

- TS-SVC-03: Id no existente (propuesto)
    - Dado: `msgDtoId` que no existe.
    - Cuando: `findById()`.
    - Entonces: excepción de negocio traducible a 404.

- TS-SVC-04: Llave incorrecta (propuesto)
    - Dado: id válido pero llave distinta a la de cifrado.
    - Cuando: `findById()`.
    - Entonces: falla de descifrado → excepción controlada y mensaje de error.

#### 4.3 Pruebas de controlador (API)
- TC-API-01: POST /message/cifrar (201)
    - Origen: `MessageControllerTest.testAddNewMessage()`
    - Cuando: se envía JSON con texto y llave.
    - Entonces: 201, `Content-Type: application/json`, headers `ETag: "1"`, `Location: /message/1`, body con `mesId=1` y `mesCipherText` esperado.
    - Cobertura: CU-01.

- TC-API-02: POST /message/decifrar (200)
    - Origen: `MessageControllerTest.testGetMessageById()`
    - Cuando: se envía JSON con `msgDtoId=1` y llave.
    - Entonces: 200, `Content-Type: application/json`, headers `ETag: "1"`, `Location: /message/1`, body con `msgDtoPlainText = "mi texto sin cifrar"`.
    - Cobertura: CU-02.

- TC-API-03: Validación de entrada (propuesto)
    - `POST /message/cifrar` con texto vacío o llave de longitud inválida → 400.

- TC-API-04: Id no encontrado (propuesto)
    - `POST /message/decifrar` con `msgDtoId` inexistente → 404.

- TC-API-05: Llave incorrecta (propuesto)
    - `POST /message/decifrar` con llave errónea → 400/401/403 según política definida, con mensaje de error claro.

---

### 5) Matriz de trazabilidad (resumen)
- CU-01 ↔ TU-AES-01, TS-SVC-01, TC-API-01
- CU-02 ↔ TU-AES-02, TS-SVC-02, TC-API-02
- Escenarios alternos ↔ TU-AES-03, TS-SVC-03/04, TC-API-03/04/05 (propuestos)

---

### 6) Datos de prueba de referencia
- Texto plano: `"mi texto sin cifrar"`
- Llave válida: `"123456789asdfghj"` (16 chars)
- Texto cifrado esperado: `"vEYs5GMsbyQ34BtYrXwe0Jz26JdWDPKW0s0tvIOcxOU="`
- Id esperado en entorno limpio (H2 en tests): `1`

---


### 7) Anexo: Ejemplos de peticiones
- Cifrar y guardar:
  ```http
  POST /message/cifrar
  Content-Type: application/json

  { "msgDtoPlainText": "mi texto sin cifrar", "msgDtoKey": "123456789asdfghj" }
  ```

- Descifrar por id:
  ```http
  POST /message/decifrar
  Content-Type: application/json

  { "msgDtoId": 1, "msgDtoKey": "123456789asdfghj" }
  ```
