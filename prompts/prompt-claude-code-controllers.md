Estás trabajando en el backend de **ArteMédica Farmacia** (Spring Boot + Spring Data JPA + PostgreSQL + Spring Security, Java 21). Ya existen entidades, repositories, DTOs (records) y services de las tareas anteriores, incluyendo `UsuarioService.autenticar()`, el patrón Strategy para FEFO/FIFO, y `SolicitudService` con `crear/aprobar/rechazar/dispensar`. En `SolicitudService`, `farmaceuticoId` se trata como id de `Empleado` (igual que `medicoId`), y `UsuarioRepository` ya tiene `findByEmpleadoId`.

Ahora vas a construir: autenticación real conectada a `Usuario`, el `GlobalExceptionHandler`, y los `Controller` REST. Trabaja las 5 tareas en orden y **haz un commit al terminar cada una**. Verifica que compile (`mvn compile` o `./mvnw compile`) antes de cada commit.

### Convenciones generales

- Todos los endpoints bajo `/api/...`.
- Los Controllers reciben y devuelven DTOs (records), nunca entidades.
- Usa `@Valid @RequestBody` en todos los endpoints que reciban un DTO de request, para que se dispare la validación de Bean Validation ya definida en los DTOs.
- Usa `ResponseEntity<T>` con el código HTTP correcto: `201 Created` al crear (con `Location` header cuando aplique), `200 OK` para lecturas y actualizaciones, `204 No Content` para bajas lógicas.
- Protege los endpoints con `@PreAuthorize("hasRole('...')")` según se indica en cada Controller. Si un método no especifica rol, solo requiere estar autenticado (cualquier rol válido).

---

### TAREA 1 — Autenticación real conectada a `Usuario`, con JWT

Ahora mismo el proyecto probablemente tiene credenciales fijas de prueba en `application.properties` (`spring.security.user.name` / `spring.security.user.password`) de una etapa anterior. Elimina esas líneas — ya no aplican, vamos a autenticar contra la tabla `Usuario` de verdad y a emitir un JWT real.

**Dependencia**: agrega a `pom.xml` (revisa primero si el proyecto ya trae alguna librería de JWT antes de duplicar):
```xml
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.12.6</version>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>0.12.6</version>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <version>0.12.6</version>
    <scope>runtime</scope>
</dependency>
```

**Propiedades** en `application.properties` (usa variables de entorno para el secreto, no lo dejes en texto plano en el repo):
```properties
app.jwt.secret=${JWT_SECRET:cambia-este-valor-por-uno-largo-y-aleatorio-solo-para-desarrollo-local}
app.jwt.expiration-ms=${JWT_EXPIRATION_MS:86400000}
```

Crea en el paquete `security`:

**`UsuarioPrincipal`** (implementa `UserDetails`):
```java
public class UsuarioPrincipal implements UserDetails {
    private final Usuario usuario;

    public UsuarioPrincipal(Usuario usuario) { this.usuario = usuario; }

    public Long getEmpleadoId() { return usuario.getEmpleado().getId(); }
    public Long getUsuarioId() { return usuario.getId(); }
    public Rol getRol() { return usuario.getRol(); }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + usuario.getRol().name()));
    }

    @Override public String getPassword() { return usuario.getPassword(); }
    @Override public String getUsername() { return usuario.getUsername(); }
    @Override public boolean isEnabled() { return usuario.isActivo(); }
    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
}
```

**`UsuarioDetailsService`** (implementa `UserDetailsService`):
```java
@Service
public class UsuarioDetailsService implements UserDetailsService {
    private final UsuarioRepository usuarioRepository;

    public UsuarioDetailsService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.findByUsernameAndActivoTrue(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado o inactivo: " + username));
        return new UsuarioPrincipal(usuario);
    }
}
```

**`JwtService`** (genera y valida tokens):
```java
@Component
public class JwtService {

    @Value("${app.jwt.secret}")
    private String secret;

    @Value("${app.jwt.expiration-ms}")
    private long expirationMs;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generarToken(UsuarioPrincipal principal) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(principal.getUsername())
                .claim("rol", principal.getRol().name())
                .claim("empleadoId", principal.getEmpleadoId())
                .claim("usuarioId", principal.getUsuarioId())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(expirationMs)))
                .signWith(getSigningKey())
                .compact();
    }

    public String extraerUsername(String token) {
        return extraerClaims(token).getSubject();
    }

    public boolean esValido(String token, UserDetails userDetails) {
        String username = extraerUsername(token);
        return username.equals(userDetails.getUsername()) && !estaExpirado(token);
    }

    private boolean estaExpirado(String token) {
        return extraerClaims(token).getExpiration().before(new Date());
    }

    private Claims extraerClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public long getExpirationMs() { return expirationMs; }
}
```

