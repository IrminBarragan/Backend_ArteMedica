# Modelos (DTOs y enums)

Todos los DTOs son `record` de Java. Los campos se serializan/deserializan en JSON en el mismo orden en que están declarados abajo. Las validaciones solo se ejecutan en los DTOs de **request** (los de response no las llevan porque el backend los construye internamente).

Leyenda de validaciones:
- **Requerido** = si falta o es `null`/vacío, la petición responde `400`.
- **Opcional** = puede omitirse o mandarse como `null`.

---

## Catálogos

### `EmpleadoRequestDTO`
| Campo | Tipo | Requerido | Validación |
|---|---|---|---|
| `nombres` | string | Sí | No vacío |
| `apellidoPaterno` | string | Sí | No vacío |
| `apellidoMaterno` | string | No | — |
| `tipo` | enum `TipoEmpleado` | Sí | Debe ser uno de los valores del enum |
| `especialidad` | string | No | Solo tiene sentido si `tipo` es `MEDICO`, pero no se valida a nivel de DTO |
| `cedulaProfesional` | string | No | — |
| `telefonoGuardia` | string | No | — |

### `EmpleadoResponseDTO`
`id`, `nombres`, `apellidoPaterno`, `apellidoMaterno`, `tipo`, `especialidad`, `cedulaProfesional`, `telefonoGuardia`, `activo` (boolean).

### `UsuarioRequestDTO`
| Campo | Tipo | Requerido | Validación |
|---|---|---|---|
| `username` | string | Sí | No vacío |
| `password` | string | Sí | No vacío, mínimo 8 caracteres |
| `empleadoId` | number | Sí | Debe existir un `Empleado` con ese id |
| `rol` | enum `Rol` | Sí | Debe ser uno de los valores del enum |

### `UsuarioResponseDTO`
`id`, `username`, `empleadoId`, `empleadoNombre` (nombres + apellido paterno del empleado), `rol`, `activo`, `createdAt`, `updatedAt`. **Nunca incluye la contraseña.**

### `AreaRequestDTO`
| Campo | Tipo | Requerido | Validación |
|---|---|---|---|
| `nombre` | string | Sí | No vacío |
| `descripcion` | string | No | — |

### `AreaResponseDTO`
`id`, `nombre`, `descripcion`, `activo`.

### `CategoriaMedicamentoRequestDTO`
| Campo | Tipo | Requerido | Validación |
|---|---|---|---|
| `nombre` | string | Sí | No vacío |
| `descripcion` | string | No | — |

### `CategoriaMedicamentoResponseDTO`
`id`, `nombre`, `descripcion`, `activo`.

### `ProveedorRequestDTO`
| Campo | Tipo | Requerido | Validación |
|---|---|---|---|
| `nombre` | string | Sí | No vacío |
| `direccion` | string | No | — |
| `telefono` | string | No | — |
| `correo` | string | No | Si se manda, debe tener formato de correo válido |

### `ProveedorResponseDTO`
`id`, `nombre`, `direccion`, `telefono`, `correo`, `activo`.

---

## Inventario

### `ProductoRequestDTO`
| Campo | Tipo | Requerido | Validación |
|---|---|---|---|
| `nombre` | string | Sí | No vacío |
| `presentacion` | string | Sí | No vacío |
| `codigoBarras` | string | No | — |
| `esControlado` | boolean | Sí (primitivo, no puede ser `null`) | Sin validación adicional; si se omite en el JSON, Jackson lo trata como `false` |
| `categoriaId` | number | Sí | Debe existir una `CategoriaMedicamento` con ese id |
| `precioVenta` | decimal | Sí | Mayor a 0 |
| `precioCompra` | decimal | Sí | Mayor a 0 |
| `stockMinimo` | entero | Sí | Mayor o igual a 0 |

Nota: **no existe un campo `stockActual` en el request** — no se puede establecer ni editar manualmente, ver [REGLAS_DE_NEGOCIO.md](./REGLAS_DE_NEGOCIO.md).

