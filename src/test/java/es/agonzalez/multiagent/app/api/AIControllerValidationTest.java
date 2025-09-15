package es.agonzalez.multiagent.app.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
    "server.port=0",
    "multiagent.datadir=./build-test-data",
    "multiagent.max-history-lines=50",
    "multiagent.summarization-every=10",
    "multiagent.llm.url=http://localhost:9999",
    "multiagent.llm.timeout-ms=1000",
    "security.apikey=test-key"
})
@AutoConfigureMockMvc
public class AIControllerValidationTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    void rejectsBlankText() throws Exception {
        String body = "{\"text\":\"\"}";
        mockMvc.perform(post("/api/v1/ai").header("X-API-Key", "test-key")
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
            .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsTooLongText() throws Exception {
        String longText = "x".repeat(600);
        String body = "{\"text\":\""+ longText +"\"}";
        mockMvc.perform(post("/api/v1/ai").header("X-API-Key", "test-key")
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
            .andExpect(status().isBadRequest());
    }

    @Test
    void validRequestWithoutCommandReturnsErrorStatusPayload() throws Exception {
        String body = "{\"text\":\"hola\"}"; // no command => intent null => status error
        mockMvc.perform(post("/api/v1/ai").header("X-API-Key", "test-key")
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("error"));
    }
}
