Estás trabajando en el backend de **ArteMédica Farmacia**. Ya está implementado todo: entidades, repositories, DTOs (records), services, seguridad JWT y controllers. Ahora vas a generar documentación en una carpeta `docs/` en la raíz del proyecto, pensada para que **otro agente de IA que construye el frontend la lea y entienda cómo consumir esta API sin tener que leer el código Java**.

Importante: no documentes de memoria lo que "debería" existir según este prompt — **lee el código real** (Controllers, DTOs, enums, `SecurityConfig`, `GlobalExceptionHandler`) antes de escribir cada archivo, porque puede haber decisiones o ajustes que se tomaron durante la implementación que difieren ligeramente de lo planeado originalmente. La documentación debe reflejar lo que el código realmente hace, no lo que se planeó.

Crea estos archivos dentro de `docs/`:

### `docs/README.md`
Índice general con: una descripción de 2-3 líneas del sistema (gestión de inventario de farmacia hospitalaria, solicitudes de médicos, aprobación y dispensación con trazabilidad FEFO/FIFO), la URL base de la API (`/api`), y un enlace a cada uno de los demás archivos de esta carpeta con una línea de qué contiene cada uno.

### `docs/AUTENTICACION.md`
- Cómo hacer login: `POST /api/auth/login`, body (`LoginRequestDTO` real, con sus campos y validaciones), respuesta (`LoginResponseDTO` real).
- Cómo usar el token en requests subsecuentes: header exacto (`Authorization: Bearer <token>`).
- Tiempo de expiración del token (lee el valor real de `application.properties`, no asumas 24h).
- Qué pasa si el token expira o es inválido (código de status y forma del error, según el `GlobalExceptionHandler` real).
- Los 3 roles del sistema (`ADMIN`, `MEDICO`, `FARMACEUTICO`) y una descripción breve de qué puede hacer cada uno en términos generales (el detalle fino de permisos por endpoint va en `API.md`).

### `docs/MODELOS.md`
Por cada DTO de request y response que exista realmente en el paquete `dto`, documenta:
- Nombre exacto del record.
- Cada campo: nombre, tipo, si es requerido u opcional, y las validaciones que tiene (`@NotBlank`, `@Positive`, etc. — tradúcelas a lenguaje natural, ej. "cantidad: entero, requerido, debe ser mayor a 0").
- Agrupa los DTOs por dominio, con subtítulos: Catálogos (Empleado, Usuario, Area, CategoriaMedicamento, Proveedor), Inventario (Producto, CodigoEquivalente, Lote), Compras, Solicitudes, Configuración.
- Incluye también los enums reales del proyecto (`TipoEmpleado`, `Rol`, `EstatusSolicitud`, `TipoMovimiento`, `OrigenMovimiento`) con sus valores exactos.

### `docs/API.md`
El documento más importante. Por cada Controller real, documenta cada endpoint con este formato:

```
### POST /api/solicitudes
Rol requerido: MEDICO
Descripcion: crea una nueva solicitud de medicamentos para el medico autenticado (el medicoId se toma del token, no se envia en el body).

Request body: SolicitudRequestDTO
Response (201): SolicitudResponseDTO

Ejemplo de request:
{ ... }

Ejemplo de response:
{ ... }

Posibles errores:
- 400 si el body no pasa las validaciones (ver formato de error en ERRORES.md)
- 404 si el area no existe
```

Los ejemplos de JSON deben tener valores realistas (nombres de medicamentos, cantidades, fechas), no placeholders vacíos como `"string"` o `0`. Cubre TODOS los endpoints que existan realmente en el código, no solo los que se mencionaron en los prompts originales — si agregaste algún endpoint adicional durante la implementación que no estaba pedido, inclúyelo también.

Organiza el documento por Controller, en este orden: Auth, Empleados, Usuarios, Areas, Categorías, Proveedores, Productos, Códigos equivalentes, Lotes, Compras, Solicitudes, Configuración.

### `docs/REGLAS_DE_NEGOCIO.md`
Explica en lenguaje natural (el agente de frontend necesita esto para saber qué botones/pantallas mostrar en cada momento, no solo qué endpoints existen):

- El diagrama de estados de `Solicitud` (`PENDIENTE → APROBADO/RECHAZADO → ENTREGADA_PARCIAL/ENTREGADA_COMPLETA`) y qué transiciones son válidas, con qué endpoint se dispara cada una.
- Qué significa que un médico solo vea sus propias solicitudes, y que el `medicoId`/`farmaceuticoId` nunca se manden en el body sino que se infieran del token.
- Qué es FEFO/FIFO en términos simples (para que el frontend pueda, por ejemplo, mostrar un toggle de configuración si el rol es ADMIN) y cómo se consulta/cambia (`GET`/`PUT /api/configuracion/{clave}`).
- Que `stockActual` de un producto es un valor calculado/cacheado que se actualiza automáticamente con cada compra y cada dispensación — el frontend no debe intentar editarlo directamente (no existe endpoint para eso).
- Que los lotes nunca se crean ni editan manualmente desde la API, solo se generan implícitamente al registrar una compra.

### `docs/ERRORES.md`
- La forma exacta del JSON de error (`ErrorResponseDTO` real, con sus campos).
- Tabla de códigos de status que puede devolver la API y en qué situación aparece cada uno (404, 400, 401, 403, 409, 500), basada en el `GlobalExceptionHandler` real.
- Un ejemplo de JSON real para un error de validación (con el mapa de campo→mensaje) y uno para un error simple (ej. 404).

---

No modifiques código de producción en esta tarea, solo agrega archivos dentro de `docs/`. Verifica que los archivos `.md` queden bien formados (encabezados, bloques de código con triple backtick) antes de hacer commit.

Commit:
```bash
git add docs/
git commit -m "docs: agregar documentacion de la API para consumo del agente de frontend"
```

No hagas `git push`.