### `ProductoResponseDTO`
`id`, `nombre`, `presentacion`, `codigoBarras`, `esControlado`, `categoriaId`, `categoriaNombre`, `precioVenta`, `precioCompra`, `stockMinimo`, `stockActual`, `activo`.

### `CodigoEquivalenteRequestDTO`
Un código de barras alterno para un mismo producto (ej. otra presentación del fabricante).

| Campo | Tipo | Requerido | Validación |
|---|---|---|---|
| `productoId` | number | Sí | Debe existir un `Producto` con ese id |
| `codigoBarras` | string | Sí | No vacío |

### `CodigoEquivalenteResponseDTO`
`id`, `productoId`, `productoNombre`, `codigoBarras`, `activo`, `createdAt`, `createdBy` (username de quien lo creó, tomado del token).

### `LoteResponseDTO`
**No existe un `LoteRequestDTO`** — los lotes nunca se crean ni editan directamente por la API, solo se generan implícitamente al registrar una compra (ver `CompraDetalleRequestDTO` abajo y [REGLAS_DE_NEGOCIO.md](./REGLAS_DE_NEGOCIO.md)).

`id`, `numeroLote`, `productoId`, `productoNombre`, `proveedorId`, `proveedorNombre`, `fechaCaducidad`, `costoCompra`, `cantidadInicial`, `existenciaActual` (lo que queda disponible hoy), `activo`.

---

## Compras

### `CompraDetalleRequestDTO`
Cada línea de una compra genera un lote nuevo.

| Campo | Tipo | Requerido | Validación |
|---|---|---|---|
| `productoId` | number | Sí | Debe existir un `Producto` con ese id |
| `numeroLote` | string | Sí | No vacío |
| `fechaCaducidad` | fecha (`YYYY-MM-DD`) | Sí | Debe ser una fecha futura (posterior a hoy) |
| `cantidad` | entero | Sí | Mayor a 0 |
| `costoUnitario` | decimal | Sí | Mayor a 0 |

### `CompraRequestDTO`
| Campo | Tipo | Requerido | Validación |
|---|---|---|---|
| `proveedorId` | number | Sí | Debe existir un `Proveedor` con ese id |
| `numeroFactura` | string | Sí | No vacío |
| `fechaCompra` | fecha (`YYYY-MM-DD`) | Sí | — |
| `detalles` | lista de `CompraDetalleRequestDTO` | Sí | No puede estar vacía; cada elemento de la lista se valida individualmente (si una línea es inválida, la petición completa se rechaza con `400`) |

### `CompraDetalleResponseDTO`
`productoId`, `productoNombre`, `cantidad`, `costoUnitario`, `subtotal` (= `cantidad` × `costoUnitario`), `loteId`, `numeroLote` (el lote que esa línea generó).

### `CompraResponseDTO`
`id`, `proveedorId`, `proveedorNombre`, `numeroFactura`, `fechaCompra`, `usuarioRegistroUsername` (quién la registró, tomado del token), `total` (suma de todos los subtotales), `detalles` (lista de `CompraDetalleResponseDTO`), `createdAt`.

---

## Solicitudes

### `SolicitudDetalleRequestDTO`
| Campo | Tipo | Requerido | Validación |
|---|---|---|---|
| `productoId` | number | Sí | Debe existir un `Producto` con ese id |
| `cantidadSolicitada` | entero | Sí | Mayor a 0 |

### `SolicitudRequestDTO`
| Campo | Tipo | Requerido | Validación |
|---|---|---|---|
| `areaId` | number | Sí | Debe existir un `Area` con ese id |
| `detalles` | lista de `SolicitudDetalleRequestDTO` | Sí | No puede estar vacía; cada elemento se valida individualmente |

**No lleva `medicoId`** — se toma del token del médico autenticado (ver [REGLAS_DE_NEGOCIO.md](./REGLAS_DE_NEGOCIO.md)).

### `SolicitudDetalleResponseDTO`
`id`, `productoId`, `productoNombre`, `presentacion`, `cantidadSolicitada`, `cantidadAutorizada` (`null` hasta que se aprueba), `cantidadEntregada` (arranca en 0), `loteId`, `numeroLote` (el primer lote que surtió esta línea; si una línea se surtió de varios lotes, aquí solo queda el primero como referencia rápida).

