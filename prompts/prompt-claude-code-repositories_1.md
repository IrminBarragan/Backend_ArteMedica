## PROMPT

Ahora vas a crear la capa de `Repository`, agregar una entidad de configuración nueva, e implementar bloqueo pesimista donde se indica. Trabaja en el orden de las 3 tareas de abajo y **haz un commit al terminar cada una**.

### Convenciones

- Los repositories van en un paquete `repository`, uno por entidad, con nombre `NombreEntidadRepository`.
- Todos extienden `JpaRepository<Entidad, Long>`.
- Usa `Optional<T>` para búsquedas de un solo resultado que pueden no existir.
- Para queries personalizadas usa `@Query` con JPQL cuando el nombre derivado del método no sea suficiente o sea ambiguo (ej. comparar dos columnas entre sí).
- No agregues lógica de negocio en los repositories, solo acceso a datos.

---

### TAREA 1 — Nueva entidad de configuración (para el toggle de FEFO)

Crea la entidad `ConfiguracionSistema` en el paquete `model`:

```java
@Entity
@Table(name = "configuracion_sistema")
public class ConfiguracionSistema {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String clave;

    @Column(nullable = false)
    private String valor;

    private String descripcion;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    private String updatedBy;
}
```

Agrega Lombok (`@Getter`, `@Setter`, `@NoArgsConstructor`, `@AllArgsConstructor`, `@Builder`).

Crea también `ConfiguracionSistemaRepository`:

```java
public interface ConfiguracionSistemaRepository extends JpaRepository<ConfiguracionSistema, Long> {
    Optional<ConfiguracionSistema> findByClave(String clave);
}
```

Agrega un `data.sql` (o el mecanismo de seed que ya use el proyecto, revísalo primero) que inserte esta fila por defecto si no existe:

```sql
INSERT INTO configuracion_sistema (clave, valor, descripcion, updated_at, updated_by)
SELECT 'INVENTARIO_FEFO_HABILITADO', 'true', 'Si esta en true, las dispensaciones descuentan primero del lote con fecha de caducidad mas proxima (FEFO). Si esta en false, se usa FIFO por fecha de ingreso.', NOW(), 'system'
WHERE NOT EXISTS (SELECT 1 FROM configuracion_sistema WHERE clave = 'INVENTARIO_FEFO_HABILITADO');
```

Commit:
```bash
git add .
git commit -m "feat: agregar entidad ConfiguracionSistema para parametros configurables (FEFO)"
```

---

### TAREA 2 — Agregar `@Version` a Producto y Solicitud (bloqueo optimista)

En la entidad `Producto`, agrega:

```java
@Version
private Long version;
```

En la entidad `Solicitud`, agrega el mismo campo:

```java
@Version
private Long version;
```

Esto agrega bloqueo optimista automático: JPA valida esa columna en cada `UPDATE` y lanza `OptimisticLockException` si otra transacción ya modificó la fila entre que la leíste y la guardaste. Esto lo manejaremos en el Service más adelante (reintento o mensaje de "el registro fue modificado por otro usuario, refresca e intenta de nuevo").

Commit:
```bash
git add .
git commit -m "feat: agregar bloqueo optimista (@Version) a Producto y Solicitud"
```

---

### TAREA 3 — Repositories

Crea los siguientes repositories en el paquete `repository`.

**`EmpleadoRepository`**
```java
public interface EmpleadoRepository extends JpaRepository<Empleado, Long> {
    List<Empleado> findByTipoAndActivoTrue(TipoEmpleado tipo);
}
```

**`UsuarioRepository`**
```java
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByUsername(String username);
    Optional<Usuario> findByUsernameAndActivoTrue(String username);
}
```

**`AreaRepository`**
```java
public interface AreaRepository extends JpaRepository<Area, Long> {
    List<Area> findByActivoTrue();
}
```

**`CategoriaMedicamentoRepository`**
```java
public interface CategoriaMedicamentoRepository extends JpaRepository<CategoriaMedicamento, Long> {
    List<CategoriaMedicamento> findByActivoTrue();
}
```

**`ProveedorRepository`**
```java
public interface ProveedorRepository extends JpaRepository<Proveedor, Long> {
    List<Proveedor> findByActivoTrue();
}
```

**`ProductoRepository`**
```java
public interface ProductoRepository extends JpaRepository<Producto, Long> {
    List<Producto> findByActivoTrue();
    Optional<Producto> findByCodigoBarras(String codigoBarras);

    @Query("SELECT p FROM Producto p WHERE p.activo = true AND p.stockActual <= p.stockMinimo")
    List<Producto> findProductosStockBajo();
}
```

**`CodigoEquivalenteRepository`**
```java
public interface CodigoEquivalenteRepository extends JpaRepository<CodigoEquivalente, Long> {
    Optional<CodigoEquivalente> findByCodigoBarrasAndActivoTrue(String codigoBarras);
    List<CodigoEquivalente> findByProductoId(Long productoId);
}
```

