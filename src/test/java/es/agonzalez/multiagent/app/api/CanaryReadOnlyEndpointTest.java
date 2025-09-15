package es.agonzalez.multiagent.app.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class CanaryReadOnlyEndpointTest {

    @Autowired
    private MockMvc mvc;

    private static final String API_KEY = "secret123"; // default from properties fallback

    @Test
    void returnsEmptyWhenNoCanaryConfigured() throws Exception {
        mvc.perform(get("/admin/models/canary").header("X-API-Key", API_KEY))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ok"))
            .andExpect(jsonPath("$.canary").exists());
    }

    @Test
    void returnsConfiguredCanaryAfterSwitch() throws Exception {
        String body = "{\n" +
            "  \"agent\": \"agentX\",\n" +
            "  \"stable\": \"llama3.2:3b\",\n" +
            "  \"canary\": \"llama3.2:8b\",\n" +
            "  \"percent\": 25\n" +
            "}";
        mvc.perform(post("/admin/models/switch")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-API-Key", API_KEY)
                .content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ok"));

        mvc.perform(get("/admin/models/canary").header("X-API-Key", API_KEY))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.canary.agentX.stable").value("llama3.2:3b"))
            .andExpect(jsonPath("$.canary.agentX.canary").value("llama3.2:8b"))
            .andExpect(jsonPath("$.canary.agentX.percentage").value(25));
    }
}
