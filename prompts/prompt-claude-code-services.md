# Prompt para Claude Code — Services, patrón Strategy (FEFO/FIFO) y lógica transaccional

Copia y pega este bloque en Claude Code. Asume que ya existen las entidades y los repositories de las tareas anteriores. Trabaja las 4 tareas en orden y **haz un commit al terminar cada una**. Verifica que compile (`mvn compile` o `./mvnw compile`) antes de cada commit.

---

## PROMPT

Estás trabajando en el backend de **ArteMédica Farmacia** (Spring Boot + Spring Data JPA + PostgreSQL). Ya existen las entidades (`model`) y repositories (`repository`). Ahora vas a construir la capa de `Service`: excepciones personalizadas, el patrón Strategy para elegir lotes (FEFO/FIFO), servicios de catálogo (CRUD simple) y los dos servicios transaccionales críticos: `CompraService` y `SolicitudService`.

### Convenciones generales

- Cada Service tiene una interfaz (`XService`) y una implementación (`impl/XServiceImpl`), inyectada por constructor (no `@Autowired` en campos).
- Los métodos que escriben en base de datos van anotados `@Transactional`. Los de solo lectura, `@Transactional(readOnly = true)`.
- Usa los DTOs de la TAREA 1 (todos son `record`, Java 21) para lo que entra y sale de los Services. No uses clases con Lombok para los DTOs — records solamente.
- Los Services nunca exponen entidades JPA directamente hacia afuera del paquete `service` (eso lo resolverá el `mapper`/`Controller` después, pero no es tu tarea ahora — solo asegúrate de que el Service reciba y devuelva DTOs o tipos simples).

---

### TAREA 1 — DTOs con `record` (Java 21)

El proyecto usa Java 21, así que **todos los DTOs deben ser `record`, no clases con Lombok**. Van en el paquete `dto`. El proyecto ya tiene `spring-boot-starter-validation` en el `pom.xml`, así que usa las anotaciones de `jakarta.validation.constraints` (o `javax.validation.constraints` si el proyecto todavía usa ese namespace — revisa el `pom.xml` antes de escribir los imports) en **todos los DTOs de request**, sin excepción, para que ningún dato inválido llegue a crear una entidad.

Dos cosas importantes sobre cómo funciona esta validación:

1. **No se activa sola**: estas anotaciones solo se ejecutan cuando el Controller reciba el DTO con `@Valid @RequestBody` (eso lo harás en la tarea de Controllers, no ahora). Por ahora solo asegúrate de anotar bien cada campo.
2. **Cascada en listas**: cuando un DTO contiene una lista de otro DTO (como `CompraRequestDTO.detalles` o `SolicitudRequestDTO.detalles`), el campo de la lista necesita `@Valid` además de `@NotEmpty`, si no, Bean Validation valida el DTO de arriba pero **no entra** a validar cada elemento de la lista. Ya está marcado así abajo, mantén ese patrón si agregas otros DTOs con listas anidadas.

**Catálogos**

```java
public record EmpleadoRequestDTO(
    @NotBlank String nombres,
    @NotBlank String apellidoPaterno,
    String apellidoMaterno,
    @NotNull TipoEmpleado tipo,
    String especialidad,
    String cedulaProfesional,
    String telefonoGuardia
) {}

public record EmpleadoResponseDTO(
    Long id, String nombres, String apellidoPaterno, String apellidoMaterno,
    TipoEmpleado tipo, String especialidad, String cedulaProfesional,
    String telefonoGuardia, boolean activo
) {}

public record UsuarioRequestDTO(
    @NotBlank String username,
    @NotBlank @Size(min = 8) String password,
    @NotNull Long empleadoId,
    @NotNull Rol rol
) {}

public record UsuarioResponseDTO(
    Long id, String username, Long empleadoId, String empleadoNombre,
    Rol rol, boolean activo, LocalDateTime createdAt, LocalDateTime updatedAt
) {}

public record LoginRequestDTO(@NotBlank String username, @NotBlank String password) {}

public record LoginResponseDTO(String username, Rol rol, Long empleadoId) {}

public record AreaRequestDTO(@NotBlank String nombre, String descripcion) {}

public record AreaResponseDTO(Long id, String nombre, String descripcion, boolean activo) {}

public record CategoriaMedicamentoRequestDTO(@NotBlank String nombre, String descripcion) {}

public record CategoriaMedicamentoResponseDTO(Long id, String nombre, String descripcion, boolean activo) {}

public record ProveedorRequestDTO(
    @NotBlank String nombre, String direccion, String telefono, @Email String correo
) {}

public record ProveedorResponseDTO(
    Long id, String nombre, String direccion, String telefono, String correo, boolean activo
) {}
```