### `SolicitudResponseDTO`
`id`, `medicoId`, `medicoNombre`, `areaId`, `areaNombre`, `fechaSolicitud`, `estatus` (enum `EstatusSolicitud`), `farmaceuticoNombre` (`null` hasta que un farmacéutico interviene), `fechaAprobacion` (se reutiliza también como fecha de resolución en un rechazo), `fechaEntrega` (solo se llena cuando queda `ENTREGADA_COMPLETA`), `motivoRechazo` (solo si `estatus = RECHAZADO`), `detalles` (lista de `SolicitudDetalleResponseDTO`), `createdAt`.

### `AprobarSolicitudRequestDTO`
| Campo | Tipo | Requerido | Validación |
|---|---|---|---|
| `cantidadesAutorizadasPorProducto` | mapa `productoId → cantidad` | Sí | No puede estar vacío. Si un producto de la solicitud no aparece en el mapa, se autoriza como 0. |

```json
{ "cantidadesAutorizadasPorProducto": { "12": 30, "45": 10 } }
```

### `RechazarSolicitudRequestDTO`
| Campo | Tipo | Requerido | Validación |
|---|---|---|---|
| `motivo` | string | Sí | No vacío |

---

## Configuración

### `ConfiguracionValorRequestDTO`
| Campo | Tipo | Requerido | Validación |
|---|---|---|---|
| `valor` | string | Sí | No vacío |

### `ConfiguracionResponseDTO`
`clave`, `valor`, `descripcion`, `updatedAt`, `updatedBy` (username de quien hizo el último cambio).

---

## Autenticación

Ver [AUTENTICACION.md](./AUTENTICACION.md) para el detalle completo del flujo.

### `LoginRequestDTO`
`username` (string, requerido, no vacío), `password` (string, requerido, no vacío).

### `LoginResponseDTO`
`token`, `tipo` (siempre `"Bearer"`), `username`, `rol`, `empleadoId`, `expiresIn` (milisegundos).

---

## Errores

### `ErrorResponseDTO`
Ver el detalle completo (tabla de status codes, ejemplos) en [ERRORES.md](./ERRORES.md). Campos: `mensaje`, `status`, `timestamp`, `errores` (mapa campo→mensaje, solo poblado en errores de validación).

---

## Enums

### `TipoEmpleado`
`MEDICO`, `FARMACEUTICO`, `ADMIN` — clasifica al `Empleado` (campo `tipo` en `EmpleadoRequestDTO`/`EmpleadoResponseDTO`). No confundir con `Rol`: un `Empleado` puede existir sin tener una cuenta de acceso (`Usuario`), y viceversa el `Rol` es el que controla permisos de la API.

### `Rol`
`ADMIN`, `MEDICO`, `FARMACEUTICO` — el rol de acceso de un `Usuario`, es el que determina qué endpoints puede usar (vía `@PreAuthorize`) y viaja como claim `rol` dentro del JWT.

### `EstatusSolicitud`
`PENDIENTE`, `APROBADO`, `RECHAZADO`, `ENTREGADA_PARCIAL`, `ENTREGADA_COMPLETA` — ver el diagrama de transiciones completo en [REGLAS_DE_NEGOCIO.md](./REGLAS_DE_NEGOCIO.md).

### `TipoMovimiento`
`ENTRADA`, `SALIDA`, `MERMA` — clasifica cada línea del kardex (`MovimientoInventario`). **Nota:** en el estado actual del código no existe ningún Controller/DTO que exponga el kardex directamente por la API; este enum vive en el modelo interno y se documenta aquí porque el prompt original lo pidió, pero el frontend no lo va a recibir en ninguna respuesta todavía.

### `OrigenMovimiento`
`COMPRA`, `SOLICITUD`, `MANUAL` — de dónde vino un movimiento de kardex (una compra, una dispensación de solicitud, o un ajuste manual). Mismo caso que `TipoMovimiento`: no se expone todavía en ninguna respuesta de la API.
