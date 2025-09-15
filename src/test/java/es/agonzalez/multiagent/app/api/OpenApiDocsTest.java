package es.agonzalez.multiagent.app.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
    "server.port=0",
    "multiagent.datadir=./build-test-data",
    "multiagent.max-history-lines=10",
    "multiagent.summarization-every=5",
    "multiagent.llm.url=http://localhost:9999",
    "multiagent.llm.timeout-ms=1000",
    "security.apikey=test-key"
})
@AutoConfigureMockMvc
public class OpenApiDocsTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    void openApiDocumentExposesInfoAndSecurity() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.info.title").value("Multi-Agents Service API"))
            .andExpect(jsonPath("$.components.securitySchemes.ApiKeyAuth.name").value("X-API-Key"));
    }
}
