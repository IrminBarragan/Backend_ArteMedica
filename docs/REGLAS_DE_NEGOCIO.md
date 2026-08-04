# Reglas de negocio

Este documento explica el "por qué" detrás de los endpoints, para que el frontend sepa qué pantallas y botones mostrar en cada momento — no solo qué llamadas puede hacer.

## Diagrama de estados de una Solicitud

```
PENDIENTE
   │
   ├── PUT /api/solicitudes/{id}/aprobar   →  APROBADO
   │
   └── PUT /api/solicitudes/{id}/rechazar  →  RECHAZADO   (estado terminal)

APROBADO
   │
   └── PUT /api/solicitudes/{id}/dispensar →  ENTREGADA_PARCIAL   (si no alcanzó a cubrir todas las líneas autorizadas)
                                           →  ENTREGADA_COMPLETA  (si cubrió todas, estado terminal)

ENTREGADA_PARCIAL
   │
   └── PUT /api/solicitudes/{id}/dispensar →  ENTREGADA_PARCIAL   (sigue sin alcanzar, ej. sigue sin stock)
                                           →  ENTREGADA_COMPLETA  (si esta vez sí alcanzó, estado terminal)
```

Reglas de transición (las valida el backend, no solo el frontend):
- `aprobar` y `rechazar` **solo** funcionan si la solicitud está en `PENDIENTE`. Si se intenta sobre cualquier otro estatus, la API responde `409` (`EstadoInvalidoException`).
- `dispensar` **solo** funciona si la solicitud está en `APROBADO` o `ENTREGADA_PARCIAL`. Sobre cualquier otro estatus responde `409`.
- `RECHAZADO` y `ENTREGADA_COMPLETA` son estados terminales — no hay ningún endpoint que los haga transicionar de nuevo.
- Implicación para el frontend: muestra el botón "Aprobar"/"Rechazar" solo cuando `estatus === "PENDIENTE"`, y el botón "Dispensar" solo cuando `estatus === "APROBADO"` o `"ENTREGADA_PARCIAL"`.

`dispensar` puede fallar sin devolver error si simplemente no hay suficiente stock disponible en ese momento: la solicitud se queda en `ENTREGADA_PARCIAL` (o no cambia si no se entregó nada) y se puede reintentar más tarde llamando `dispensar` otra vez (típicamente después de registrar una nueva compra que reabastece el producto).

## `medicoId` / `farmaceuticoId` nunca van en el body

Ningún endpoint de `Solicitud` recibe `medicoId` o `farmaceuticoId` como parte del JSON que manda el cliente. Ambos se infieren siempre del JWT (`principal.getEmpleadoId()`):

- `POST /api/solicitudes` — el creador de la solicitud es siempre el médico autenticado.
- `PUT .../aprobar`, `PUT .../rechazar`, `PUT .../dispensar` — el farmacéutico que ejecuta la acción es siempre el autenticado.

Esto es intencional por seguridad: un médico no puede crear una solicitud "a nombre de" otro médico simplemente mandando un `medicoId` distinto en el body, porque ese campo ni siquiera existe en `SolicitudRequestDTO`.

Lo mismo aplica para **ver** solicitudes: `GET /api/solicitudes` filtra automáticamente por `empleadoId` del token si el usuario autenticado tiene rol `MEDICO` — un médico solo puede ver sus propias solicitudes, nunca las de otro médico, sin importar qué query params mande. `ADMIN` y `FARMACEUTICO` sí ven todas las solicitudes de todos los médicos.

## FEFO / FIFO

Cuando se dispensa una solicitud, el sistema tiene que decidir de qué lote descontar primero si un producto tiene varios lotes con existencia disponible. Hay dos estrategias posibles, y el sistema usa una u otra según una configuración global:

- **FEFO** (*First-Expired, First-Out*): se descuenta primero del lote que caduca más pronto. Es la práctica estándar en farmacia para minimizar medicamento caducado.
- **FIFO** (*First-In, First-Out*): se descuenta primero del lote que entró más temprano al inventario (por fecha de creación del lote), sin importar cuál caduca antes.

El toggle vive en `ConfiguracionSistema`, bajo la clave `INVENTARIO_FEFO_HABILITADO` (valor `"true"` o `"false"` como string). Se consulta y cambia con:
- `GET /api/configuracion/INVENTARIO_FEFO_HABILITADO` (ADMIN o FARMACEUTICO pueden leerlo)
- `PUT /api/configuracion/INVENTARIO_FEFO_HABILITADO` (**solo ADMIN** puede cambiarlo)

Implicación para el frontend: el toggle de "usar FEFO" en una pantalla de configuración solo debería mostrarse (o estar habilitado para editar) si el usuario autenticado tiene rol `ADMIN`; un `FARMACEUTICO` puede consultarlo pero el botón de guardar debería estar deshabilitado u oculto para ese rol. Si la clave no se ha creado todavía en la base de datos, el `GET` responde `404` — en la práctica esto no debería pasar porque el proyecto siembra esa fila por defecto (`valor = "true"`, es decir FEFO habilitado) al arrancar.

## `stockActual` es un valor calculado, no editable

`Producto.stockActual` se actualiza automáticamente en dos momentos:
- **Sube** cuando se registra una compra (`POST /api/compras`) — se suma la `cantidad` de cada línea.
- **Baja** cuando se dispensa una solicitud (`PUT /api/solicitudes/{id}/dispensar`) — se resta lo efectivamente entregado.

No existe ningún endpoint para editarlo directamente: `ProductoRequestDTO` (usado tanto en `POST` como en `PUT /api/productos/{id}`) ni siquiera tiene ese campo. El frontend no debe intentar mostrar un formulario para "ajustar stock manualmente" — no hay dónde mandarlo.

## Los lotes nunca se crean ni editan manualmente

`LoteController` (`/api/lotes`) es **solo lectura** — no expone `POST`, `PUT` ni `DELETE`. Un lote nuevo se genera automáticamente, uno por cada línea (`CompraDetalleRequestDTO`) de un `POST /api/compras`. Si el frontend necesita dar de alta inventario nuevo, el único camino es registrar una compra, nunca un formulario de "crear lote" directo.
