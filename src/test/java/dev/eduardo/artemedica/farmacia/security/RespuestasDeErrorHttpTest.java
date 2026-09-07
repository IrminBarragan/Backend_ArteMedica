package dev.eduardo.artemedica.farmacia.security;

import dev.eduardo.artemedica.farmacia.model.Empleado;
import dev.eduardo.artemedica.farmacia.model.Usuario;
import dev.eduardo.artemedica.farmacia.model.enums.Rol;
import dev.eduardo.artemedica.farmacia.model.enums.TipoEmpleado;
import dev.eduardo.artemedica.farmacia.repository.EmpleadoRepository;
import dev.eduardo.artemedica.farmacia.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifica que la API responde siempre con el mismo contrato de error (ErrorResponseDTO en JSON),
 * incluso cuando el fallo ocurre en la cadena de filtros de seguridad y no llega a un controller.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("Respuestas de error HTTP")
class RespuestasDeErrorHttpTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private EmpleadoRepository empleadoRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    private String tokenMedico;

    @BeforeEach
    void prepararUsuarioMedico() {
        Empleado empleado = empleadoRepository.save(Empleado.builder()
                .nombres("Ana").apellidoPaterno("Perez")
                .tipo(TipoEmpleado.MEDICO).activo(true).build());
        Usuario usuario = usuarioRepository.save(Usuario.builder()
                .username("medico.test").password(passwordEncoder.encode("secreto123"))
                .empleado(empleado).rol(Rol.MEDICO).activo(true).build());
        tokenMedico = jwtService.generarToken(new UsuarioPrincipal(usuario));
    }

    @Test
    @DisplayName("sin token responde 401 con cuerpo JSON, no 403 vacio")
    void sinTokenResponde401ConJson() throws Exception {
        mockMvc.perform(get("/api/productos"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.mensaje").isNotEmpty())
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    @Test
    @DisplayName("con token invalido responde 401 con cuerpo JSON")
    void tokenInvalidoResponde401ConJson() throws Exception {
        mockMvc.perform(get("/api/productos").header("Authorization", "Bearer token.falso.aqui"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("un rol sin permiso responde 403 con cuerpo JSON")
    void rolSinPermisoResponde403ConJson() throws Exception {
        // Un MEDICO no puede dar de baja productos: solo ADMIN o FARMACEUTICO.
        // Se usa un endpoint sin cuerpo a proposito: en Spring la validacion de los argumentos
        // corre antes que @PreAuthorize, asi que un body invalido daria 400 y taparia el 403.
        mockMvc.perform(delete("/api/productos/1").header("Authorization", "Bearer " + tokenMedico))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @DisplayName("JSON mal formado responde 400, no 500")
    void jsonMalFormadoResponde400() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{esto no es json valido"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("un id con tipo invalido en la ruta responde 400, no 500")
    void idNoNumericoResponde400() throws Exception {
        mockMvc.perform(get("/api/productos/abc").header("Authorization", "Bearer " + tokenMedico))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("credenciales incorrectas responden 401")
    void credencialesIncorrectasResponden401() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"medico.test\",\"password\":\"equivocada\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }
}
