package es.agonzalez.multiagent.app.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Verifica que el filtro de rate limiting devuelve 429 cuando se excede la cuota.
 */
@SpringBootTest(properties = {
    "security.apikey=test-key",
    "ratelimit.enabled=true",
    "ratelimit.capacity=3",
    "ratelimit.refill.tokens=3",
    "ratelimit.refill.period=60s"
})
@AutoConfigureMockMvc
public class RateLimitingFilterTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    @DisplayName("Excede límite y devuelve 429")
    void exceedLimit() throws Exception {
        // Consumimos 3 OK
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(get("/admin/models").header("X-API-Key", "test-key"))
                .andExpect(status().isOk());
        }
        // 4ª debe fallar
        mockMvc.perform(get("/admin/models").header("X-API-Key", "test-key"))
            .andExpect(status().isTooManyRequests())
            .andExpect(jsonPath("$.error").value("rate_limited"));
    }
}
