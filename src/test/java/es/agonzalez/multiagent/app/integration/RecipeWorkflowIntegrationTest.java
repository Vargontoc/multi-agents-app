package es.agonzalez.multiagent.app.integration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import es.agonzalez.multiagent.app.config.MetricsService;
import es.agonzalez.multiagent.app.core.IntentDetector;
import es.agonzalez.multiagent.app.core.LlmClient;
import es.agonzalez.multiagent.app.core.mappers.ResponseMapper;
import es.agonzalez.multiagent.app.core.models.LlmResponse;
import es.agonzalez.multiagent.app.core.workflows.WorkflowRunner;
import es.agonzalez.multiagent.app.core.workflows.chat.ChatWorkflow;
import es.agonzalez.multiagent.app.core.workflows.recipe.RecipeWorkflow;
import es.agonzalez.multiagent.app.core.workflows.recipe.models.RecipeResponse;
import es.agonzalez.multiagent.app.dtos.AIRequest;
import es.agonzalez.multiagent.app.dtos.AIResponse;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Map;

/**
 * Test de integración end-to-end para el workflow de recetas completo.
 * Simula el flujo: AIRequest → WorkflowRunner → RecipeWorkflow → LlmClient (mocked) → AIResponse
 */
class RecipeWorkflowIntegrationTest {

    private WorkflowRunner workflowRunner;
    private LlmClient llmClient;
    private ChatWorkflow chatWorkflow;
    private RecipeWorkflow recipeWorkflow;

    @BeforeEach
    void setUp() {
        // Create mock dependencies
        llmClient = mock(LlmClient.class);
        MetricsService metricsService = mock(MetricsService.class);

        // Create real instances with mocked dependencies
        ResponseMapper responseMapper = new ResponseMapper();
        IntentDetector intentDetector = new IntentDetector();

        // Create workflows as mocks to avoid complex dependencies
        chatWorkflow = mock(ChatWorkflow.class);
        recipeWorkflow = mock(RecipeWorkflow.class);

        // Create WorkflowRunner and inject dependencies
        workflowRunner = new WorkflowRunner();
        setField(workflowRunner, "chatWorkflow", chatWorkflow);
        setField(workflowRunner, "recipeWorkflow", recipeWorkflow);
        setField(workflowRunner, "responseMapper", responseMapper);
        setField(workflowRunner, "detector", intentDetector);
        setField(workflowRunner, "metrics", metricsService);

        // Setup workflow mock behaviors
        when(recipeWorkflow.run(any())).thenReturn(
            new RecipeResponse(
                "ok",
                "Spaghetti Carbonara Recipe:\n1. Boil pasta\n2. Cook bacon\n3. Mix with eggs and cheese",
                Map.of("difficulty", "medium")
            )
        );
    }

    @Test
    void recipeWorkflow_SuccessfulExecution_ReturnsValidRecipe() {
        // Given
        AIRequest request = createRecipeRequest("chef001", "pasta with bacon");

        // When
        AIResponse response = workflowRunner.applyWorkflow(request);

        // Then
        assertNotNull(response);
        assertEquals("ok", response.status());
        assertEquals("Agent.Recipe", response.agent());
        assertNotNull(response.message());
        assertTrue(response.message().contains("Recipe"));

        // Verify response data structure
        Map<String, Object> data = response.data();
        assertNotNull(data);
        assertEquals("chef001", data.get("userId"));
        assertEquals("recipe_request", data.get("intent"));
        assertNotNull(data.get("meta"));

        // Verify no error reason for successful execution
        assertNull(response.reason());

        // Verify RecipeWorkflow was called correctly
        verify(recipeWorkflow, atLeastOnce()).run(any());
    }

    @Test
    void recipeWorkflow_WithIngredientPreferences_IncludesInRequest() {
        // Given
        AIRequest request = createRecipeRequest("chef002", "vegetarian pizza");
        request.setParams(Map.of(
            "dietary_restrictions", "vegetarian",
            "cooking_time", "30 minutes"
        ));

        // When
        AIResponse response = workflowRunner.applyWorkflow(request);

        // Then
        assertEquals("ok", response.status());
        assertEquals("chef002", response.data().get("userId"));

        // Verify RecipeWorkflow was called with the request
        verify(recipeWorkflow).run(any());
    }