**Inventario**

```java
public record ProductoRequestDTO(
    @NotBlank String nombre,
    @NotBlank String presentacion,
    String codigoBarras,
    boolean esControlado,
    @NotNull Long categoriaId,
    @NotNull @Positive BigDecimal precioVenta,
    @NotNull @Positive BigDecimal precioCompra,
    @NotNull @PositiveOrZero Integer stockMinimo
) {}

public record ProductoResponseDTO(
    Long id, String nombre, String presentacion, String codigoBarras, boolean esControlado,
    Long categoriaId, String categoriaNombre, BigDecimal precioVenta, BigDecimal precioCompra,
    Integer stockMinimo, Integer stockActual, boolean activo
) {}

public record CodigoEquivalenteRequestDTO(@NotNull Long productoId, @NotBlank String codigoBarras) {}

public record CodigoEquivalenteResponseDTO(
    Long id, Long productoId, String productoNombre, String codigoBarras,
    boolean activo, LocalDateTime createdAt, String createdBy
) {}

public record LoteResponseDTO(
    Long id, String numeroLote, Long productoId, String productoNombre,
    Long proveedorId, String proveedorNombre, LocalDate fechaCaducidad,
    BigDecimal costoCompra, Integer cantidadInicial, Integer existenciaActual, boolean activo
) {}
```

(`Lote` no necesita un `RequestDTO` propio porque siempre se crea implícitamente desde `CompraDetalleRequestDTO`, nunca directo.)

**Compras**

```java
public record CompraDetalleRequestDTO(
    @NotNull Long productoId,
    @NotBlank String numeroLote,
    @NotNull @Future LocalDate fechaCaducidad,
    @NotNull @Positive Integer cantidad,
    @NotNull @Positive BigDecimal costoUnitario
) {}

public record CompraRequestDTO(
    @NotNull Long proveedorId,
    @NotBlank String numeroFactura,
    @NotNull LocalDate fechaCompra,
    @NotEmpty @Valid List<CompraDetalleRequestDTO> detalles
) {}

public record CompraDetalleResponseDTO(
    Long productoId, String productoNombre, Integer cantidad,
    BigDecimal costoUnitario, BigDecimal subtotal, Long loteId, String numeroLote
) {}

public record CompraResponseDTO(
    Long id, Long proveedorId, String proveedorNombre, String numeroFactura,
    LocalDate fechaCompra, String usuarioRegistroUsername, BigDecimal total,
    List<CompraDetalleResponseDTO> detalles, LocalDateTime createdAt
) {}
```

**Solicitudes**

```java
public record SolicitudDetalleRequestDTO(@NotNull Long productoId, @NotNull @Positive Integer cantidadSolicitada) {}

public record SolicitudRequestDTO(@NotNull Long areaId, @NotEmpty @Valid List<SolicitudDetalleRequestDTO> detalles) {}

public record SolicitudDetalleResponseDTO(
    Long id, Long productoId, String productoNombre, String presentacion,
    Integer cantidadSolicitada, Integer cantidadAutorizada, Integer cantidadEntregada,
    Long loteId, String numeroLote
) {}

public record SolicitudResponseDTO(
    Long id, Long medicoId, String medicoNombre, Long areaId, String areaNombre,
    LocalDateTime fechaSolicitud, EstatusSolicitud estatus, String farmaceuticoNombre,
    LocalDateTime fechaAprobacion, LocalDateTime fechaEntrega, String motivoRechazo,
    List<SolicitudDetalleResponseDTO> detalles, LocalDateTime createdAt
) {}

public record AprobarSolicitudRequestDTO(@NotEmpty Map<Long, Integer> cantidadesAutorizadasPorProducto) {}

public record RechazarSolicitudRequestDTO(@NotBlank String motivo) {}
```

Verifica que el proyecto compile antes del commit.

