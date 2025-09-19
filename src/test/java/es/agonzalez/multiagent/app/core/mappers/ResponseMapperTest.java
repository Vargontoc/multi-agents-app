package es.agonzalez.multiagent.app.core.mappers;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import es.agonzalez.multiagent.app.core.workflows.chat.models.ChatResult;
import es.agonzalez.multiagent.app.core.workflows.recipe.models.RecipeResponse;
import es.agonzalez.multiagent.app.dtos.AIRequest;
import es.agonzalez.multiagent.app.dtos.AIResponse;

class ResponseMapperTest {

    private ResponseMapper responseMapper;

    @BeforeEach
    public void setUp() {
        responseMapper = new ResponseMapper();
    }

    @Test
    void mapChatResult_Success_ReturnsCorrectResponse() {
        // Given
        ChatResult chatResult = new ChatResult("ok", "Chat response", Map.of("data", "value"));
        AIRequest request = createRequest("user123", "chat", "Hello");
        String intent = "chat";
        String agent = "ChatAgent";

        // When
        AIResponse response = responseMapper.mapChatResult(chatResult, request, intent, agent);

        // Then
        assertEquals("ok", response.status());
        assertEquals("ChatAgent", response.agent());
        assertEquals("Chat response", response.message());
        assertEquals("user123", response.data().get("userId"));
        assertEquals("chat", response.data().get("intent"));
        assertEquals(Map.of("data", "value"), response.data().get("meta"));
        assertNull(response.reason()); // Success case should not have reason
    }

    @Test
    void mapChatResult_Error_ReturnsResponseWithReason() {
        // Given
        ChatResult chatResult = new ChatResult("error", "Chat failed", Map.of());
        AIRequest request = createRequest("user123", "chat", "Hello");
        String intent = "chat";
        String agent = "ChatAgent";

        // When
        AIResponse response = responseMapper.mapChatResult(chatResult, request, intent, agent);

        // Then
        assertEquals("error", response.status());
        assertEquals("ChatAgent", response.agent());
        assertEquals("Chat failed", response.message());
        assertEquals("workflow_error", response.reason());
    }

    @Test
    void mapRecipeResult_Success_ReturnsCorrectResponse() {
        // Given
        RecipeResponse recipeResult = new RecipeResponse("ok", "Recipe created", Map.of("recipe", "pasta"));
        AIRequest request = createRequest("user456", "recipe_request", "Make pasta");
        String intent = "recipe_request";
        String agent = "RecipeAgent";

        // When
        AIResponse response = responseMapper.mapRecipeResult(recipeResult, request, intent, agent);

        // Then
        assertEquals("ok", response.status());
        assertEquals("RecipeAgent", response.agent());
        assertEquals("Recipe created", response.message());
        assertEquals("user456", response.data().get("userId"));
        assertEquals("recipe_request", response.data().get("intent"));
        assertEquals(Map.of("recipe", "pasta"), response.data().get("meta"));
        assertNull(response.reason()); // Success case should not have reason
    }

    @Test
    void mapRecipeResult_Error_ReturnsResponseWithReason() {
        // Given
        RecipeResponse recipeResult = new RecipeResponse("error", "Recipe failed", Map.of());
        AIRequest request = createRequest("user456", "recipe_request", "Make pasta");
        String intent = "recipe_request";
        String agent = "RecipeAgent";

        // When
        AIResponse response = responseMapper.mapRecipeResult(recipeResult, request, intent, agent);

        // Then
        assertEquals("error", response.status());
        assertEquals("RecipeAgent", response.agent());
        assertEquals("Recipe failed", response.message());
        assertEquals("workflow_error", response.reason());
    }

    private AIRequest createRequest(String userId, String intent, String text) {
        AIRequest request = new AIRequest();
        request.setUserId(userId);
        request.setIntent(intent);
        request.setText(text);
        return request;
    }
}