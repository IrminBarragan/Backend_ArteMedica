# Arranque local

## Requisitos

- Java 21
- PostgreSQL con una base de datos vacía llamada `artemedica_farmacia_db`

## Variables de entorno

| Variable | Obligatoria | Default | Para qué sirve |
|---|---|---|---|
| `DB_USERNAME` | sí | — | Usuario de PostgreSQL |
| `DB_PASSWORD` | sí | — | Contraseña de PostgreSQL |
| `JWT_SECRET` | en producción | valor de desarrollo | Clave de firma de los tokens |
| `JWT_EXPIRATION_MS` | no | `86400000` (24 h) | Vigencia del token |
| `CORS_ALLOWED_ORIGINS` | no | `http://localhost:5173` | Orígenes del frontend, separados por coma |
| `SEED_ADMIN_USERNAME` | no | `admin` | Usuario administrador inicial |
| `SEED_ADMIN_PASSWORD` | en producción | `Admin12345` | Contraseña del administrador inicial |
| `SEED_DEMO_ENABLED` | no | `true` | Sembrar catálogos y datos de demostración |

## Levantar la API

```bash
export DB_USERNAME=tu_usuario
export DB_PASSWORD=tu_password
./mvnw spring-boot:run
```

La API queda en `http://localhost:8080/api`.

## Qué se siembra al arrancar

`DataSeeder` (`bootstrap/DataSeeder.java`) corre en cada arranque y es idempotente: cada bloque
comprueba primero si sus datos ya existen, así que reiniciar no duplica nada.

**Siempre:**

1. La clave de configuración `INVENTARIO_FEFO_HABILITADO` con valor `"true"`.
2. Un empleado y un usuario administrador (`SEED_ADMIN_USERNAME` / `SEED_ADMIN_PASSWORD`).

Sin el paso 2 el sistema es inusable en una base limpia: crear usuarios requiere estar autenticado
como `ADMIN`, y para autenticarse tiene que existir ya un usuario.

**Solo si `SEED_DEMO_ENABLED=true` y la tabla `producto` está vacía:**

| Dato | Contenido |
|---|---|
| Áreas | Urgencias, Hospitalización, Quirófano |
| Categorías | Analgésicos, Antibióticos, Antihipertensivos |
| Proveedor | Distribuidora Farmacéutica del Norte |
| Productos | Paracetamol, Amoxicilina, Ketorolaco |
| Usuarios | `medico.demo` (rol MEDICO), `farmacia.demo` (rol FARMACEUTICO) |
| Compras | Dos facturas con cuatro lotes, registradas vía `CompraService` |

Los usuarios demo usan la misma contraseña de desarrollo que el administrador.

### Los lotes demo están diseñados para demostrar FEFO vs. FIFO

Las dos compras se registran por separado y a propósito, de modo que para el Paracetamol el orden
de entrada y el orden de caducidad sean opuestos:

| Lote | Entró | Caduca | Lo elige |
|---|---|---|---|
| `PAR-2024-A` | primero | a 18 meses | FIFO |
| `PAR-2025-B` | después | a 3 meses | FEFO |

Así, al dispensar una solicitud de Paracetamol, el lote consumido cambia según el valor de
`INVENTARIO_FEFO_HABILITADO` — el efecto del toggle es observable sin capturar datos a mano.

## Producción

Antes de desplegar fuera de tu máquina, define `JWT_SECRET` y `SEED_ADMIN_PASSWORD`, y pon
`SEED_DEMO_ENABLED=false`. El seeder emite una advertencia en el log si detecta que sigues usando
las credenciales de desarrollo.