Commit:
```bash
git add .
git commit -m "feat: agregar DTOs (records) para catalogos, inventario, compras y solicitudes"
```

---

### TAREA 2 — Excepciones y patrón Strategy

**Excepciones** en el paquete `exception`:

```java
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) { super(message); }
}

public class StockInsuficienteException extends RuntimeException {
    public StockInsuficienteException(String message) { super(message); }
}

public class EstadoInvalidoException extends RuntimeException {
    public EstadoInvalidoException(String message) { super(message); }
}

public class ConflictoConcurrenciaException extends RuntimeException {
    public ConflictoConcurrenciaException(String message) { super(message); }
}
```

**Patrón Strategy** para selección de lotes, en el paquete `service.strategy`:

```java
public interface LoteSeleccionStrategy {
    List<Lote> obtenerLotesDisponiblesParaConsumo(Long productoId);
}
```

```java
@Component("fefoStrategy")
public class FefoSeleccionStrategy implements LoteSeleccionStrategy {
    private final LoteRepository loteRepository;

    public FefoSeleccionStrategy(LoteRepository loteRepository) {
        this.loteRepository = loteRepository;
    }

    @Override
    public List<Lote> obtenerLotesDisponiblesParaConsumo(Long productoId) {
        return loteRepository.findLotesDisponiblesFefoForUpdate(productoId);
    }
}
```

```java
@Component("fifoStrategy")
public class FifoSeleccionStrategy implements LoteSeleccionStrategy {
    private final LoteRepository loteRepository;

    public FifoSeleccionStrategy(LoteRepository loteRepository) {
        this.loteRepository = loteRepository;
    }

    @Override
    public List<Lote> obtenerLotesDisponiblesParaConsumo(Long productoId) {
        return loteRepository.findLotesDisponiblesFifoForUpdate(productoId);
    }
}
```

**Resolver**, en el paquete `service.strategy`:

```java
@Component
public class LoteSeleccionStrategyResolver {

    private static final String CLAVE_FEFO = "INVENTARIO_FEFO_HABILITADO";

    private final Map<String, LoteSeleccionStrategy> strategies;
    private final ConfiguracionSistemaRepository configuracionSistemaRepository;

    public LoteSeleccionStrategyResolver(Map<String, LoteSeleccionStrategy> strategies,
                                          ConfiguracionSistemaRepository configuracionSistemaRepository) {
        this.strategies = strategies;
        this.configuracionSistemaRepository = configuracionSistemaRepository;
    }

    public LoteSeleccionStrategy resolver() {
        boolean fefoHabilitado = configuracionSistemaRepository.findByClave(CLAVE_FEFO)
                .map(c -> Boolean.parseBoolean(c.getValor()))
                .orElse(true);
        return fefoHabilitado ? strategies.get("fefoStrategy") : strategies.get("fifoStrategy");
    }
}
```

(Spring inyecta automáticamente un `Map<String, LoteSeleccionStrategy>` con todos los beans que implementan esa interfaz, usando el nombre del `@Component` como key — no necesitas configurarlo manualmente.)

Commit:
```bash
git add .
git commit -m "feat: agregar excepciones personalizadas y patron Strategy para seleccion de lotes (FEFO/FIFO)"
```

---

### TAREA 3 — Servicios de catálogo (CRUD simple)

Crea `EmpleadoService`, `UsuarioService`, `AreaService`, `CategoriaMedicamentoService`, `ProveedorService` (interfaz + impl), cada uno con las operaciones estándar:

- `crear(RequestDTO dto)`
- `actualizar(Long id, RequestDTO dto)`
- `obtenerPorId(Long id)` → lanza `ResourceNotFoundException` si no existe
- `listarActivos()`
- `desactivar(Long id)` (baja lógica: `activo = false`, nunca hard-delete)

Para `UsuarioService` en particular:
- El método `crear` debe encriptar la contraseña con `PasswordEncoder` (inyéctalo por constructor; si no existe un bean `PasswordEncoder` en el proyecto, créalo como `@Bean` en la clase de configuración de seguridad existente, usando `BCryptPasswordEncoder`).
- Agrega un método `autenticar(String username, String password)` que busque el usuario activo por username y valide la contraseña con `passwordEncoder.matches(...)`, lanzando una excepción si no coincide (puedes usar `EstadoInvalidoException` o crear una específica de autenticación si lo prefieres).

