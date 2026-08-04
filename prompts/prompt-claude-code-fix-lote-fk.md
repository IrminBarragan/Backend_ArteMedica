Estás trabajando en el backend de **ArteMédica Farmacia**. Hay un hueco de diseño que hay que corregir: `CompraDetalle` no tiene una relación directa hacia `Lote`, y actualmente `CompraService`/`CompraRepository` reconstruyen esa relación leyendo `MovimientoInventario` por `origenTipo=COMPRA` y `origenId`. Eso es ambiguo cuando una misma `Compra` tiene dos `CompraDetalle` del mismo `Producto` (dos lotes distintos del mismo medicamento en una sola factura) — no hay forma determinística de saber qué línea generó qué lote.

Corrige esto agregando la FK directa:

1. En la entidad `CompraDetalle`, agrega:
```java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "lote_id")
private Lote lote;
```
(nullable a nivel de columna no es necesario forzarlo con `nullable = false`, pero en la práctica siempre se va a llenar al crear la compra — no debería quedar nulo salvo datos legacy previos a este cambio).

2. En `CompraService.registrarCompra(...)`, en el punto donde ya se crea y guarda el `Lote` para cada línea (paso "d" y "e" del flujo original), asigna ese mismo objeto `Lote` recién guardado a `compraDetalle.setLote(lote)` antes de persistir el `CompraDetalle` (o si el `CompraDetalle` ya se guardó antes de crear el lote, actualízalo y guárdalo de nuevo con la referencia).

3. Simplifica los métodos de lectura de `CompraService`/`CompraRepository` que estaban reconstruyendo `loteId`/`numeroLote` vía `MovimientoInventarioRepository.findByOrigenTipoAndOrigenId(...)` — ya no hace falta ese rodeo, lee directo `compraDetalle.getLote()`. Deja `MovimientoInventarioRepository.findByOrigenTipoAndOrigenId` si la sigues usando para otras cosas (trazabilidad general del kardex); si no la usa nada más, puedes dejarla de todos modos, no hace daño.

4. Si el proyecto usa una migración (Flyway/Liquibase) para el esquema en vez de `ddl-auto=update`, agrega la migración correspondiente (`ALTER TABLE compra_detalle ADD COLUMN lote_id BIGINT REFERENCES lote(id)`). Si usa `ddl-auto=update`, Hibernate la crea sola, no hace falta nada extra.

Verifica que compile antes del commit.

Commit:
```bash
git add .
git commit -m "fix: agregar FK directa CompraDetalle.lote para trazabilidad determinista, eliminar reconstruccion via kardex"
```

No hagas `git push`.
