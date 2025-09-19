package es.agonzalez.multiagent.app.integration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;

import es.agonzalez.multiagent.app.config.AppProperties;
import es.agonzalez.multiagent.app.config.MetricsService;
import es.agonzalez.multiagent.app.core.IntentDetector;
import es.agonzalez.multiagent.app.core.LlmClient;
import es.agonzalez.multiagent.app.core.mappers.ResponseMapper;
import es.agonzalez.multiagent.app.core.models.LlmResponse;
import es.agonzalez.multiagent.app.core.workflows.WorkflowRunner;
import es.agonzalez.multiagent.app.core.workflows.chat.ChatWorkflow;
import es.agonzalez.multiagent.app.core.workflows.chat.models.ChatResult;
import es.agonzalez.multiagent.app.core.workflows.recipe.RecipeWorkflow;
import es.agonzalez.multiagent.app.core.workflows.recipe.models.RecipeResponse;
import es.agonzalez.multiagent.app.dtos.AIRequest;
import es.agonzalez.multiagent.app.dtos.AIResponse;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Map;

/**
 * Test de integración enfocado específicamente en WorkflowRunner y sus dependencias inmediatas.
 * Usa mocks para LLM pero testing real de workflow logic.
 */
@SpringBootTest(classes = {
    WorkflowRunner.class,
    ResponseMapper.class,
    IntentDetector.class,
    AppProperties.class
})
@TestPropertySource(properties = {
    "multiagent.datadir=/tmp/test-workflow-runner",
    "multiagent.modelconfig=classpath:models.yaml",
    "spring.main.banner-mode=off"
})
class WorkflowRunnerIntegrationTest {

    private WorkflowRunner workflowRunner;

    @MockBean
    private ChatWorkflow chatWorkflow;

    @MockBean
    private RecipeWorkflow recipeWorkflow;

    @MockBean
    private MetricsService metricsService;

    @MockBean
    private LlmClient llmClient;

    private ResponseMapper responseMapper;
    private IntentDetector intentDetector;

    @BeforeEach
    void setUp() {
        responseMapper = new ResponseMapper();
        intentDetector = new IntentDetector();

        workflowRunner = new WorkflowRunner();
        // Inject dependencies manually for this focused test
        setField(workflowRunner, "chatWorkflow", chatWorkflow);
        setField(workflowRunner, "recipeWorkflow", recipeWorkflow);
        setField(workflowRunner, "responseMapper", responseMapper);
        setField(workflowRunner, "detector", intentDetector);
        setField(workflowRunner, "metrics", metricsService);
    }

    @Test
    void workflowRunner_ChatIntent_CallsChatWorkflow() {
        // Given
        AIRequest request = createRequest("user123", "chat", "Hello there");

        ChatResult chatResult = new ChatResult("ok", "Hi! How can I help you?", Map.of("tokens", 150));
        when(chatWorkflow.run(any())).thenReturn(chatResult);

        // When
        AIResponse response = workflowRunner.applyWorkflow(request);

        // Then
        assertNotNull(response);
        assertEquals("ok", response.status());
        assertEquals("Agent.Chat", response.agent());
        assertEquals("Hi! How can I help you?", response.message());

        Map<String, Object> data = response.data();
        assertEquals("user123", data.get("userId"));
        assertEquals("chat", data.get("intent"));
        assertEquals(Map.of("tokens", 150), data.get("meta"));

        // Verify correct workflow was called
        verify(chatWorkflow).run(any());
        verify(recipeWorkflow, never()).run(any());
    }

