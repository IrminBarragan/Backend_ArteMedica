# Autenticación

La API usa JWT (JSON Web Token) sin sesiones de servidor (`stateless`). No hay usuario/contraseña fijos de prueba: la autenticación es real, contra la tabla `Usuario`.

## Login

```
POST /api/auth/login
```

No requiere autenticación previa (es el único endpoint público de la API).

**Request body — `LoginRequestDTO`:**

| Campo | Tipo | Requerido | Validación |
|---|---|---|---|
| `username` | string | Sí | No puede estar vacío |
| `password` | string | Sí | No puede estar vacío |

```json
{
  "username": "cmendoza",
  "password": "FarmaciaSegura2026"
}
```

**Response `200 OK` — `LoginResponseDTO`:**

| Campo | Tipo | Descripción |
|---|---|---|
| `token` | string | El JWT. Se manda en cada request subsecuente. |
| `tipo` | string | Siempre el literal `"Bearer"`. |
| `username` | string | El username del usuario autenticado. |
| `rol` | string (enum `Rol`) | `ADMIN`, `MEDICO` o `FARMACEUTICO`. |
| `empleadoId` | number | Id del `Empleado` ligado a este usuario (no el id del `Usuario`). |
| `expiresIn` | number | Milisegundos de vida del token desde que se emitió. |

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJjbWVuZG96YSIsInJvbCI6IkZBUk1BQ0VVVElDTyIsImVtcGxlYWRvSWQiOjMsInVzdWFyaW9JZCI6Miwi...",
  "tipo": "Bearer",
  "username": "cmendoza",
  "rol": "FARMACEUTICO",
  "empleadoId": 3,
  "expiresIn": 86400000
}
```

**Errores posibles:**
- `400` — el body no pasa las validaciones (`username`/`password` vacíos). Ver formato en [ERRORES.md](./ERRORES.md).
- `401` — el usuario no existe, está inactivo (`activo = false`), o la contraseña no coincide. El mensaje es genérico ("Usuario o contraseña invalidos") a propósito, para no revelar si el username existe o no.

## Cómo usar el token

En cada request a un endpoint protegido (todos excepto `POST /api/auth/login`), manda el token en el header `Authorization`:

```
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJjbWVuZG96YSIs...
```

No hay refresh token ni endpoint de logout — el cliente simplemente descarta el token y vuelve a hacer login cuando expira.

## Expiración del token

El tiempo de vida real, leído de `application.properties` (`app.jwt.expiration-ms`), es de **86 400 000 ms = 24 horas** por defecto. Es configurable con la variable de entorno `JWT_EXPIRATION_MS`; el valor real vigente siempre viene en el campo `expiresIn` de la respuesta de login, así que el frontend no debería asumir un número fijo — debe leerlo de ahí.

El secreto de firma (`app.jwt.secret` / variable de entorno `JWT_SECRET`) tiene un valor de desarrollo por defecto inseguro; en producción debe configurarse por variable de entorno.

## Qué pasa si el token falta, es inválido o expiró

Esto se verificó ejecutando la API real, y el comportamiento tiene un matiz importante:

- **Sin header `Authorization`, o con un token inválido/expirado**, en cualquier endpoint protegido: la petición **nunca llega al controller**. El filtro de seguridad (`JwtAuthenticationFilter`) intenta validar el token; si falla, simplemente no autentica y deja pasar la petición como anónima. Como el endpoint requiere autenticación, Spring Security la rechaza en la capa de filtros, **antes** de que `GlobalExceptionHandler` pueda intervenir. Resultado verificado: **`403 Forbidden` con el body vacío** (sin el JSON de `ErrorResponseDTO`).
- **Login con credenciales incorrectas** (`POST /api/auth/login`): este caso sí pasa por el controller y el `GlobalExceptionHandler`, así que responde `401` con el JSON normal de error (ver [ERRORES.md](./ERRORES.md)).
- El código también tiene un `@ExceptionHandler` para `JwtException`/`ExpiredJwtException` que devuelve `401` con JSON, pero en el flujo actual es efectivamente inalcanzable para peticiones normales (el filtro ya atrapa esas excepciones antes) — está ahí como red de seguridad por si algún otro flujo futuro deja escapar esa excepción.

En resumen: si el frontend recibe un `403` con body vacío en cualquier endpoint que no sea login, trátalo como "no autenticado, hay que volver a hacer login" — no esperes un JSON con `mensaje`.

## Roles del sistema

| Rol | Qué puede hacer, en términos generales |
|---|---|
| `ADMIN` | Control total: gestiona empleados, usuarios del sistema, catálogos (áreas, categorías, proveedores), y es el único que puede cambiar la configuración del sistema (ej. el toggle de FEFO/FIFO). También puede hacer todo lo que puede un `FARMACEUTICO`. |
| `MEDICO` | Crea solicitudes de medicamentos para sus pacientes/área. Solo puede ver sus propias solicitudes, nunca las de otro médico. No gestiona inventario, compras ni catálogos. |
| `FARMACEUTICO` | Aprueba, rechaza y dispensa las solicitudes de los médicos. Gestiona productos, proveedores, códigos equivalentes y registra compras (que generan lotes automáticamente). Puede consultar (pero no cambiar) la configuración del sistema. |

El detalle exacto de qué rol requiere cada endpoint está en [API.md](./API.md).
