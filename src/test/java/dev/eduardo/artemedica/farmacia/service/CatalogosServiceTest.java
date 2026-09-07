package dev.eduardo.artemedica.farmacia.service;

import dev.eduardo.artemedica.farmacia.dto.ProductoRequestDTO;
import dev.eduardo.artemedica.farmacia.dto.UsuarioRequestDTO;
import dev.eduardo.artemedica.farmacia.exception.AutenticacionException;
import dev.eduardo.artemedica.farmacia.exception.ResourceNotFoundException;
import dev.eduardo.artemedica.farmacia.model.CategoriaMedicamento;
import dev.eduardo.artemedica.farmacia.model.Empleado;
import dev.eduardo.artemedica.farmacia.model.Producto;
import dev.eduardo.artemedica.farmacia.model.Usuario;
import dev.eduardo.artemedica.farmacia.model.enums.Rol;
import dev.eduardo.artemedica.farmacia.repository.CategoriaMedicamentoRepository;
import dev.eduardo.artemedica.farmacia.repository.EmpleadoRepository;
import dev.eduardo.artemedica.farmacia.repository.ProductoRepository;
import dev.eduardo.artemedica.farmacia.repository.UsuarioRepository;
import dev.eduardo.artemedica.farmacia.service.impl.ProductoServiceImpl;
import dev.eduardo.artemedica.farmacia.service.impl.UsuarioServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@DisplayName("Catalogos")
class CatalogosServiceTest {

    @Nested
    @ExtendWith(MockitoExtension.class)
    @DisplayName("Usuarios")
    class Usuarios {

        @Mock private UsuarioRepository usuarioRepository;
        @Mock private EmpleadoRepository empleadoRepository;

        // Se usa el codificador real, no un mock: lo que interesa comprobar es justamente que
        // la contrasena queda cifrada y que la verificacion contra el hash funciona.
        private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

        private UsuarioServiceImpl service;
        private Empleado empleado;

        @BeforeEach
        void prepararServicio() {
            service = new UsuarioServiceImpl(usuarioRepository, empleadoRepository, passwordEncoder);
            empleado = Empleado.builder().id(1L).nombres("Carlos").apellidoPaterno("Vega").build();
        }