    @Test
    void workflowRunner_RecipeIntent_CallsRecipeWorkflow() {
        // Given
        AIRequest request = createRequest("chef456", "recipe_request", "chocolate cake");

        RecipeResponse recipeResult = new RecipeResponse("ok", "Here's a chocolate cake recipe...", Map.of("difficulty", "medium"));
        when(recipeWorkflow.run(any())).thenReturn(recipeResult);

        // When
        AIResponse response = workflowRunner.applyWorkflow(request);

        // Then
        assertEquals("ok", response.status());
        assertEquals("Agent.Recipe", response.agent());
        assertEquals("Here's a chocolate cake recipe...", response.message());

        Map<String, Object> data = response.data();
        assertEquals("chef456", data.get("userId"));
        assertEquals("recipe_request", data.get("intent"));
        assertEquals(Map.of("difficulty", "medium"), data.get("meta"));

        // Verify correct workflow was called
        verify(recipeWorkflow).run(any());
        verify(chatWorkflow, never()).run(any());
    }

    @Test
    void workflowRunner_InvalidIntent_ReturnsError() {
        // Given
        AIRequest request = createRequest("user789", "unknown_intent", "some text");

        // When
        AIResponse response = workflowRunner.applyWorkflow(request);

        // Then
        assertEquals("error", response.status());
        assertNotNull(response.message());
        assertEquals("invalid_intent", response.reason());

        // No workflows should be called for invalid intent
        verify(chatWorkflow, never()).run(any());
        verify(recipeWorkflow, never()).run(any());
    }

    @Test
    void workflowRunner_ChatWorkflowError_ReturnsErrorResponse() {
        // Given
        AIRequest request = createRequest("error_user", "chat", "trigger error");

        ChatResult errorResult = new ChatResult("error", "Something went wrong", Map.of());
        when(chatWorkflow.run(any())).thenReturn(errorResult);

        // When
        AIResponse response = workflowRunner.applyWorkflow(request);

        // Then
        assertEquals("error", response.status());
        assertEquals("Something went wrong", response.message());
        assertNotNull(response.reason()); // Should have error reason

        verify(chatWorkflow).run(any());
        verify(metricsService).incErrors(); // Should increment error metrics
    }

    @Test
    void workflowRunner_RecipeWorkflowError_ReturnsErrorResponse() {
        // Given
        AIRequest request = createRequest("error_chef", "recipe_request", "impossible recipe");

        RecipeResponse errorResult = new RecipeResponse("error", "Cannot create recipe", Map.of());
        when(recipeWorkflow.run(any())).thenReturn(errorResult);

        // When
        AIResponse response = workflowRunner.applyWorkflow(request);

        // Then
        assertEquals("error", response.status());
        assertEquals("Cannot create recipe", response.message());

        verify(recipeWorkflow).run(any());
        verify(metricsService).incErrors();
    }

    @Test
    void workflowRunner_MultipleRequests_HandlesIndependently() {
        // Given
        AIRequest chatRequest = createRequest("user1", "chat", "hello");
        AIRequest recipeRequest = createRequest("user2", "recipe_request", "pasta");

        ChatResult chatResult = new ChatResult("ok", "Chat response", Map.of());
        RecipeResponse recipeResult = new RecipeResponse("ok", "Recipe response", Map.of());

        when(chatWorkflow.run(any())).thenReturn(chatResult);
        when(recipeWorkflow.run(any())).thenReturn(recipeResult);

        // When
        AIResponse chatResponse = workflowRunner.applyWorkflow(chatRequest);
        AIResponse recipeResponse = workflowRunner.applyWorkflow(recipeRequest);

        // Then
        assertEquals("ok", chatResponse.status());
        assertEquals("Agent.Chat", chatResponse.agent());
        assertEquals("user1", chatResponse.data().get("userId"));

        assertEquals("ok", recipeResponse.status());
        assertEquals("Agent.Recipe", recipeResponse.agent());
        assertEquals("user2", recipeResponse.data().get("userId"));

        // Verify both workflows were called
        verify(chatWorkflow).run(any());
        verify(recipeWorkflow).run(any());
    }

    private AIRequest createRequest(String userId, String intent, String text) {
        AIRequest request = new AIRequest();
        request.setUserId(userId);
        request.setIntent(intent);
        request.setText(text);
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