**`LoteRepository`** — la más importante, incluye bloqueo pesimista y las dos variantes de orden (FEFO / FIFO):

```java
public interface LoteRepository extends JpaRepository<Lote, Long> {

    // Lectura normal, sin bloqueo, para consultas (ej. mostrar disponibilidad en pantalla)
    List<Lote> findByProductoIdAndActivoTrueAndExistenciaActualGreaterThan(Long productoId, Integer cantidad);

    @Query("SELECT l FROM Lote l WHERE l.activo = true AND l.existenciaActual > 0 AND l.fechaCaducidad < CURRENT_DATE")
    List<Lote> findLotesVencidos();

    @Query("SELECT l FROM Lote l WHERE l.activo = true AND l.existenciaActual > 0 AND l.fechaCaducidad BETWEEN CURRENT_DATE AND :fechaLimite")
    List<Lote> findLotesPorVencer(@Param("fechaLimite") LocalDate fechaLimite);

    // --- Con bloqueo pesimista, para usarse SIEMPRE dentro de una transaccion en el Service ---

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints({@QueryHint(name = "jakarta.persistence.lock.timeout", value = "5000")})
    @Query("SELECT l FROM Lote l WHERE l.id = :id")
    Optional<Lote> findByIdForUpdate(@Param("id") Long id);

    // Orden FEFO: primero el lote que caduca mas pronto
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints({@QueryHint(name = "jakarta.persistence.lock.timeout", value = "5000")})
    @Query("SELECT l FROM Lote l WHERE l.producto.id = :productoId AND l.activo = true " +
           "AND l.existenciaActual > 0 AND l.fechaCaducidad >= CURRENT_DATE " +
           "ORDER BY l.fechaCaducidad ASC")
    List<Lote> findLotesDisponiblesFefoForUpdate(@Param("productoId") Long productoId);

    // Orden FIFO: primero el lote que entro mas temprano al inventario (fallback si FEFO esta deshabilitado)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints({@QueryHint(name = "jakarta.persistence.lock.timeout", value = "5000")})
    @Query("SELECT l FROM Lote l WHERE l.producto.id = :productoId AND l.activo = true " +
           "AND l.existenciaActual > 0 AND l.fechaCaducidad >= CURRENT_DATE " +
           "ORDER BY l.createdAt ASC")
    List<Lote> findLotesDisponiblesFifoForUpdate(@Param("productoId") Long productoId);
}
```

Nota: si el proyecto usa `javax.persistence.*` en vez de `jakarta.persistence.*` (depende de la versión de Spring Boot), ajusta el import y el nombre del hint (`javax.persistence.lock.timeout` en vez de `jakarta.persistence.lock.timeout`). Revisa qué versión de Spring Boot está usando el proyecto (`pom.xml`) antes de escribir esto.

**`CompraRepository`**
```java
public interface CompraRepository extends JpaRepository<Compra, Long> {
    List<Compra> findByProveedorId(Long proveedorId);
    List<Compra> findByFechaCompraBetween(LocalDate inicio, LocalDate fin);
}
```

**`CompraDetalleRepository`**
```java
public interface CompraDetalleRepository extends JpaRepository<CompraDetalle, Long> {
    List<CompraDetalle> findByCompraId(Long compraId);
}
```

**`SolicitudRepository`** — también con una variante bloqueada, para el momento de aprobar/dispensar:

```java
public interface SolicitudRepository extends JpaRepository<Solicitud, Long> {
    List<Solicitud> findByEstatus(EstatusSolicitud estatus);
    List<Solicitud> findByMedicoId(Long medicoId);
    List<Solicitud> findByAreaId(Long areaId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints({@QueryHint(name = "jakarta.persistence.lock.timeout", value = "5000")})
    @Query("SELECT s FROM Solicitud s WHERE s.id = :id")
    Optional<Solicitud> findByIdForUpdate(@Param("id") Long id);
}
```

**`SolicitudDetalleRepository`**
```java
public interface SolicitudDetalleRepository extends JpaRepository<SolicitudDetalle, Long> {
    List<SolicitudDetalle> findBySolicitudId(Long solicitudId);
}
```

**`MovimientoInventarioRepository`**
```java
public interface MovimientoInventarioRepository extends JpaRepository<MovimientoInventario, Long> {
    List<MovimientoInventario> findByProductoIdOrderByFechaMovimientoDesc(Long productoId);
    List<MovimientoInventario> findByLoteIdOrderByFechaMovimientoDesc(Long loteId);
    List<MovimientoInventario> findTop5ByOrderByFechaMovimientoDesc();
}
```

Verifica que el proyecto compile (`mvn compile` o `./mvnw compile`) antes de hacer el commit.

Commit:
```bash
git add .
git commit -m "feat: agregar capa de repositories con bloqueo pesimista en Lote y Solicitud"
```

---

No hagas `git push`. Deja los commits listos localmente para revisión.
