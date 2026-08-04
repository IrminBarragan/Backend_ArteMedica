# API — Catálogo de endpoints

Todos los endpoints cuelgan de `/api`. Todos requieren el header `Authorization: Bearer <token>` (ver [AUTENTICACION.md](./AUTENTICACION.md)) **excepto** `POST /api/auth/login`.

Convenciones de respuesta usadas en todo el proyecto:
- `201 Created` al crear un recurso (con header `Location` apuntando al recurso nuevo).
- `200 OK` para lecturas y actualizaciones.
- `204 No Content` para bajas lógicas (`DELETE`, que en realidad hace `activo = false`, nunca borra el registro).
- El detalle de todos los códigos de error está en [ERRORES.md](./ERRORES.md); aquí solo se listan los que aplican a cada endpoint en particular.

---

## Auth

### POST /api/auth/login
Rol requerido: ninguno (público)
Descripción: autentica contra la tabla `Usuario` y devuelve un JWT.

Request body: `LoginRequestDTO`
Response (200): `LoginResponseDTO`

Ejemplo de request:
```json
{ "username": "cmendoza", "password": "FarmaciaSegura2026" }
```

Ejemplo de response:
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJjbWVuZG96YSJ9...",
  "tipo": "Bearer",
  "username": "cmendoza",
  "rol": "FARMACEUTICO",
  "empleadoId": 3,
  "expiresIn": 86400000
}
```

Posibles errores:
- 400 si el body no pasa las validaciones
- 401 si el usuario no existe, está inactivo, o la contraseña no coincide

---

## Empleados

### POST /api/empleados
Rol requerido: ADMIN
Descripción: crea un empleado del hospital (médico, farmacéutico o administrativo — no implica que tenga acceso al sistema, eso es `Usuario`).

Request body: `EmpleadoRequestDTO`
Response (201): `EmpleadoResponseDTO`

Ejemplo de request:
```json
{
  "nombres": "Sofía",
  "apellidoPaterno": "Ramírez",
  "apellidoMaterno": "Torres",
  "tipo": "MEDICO",
  "especialidad": "Pediatría",
  "cedulaProfesional": "9087654",
  "telefonoGuardia": "477-123-4567"
}
```

Ejemplo de response:
```json
{
  "id": 1,
  "nombres": "Sofía",
  "apellidoPaterno": "Ramírez",
  "apellidoMaterno": "Torres",
  "tipo": "MEDICO",
  "especialidad": "Pediatría",
  "cedulaProfesional": "9087654",
  "telefonoGuardia": "477-123-4567",
  "activo": true
}
```

Posibles errores:
- 400 si el body no pasa las validaciones
- 403 si el usuario autenticado no es ADMIN

### PUT /api/empleados/{id}
Rol requerido: ADMIN
Descripción: actualiza los datos de un empleado existente.

Request body: `EmpleadoRequestDTO`
Response (200): `EmpleadoResponseDTO`

Posibles errores:
- 400 validación, 403 no ADMIN, 404 si el empleado no existe

### GET /api/empleados/{id}
Rol requerido: cualquier autenticado
Descripción: obtiene un empleado por id.

Response (200): `EmpleadoResponseDTO`

Posibles errores:
- 404 si no existe

### GET /api/empleados
Rol requerido: cualquier autenticado
Descripción: lista los empleados activos.

Response (200): `EmpleadoResponseDTO[]`

### DELETE /api/empleados/{id}
Rol requerido: ADMIN
Descripción: baja lógica (`activo = false`).

Response (204): sin body

Posibles errores:
- 403 no ADMIN, 404 si no existe

---

## Usuarios

Todos los endpoints de este Controller requieren rol ADMIN (nadie más puede crear cuentas de acceso).

### POST /api/usuarios
Rol requerido: ADMIN
Descripción: crea una cuenta de acceso (`Usuario`) ligada a un `Empleado` existente. La contraseña se guarda hasheada (BCrypt), nunca en texto plano.

Request body: `UsuarioRequestDTO`
Response (201): `UsuarioResponseDTO`

Ejemplo de request:
```json
{
  "username": "sramirez",
  "password": "ClaveSegura123",
  "empleadoId": 1,
  "rol": "MEDICO"
}
```

Ejemplo de response:
```json
{
  "id": 5,
  "username": "sramirez",
  "empleadoId": 1,
  "empleadoNombre": "Sofía Ramírez",
  "rol": "MEDICO",
  "activo": true,
  "createdAt": "2026-08-03T09:15:00",
  "updatedAt": "2026-08-03T09:15:00"
}
```

Posibles errores:
- 400 validación (incluye `password` con menos de 8 caracteres)
- 403 si no es ADMIN
- 404 si `empleadoId` no existe

### GET /api/usuarios
Rol requerido: ADMIN
Descripción: lista usuarios activos.

Response (200): `UsuarioResponseDTO[]`

Posibles errores:
- 403 si no es ADMIN

### DELETE /api/usuarios/{id}
Rol requerido: ADMIN
Descripción: baja lógica de la cuenta de acceso (el `Empleado` no se toca).

Response (204): sin body

Posibles errores:
- 403 no ADMIN, 404 si no existe

---

## Áreas

### POST /api/areas
Rol requerido: ADMIN
Descripción: crea un área del hospital (ej. un servicio o departamento) que puede recibir solicitudes de medicamentos.

Request body: `AreaRequestDTO`
Response (201): `AreaResponseDTO`

Ejemplo de request:
```json
{ "nombre": "Urgencias Pediátricas", "descripcion": "Servicio de urgencias del área pediátrica" }
```

Ejemplo de response:
```json
{ "id": 1, "nombre": "Urgencias Pediátricas", "descripcion": "Servicio de urgencias del área pediátrica", "activo": true }
```

Posibles errores:
- 400 validación, 403 no ADMIN

### PUT /api/areas/{id}
Rol requerido: ADMIN
Request body: `AreaRequestDTO`
Response (200): `AreaResponseDTO`

Posibles errores:
- 400, 403, 404 si no existe

### GET /api/areas/{id}
Rol requerido: cualquier autenticado
Response (200): `AreaResponseDTO`

Posibles errores:
- 404 si no existe

### GET /api/areas
Rol requerido: cualquier autenticado
Descripción: lista áreas activas.
Response (200): `AreaResponseDTO[]`

### DELETE /api/areas/{id}
Rol requerido: ADMIN
Response (204): sin body

Posibles errores:
- 403 no ADMIN, 404 si no existe

---

## Categorías

Mismo patrón que Áreas: escritura solo ADMIN, lectura abierta a cualquier autenticado.

### POST /api/categorias
Rol requerido: ADMIN
Request body: `CategoriaMedicamentoRequestDTO`
Response (201): `CategoriaMedicamentoResponseDTO`

Ejemplo de request:
```json
{ "nombre": "Analgésicos", "descripcion": "Medicamentos para el control del dolor" }
```

Posibles errores:
- 400, 403

### PUT /api/categorias/{id}
Rol requerido: ADMIN
Request body: `CategoriaMedicamentoRequestDTO`
Response (200): `CategoriaMedicamentoResponseDTO`

Posibles errores:
- 400, 403, 404

### GET /api/categorias/{id}
Rol requerido: cualquier autenticado
Response (200): `CategoriaMedicamentoResponseDTO`

Posibles errores:
- 404

### GET /api/categorias
Rol requerido: cualquier autenticado
Response (200): `CategoriaMedicamentoResponseDTO[]`

### DELETE /api/categorias/{id}
Rol requerido: ADMIN
Response (204): sin body

Posibles errores:
- 403, 404

---

## Proveedores

Escritura: ADMIN o FARMACEUTICO. Lectura: cualquier autenticado.

### POST /api/proveedores
Rol requerido: ADMIN o FARMACEUTICO
Request body: `ProveedorRequestDTO`
Response (201): `ProveedorResponseDTO`

Ejemplo de request:
```json
{
  "nombre": "Distribuidora Farmacéutica del Bajío S.A. de C.V.",
  "direccion": "Blvd. Adolfo López Mateos 1450, León, Gto.",
  "telefono": "477-800-1122",
  "correo": "ventas@dfbajio.com.mx"
}
```

Posibles errores:
- 400 (incluye `correo` con formato inválido), 403

### PUT /api/proveedores/{id}
Rol requerido: ADMIN o FARMACEUTICO
Request body: `ProveedorRequestDTO`
Response (200): `ProveedorResponseDTO`

Posibles errores:
- 400, 403, 404

### GET /api/proveedores/{id}
Rol requerido: cualquier autenticado
Response (200): `ProveedorResponseDTO`

Posibles errores:
- 404

### GET /api/proveedores
Rol requerido: cualquier autenticado
Response (200): `ProveedorResponseDTO[]`

### DELETE /api/proveedores/{id}
Rol requerido: ADMIN o FARMACEUTICO
Response (204): sin body

Posibles errores:
- 403, 404

---

## Productos

### POST /api/productos
Rol requerido: ADMIN o FARMACEUTICO
Descripción: da de alta un medicamento en el catálogo. `stockActual` siempre arranca en 0 (no se puede fijar desde el request).

Request body: `ProductoRequestDTO`
Response (201): `ProductoResponseDTO`

Ejemplo de request:
```json
{
  "nombre": "Paracetamol",
  "presentacion": "Tabletas 500mg caja c/20",
  "codigoBarras": "7501234567890",
  "esControlado": false,
  "categoriaId": 1,
  "precioVenta": 45.50,
  "precioCompra": 28.00,
  "stockMinimo": 50
}
```

Ejemplo de response:
```json
{
  "id": 12,
  "nombre": "Paracetamol",
  "presentacion": "Tabletas 500mg caja c/20",
  "codigoBarras": "7501234567890",
  "esControlado": false,
  "categoriaId": 1,
  "categoriaNombre": "Analgésicos",
  "precioVenta": 45.50,
  "precioCompra": 28.00,
  "stockMinimo": 50,
  "stockActual": 0,
  "activo": true
}
```

Posibles errores:
- 400 validación, 403, 404 si `categoriaId` no existe

### PUT /api/productos/{id}
Rol requerido: ADMIN o FARMACEUTICO
Descripción: actualiza los datos del producto. **No permite tocar `stockActual`** (ni siquiera está en el request DTO).

Request body: `ProductoRequestDTO`
Response (200): `ProductoResponseDTO`

Posibles errores:
- 400, 403, 404 si el producto o la categoría no existen

### GET /api/productos/{id}
Rol requerido: cualquier autenticado
Response (200): `ProductoResponseDTO`

Posibles errores:
- 404

### GET /api/productos
Rol requerido: cualquier autenticado (incluye MEDICO, que lo necesita para armar sus solicitudes)
Descripción: lista productos activos.
Response (200): `ProductoResponseDTO[]`

### GET /api/productos/stock-bajo
Rol requerido: cualquier autenticado
Descripción: lista los productos activos donde `stockActual <= stockMinimo`.
Response (200): `ProductoResponseDTO[]`

### DELETE /api/productos/{id}
Rol requerido: ADMIN o FARMACEUTICO
Response (204): sin body

Posibles errores:
- 403, 404

---

## Códigos equivalentes

Todos los endpoints de este Controller requieren ADMIN o FARMACEUTICO.

### POST /api/codigos-equivalentes
Rol requerido: ADMIN o FARMACEUTICO
Descripción: registra un código de barras alterno para un producto (ej. el mismo medicamento con otro código del fabricante). `createdBy` se toma del `username` del token, no se manda en el body.

Request body: `CodigoEquivalenteRequestDTO`
Response (201): `CodigoEquivalenteResponseDTO`

Ejemplo de request:
```json
{ "productoId": 12, "codigoBarras": "7501234567906" }
```

Ejemplo de response:
```json
{
  "id": 7,
  "productoId": 12,
  "productoNombre": "Paracetamol",
  "codigoBarras": "7501234567906",
  "activo": true,
  "createdAt": "2026-08-03T10:02:11",
  "createdBy": "cmendoza"
}
```

Posibles errores:
- 400, 403, 404 si `productoId` no existe

### GET /api/codigos-equivalentes/producto/{productoId}
Rol requerido: ADMIN o FARMACEUTICO
Descripción: lista los códigos equivalentes de un producto.
Response (200): `CodigoEquivalenteResponseDTO[]`

### DELETE /api/codigos-equivalentes/{id}
Rol requerido: ADMIN o FARMACEUTICO
Response (204): sin body

Posibles errores:
- 403, 404

---

## Lotes

Este Controller es **solo lectura** — no hay `POST`/`PUT`/`DELETE`. Los lotes solo se generan implícitamente al registrar una compra (ver sección Compras). Todos los endpoints están abiertos a cualquier autenticado.

### GET /api/lotes/producto/{productoId}
Descripción: lista los lotes activos de un producto (todos, sin filtrar por existencia ni vigencia).
Response (200): `LoteResponseDTO[]`

```json
[
  {
    "id": 101,
    "numeroLote": "LOT-2026-0817",
    "productoId": 12,
    "productoNombre": "Paracetamol",
    "proveedorId": 1,
    "proveedorNombre": "Distribuidora Farmacéutica del Bajío S.A. de C.V.",
    "fechaCaducidad": "2027-06-30",
    "costoCompra": 28.00,
    "cantidadInicial": 200,
    "existenciaActual": 173,
    "activo": true
  }
]
```

### GET /api/lotes/vencidos
Descripción: lista lotes activos con `existenciaActual > 0` cuya `fechaCaducidad` ya pasó.
Response (200): `LoteResponseDTO[]`

### GET /api/lotes/por-vencer?fechaLimite=2026-09-30
Descripción: lista lotes activos con `existenciaActual > 0` cuya `fechaCaducidad` está entre hoy y `fechaLimite`.
Query param: `fechaLimite` (fecha `YYYY-MM-DD`, **requerido**, sin valor por defecto).
Response (200): `LoteResponseDTO[]`

Posibles errores:
- 400 si falta `fechaLimite` o no tiene formato de fecha válido. Nota: este caso particular (parámetro de query faltante/mal formado) no tiene un `@ExceptionHandler` dedicado en `GlobalExceptionHandler` — no se verificó en pruebas reales si efectivamente cae en 400 o en el manejador genérico (500); si el frontend depende de esto, confírmalo contra el ambiente real antes de asumirlo.

---

## Compras

Todos los endpoints de este Controller requieren ADMIN o FARMACEUTICO.

### POST /api/compras
Rol requerido: ADMIN o FARMACEUTICO
Descripción: registra una compra completa. Por cada línea del body crea un `Lote` nuevo, suma `cantidad` al `stockActual` del producto correspondiente, y registra un movimiento de kardex de tipo `ENTRADA`. `usuarioId` se toma del token (`principal.getUsuarioId()`), no se manda en el body.

Request body: `CompraRequestDTO`
Response (201): `CompraResponseDTO`

Ejemplo de request:
```json
{
  "proveedorId": 1,
  "numeroFactura": "FAC-A-00458",
  "fechaCompra": "2026-08-01",
  "detalles": [
    {
      "productoId": 12,
      "numeroLote": "LOT-2026-0817",
      "fechaCaducidad": "2027-06-30",
      "cantidad": 200,
      "costoUnitario": 28.00
    },
    {
      "productoId": 45,
      "numeroLote": "LOT-2026-0818",
      "fechaCaducidad": "2027-03-15",
      "cantidad": 100,
      "costoUnitario": 65.50
    }
  ]
}
```

Ejemplo de response:
```json
{
  "id": 30,
  "proveedorId": 1,
  "proveedorNombre": "Distribuidora Farmacéutica del Bajío S.A. de C.V.",
  "numeroFactura": "FAC-A-00458",
  "fechaCompra": "2026-08-01",
  "usuarioRegistroUsername": "cmendoza",
  "total": 12150.00,
  "detalles": [
    {
      "productoId": 12,
      "productoNombre": "Paracetamol",
      "cantidad": 200,
      "costoUnitario": 28.00,
      "subtotal": 5600.00,
      "loteId": 101,
      "numeroLote": "LOT-2026-0817"
    },
    {
      "productoId": 45,
      "productoNombre": "Amoxicilina",
      "cantidad": 100,
      "costoUnitario": 65.50,
      "subtotal": 6550.00,
      "loteId": 102,
      "numeroLote": "LOT-2026-0818"
    }
  ],
  "createdAt": "2026-08-01T11:20:45"
}
```

Posibles errores:
- 400 si el body no pasa las validaciones (ej. `fechaCaducidad` no es futura, `cantidad`/`costoUnitario` no son positivos, `detalles` vacío)
- 403 si no es ADMIN/FARMACEUTICO
- 404 si `proveedorId` o algún `productoId` de las líneas no existen
- 409 si no se pudo actualizar el stock del producto por conflicto de concurrencia (bloqueo optimista agotó reintentos)

### GET /api/compras/{id}
Rol requerido: ADMIN o FARMACEUTICO
Response (200): `CompraResponseDTO`

Posibles errores:
- 404 si no existe

### GET /api/compras
Rol requerido: ADMIN o FARMACEUTICO
Descripción: lista compras, con filtros opcionales por query param.
Query params (todos opcionales): `proveedorId` (número), `desde` y `hasta` (fecha `YYYY-MM-DD`, deben mandarse juntos). Si no se manda ningún filtro, devuelve todas. Si se manda `proveedorId`, ese filtro tiene prioridad sobre `desde`/`hasta`.
Response (200): `CompraResponseDTO[]`

---

## Solicitudes

### POST /api/solicitudes
Rol requerido: MEDICO
Descripción: crea una nueva solicitud de medicamentos con estatus inicial `PENDIENTE`. El `medicoId` **no se manda en el body**, se toma del token (`principal.getEmpleadoId()`) — un médico solo puede crear solicitudes a su propio nombre.

Request body: `SolicitudRequestDTO`
Response (201): `SolicitudResponseDTO`

Ejemplo de request:
```json
{
  "areaId": 1,
  "detalles": [
    { "productoId": 12, "cantidadSolicitada": 40 },
    { "productoId": 45, "cantidadSolicitada": 15 }
  ]
}
```

Ejemplo de response:
```json
{
  "id": 58,
  "medicoId": 1,
  "medicoNombre": "Sofía Ramírez",
  "areaId": 1,
  "areaNombre": "Urgencias Pediátricas",
  "fechaSolicitud": "2026-08-03T08:30:00",
  "estatus": "PENDIENTE",
  "farmaceuticoNombre": null,
  "fechaAprobacion": null,
  "fechaEntrega": null,
  "motivoRechazo": null,
  "detalles": [
    { "id": 120, "productoId": 12, "productoNombre": "Paracetamol", "presentacion": "Tabletas 500mg caja c/20", "cantidadSolicitada": 40, "cantidadAutorizada": null, "cantidadEntregada": 0, "loteId": null, "numeroLote": null },
    { "id": 121, "productoId": 45, "productoNombre": "Amoxicilina", "presentacion": "Cápsulas 500mg caja c/12", "cantidadSolicitada": 15, "cantidadAutorizada": null, "cantidadEntregada": 0, "loteId": null, "numeroLote": null }
  ],
  "createdAt": "2026-08-03T08:30:00"
}
```

Posibles errores:
- 400 si el body no pasa las validaciones
- 403 si el usuario autenticado no es MEDICO
- 404 si `areaId` o algún `productoId` de las líneas no existen

### PUT /api/solicitudes/{id}/aprobar
Rol requerido: FARMACEUTICO o ADMIN
Descripción: aprueba una solicitud `PENDIENTE`. Valida que haya stock vigente suficiente para cada cantidad autorizada (sumando `existenciaActual` de los lotes no vencidos del producto) antes de autorizar. `farmaceuticoId` se toma del token.

Request body: `AprobarSolicitudRequestDTO`
Response (200): `SolicitudResponseDTO` (con `estatus = "APROBADO"`, `cantidadAutorizada` lleno en cada detalle, `farmaceuticoNombre` y `fechaAprobacion` llenos)

Ejemplo de request:
```json
{ "cantidadesAutorizadasPorProducto": { "12": 40, "45": 15 } }
```

Posibles errores:
- 400 validación
- 403 si no es FARMACEUTICO/ADMIN
- 404 si la solicitud o el farmacéutico no existen
- 409 si la solicitud no está en estatus `PENDIENTE`
- 409 si no hay stock vigente suficiente para alguna de las cantidades autorizadas (el mensaje incluye el producto y la cantidad disponible)
- 409 si otro proceso tiene la solicitud bloqueada en ese momento (conflicto de concurrencia)

### PUT /api/solicitudes/{id}/rechazar
Rol requerido: FARMACEUTICO o ADMIN
Descripción: rechaza una solicitud `PENDIENTE`. `farmaceuticoId` se toma del token.

Request body: `RechazarSolicitudRequestDTO`
Response (200): `SolicitudResponseDTO` (con `estatus = "RECHAZADO"`, `motivoRechazo` lleno)

Ejemplo de request:
```json
{ "motivo": "No hay existencia del medicamento y no se espera reabasto esta semana" }
```

Posibles errores:
- 400, 403, 404, 409 si la solicitud no está en estatus `PENDIENTE`, 409 por conflicto de concurrencia

### PUT /api/solicitudes/{id}/dispensar
Rol requerido: FARMACEUTICO o ADMIN
Descripción: entrega físicamente los medicamentos autorizados, descontando de los lotes según la estrategia FEFO/FIFO configurada (ver [REGLAS_DE_NEGOCIO.md](./REGLAS_DE_NEGOCIO.md)), registrando kardex y descontando `stockActual` del producto. Sin body. `farmaceuticoId` se toma del token. Puede llamarse más de una vez sobre la misma solicitud si quedó `ENTREGADA_PARCIAL` (por ejemplo, después de una compra que reabastece el producto).

Response (200): `SolicitudResponseDTO` (`estatus` pasa a `ENTREGADA_PARCIAL` o `ENTREGADA_COMPLETA` según si alcanzó a cubrir todas las líneas)

Posibles errores:
- 403 si no es FARMACEUTICO/ADMIN
- 404 si la solicitud, el farmacéutico o su usuario asociado no existen
- 409 si la solicitud no está en estatus `APROBADO` ni `ENTREGADA_PARCIAL`
- 409 si otro proceso tiene bloqueado el mismo lote o la misma solicitud en ese momento

### GET /api/solicitudes/{id}
Rol requerido: cualquier autenticado
Response (200): `SolicitudResponseDTO`

Posibles errores:
- 404

### GET /api/solicitudes?estatus=PENDIENTE
Rol requerido: cualquier autenticado
Descripción: lista solicitudes. `estatus` es un query param opcional (uno de los valores del enum `EstatusSolicitud`). **Si el usuario autenticado tiene rol `MEDICO`, el backend filtra automáticamente por su propio `empleadoId`** — un médico nunca ve las solicitudes de otro médico, sin importar qué filtros mande. `ADMIN` y `FARMACEUTICO` ven todas las solicitudes.
Response (200): `SolicitudResponseDTO[]`

---

## Configuración

Parámetros configurables del sistema (ej. el toggle de FEFO/FIFO, ver [REGLAS_DE_NEGOCIO.md](./REGLAS_DE_NEGOCIO.md)).

### GET /api/configuracion/{clave}
Rol requerido: ADMIN o FARMACEUTICO
Descripción: obtiene el valor actual de una clave de configuración.

Response (200): `ConfiguracionResponseDTO`

Ejemplo de response:
```json
{
  "clave": "INVENTARIO_FEFO_HABILITADO",
  "valor": "true",
  "descripcion": "Si esta en true, las dispensaciones descuentan primero del lote con fecha de caducidad mas proxima (FEFO). Si esta en false, se usa FIFO por fecha de ingreso.",
  "updatedAt": "2026-07-01T00:00:00",
  "updatedBy": "system"
}
```

Posibles errores:
- 403 si no es ADMIN/FARMACEUTICO
- 404 si la clave no existe

### PUT /api/configuracion/{clave}
Rol requerido: ADMIN
Descripción: cambia el valor de una clave de configuración existente.

Request body: `ConfiguracionValorRequestDTO`
Response (200): `ConfiguracionResponseDTO`

Ejemplo de request:
```json
{ "valor": "false" }
```

Posibles errores:
- 400 validación, 403 si no es ADMIN, 404 si la clave no existe
