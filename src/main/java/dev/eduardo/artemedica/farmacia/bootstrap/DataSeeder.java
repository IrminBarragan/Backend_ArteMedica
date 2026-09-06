package dev.eduardo.artemedica.farmacia.bootstrap;

import dev.eduardo.artemedica.farmacia.dto.CompraDetalleRequestDTO;
import dev.eduardo.artemedica.farmacia.dto.CompraRequestDTO;
import dev.eduardo.artemedica.farmacia.model.Area;
import dev.eduardo.artemedica.farmacia.model.CategoriaMedicamento;
import dev.eduardo.artemedica.farmacia.model.ConfiguracionSistema;
import dev.eduardo.artemedica.farmacia.model.Empleado;
import dev.eduardo.artemedica.farmacia.model.Producto;
import dev.eduardo.artemedica.farmacia.model.Proveedor;
import dev.eduardo.artemedica.farmacia.model.Usuario;
import dev.eduardo.artemedica.farmacia.model.enums.Rol;
import dev.eduardo.artemedica.farmacia.model.enums.TipoEmpleado;
import dev.eduardo.artemedica.farmacia.repository.AreaRepository;
import dev.eduardo.artemedica.farmacia.repository.CategoriaMedicamentoRepository;
import dev.eduardo.artemedica.farmacia.repository.ConfiguracionSistemaRepository;
import dev.eduardo.artemedica.farmacia.repository.EmpleadoRepository;
import dev.eduardo.artemedica.farmacia.repository.ProductoRepository;
import dev.eduardo.artemedica.farmacia.repository.ProveedorRepository;
import dev.eduardo.artemedica.farmacia.repository.UsuarioRepository;
import dev.eduardo.artemedica.farmacia.service.CompraService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Siembra los datos mínimos para que el sistema sea usable en un arranque limpio.
 *
 * Sin esto existe un bloqueo circular: crear usuarios requiere estar autenticado como ADMIN,
 * pero para autenticarse hace falta que ya exista un usuario en la base de datos.
 *
 * El seeder es idempotente: cada bloque comprueba primero si sus datos ya existen, así que
 * puede ejecutarse en cada arranque sin duplicar nada.
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    /** Password de conveniencia para desarrollo local. Si sigue en uso se emite una advertencia. */
    private static final String PASSWORD_DEV_POR_DEFECTO = "Admin12345";

    private static final String CLAVE_FEFO = "INVENTARIO_FEFO_HABILITADO";

    private final UsuarioRepository usuarioRepository;
    private final EmpleadoRepository empleadoRepository;
    private final AreaRepository areaRepository;
    private final CategoriaMedicamentoRepository categoriaRepository;
    private final ConfiguracionSistemaRepository configuracionRepository;
    private final ProveedorRepository proveedorRepository;
    private final ProductoRepository productoRepository;
    private final CompraService compraService;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.seed.admin.username}")
    private String adminUsername;

    @Value("${app.seed.admin.password}")
    private String adminPassword;

    @Value("${app.seed.demo.enabled}")
    private boolean demoHabilitado;

    public DataSeeder(UsuarioRepository usuarioRepository,
                      EmpleadoRepository empleadoRepository,
                      AreaRepository areaRepository,
                      CategoriaMedicamentoRepository categoriaRepository,
                      ConfiguracionSistemaRepository configuracionRepository,
                      ProveedorRepository proveedorRepository,
                      ProductoRepository productoRepository,
                      CompraService compraService,
                      PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.empleadoRepository = empleadoRepository;
        this.areaRepository = areaRepository;
        this.categoriaRepository = categoriaRepository;
        this.configuracionRepository = configuracionRepository;
        this.proveedorRepository = proveedorRepository;
        this.productoRepository = productoRepository;
        this.compraService = compraService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        sembrarConfiguracion();
        sembrarAdmin();
        if (demoHabilitado) {
            sembrarDatosDemo();
        } else {
            log.info("Seed de datos demo deshabilitado (app.seed.demo.enabled=false).");
        }
    }

    // ---------------------------------------------------------------------
    // Configuración del sistema
    // ---------------------------------------------------------------------

    /**
     * Crea los parámetros configurables que el sistema espera encontrar en base de datos.
     *
     * Sin la clave FEFO el resolver de estrategias cae en su valor por defecto y el endpoint
     * GET /api/configuracion/{clave} responde 404, así que un ADMIN no puede cambiar el toggle.
     */
    private void sembrarConfiguracion() {
        if (configuracionRepository.findByClave(CLAVE_FEFO).isPresent()) {
            return;
        }
        configuracionRepository.save(ConfiguracionSistema.builder()
                .clave(CLAVE_FEFO)
                .valor("true")
                .descripcion("Si esta en true, las dispensaciones descuentan primero del lote con fecha de "
                        + "caducidad mas proxima (FEFO). Si esta en false, se usa FIFO por fecha de ingreso.")
                .updatedAt(LocalDateTime.now())
                .updatedBy("system")
                .build());
        log.info("Configuracion '{}' creada con valor por defecto 'true' (FEFO habilitado).", CLAVE_FEFO);
    }

    // ---------------------------------------------------------------------
    // Administrador inicial
    // ---------------------------------------------------------------------

    private void sembrarAdmin() {
        if (usuarioRepository.findByUsername(adminUsername).isPresent()) {
            log.info("El usuario administrador '{}' ya existe, no se vuelve a crear.", adminUsername);
            return;
        }

        Empleado empleado = empleadoRepository.save(Empleado.builder()
                .nombres("Administrador")
                .apellidoPaterno("del")
                .apellidoMaterno("Sistema")
                .tipo(TipoEmpleado.ADMIN)
                .activo(true)
                .build());

        crearUsuario(adminUsername, adminPassword, empleado, Rol.ADMIN);

        log.info("Usuario administrador '{}' creado.", adminUsername);
        if (PASSWORD_DEV_POR_DEFECTO.equals(adminPassword)) {
            log.warn("ATENCION: el administrador se creo con la contrasena por defecto de desarrollo. "
                    + "Define la variable de entorno SEED_ADMIN_PASSWORD antes de desplegar.");
        }
    }

    // ---------------------------------------------------------------------
    // Datos de demostración
    // ---------------------------------------------------------------------

    private void sembrarDatosDemo() {
        if (productoRepository.count() > 0) {
            log.info("Ya existen productos en la base de datos, se omite el seed de datos demo.");
            return;
        }

        log.info("Sembrando datos de demostracion...");

        Area urgencias = guardarArea("Urgencias", "Area de atencion de urgencias medicas");
        guardarArea("Hospitalizacion", "Pisos de hospitalizacion general");
        guardarArea("Quirofano", "Area quirurgica");

        CategoriaMedicamento analgesicos = guardarCategoria("Analgesicos", "Medicamentos para el manejo del dolor");
        CategoriaMedicamento antibioticos = guardarCategoria("Antibioticos", "Medicamentos contra infecciones bacterianas");
        guardarCategoria("Antihipertensivos", "Medicamentos para el control de la presion arterial");

        Proveedor proveedor = proveedorRepository.save(Proveedor.builder()
                .nombre("Distribuidora Farmaceutica del Norte")
                .direccion("Av. Reforma 1234, Col. Centro")
                .telefono("8181234567")
                .correo("ventas@dfnorte.example")
                .activo(true)
                .build());

        Producto paracetamol = guardarProducto("Paracetamol", "Tabletas 500 mg", "7501000000011",
                analgesicos, new BigDecimal("2.50"), new BigDecimal("1.20"), 50);
        Producto amoxicilina = guardarProducto("Amoxicilina", "Capsulas 500 mg", "7501000000028",
                antibioticos, new BigDecimal("8.00"), new BigDecimal("4.50"), 30);
        Producto ketorolaco = guardarProducto("Ketorolaco", "Solucion inyectable 30 mg/ml", "7501000000035",
                analgesicos, new BigDecimal("15.00"), new BigDecimal("9.00"), 20);

        Empleado medico = empleadoRepository.save(Empleado.builder()
                .nombres("Laura")
                .apellidoPaterno("Mendoza")
                .apellidoMaterno("Rios")
                .tipo(TipoEmpleado.MEDICO)
                .especialidad("Medicina interna")
                .cedulaProfesional("12345678")
                .telefonoGuardia("8189876543")
                .activo(true)
                .build());
        crearUsuario("medico.demo", PASSWORD_DEV_POR_DEFECTO, medico, Rol.MEDICO);

        Empleado farmaceutico = empleadoRepository.save(Empleado.builder()
                .nombres("Carlos")
                .apellidoPaterno("Vega")
                .apellidoMaterno("Ortiz")
                .tipo(TipoEmpleado.FARMACEUTICO)
                .cedulaProfesional("87654321")
                .activo(true)
                .build());
        Usuario usuarioFarmaceutico = crearUsuario("farmacia.demo", PASSWORD_DEV_POR_DEFECTO, farmaceutico, Rol.FARMACEUTICO);

        sembrarComprasDemo(proveedor, usuarioFarmaceutico, paracetamol, amoxicilina, ketorolaco);

        log.info("Datos demo sembrados. Area de ejemplo para solicitudes: '{}' (id={}).",
                urgencias.getNombre(), urgencias.getId());
        log.warn("Los usuarios demo ('medico.demo', 'farmacia.demo') usan la contrasena de desarrollo. "
                + "Desactiva el seed demo con SEED_DEMO_ENABLED=false fuera de tu entorno local.");
    }

    /**
     * Registra dos compras separadas del mismo producto para que FEFO y FIFO den resultados distintos:
     * el lote que entra primero (FIFO) es el de caducidad más lejana, y el que entra después es el que
     * caduca antes (FEFO). Así el efecto del toggle INVENTARIO_FEFO_HABILITADO es observable al dispensar.
     */
    private void sembrarComprasDemo(Proveedor proveedor, Usuario usuarioRegistro,
                                    Producto paracetamol, Producto amoxicilina, Producto ketorolaco) {
        LocalDate hoy = LocalDate.now();

        compraService.registrarCompra(new CompraRequestDTO(
                proveedor.getId(),
                "FAC-DEMO-001",
                hoy.minusDays(30),
                List.of(
                        new CompraDetalleRequestDTO(paracetamol.getId(), "PAR-2024-A",
                                hoy.plusMonths(18), 100, new BigDecimal("1.20")),
                        new CompraDetalleRequestDTO(amoxicilina.getId(), "AMX-2024-A",
                                hoy.plusMonths(12), 60, new BigDecimal("4.50"))
                )
        ), usuarioRegistro.getId());

        compraService.registrarCompra(new CompraRequestDTO(
                proveedor.getId(),
                "FAC-DEMO-002",
                hoy.minusDays(5),
                List.of(
                        // Entra después que PAR-2024-A pero caduca mucho antes: es el lote que FEFO elige primero.
                        new CompraDetalleRequestDTO(paracetamol.getId(), "PAR-2025-B",
                                hoy.plusMonths(3), 40, new BigDecimal("1.35")),
                        new CompraDetalleRequestDTO(ketorolaco.getId(), "KET-2025-A",
                                hoy.plusMonths(9), 25, new BigDecimal("9.00"))
                )
        ), usuarioRegistro.getId());
    }

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------

    private Usuario crearUsuario(String username, String password, Empleado empleado, Rol rol) {
        LocalDateTime ahora = LocalDateTime.now();
        return usuarioRepository.save(Usuario.builder()
                .username(username)
                .password(passwordEncoder.encode(password))
                .empleado(empleado)
                .rol(rol)
                .activo(true)
                .createdAt(ahora)
                .updatedAt(ahora)
                .build());
    }

    private Area guardarArea(String nombre, String descripcion) {
        return areaRepository.save(Area.builder()
                .nombre(nombre)
                .descripcion(descripcion)
                .activo(true)
                .build());
    }

    private CategoriaMedicamento guardarCategoria(String nombre, String descripcion) {
        return categoriaRepository.save(CategoriaMedicamento.builder()
                .nombre(nombre)
                .descripcion(descripcion)
                .activo(true)
                .build());
    }

    private Producto guardarProducto(String nombre, String presentacion, String codigoBarras,
                                     CategoriaMedicamento categoria, BigDecimal precioVenta,
                                     BigDecimal precioCompra, int stockMinimo) {
        return productoRepository.save(Producto.builder()
                .nombre(nombre)
                .presentacion(presentacion)
                .codigoBarras(codigoBarras)
                .esControlado(false)
                .categoria(categoria)
                .precioVenta(precioVenta)
                .precioCompra(precioCompra)
                .stockMinimo(stockMinimo)
                .stockActual(0)
                .activo(true)
                .build());
    }
}
