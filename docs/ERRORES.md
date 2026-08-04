# Errores

## Forma del JSON de error

Todos los errores que pasan por el manejador central de la API (`GlobalExceptionHandler`) tienen esta forma exacta (`ErrorResponseDTO`):

```json
{
  "mensaje": "string",
  "status": 404,
  "timestamp": "2026-08-03T21:55:25.739164",
  "errores": null
}
```

| Campo | Tipo | Descripción |
|---|---|---|
| `mensaje` | string | Descripción legible del error. En errores de validación es un texto genérico ("Error de validacion"); el detalle campo por campo va en `errores`. |
| `status` | number | El mismo código HTTP de la respuesta, repetido en el body. |
| `timestamp` | string (fecha-hora ISO) | Momento en que se generó el error, en el servidor. |
| `errores` | mapa `campo → mensaje`, o `null` | **Solo viene poblado en errores de validación** (`400`). En cualquier otro tipo de error viene `null`. |

**Importante:** no todos los errores de la API traen este JSON — ver la nota sobre `403` más abajo.

## Tabla de códigos de status

| Status | Cuándo aparece | Origen |
|---|---|---|
| `400` | El body de la petición no pasa las validaciones de Bean Validation (`@NotBlank`, `@Positive`, `@Future`, etc.) | `MethodArgumentNotValidException` — este es el único caso donde `errores` viene poblado |
| `401` | Login (`POST /api/auth/login`) con usuario inexistente/inactivo o contraseña incorrecta | `AutenticacionException` |
| `401` | (defensivo, no debería ocurrir en uso normal de la API) un error de JWT logra escapar fuera del filtro de autenticación | `JwtException` / `ExpiredJwtException` |
| `403` | **Falta el header `Authorization`, o el token es inválido/expiró**, en cualquier endpoint protegido | Rechazado por Spring Security en la capa de filtros, **antes** de llegar al controller — la respuesta viene **con el body vacío**, no con el JSON de arriba. Verificado empíricamente contra la API real. |
| `403` | El usuario autenticado sí tiene un token válido, pero su rol no cumple el `@PreAuthorize` del endpoint | `AccessDeniedException` — este caso **sí** llega al controller y **sí** trae el JSON completo |
| `404` | El recurso pedido (empleado, producto, solicitud, clave de configuración, etc.) no existe | `ResourceNotFoundException` |
| `409` | Se intentó aprobar una solicitud sin stock vigente suficiente | `StockInsuficienteException` |
| `409` | Se intentó una transición de estatus inválida (ej. aprobar una solicitud que no está `PENDIENTE`, dispensar una que no está `APROBADO`/`ENTREGADA_PARCIAL`) | `EstadoInvalidoException` |
| `409` | Conflicto de concurrencia: otro proceso modificó el mismo registro al mismo tiempo (bloqueo optimista en `Producto`/`Solicitud`, o pesimista en `Lote`/`Solicitud` durante aprobación/dispensación) | `ConflictoConcurrenciaException` |
| `500` | Cualquier error no controlado explícitamente. El mensaje es siempre genérico ("Ocurrió un error inesperado") — la API **nunca** expone el stacktrace ni el mensaje interno real de la excepción original | `Exception` (catch-all) |

### La particularidad del 403

Hay dos causas distintas para un `403`, y se distinguen por si traen JSON o no:

1. **No autenticado** (sin token, token inválido, o token expirado): la petición nunca llega al controller, Spring Security la corta en el filtro. Respuesta verificada contra la API real: `403 Forbidden`, `Content-Length: 0` (body vacío).
2. **Autenticado pero sin permiso** (`@PreAuthorize` rechaza el rol): sí llega al controller, `GlobalExceptionHandler` la atrapa y responde con el JSON normal, `mensaje: "No tienes permisos para realizar esta accion"`.

Si el frontend recibe un `403` sin body parseable como JSON, debe interpretarlo como "sesión inválida, hay que volver a hacer login" en vez de "no tienes permiso" — son casos distintos que requieren manejo distinto en la UI.

## Ejemplos reales

**Error de validación** (`400`, probado contra la API real con `POST /api/auth/login` y body `{"username":"","password":""}`):
```json
{
  "mensaje": "Error de validacion",
  "status": 400,
  "timestamp": "2026-08-03T21:55:25.845910",
  "errores": {
    "username": "no debe estar vacío",
    "password": "no debe estar vacío"
  }
}
```

**Error simple** (`401`, probado contra la API real con `POST /api/auth/login` y credenciales incorrectas):
```json
{
  "mensaje": "Usuario o contraseña invalidos",
  "status": 401,
  "timestamp": "2026-08-03T21:55:25.739164",
  "errores": null
}
```

**Ejemplo de `404`** (no probado en vivo, pero construido siguiendo exactamente el mismo código de `GlobalExceptionHandler`):
```json
{
  "mensaje": "Producto no encontrado: 999",
  "status": 404,
  "timestamp": "2026-08-03T21:58:00.123456",
  "errores": null
}
```
