package es.agonzalez.multiagent.app.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ApiKeyFilterSecurityTest {

    @Autowired
    MockMvc mockMvc;

    private static final String VALID_KEY = "secret123"; // Debe coincidir con security.apikey en test

    @Nested
    @SuppressWarnings("unused") // Referenciado implícitamente por JUnit para agrupar tests
    class UnauthorizedCases {
        @Test
        @DisplayName("Sin cabecera X-API-Key devuelve 401 con body JSON y WWW-Authenticate")
        void noHeader() throws Exception {
            mockMvc.perform(get("/admin/models"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", "ApiKey realm=multi-agents-service"))
                .andExpect(jsonPath("$.error").value("unauthorized"))
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.timestamp").exists());
        }

        @Test
        @DisplayName("API key incorrecta devuelve 401")
        void wrongKey() throws Exception {
            mockMvc.perform(get("/admin/models").header("X-API-Key", "wrong"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("unauthorized"))
                .andExpect(jsonPath("$.timestamp").exists());
        }
    }

    @Test
    @DisplayName("API key válida permite acceso 200")
    void validKey() throws Exception {
        mockMvc.perform(get("/admin/models").header("X-API-Key", VALID_KEY))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.agents").exists())
            .andExpect(jsonPath("$.defaults").exists());
    }
}
