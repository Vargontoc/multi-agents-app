package es.agonzalez.multiagent.app.core;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.fasterxml.jackson.databind.ObjectMapper;

import es.agonzalez.multiagent.app.core.models.dto.OllamaChatResponse;

@SpringBootTest
public class ObjectMapperUnknownFieldsTest {

    @Autowired
    private ObjectMapper om;

    @Test
    void ignoresUnknownFields() throws Exception {
                String json = """
                        {
                            \"model\": \"llama3\",
                            \"created_at\": \"2025-09-15T12:00:00Z\",
                            \"message\": { \"role\": \"assistant\", \"content\": \"hola\" },
                            \"prompt_eval_count\": 5,
                            \"eval_count\": 10,
                            \"extra_field\": 12345,
                            \"another_unused\": { \"inner\": true }
                        }
                        """;

        OllamaChatResponse resp = assertDoesNotThrow(() -> om.readValue(json, OllamaChatResponse.class));
        assertEquals(5, resp.promptCount());
        assertEquals(10, resp.completionCount());
        assertEquals("hola", resp.contentOrEmpty(false));
    }
}
