# API de Fondos - Documentación de Endpoints

## Módulo de Gestión de Fondos
Este módulo maneja las cuentas, saldos y transferencias entre cuentas en NaivePay.

---

## 📋 AccountController
**Base URL**: `/api/funds/accounts`

### 1. Consultar Saldo
- **Método**: `GET`
- **Ruta**: `/api/funds/accounts/balance/{userId}`
- **Descripción**: Obtiene el saldo disponible de la cuenta de un usuario
- **Parámetros**:
  - `userId` (path) - ID del usuario
- **Respuesta**: `AccountBalanceResponse`
  ```json
  {
    "accountId": 1,
    "userId": 123,
    "availableBalance": 10000.00,
    "lastUpdate": "2025-10-04T03:00:00"
  }
  ```

### 2. Crear Cuenta
- **Método**: `POST`
- **Ruta**: `/api/funds/accounts/create/{userId}`
- **Descripción**: Crea una nueva cuenta para un usuario (llamado por módulo de Registro)
- **Parámetros**:
  - `userId` (path) - ID del usuario
- **Respuesta**: `AccountBalanceResponse` (HTTP 201)

### 3. Verificar Existencia de Cuenta
- **Método**: `GET`
- **Ruta**: `/api/funds/accounts/exists/{userId}`
- **Descripción**: Verifica si existe una cuenta para el usuario
- **Parámetros**:
  - `userId` (path) - ID del usuario
- **Respuesta**: `boolean`

---

## 💸 TransactionController
**Base URL**: `/api/funds/transactions`

### 1. Realizar Transferencia
- **Método**: `POST`
- **Ruta**: `/api/funds/transactions/transfer`
- **Descripción**: Ejecuta una transferencia entre dos cuentas (usado por módulo de Pagos)
- **Body**: `TransferRequest`
  ```json
  {
    "originAccountId": 1,
    "destinationAccountId": 2,
    "amount": 1500.00,
    "description": "Pago en comercio X"
  }
  ```
- **Respuesta**: `TransferResponse`
  ```json
  {
    "success": true,
    "message": "Transferencia realizada exitosamente",
    "transactionId": 123
  }
  ```

### 2. Cargar Saldo
- **Método**: `POST`
- **Ruta**: `/api/funds/transactions/add-funds/{userId}`
- **Descripción**: Carga saldo a la cuenta de un usuario (desde cuenta sistema)
- **Parámetros**:
  - `userId` (path) - ID del usuario
  - `amount` (query) - Monto a cargar
- **Respuesta**: `TransferResponse`

### 3. Historial de Transacciones
- **Método**: `GET`
- **Ruta**: `/api/funds/transactions/history/{userId}`
- **Descripción**: Obtiene el historial de transacciones de un usuario
- **Parámetros**:
  - `userId` (path) - ID del usuario
- **Respuesta**: `List<TransactionResponse>`
  ```json
  [
    {
      "id": 1,
      "amount": 5000.00,
      "dateTime": "2025-10-02T10:00:00",
      "description": "Carga inicial",
      "originAccountId": 0,
      "destinationAccountId": 1
    }
  ]
  ```

### 4. Detalle de Transacción
- **Método**: `GET`
- **Ruta**: `/api/funds/transactions/{transactionId}`
- **Descripción**: Obtiene el detalle de una transacción específica
- **Parámetros**:
  - `transactionId` (path) - ID de la transacción
- **Respuesta**: `TransactionResponse`

### 5. Validar Saldo
- **Método**: `GET`
- **Ruta**: `/api/funds/transactions/validate-balance/{accountId}`
- **Descripción**: Valida si una cuenta tiene saldo suficiente (usado antes de crear solicitudes de pago)
- **Parámetros**:
  - `accountId` (path) - ID de la cuenta
  - `amount` (query) - Monto a validar
- **Respuesta**: `boolean`

---

## 🔗 Integración con Otros Módulos

### Módulo de Registro
- Debe llamar a `POST /api/funds/accounts/create/{userId}` cuando un usuario se registra

### Módulo de Pagos
- Debe llamar a `POST /api/funds/transactions/transfer` cuando se aprueba un pago
- Puede llamar a `GET /api/funds/transactions/validate-balance/{accountId}` antes de crear una solicitud de pago

### Módulo de Reportes
- Puede llamar a `GET /api/funds/transactions/history/{userId}` para generar reportes de gastos

---

## ⚠️ Notas Importantes

1. **Cuenta Sistema**: Las cargas de saldo se realizan desde la cuenta con ID = 0 (cuenta sistema)
2. **Validaciones**: 
   - El monto debe ser mayor a cero
   - La cuenta origen debe tener saldo suficiente
3. **Transacciones**: Solo se guardan transacciones exitosas
4. **Estados**: No manejamos estados de transacciones (pendiente/aprobada/rechazada) - eso lo hace el módulo de Pagos

---