Commit:
```bash
git add .
git commit -m "feat: agregar services de catalogo (Empleado, Usuario, Area, CategoriaMedicamento, Proveedor)"
```

---

### TAREA 4 — Inventario y compras

Crea `ProductoService`, `CodigoEquivalenteService`, `LoteService` (CRUD estándar como en la tarea 2, más lo específico de cada uno: `ProductoService` expone `listarStockBajo()` usando el repository correspondiente).

**`CompraService`** — este es transaccional y crea `Compra` + `CompraDetalle` + `Lote` + `MovimientoInventario` en una sola operación:

```java
public interface CompraService {
    CompraResponseDTO registrarCompra(CompraRequestDTO dto, Long usuarioId);
}
```

Lógica de `registrarCompra` (anotado `@Transactional`):

1. Cargar el `Proveedor` y el `Usuario` (lanzar `ResourceNotFoundException` si no existen).
2. Crear y guardar la `Compra` (encabezado).
3. Por cada línea del DTO:
   a. Cargar el `Producto`.
   b. Crear el `CompraDetalle` (cantidad, costoUnitario, ligado a la compra).
   c. Crear un `Lote` nuevo: `producto`, `proveedor` (el mismo de la compra), `numeroLote` (del DTO), `fechaCaducidad` (del DTO), `costoCompra = costoUnitario`, `cantidadInicial = cantidad`, `existenciaActual = cantidad`, `activo = true`.
   d. Guardar el `Lote`.
   e. Incrementar `producto.stockActual += cantidad` y guardar el producto (usa el método de reintento con `@Version` que se describe abajo).
   f. Registrar un `MovimientoInventario`: `tipoMovimiento = ENTRADA`, `cantidad`, `saldoResultante = lote.existenciaActual` (recién creado, igual a `cantidadInicial`), `usuario`, `origenTipo = COMPRA`, `origenId = compra.getId()`, `fechaMovimiento = now`.
4. Devolver el DTO de respuesta con el resumen de la compra.

**Reintento de bloqueo optimista para `Producto`**: crea un método privado/utilitario en `ProductoServiceImpl` (o una clase de soporte compartida) así:

```java
private Producto actualizarStockConReintento(Long productoId, int delta, int maxIntentos) {
    int intentos = 0;
    while (true) {
        try {
            Producto producto = productoRepository.findById(productoId)
                    .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado: " + productoId));
            producto.setStockActual(producto.getStockActual() + delta);
            return productoRepository.save(producto);
        } catch (ObjectOptimisticLockingFailureException e) {
            intentos++;
            if (intentos >= maxIntentos) {
                throw new ConflictoConcurrenciaException(
                    "No se pudo actualizar el stock del producto " + productoId + " tras " + maxIntentos + " intentos, intenta de nuevo.");
            }
        }
    }
}
```

Úsalo con `maxIntentos = 3` tanto en `CompraService` (delta positivo) como en `SolicitudService` (delta negativo, tarea 4).

Commit:
```bash
git add .
git commit -m "feat: agregar services de inventario y compras con registro de kardex"
```

---

### TAREA 5 — Solicitudes y dispensación (el núcleo del sistema)

```java
public interface SolicitudService {
    SolicitudResponseDTO crear(SolicitudRequestDTO dto, Long medicoId);
    SolicitudResponseDTO aprobar(Long solicitudId, Map<Long, Integer> cantidadesAutorizadasPorProducto, Long farmaceuticoId);
    SolicitudResponseDTO rechazar(Long solicitudId, String motivo, Long farmaceuticoId);
    SolicitudResponseDTO dispensar(Long solicitudId, Long farmaceuticoId);
}
```

**`crear`**: valida médico y área, crea `Solicitud` con `estatus = PENDIENTE` y sus `SolicitudDetalle` con `cantidadSolicitada` (sin `cantidadAutorizada` todavía).

