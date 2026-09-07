# ArteMédica Farmacia — Documentación de la API

ArteMédica Farmacia es un sistema de gestión de inventario para la farmacia de un hospital. Los médicos crean solicitudes de medicamentos, el personal de farmacia las aprueba, rechaza o dispensa, y cada movimiento de inventario (compras y dispensaciones) queda registrado en un kardex con trazabilidad de lote. La salida de inventario respeta un orden configurable FEFO (primero el lote que caduca antes) o FIFO (primero el lote que entró antes).

Esta carpeta está pensada para que un agente o desarrollador de frontend entienda cómo consumir la API **sin tener que leer el código Java**. Todo lo documentado aquí se generó leyendo el código real (Controllers, DTOs, enums, configuración de seguridad y manejo de errores), no la especificación original del proyecto.

**URL base de la API:** `/api` (todos los endpoints cuelgan de este prefijo, ej. `http://localhost:8080/api/productos`).

## Contenido de esta carpeta

- **[ARRANQUE.md](./ARRANQUE.md)** — Cómo levantar la API en local: variables de entorno, qué datos se siembran al arrancar y con qué credenciales entrar la primera vez.
- **[AUTENTICACION.md](./AUTENTICACION.md)** — Cómo hacer login, cómo usar el token JWT, expiración, qué pasa cuando el token falta o es inválido, y los 3 roles del sistema.
- **[MODELOS.md](./MODELOS.md)** — Todos los DTOs (request/response) reales del proyecto, campo por campo, con sus validaciones traducidas a lenguaje natural, y los enums del dominio.
- **[API.md](./API.md)** — El catálogo completo de endpoints, uno por uno, con rol requerido, body, respuesta, ejemplos de JSON realistas y posibles errores.
- **[REGLAS_DE_NEGOCIO.md](./REGLAS_DE_NEGOCIO.md)** — El diagrama de estados de una Solicitud, cómo funciona FEFO/FIFO, por qué `stockActual` no se edita directamente, y otras reglas que el frontend necesita para decidir qué mostrar y cuándo.
- **[ERRORES.md](./ERRORES.md)** — La forma exacta del JSON de error, la tabla completa de códigos de status y en qué situación aparece cada uno (incluye un caso importante y no obvio: los 403 por token ausente/inválido **no** traen JSON).