Ajusta los nombres de métodos/imports de `jjwt` si la versión que termina resolviendo Maven difiere ligeramente en la API (0.12.x usa el estilo builder de arriba; si Maven resuelve una versión distinta, adapta la sintaxis a esa versión pero mantén el mismo comportamiento).

**`JwtAuthenticationFilter`** (`extends OncePerRequestFilter`):
```java
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UsuarioDetailsService usuarioDetailsService;

    public JwtAuthenticationFilter(JwtService jwtService, UsuarioDetailsService usuarioDetailsService) {
        this.jwtService = jwtService;
        this.usuarioDetailsService = usuarioDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = header.substring(7);
        try {
            String username = jwtService.extraerUsername(token);
            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = usuarioDetailsService.loadUserByUsername(username);
                if (jwtService.esValido(token, userDetails)) {
                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (JwtException | UsernameNotFoundException e) {
            // Token invalido, expirado o usuario no encontrado: no se autentica, la cadena de filtros sigue
            // y Spring Security devolvera 401 mas adelante si el endpoint requiere autenticacion.
        }

        filterChain.doFilter(request, response);
    }
}
```

Actualiza `SecurityConfig` (la clase que ya existe con el bean `PasswordEncoder`):
- Agrega `@EnableMethodSecurity` a nivel de clase, para que `@PreAuthorize` funcione en los Controllers.
- Configura el `SecurityFilterChain` con: CSRF deshabilitado (API stateless), `sessionManagement` en `STATELESS`, **sin** `httpBasic()` ni `formLogin()`, y agrega `JwtAuthenticationFilter` antes de `UsernamePasswordAuthenticationFilter` con `.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)`.
- En `authorizeHttpRequests`: `permitAll()` para `POST /api/auth/login`, todo lo demás `anyRequest().authenticated()`.
- Expón el bean `AuthenticationManager` a partir de `AuthenticationConfiguration` (necesario para que `UsuarioService.autenticar()` lo use, si no lo usa ya directamente con el `PasswordEncoder`).

**Actualiza el record `LoginResponseDTO`** (creado en la tarea de DTOs) para incluir el token:
```java
public record LoginResponseDTO(String token, String tipo, String username, Rol rol, Long empleadoId, long expiresIn) {}
```
(`tipo` siempre será el literal `"Bearer"`.)

Crea `AuthController`:
```java
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    // POST /api/auth/login → recibe LoginRequestDTO (@Valid), usa usuarioService.autenticar(...) para validar credenciales,
    // genera el token con jwtService.generarToken(...), y arma el LoginResponseDTO con token, "Bearer", username, rol, empleadoId, expiresIn.
    // No requiere autenticacion previa (ya esta en permitAll en SecurityConfig).
}
```

A partir de aquí, el frontend debe mandar `Authorization: Bearer <token>` en cada request subsecuente — ya no hay usuario/contraseña en cada llamada.

Commit:
```bash
git add .
git commit -m "feat: implementar autenticacion JWT (JwtService, JwtAuthenticationFilter, AuthController)"
```

---

### TAREA 2 — `ErrorResponseDTO` y `GlobalExceptionHandler`

Crea el DTO de error en `dto`:
```java
public record ErrorResponseDTO(
    String mensaje,
    int status,
    LocalDateTime timestamp,
    Map<String, String> errores
) {}
```
(`errores` va vacío/null salvo en errores de validación, donde lleva `campo → mensaje`.)

Crea `GlobalExceptionHandler` en el paquete `exception`, anotado `@RestControllerAdvice`, con un `@ExceptionHandler` por cada uno de estos casos:

| Excepción | Status HTTP |
|---|---|
| `ResourceNotFoundException` | 404 |
| `StockInsuficienteException` | 409 |
| `EstadoInvalidoException` | 409 |
| `ConflictoConcurrenciaException` | 409 |
| `AutenticacionException` | 401 |
| `ExpiredJwtException` / `JwtException` (de la librería `jjwt`, por si algún flujo la deja escapar fuera del filtro) | 401 |
| `MethodArgumentNotValidException` (validación de `@Valid`) | 400, con el mapa `errores` lleno campo por campo usando `bindingResult.getFieldErrors()` |
| `AccessDeniedException` (de Spring Security, cuando `@PreAuthorize` rechaza) | 403 |
| `Exception` genérica (catch-all) | 500, con mensaje genérico ("Ocurrió un error inesperado") — no expongas el stacktrace ni el mensaje interno de la excepción original en la respuesta |

Cada handler arma un `ErrorResponseDTO` y lo devuelve envuelto en `ResponseEntity` con el status correspondiente.

Commit:
```bash
git add .
git commit -m "feat: agregar ErrorResponseDTO y GlobalExceptionHandler"
```

---

### TAREA 3 — Controllers de catálogo

Crea, en el paquete `controller`:

**`EmpleadoController`** (`/api/empleados`) — CRUD completo, protegido con `@PreAuthorize("hasRole('ADMIN')")` en crear/actualizar/desactivar; `GET` abierto a cualquier rol autenticado.

