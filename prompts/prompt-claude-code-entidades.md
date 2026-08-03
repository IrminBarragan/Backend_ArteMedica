# Prompt para Claude Code — Entidades JPA de ArteMédica Farmacia

Copia y pega el siguiente bloque tal cual en Claude Code, dentro de la raíz de tu proyecto backend.

---

## PROMPT

Estás trabajando en el backend de **ArteMédica Farmacia**, un módulo Spring Boot (Java) para la gestión de inventario de medicamentos de un hospital. Usa Spring Data JPA, Lombok, y PostgreSQL. El paquete base del proyecto es `dev.eduardo.artemedica.farmacia` (si el paquete real de tu proyecto es distinto, ajústalo al que encuentres en `src/main/java`).

### Tu tarea

Crear las entidades JPA descritas abajo, divididas en 3 bloques. Trabaja **un bloque a la vez**, en este orden, y **haz un commit de Git al terminar cada bloque** (no dejes todo para un solo commit al final).

Antes de empezar, revisa la estructura de carpetas existente del proyecto (`src/main/java/.../`) y crea, si no existen, los siguientes paquetes:

- `model` (o `entity`, usa el que ya exista en el proyecto; si no existe ninguno, usa `model`)
- `model/enums` para los enums de estado

### Convenciones a seguir en todas las entidades

- Usa Lombok (`@Getter`, `@Setter`, `@NoArgsConstructor`, `@AllArgsConstructor`, `@Builder`) para reducir boilerplate.
- Usa `@Entity` y `@Table(name = "nombre_tabla_snake_case")` en cada clase.
- Usa `@Id @GeneratedValue(strategy = GenerationType.IDENTITY)` para las llaves primarias.
- Relaciones `@ManyToOne` con `fetch = FetchType.LAZY` por defecto.
- Usa `@Column(nullable = false)` en los campos obligatorios según se indica.
- Los campos `activo` deben tener `@Column(nullable = false)` con valor por defecto `true`.
- Los campos de fecha usa `LocalDate` (solo fecha) o `LocalDateTime` (fecha y hora) según corresponda, nunca `Date`.
- Los enums van en `model/enums` como `enum` de Java estándar (no como String suelto).
- No agregues lógica de negocio en las entidades (eso va después en los Services); las entidades solo representan la estructura de datos y sus relaciones.

---

### BLOQUE 1 — Catálogos base

Crea estas entidades:

**`Empleado`**
- `id` (Long)
- `nombres` (String, requerido)
- `apellidoPaterno` (String, requerido)
- `apellidoMaterno` (String, opcional)
- `tipo` (enum `TipoEmpleado`: `MEDICO`, `FARMACEUTICO`, `ADMIN`)
- `especialidad` (String, opcional — solo aplica si tipo es MEDICO)
- `cedulaProfesional` (String, opcional)
- `telefonoGuardia` (String, opcional)
- `activo` (boolean, default true)

**`Usuario`**
- `id` (Long)
- `username` (String, único, requerido)
- `password` (String, requerido — se guardará hasheado, eso lo maneja el Service más adelante, no la entidad)
- `empleado` (`@ManyToOne` a `Empleado`, requerido)
- `rol` (enum `Rol`: `ADMIN`, `MEDICO`, `FARMACEUTICO`)
- `activo` (boolean, default true)
- `createdAt` (LocalDateTime)
- `updatedAt` (LocalDateTime)

**`Area`**
- `id` (Long)
- `nombre` (String, requerido)
- `descripcion` (String, opcional)
- `activo` (boolean, default true)

**`CategoriaMedicamento`**
- `id` (Long)
- `nombre` (String, requerido)
- `descripcion` (String, opcional)
- `activo` (boolean, default true)

**`Proveedor`**
- `id` (Long)
- `nombre` (String, requerido)
- `direccion` (String, opcional)
- `telefono` (String, opcional)
- `correo` (String, opcional)
- `activo` (boolean, default true)

Crea también el enum `TipoEmpleado` y el enum `Rol` en `model/enums`.

Al terminar este bloque, ejecuta:
```bash
git add .
git commit -m "feat: agregar entidades de catálogos base (Empleado, Usuario, Area, CategoriaMedicamento, Proveedor)"
```

---

### BLOQUE 2 — Inventario y compras

Crea estas entidades (dependen de las del Bloque 1, así que este bloque va después):

**`Producto`** (representa al medicamento)
- `id` (Long)
- `nombre` (String, requerido)
- `presentacion` (String, requerido)
- `codigoBarras` (String, único, opcional)
- `esControlado` (boolean, default false)
- `categoria` (`@ManyToOne` a `CategoriaMedicamento`, requerido)
- `precioVenta` (BigDecimal)
- `precioCompra` (BigDecimal)
- `stockMinimo` (Integer, requerido)
- `stockActual` (Integer, default 0 — es un campo caché que se recalcula desde los movimientos de inventario, no se edita manualmente desde fuera)
- `activo` (boolean, default true)