## ✅ Estado Actual
**API Completamente Funcional**: Todos los endpoints están implementados, probados y funcionando correctamente.

---

## 🧪 Ejemplos de Uso (Testing Real)

### Escenario Completo: Cargar fondos y realizar transferencia

```bash
# 1. Consultar saldo inicial del usuario 1
GET http://localhost:8080/api/funds/accounts/balance/1
Response: {
  "accountId": 2,
  "userId": 1,
  "availableBalance": 0.00,
  "lastUpdate": "2025-10-04T12:00:00"
}

# 2. Cargar $10,000 al usuario 1
POST http://localhost:8080/api/funds/transactions/add-funds/1?amount=10000
Response: {
  "success": true,
  "message": "Transfer completed successfully",
  "transactionId": 1
}

# 3. Consultar nuevo saldo
GET http://localhost:8080/api/funds/accounts/balance/1
Response: {
  "accountId": 2,
  "userId": 1,
  "availableBalance": 10000.00,
  "lastUpdate": "2025-10-04T12:00:52"
}

# 4. Transferir $1,500 del usuario 1 al usuario 2
POST http://localhost:8080/api/funds/transactions/transfer
Content-Type: application/json
{
  "originAccountId": 2,
  "destinationAccountId": 3,
  "amount": 1500.00,
  "description": "Pago a usuario 2"
}
Response: {
  "success": true,
  "message": "Transfer completed successfully",
  "transactionId": 3
}

# 5. Ver historial de transacciones del usuario 1
GET http://localhost:8080/api/funds/transactions/history/1
Response: [
  {
    "id": 3,
    "amount": 1500.00,
    "dateTime": "2025-10-04T12:05:57",
    "description": "Pago a usuario 2",
    "originAccountId": 2,
    "destinationAccountId": 3
  },
  {
    "id": 1,
    "amount": 10000.00,
    "dateTime": "2025-10-04T12:00:52",
    "description": "Balance load",
    "originAccountId": 1,
    "destinationAccountId": 2
  }
]

# 6. Validar saldo antes de un pago
GET http://localhost:8080/api/funds/transactions/validate-balance/2?amount=1000
Response: true

# 7. Intentar transferencia con saldo insuficiente
POST http://localhost:8080/api/funds/transactions/transfer
{
  "originAccountId": 2,
  "destinationAccountId": 3,
  "amount": 50000.00,
  "description": "Monto muy alto"
}
Response: {
  "success": false,
  "message": "Insufficient balance",
  "transactionId": null
}
```

---

## 💡 Casos de Uso para UI

### Caso de Uso 1: Dashboard de Usuario
**Pantalla que muestra:**
- Saldo disponible en tiempo real (polling cada 5 segundos o WebSocket)
- Historial de transacciones (últimas 10)
- Botón para cargar fondos

**Endpoints necesarios:**
- `GET /api/funds/accounts/balance/{userId}` - Para mostrar saldo
- `GET /api/funds/transactions/history/{userId}` - Para listar transacciones

### Caso de Uso 2: Cargar Fondos
**Formulario con:**
- Input de monto (validación: mayor a 0)
- Botón "Cargar Fondos"
- Mensaje de confirmación

**Endpoint:**
- `POST /api/funds/transactions/add-funds/{userId}?amount={amount}`

### Caso de Uso 3: Realizar Transferencia
**Formulario con:**
- Select o input de cuenta destino
- Input de monto
- Input de descripción
- Validación previa de saldo

**Endpoints:**
- `GET /api/funds/transactions/validate-balance/{accountId}?amount={amount}` - Validar antes
- `POST /api/funds/transactions/transfer` - Ejecutar transferencia

---

## 🎨 Estructura de Respuestas para UI

### AccountBalanceResponse
```typescript
interface AccountBalanceResponse {
  accountId: number;
  userId: number;
  availableBalance: number;
  lastUpdate: string; // ISO 8601 datetime
}
```

### TransactionResponse
```typescript
interface TransactionResponse {
  id: number;
  amount: number;
  dateTime: string; // ISO 8601 datetime
  description: string;
  originAccountId: number;
  destinationAccountId: number;
}
```

### TransferResponse
```typescript
interface TransferResponse {
  success: boolean;
  message: string;
  transactionId: number | null;
}
```

---

## 🔄 Flujo Recomendado para UI

1. **Al cargar la página:**
   - Obtener `userId` del usuario autenticado
   - Consultar saldo: `GET /accounts/balance/{userId}`
   - Consultar historial: `GET /transactions/history/{userId}`

2. **Al cargar fondos:**
   - Usuario ingresa monto
   - Validar monto > 0 en cliente
   - Llamar `POST /transactions/add-funds/{userId}?amount={amount}`
   - Si `success === true`, actualizar saldo y agregar transacción al historial
   - Mostrar notificación de éxito

3. **Actualización de saldo:**
   - Implementar polling cada 5-10 segundos
   - O usar Server-Sent Events / WebSocket (futuro)