**`UsuarioController`** (`/api/usuarios`) — crear/listar/desactivar, todo `@PreAuthorize("hasRole('ADMIN')")` (nadie más debe poder crear usuarios).

**`AreaController`** (`/api/areas`) — CRUD, escritura solo `ADMIN`, lectura abierta a autenticados.

**`CategoriaMedicamentoController`** (`/api/categorias`) — igual patrón que `AreaController`.

**`ProveedorController`** (`/api/proveedores`) — escritura `ADMIN` o `FARMACEUTICO`, lectura abierta a autenticados.

Commit:
```bash
git add .
git commit -m "feat: agregar controllers de catalogo (Empleado, Usuario, Area, CategoriaMedicamento, Proveedor)"
```

---

### TAREA 4 — Controllers de inventario y compras

**`ProductoController`** (`/api/productos`):
- CRUD estándar, escritura `ADMIN`/`FARMACEUTICO`, lectura abierta a autenticados (incluye `MEDICO`, que necesita ver el catálogo para armar sus solicitudes).
- `GET /api/productos/stock-bajo` → `listarStockBajo()`, cualquier autenticado.

**`CodigoEquivalenteController`** (`/api/codigos-equivalentes`) — crear/listar por producto/desactivar, `ADMIN`/`FARMACEUTICO`.

**`LoteController`** (`/api/lotes`) — **solo lectura**, no expongas `POST`/`PUT` (recuerda: los lotes solo se crean implícitamente desde `CompraService`):
- `GET /api/lotes/producto/{productoId}` → lotes disponibles de ese producto.
- `GET /api/lotes/vencidos`
- `GET /api/lotes/por-vencer`

Todo abierto a cualquier autenticado.

**`CompraController`** (`/api/compras`):
- `POST /api/compras` → `@PreAuthorize("hasRole('ADMIN') or hasRole('FARMACEUTICO')")`. Recibe `CompraRequestDTO`, obtiene el `usuarioId` del `@AuthenticationPrincipal UsuarioPrincipal principal` (usa `principal.getUsuarioId()`), y llama `compraService.registrarCompra(dto, usuarioId)`.
- `GET /api/compras/{id}` y `GET /api/compras` (con filtro opcional por proveedor/rango de fechas si ya existen esos métodos en el repository) → abierto a `ADMIN`/`FARMACEUTICO`.

Commit:
```bash
git add .
git commit -m "feat: agregar controllers de inventario y compras"
```

---

### TAREA 5 — Controller de solicitudes y de configuración

**`SolicitudController`** (`/api/solicitudes`):
- `POST /api/solicitudes` → `@PreAuthorize("hasRole('MEDICO')")`. El `medicoId` **no viene en el body**, se obtiene de `@AuthenticationPrincipal UsuarioPrincipal principal` con `principal.getEmpleadoId()` — un médico solo puede crear solicitudes a su propio nombre, nunca a nombre de otro.
- `PUT /api/solicitudes/{id}/aprobar` → `@PreAuthorize("hasRole('FARMACEUTICO') or hasRole('ADMIN')")`. Recibe `AprobarSolicitudRequestDTO`, usa `principal.getEmpleadoId()` como `farmaceuticoId`.
- `PUT /api/solicitudes/{id}/rechazar` → mismos roles, recibe `RechazarSolicitudRequestDTO`.
- `PUT /api/solicitudes/{id}/dispensar` → mismos roles, sin body, usa `principal.getEmpleadoId()` como `farmaceuticoId`.
- `GET /api/solicitudes/{id}` → abierto a cualquier autenticado.
- `GET /api/solicitudes?estatus=PENDIENTE` (filtro opcional por query param usando `EstatusSolicitud`) → abierto a cualquier autenticado. Si el usuario autenticado tiene rol `MEDICO`, filtra además por su propio `empleadoId` (un médico solo debe ver sus propias solicitudes, no las de otros médicos) — `ADMIN` y `FARMACEUTICO` ven todas.

**`ConfiguracionController`** (`/api/configuracion`) — para el toggle de FEFO que se agregó en la tarea de repositories:
- `GET /api/configuracion/{clave}` → devuelve el valor actual, abierto a `ADMIN`/`FARMACEUTICO`.
- `PUT /api/configuracion/{clave}` → recibe un DTO simple `{ "valor": "true" }` (créalo como record si no existe), actualiza `ConfiguracionSistema` (usa `configuracionSistemaRepository`, no hace falta un Service dedicado si es una operación tan simple, pero créalo si prefieres mantener la capa de Service consistente), `@PreAuthorize("hasRole('ADMIN')")` — solo un administrador debe poder cambiar esto en producción.

Commit:
```bash
git add .
git commit -m "feat: agregar controllers de solicitudes y configuracion del sistema"
```

---

No hagas `git push`. Deja los commits listos localmente para revisión.