**`CodigoEquivalente`**
- `id` (Long)
- `producto` (`@ManyToOne` a `Producto`, requerido)
- `codigoBarras` (String, requerido)
- `activo` (boolean, default true)
- `createdAt` (LocalDateTime)
- `createdBy` (String — username de quien lo creó)

**`Lote`**
- `id` (Long)
- `numeroLote` (String, requerido)
- `producto` (`@ManyToOne` a `Producto`, requerido)
- `proveedor` (`@ManyToOne` a `Proveedor`, requerido)
- `fechaCaducidad` (LocalDate, requerido)
- `costoCompra` (BigDecimal, requerido)
- `cantidadInicial` (Integer, requerido)
- `existenciaActual` (Integer, requerido)
- `activo` (boolean, default true)
- `createdAt` (LocalDateTime)
- `updatedAt` (LocalDateTime)

**`Compra`** (encabezado)
- `id` (Long)
- `proveedor` (`@ManyToOne` a `Proveedor`, requerido)
- `numeroFactura` (String, requerido)
- `fechaCompra` (LocalDate, requerido)
- `usuarioRegistro` (`@ManyToOne` a `Usuario`, requerido)
- `createdAt` (LocalDateTime)

**`CompraDetalle`**
- `id` (Long)
- `compra` (`@ManyToOne` a `Compra`, requerido)
- `producto` (`@ManyToOne` a `Producto`, requerido)
- `cantidad` (Integer, requerido)
- `costoUnitario` (BigDecimal, requerido)

En `Compra`, agrega una relación `@OneToMany(mappedBy = "compra", cascade = CascadeType.ALL)` hacia `CompraDetalle` como `List<CompraDetalle> detalles`.

Al terminar este bloque, ejecuta:
```bash
git add .
git commit -m "feat: agregar entidades de inventario y compras (Producto, CodigoEquivalente, Lote, Compra, CompraDetalle)"
```

---

### BLOQUE 3 — Solicitudes y kardex

Crea estas entidades:

**`Solicitud`** (pedido que hace el médico)
- `id` (Long)
- `medico` (`@ManyToOne` a `Empleado`, requerido)
- `area` (`@ManyToOne` a `Area`, requerido)
- `fechaSolicitud` (LocalDateTime, requerido)
- `estatus` (enum `EstatusSolicitud`: `PENDIENTE`, `APROBADO`, `RECHAZADO`, `ENTREGADA_PARCIAL`, `ENTREGADA_COMPLETA`)
- `farmaceutico` (`@ManyToOne` a `Empleado`, opcional — quien aprueba/gestiona la entrega)
- `fechaAprobacion` (LocalDateTime, opcional)
- `fechaEntrega` (LocalDateTime, opcional)
- `motivoRechazo` (String, opcional — solo aplica si estatus es RECHAZADO)
- `createdAt` (LocalDateTime)

**`SolicitudDetalle`**
- `id` (Long)
- `solicitud` (`@ManyToOne` a `Solicitud`, requerido)
- `producto` (`@ManyToOne` a `Producto`, requerido)
- `cantidadSolicitada` (Integer, requerido)
- `cantidadAutorizada` (Integer, opcional — se llena al aprobar)
- `cantidadEntregada` (Integer, default 0)
- `lote` (`@ManyToOne` a `Lote`, opcional — se asigna al momento de dispensar)

**`MovimientoInventario`** (kardex)
- `id` (Long)
- `lote` (`@ManyToOne` a `Lote`, requerido)
- `producto` (`@ManyToOne` a `Producto`, requerido)
- `tipoMovimiento` (enum `TipoMovimiento`: `ENTRADA`, `SALIDA`, `MERMA`)
- `cantidad` (Integer, requerido)
- `saldoResultante` (Integer, requerido — existencia del lote después de este movimiento)
- `motivo` (String, opcional)
- `usuario` (`@ManyToOne` a `Usuario`, requerido)
- `fechaMovimiento` (LocalDateTime, requerido)
- `origenTipo` (enum `OrigenMovimiento`: `COMPRA`, `SOLICITUD`, `MANUAL`)
- `origenId` (Long, opcional — id de la Compra o Solicitud que originó el movimiento)

En `Solicitud`, agrega `@OneToMany(mappedBy = "solicitud", cascade = CascadeType.ALL)` hacia `SolicitudDetalle` como `List<SolicitudDetalle> detalles`.

Crea los enums `EstatusSolicitud`, `TipoMovimiento` y `OrigenMovimiento` en `model/enums`.

Al terminar este bloque, ejecuta:
```bash
git add .
git commit -m "feat: agregar entidades de solicitudes y kardex de inventario (Solicitud, SolicitudDetalle, MovimientoInventario)"
```

---

### Al terminar los 3 bloques

Verifica que el proyecto compile sin errores (`mvn compile` o `./mvnw compile`) antes de cada commit. Si hay errores de compilación por referencias cruzadas entre entidades de distintos bloques, es válido crear las clases en el orden que evite el error, pero los commits deben seguir agrupados como se indicó arriba.

No hagas `git push` — deja los commits listos localmente para que yo los revise antes de subirlos.
