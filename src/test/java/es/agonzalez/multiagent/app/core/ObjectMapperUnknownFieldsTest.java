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
        String json = "{\n" +
            "  \"model\": \"llama3\",\n" +
            "  \"created_at\": \"2025-09-15T12:00:00Z\",\n" +
            "  \"message\": { \"role\": \"assistant\", \"content\": \"hola\" },\n" +
            "  \"prompt_eval_count\": 5,\n" +
            "  \"eval_count\": 10,\n" +
            "  \"extra_field\": 12345,\n" +
            "  \"another_unused\": { \"inner\": true }\n" +
            "}";

        OllamaChatResponse resp = assertDoesNotThrow(() -> om.readValue(json, OllamaChatResponse.class));
        assertEquals(5, resp.promptCount());
        assertEquals(10, resp.completionCount());
        assertEquals("hola", resp.contentOrEmpty(false));
    }
}