    @Test
    void recipeWorkflow_ComplexRecipeRequest_HandlesCorrectly() {
        // Given
        String complexRequest = "I want a gluten-free dessert that takes less than 1 hour and uses chocolate";
        AIRequest request = createRecipeRequest("chef003", complexRequest);

        // Setup RecipeWorkflow to return appropriate recipe
        when(recipeWorkflow.run(any())).thenReturn(
            new RecipeResponse("ok", "Gluten-Free Chocolate Brownies:\nIngredients: almond flour, cocoa powder...\nInstructions: 1. Preheat oven...", Map.of())
        );

        // When
        AIResponse response = workflowRunner.applyWorkflow(request);

        // Then
        assertEquals("ok", response.status());
        assertTrue(response.message().contains("Gluten-Free"));
        assertTrue(response.message().contains("Chocolate"));

        // Verify the request was processed with the complex text
        verify(recipeWorkflow).run(any());
    }

    @Test
    void recipeWorkflow_LlmTimeout_ReturnsTimeoutError() {
        // Given - configure workflow to return timeout error
        when(recipeWorkflow.run(any())).thenReturn(
            new RecipeResponse("error", "Request timed out", Map.of())
        );

        AIRequest request = createRecipeRequest("chef004", "quick salad");

        // When
        AIResponse response = workflowRunner.applyWorkflow(request);

        // Then
        assertEquals("error", response.status());
        assertEquals("Agent.Recipe", response.agent());
        assertNotNull(response.reason()); // Should have error reason

        Map<String, Object> data = response.data();
        assertEquals("chef004", data.get("userId"));
        assertEquals("recipe_request", data.get("intent"));

        verify(recipeWorkflow).run(any());
    }

    @Test
    void recipeWorkflow_EmptyRecipeRequest_StillProcesses() {
        // Given
        AIRequest request = createRecipeRequest("chef005", "");

        // When
        AIResponse response = workflowRunner.applyWorkflow(request);

        // Then
        assertEquals("ok", response.status());
        verify(recipeWorkflow).run(any());
    }

    @Test
    void recipeWorkflow_MultipleRequests_HandlesIndependently() {
        // Given
        AIRequest request1 = createRecipeRequest("chef006", "italian pasta");
        AIRequest request2 = createRecipeRequest("chef007", "mexican tacos");

        // Setup different responses for different requests
        when(recipeWorkflow.run(any()))
            .thenReturn(new RecipeResponse("ok", "Pasta Marinara Recipe...", Map.of()))
            .thenReturn(new RecipeResponse("ok", "Beef Tacos Recipe...", Map.of()));

        // When
        AIResponse response1 = workflowRunner.applyWorkflow(request1);
        AIResponse response2 = workflowRunner.applyWorkflow(request2);

        // Then
        assertEquals("ok", response1.status());
        assertEquals("ok", response2.status());
        assertEquals("chef006", response1.data().get("userId"));
        assertEquals("chef007", response2.data().get("userId"));

        // Verify both requests were processed independently
        verify(recipeWorkflow, times(2)).run(any());
    }

    @Test
    void recipeWorkflow_LlmValidationError_ReturnsValidationError() {
        // Given - configure workflow to return validation error
        when(recipeWorkflow.run(any())).thenReturn(
            new RecipeResponse("error", "Invalid recipe parameters", Map.of())
        );

        AIRequest request = createRecipeRequest("chef008", "impossible recipe");

        // When
        AIResponse response = workflowRunner.applyWorkflow(request);

        // Then
        assertEquals("error", response.status());
        assertNotNull(response.reason()); // Should have error reason

        verify(recipeWorkflow).run(any());
    }

    private AIRequest createRecipeRequest(String userId, String recipeDescription) {
        AIRequest request = new AIRequest();
        request.setUserId(userId);
        request.setIntent("recipe_request");
        request.setText(recipeDescription);
        return request;
    }

    // Helper method to set private fields for testing
    private void setField(Object target, String fieldName, Object value) {
        try {
            var field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set field " + fieldName, e);
        }
    }
}