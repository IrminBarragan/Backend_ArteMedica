package dev.eduardo.artemedica.farmacia.security;

import com.jayway.jsonpath.JsonPath;
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
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * El refresh token es opaco (no JWT) y se guarda hasheado en BD, lo que permite revocarlo de
 * verdad: estas pruebas verifican que login lo emite, que /refresh lo rota (revoca el viejo y
 * emite uno nuevo) y que /logout lo revoca, todo contra la API real via MockMvc.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("Flujo de refresh token y logout")
class RefreshTokenFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private EmpleadoRepository empleadoRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void prepararUsuario() {
        Empleado empleado = empleadoRepository.save(Empleado.builder()
                .nombres("Ana").apellidoPaterno("Perez")
                .tipo(TipoEmpleado.MEDICO).activo(true).build());
        usuarioRepository.save(Usuario.builder()
                .username("medico.refresh").password(passwordEncoder.encode("secreto123"))
                .empleado(empleado).rol(Rol.MEDICO).activo(true).build());
    }

    private String login() throws Exception {
        String body = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"medico.refresh\",\"password\":\"secreto123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.refreshToken");
    }

    @Test
    @DisplayName("login emite un refresh token ademas del access token")
    void loginEmiteRefreshToken() throws Exception {
        login();
    }

    @Test
    @DisplayName("refresh con un token valido devuelve un access token y un refresh token nuevos")
    void refreshRotaElToken() throws Exception {
        String refreshToken = login();

        String body = mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + refreshToken + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andReturn().getResponse().getContentAsString();

        String nuevoRefreshToken = JsonPath.read(body, "$.refreshToken");
        org.assertj.core.api.Assertions.assertThat(nuevoRefreshToken).isNotEqualTo(refreshToken);
    }

    @Test
    @DisplayName("reusar un refresh token ya rotado responde 401")
    void reusarTokenRotadoFalla() throws Exception {
        String refreshToken = login();

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + refreshToken + "\"}"))
                .andExpect(status().isOk());

        // El mismo token ya fue revocado por la rotacion anterior: reusarlo debe rechazarse.
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + refreshToken + "\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("un refresh token que no existe responde 401")
    void tokenInexistenteFalla() throws Exception {
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"no-existe-este-token\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("logout revoca el refresh token: un refresh posterior con el mismo token falla")
    void logoutRevocaElToken() throws Exception {
        String refreshToken = login();

        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + refreshToken + "\"}"))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + refreshToken + "\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("login y refresh no requieren token de autenticacion")
    void endpointsDeAuthSonPublicos() throws Exception {
        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"cualquiera\"}"))
                .andExpect(status().isNoContent());
    }
}
