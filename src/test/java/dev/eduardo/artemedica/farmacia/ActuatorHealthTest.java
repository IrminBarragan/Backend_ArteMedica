package dev.eduardo.artemedica.farmacia;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * /actuator/health debe quedar accesible sin token: es lo que consulta un orquestador
 * (Docker/Kubernetes) para saber si reiniciar el contenedor, y nunca tiene un JWT a la mano.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Actuator health")
class ActuatorHealthTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("responde 200 UP sin necesidad de autenticacion")
    void healthEsPublicoYResponde200() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    @DisplayName("/actuator/info sin token responde 401, no se expone informacion interna")
    void infoRequiereAutenticacion() throws Exception {
        mockMvc.perform(get("/actuator/info"))
                .andExpect(status().isUnauthorized());
    }
}
