package es.agonzalez.multiagent.app.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SwitchModelRequestValidationTest {

    @Autowired
    MockMvc mockMvc;

    private static final String URL = "/admin/models/switch";
        private static final String API_KEY = "secret123"; // debe coincidir con security.apikey de test (default)

    @Nested
    class InvalidCases {
        @Test
        @DisplayName("percent>0 sin canary retorna 400")
        void percentWithoutCanary() throws Exception {
                mockMvc.perform(post(URL)
                    .header("X-API-Key", API_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{" +
                    "\"agent\":\"chat\"," +
                    "\"stable\":\"llama3:instruct\"," +
                    "\"percent\":30" +
                "}"))
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("percent>0 con canary==stable 400")
        void canaryEqualsStable() throws Exception {
                mockMvc.perform(post(URL)
                    .header("X-API-Key", API_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{" +
                    "\"agent\":\"chat\"," +
                    "\"stable\":\"llama3:instruct\"," +
                    "\"canary\":\"llama3:instruct\"," +
                    "\"percent\":10" +
                "}"))
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("agent con caracteres inválidos 400")
        void invalidAgentPattern() throws Exception {
                mockMvc.perform(post(URL)
                    .header("X-API-Key", API_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{" +
                    "\"agent\":\"chat!\"," +
                    "\"stable\":\"llama3:instruct\"," +
                    "\"canary\":\"llama3:small\"," +
                    "\"percent\":20" +
                "}"))
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("canary presente y percent=0 es válido (200)")
        void canaryWithZeroPercentOk() throws Exception {
                mockMvc.perform(post(URL)
                    .header("X-API-Key", API_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{" +
                    "\"agent\":\"chat\"," +
                    "\"stable\":\"llama3:instruct\"," +
                    "\"canary\":\"llama3:small\"," +
                    "\"percent\":0" +
                "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"));
        }
    }

    @Test
    @DisplayName("Caso válido percent>0 con canary distinto retorna 200")
    void validCase() throws Exception {
            mockMvc.perform(post(URL)
                .header("X-API-Key", API_KEY)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{" +
                "\"agent\":\"search\"," +
                "\"stable\":\"llama3:instruct\"," +
                "\"canary\":\"llama3:small\"," +
                "\"percent\":25" +
            "}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ok"));
    }
}