        @Test
        @DisplayName("la contrasena nunca se guarda en claro")
        void contrasenaSeGuardaCifrada() {
            when(empleadoRepository.findById(1L)).thenReturn(Optional.of(empleado));
            when(usuarioRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.crear(new UsuarioRequestDTO("farmacia", "secreto123", 1L, Rol.FARMACEUTICO));

            ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
            org.mockito.Mockito.verify(usuarioRepository).save(captor.capture());
            String guardada = captor.getValue().getPassword();

            assertThat(guardada).isNotEqualTo("secreto123");
            assertThat(guardada).startsWith("$2");
            assertThat(passwordEncoder.matches("secreto123", guardada)).isTrue();
        }

        @Test
        @DisplayName("dos usuarios con la misma contrasena producen hashes distintos")
        void mismaContrasenaDistintoHash() {
            when(empleadoRepository.findById(1L)).thenReturn(Optional.of(empleado));
            when(usuarioRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
            service.crear(new UsuarioRequestDTO("uno", "misma123", 1L, Rol.MEDICO));
            service.crear(new UsuarioRequestDTO("dos", "misma123", 1L, Rol.MEDICO));
            org.mockito.Mockito.verify(usuarioRepository, org.mockito.Mockito.times(2)).save(captor.capture());

            // BCrypt genera una sal aleatoria por hash: por eso se verifica con matches y
            // nunca comparando cadenas.
            assertThat(captor.getAllValues().get(0).getPassword())
                    .isNotEqualTo(captor.getAllValues().get(1).getPassword());
        }

        @Test
        @DisplayName("autenticar acepta la contrasena correcta y rechaza la incorrecta")
        void autenticacion() {
            Usuario usuario = Usuario.builder().id(1L).username("farmacia")
                    .password(passwordEncoder.encode("secreto123"))
                    .empleado(empleado).rol(Rol.FARMACEUTICO).activo(true).build();
            when(usuarioRepository.findByUsernameAndActivoTrue("farmacia")).thenReturn(Optional.of(usuario));

            service.autenticar("farmacia", "secreto123");

            assertThatThrownBy(() -> service.autenticar("farmacia", "equivocada"))
                    .isInstanceOf(AutenticacionException.class);
        }

        @Test
        @DisplayName("un usuario inactivo no puede autenticarse")
        void usuarioInactivoNoAutentica() {
            when(usuarioRepository.findByUsernameAndActivoTrue("baja")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.autenticar("baja", "loQueSea"))
                    .isInstanceOf(AutenticacionException.class);
        }

        @Test
        @DisplayName("el mensaje de error no revela si el usuario existe")
        void mensajeNoRevelaExistencia() {
            Usuario usuario = Usuario.builder().username("existe")
                    .password(passwordEncoder.encode("correcta")).empleado(empleado).activo(true).build();
            when(usuarioRepository.findByUsernameAndActivoTrue("existe")).thenReturn(Optional.of(usuario));
            when(usuarioRepository.findByUsernameAndActivoTrue("noexiste")).thenReturn(Optional.empty());

            String conUsuarioReal = catchMensaje(() -> service.autenticar("existe", "mala"));
            String sinUsuario = catchMensaje(() -> service.autenticar("noexiste", "mala"));

            // Mensajes distintos permitirian enumerar usuarios validos probando nombres.
            assertThat(conUsuarioReal).isEqualTo(sinUsuario);
        }

        private String catchMensaje(Runnable accion) {
            try {
                accion.run();
                return null;
            } catch (RuntimeException e) {
                return e.getMessage();
            }
        }

        @Test
        @DisplayName("desactivar marca inactivo sin borrar el registro")
        void desactivarEsBajaLogica() {
            Usuario usuario = Usuario.builder().id(9L).username("x").empleado(empleado).activo(true).build();
            when(usuarioRepository.findById(9L)).thenReturn(Optional.of(usuario));
            when(usuarioRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.desactivar(9L);

            assertThat(usuario.isActivo()).isFalse();
            org.mockito.Mockito.verify(usuarioRepository, org.mockito.Mockito.never()).delete(any());
        }
    }

    @Nested
    @ExtendWith(MockitoExtension.class)
    @DisplayName("Productos")
    class Productos {

        @Mock private ProductoRepository productoRepository;
        @Mock private CategoriaMedicamentoRepository categoriaRepository;

        @InjectMocks private ProductoServiceImpl service;

        private CategoriaMedicamento categoria;

        @BeforeEach
        void prepararDatos() {
            categoria = CategoriaMedicamento.builder().id(1L).nombre("Analgesicos").activo(true).build();
        }

        private ProductoRequestDTO dto() {
            return new ProductoRequestDTO("Paracetamol", "Tabletas 500 mg", "750100001", false,
                    1L, new BigDecimal("2.50"), new BigDecimal("1.20"), 50);
        }

        @Test
        @DisplayName("un producto nuevo arranca con stock en cero")
        void productoNuevoArrancaEnCero() {
            when(categoriaRepository.findById(1L)).thenReturn(Optional.of(categoria));
            when(productoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            ArgumentCaptor<Producto> captor = ArgumentCaptor.forClass(Producto.class);
            service.crear(dto());
            org.mockito.Mockito.verify(productoRepository).save(captor.capture());

            // El stock solo se mueve por compras, dispensaciones, mermas y ajustes:
            // no se puede fijar al dar de alta el producto.
            assertThat(captor.getValue().getStockActual()).isZero();
        }

        @Test
        @DisplayName("actualizar un producto no altera su stock")
        void actualizarNoTocaElStock() {
            Producto existente = Producto.builder().id(5L).nombre("Viejo").presentacion("X")
                    .categoria(categoria).stockActual(137).stockMinimo(10).activo(true).build();
            when(productoRepository.findById(5L)).thenReturn(Optional.of(existente));
            when(categoriaRepository.findById(1L)).thenReturn(Optional.of(categoria));
            when(productoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.actualizar(5L, dto());

            assertThat(existente.getStockActual()).isEqualTo(137);
            assertThat(existente.getNombre()).isEqualTo("Paracetamol");
        }

        @Test
        @DisplayName("desactivar es baja logica, no borrado")
        void desactivarEsBajaLogica() {
            Producto producto = Producto.builder().id(5L).nombre("X").activo(true).build();
            when(productoRepository.findById(5L)).thenReturn(Optional.of(producto));
            when(productoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.desactivar(5L);

            assertThat(producto.isActivo()).isFalse();
            org.mockito.Mockito.verify(productoRepository, org.mockito.Mockito.never()).delete(any());
        }

        @Test
        @DisplayName("un producto inexistente da error de recurso no encontrado")
        void productoInexistente() {
            when(productoRepository.findById(404L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.obtenerPorId(404L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("el listado de stock bajo delega en la consulta que compara contra el minimo")
        void stockBajo() {
            when(productoRepository.findProductosStockBajo()).thenReturn(List.of(
                    Producto.builder().id(1L).nombre("Paracetamol").categoria(categoria)
                            .stockActual(5).stockMinimo(50).activo(true).build()));

            assertThat(service.listarStockBajo()).hasSize(1);
        }
    }
}