**`aprobar`** (`@Transactional`):
1. Bloquear la solicitud: `solicitudRepository.findByIdForUpdate(solicitudId)`.
2. Validar que `estatus == PENDIENTE`, si no lanzar `EstadoInvalidoException`.
3. Por cada detalle, verificar que exista stock suficiente sumando `existenciaActual` de los lotes vigentes del producto (lectura simple, sin lock — el lock real es al dispensar). Si no alcanza para la cantidad que se quiere autorizar, lanzar `StockInsuficienteException` con el nombre del producto y la cantidad disponible.
4. Asignar `cantidadAutorizada` a cada detalle según el mapa recibido.
5. Cambiar `estatus = APROBADO`, `farmaceutico`, `fechaAprobacion = now`.

**`rechazar`** (`@Transactional`): bloquear, validar `PENDIENTE`, cambiar a `RECHAZADO`, guardar `motivoRechazo`, `farmaceutico`, `fechaAprobacion = now` (se reutiliza como "fecha de resolución").

**`dispensar`** (`@Transactional`) — la lógica más delicada:

1. Bloquear la solicitud (`findByIdForUpdate`). Validar `estatus == APROBADO` (o `ENTREGADA_PARCIAL`, si vas a permitir completar una entrega parcial en una segunda pasada — soporta ambos casos).
2. Por cada `SolicitudDetalle`:
   a. Calcular `pendiente = cantidadAutorizada - cantidadEntregada`. Si `pendiente <= 0`, saltar al siguiente detalle.
   b. Obtener los lotes disponibles y bloqueados con `loteSeleccionStrategyResolver.resolver().obtenerLotesDisponiblesParaConsumo(productoId)`.
   c. Recorrer los lotes en el orden que devuelve la estrategia, descontando de cada uno hasta cubrir `pendiente` o quedarse sin lotes:
      - `cantidadTomada = Math.min(pendiente, lote.getExistenciaActual())`
      - `lote.setExistenciaActual(lote.getExistenciaActual() - cantidadTomada)`, guardar el lote.
      - Registrar un `MovimientoInventario`: `SALIDA`, `cantidad = cantidadTomada`, `saldoResultante = lote.getExistenciaActual()`, `origenTipo = SOLICITUD`, `origenId = solicitud.getId()`, `usuario = farmaceutico`.
      - `pendiente -= cantidadTomada`
      - Actualizar `detalle.cantidadEntregada += cantidadTomada` y, si el detalle aún no tiene `lote` asignado, asignar este como el lote de referencia (nota: si un mismo detalle termina surtiéndose de más de un lote, el `SolicitudDetalle.lote` solo guardará el primero como referencia rápida — la trazabilidad exacta de qué lote surtió qué cantidad vive en `MovimientoInventario`, que si soporta múltiples lotes por movimiento).
      - Descontar del stock del producto con `actualizarStockConReintento(productoId, -cantidadTomada, 3)`.
      - Si `pendiente == 0`, salir del ciclo de lotes para este detalle.
   d. Si terminaste de recorrer todos los lotes disponibles y `pendiente > 0`, no hay error — simplemente ese detalle queda parcialmente entregado (puede completarse después con otra compra + otra llamada a `dispensar`).
3. Al terminar todos los detalles, recalcular el estatus de la solicitud:
   - Si todos los detalles tienen `cantidadEntregada == cantidadAutorizada` → `estatus = ENTREGADA_COMPLETA`, `fechaEntrega = now`.
   - Si al menos uno tiene `cantidadEntregada > 0` pero no todos están completos → `estatus = ENTREGADA_PARCIAL`.
   - Guardar la solicitud.
4. Devolver el DTO de respuesta.

**Manejo de `PessimisticLockException` / timeout de bloqueo**: envuelve la lógica de `dispensar` (o al menos la sección que usa los repositorios con `@Lock`) de forma que, si se lanza `PessimisticLockException` o `LockTimeoutException`, se traduzca a `ConflictoConcurrenciaException` con un mensaje claro como *"Otro usuario está procesando este mismo lote/solicitud en este momento, intenta de nuevo en unos segundos."* — esto lo va a necesitar el `GlobalExceptionHandler` que se hará en la siguiente tarea (controllers), así que por ahora solo asegúrate de que la excepción se propague de forma clara, no la dejes como una excepción genérica de JPA.

Commit:
```bash
git add .
git commit -m "feat: agregar SolicitudService con flujo de aprobacion, rechazo y dispensacion FEFO/FIFO con kardex"
```

---

No hagas `git push`. Deja los commits listos localmente para revisión.
