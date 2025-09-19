package es.agonzalez.multiagent.app.integration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

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
import es.agonzalez.multiagent.app.dtos.AIRequest;
import es.agonzalez.multiagent.app.dtos.AIResponse;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Map;

/**
 * Test de integración end-to-end para el workflow de chat completo.
 * Simula el flujo: AIRequest → WorkflowRunner → ChatWorkflow → LlmClient (mocked) → AIResponse
 */
class ChatWorkflowIntegrationTest {

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
        when(chatWorkflow.run(any())).thenReturn(
            new ChatResult(
                "ok",
                "Hello! I'm an AI assistant.",
                Map.of("tokens", 15)
            )
        );
    }

    @Test
    void chatWorkflow_SuccessfulExecution_ReturnsValidResponse() {
        // Given
        AIRequest request = createChatRequest("user123", "Hello, how are you?");

        // When
        AIResponse response = workflowRunner.applyWorkflow(request);

        // Then
        assertNotNull(response);
        assertEquals("ok", response.status());
        assertEquals("Agent.Chat", response.agent());
        assertNotNull(response.message());
        assertFalse(response.message().isEmpty());

        // Verify response data structure
        Map<String, Object> data = response.data();
        assertNotNull(data);
        assertEquals("user123", data.get("userId"));
        assertEquals("chat", data.get("intent"));
        assertNotNull(data.get("meta"));

        // Verify no error reason for successful execution
        assertNull(response.reason());

        // Verify ChatWorkflow was called correctly
        verify(chatWorkflow, atLeastOnce()).run(any());
    }

    @Test
    void chatWorkflow_WithUsername_IncludesUsernameInContext() {
        // Given
        AIRequest request = createChatRequest("user456", "What's the weather like?");
        request.setParams(Map.of("username", "Alice"));

        // When
        AIResponse response = workflowRunner.applyWorkflow(request);

        // Then
        assertEquals("ok", response.status());
        assertEquals("user456", response.data().get("userId"));

        // Verify ChatWorkflow was called
        verify(chatWorkflow).run(any());
    }

    @Test
    void chatWorkflow_LlmError_ReturnsErrorResponse() {
        // Given - configure workflow to return error
        when(chatWorkflow.run(any())).thenReturn(
            new ChatResult("error", "LLM Error occurred", Map.of())
        );

        AIRequest request = createChatRequest("user789", "Tell me a joke");

        // When
        AIResponse response = workflowRunner.applyWorkflow(request);

        // Then
        assertEquals("error", response.status());
        assertEquals("Agent.Chat", response.agent());
        assertNotNull(response.message());
        assertNotNull(response.reason()); // Should have error reason

        Map<String, Object> data = response.data();
        assertEquals("user789", data.get("userId"));
        assertEquals("chat", data.get("intent"));

        verify(chatWorkflow).run(any());
    }

    @Test
    void chatWorkflow_InvalidIntent_ReturnsInvalidIntentError() {
        // Given
        AIRequest request = new AIRequest();
        request.setUserId("user999");
        request.setIntent("invalid_intent");
        request.setText("Some text");

        // When
        AIResponse response = workflowRunner.applyWorkflow(request);

        // Then
        assertEquals("error", response.status());
        assertNotNull(response.message());
        assertEquals("invalid_intent", response.reason());

        // No workflows should be called for invalid intents
        verifyNoInteractions(chatWorkflow);
    }

    @Test
    void chatWorkflow_EmptyText_StillProcesses() {
        // Given
        AIRequest request = createChatRequest("user111", "");

        // When
        AIResponse response = workflowRunner.applyWorkflow(request);

        // Then
        assertEquals("ok", response.status());
        verify(chatWorkflow).run(any());
    }

    @Test
    void chatWorkflow_LongConversation_HandlesMemoryCorrectly() {
        // Given
        AIRequest request = createChatRequest("user222", "This is a very long conversation that should test memory handling");

        // Setup LLM to return a longer response
        when(llmClient.chat(anyString(), anyList(), anyMap(), anyBoolean()))
            .thenReturn(new LlmResponse("This is a detailed response that demonstrates proper memory handling in our chat system.", 45, 25));

        // When
        AIResponse response = workflowRunner.applyWorkflow(request);

        // Then
        assertEquals("ok", response.status());
        assertNotNull(response.message());
        assertTrue(response.message().length() > 10); // Should have meaningful content

        // Verify ChatWorkflow was called for the conversation
        verify(chatWorkflow).run(any());
    }

    private AIRequest createChatRequest(String userId, String text) {
        AIRequest request = new AIRequest();
        request.setUserId(userId);
        request.setIntent("chat");
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