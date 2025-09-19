package es.agonzalez.multiagent.app.core.workflows;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import es.agonzalez.multiagent.app.config.MetricsService;
import es.agonzalez.multiagent.app.core.IntentDetector;
import es.agonzalez.multiagent.app.core.llm.exceptions.LlmException;
import es.agonzalez.multiagent.app.core.mappers.ResponseMapper;
import es.agonzalez.multiagent.app.core.workflows.chat.ChatWorkflow;
import es.agonzalez.multiagent.app.core.workflows.chat.models.ChatInput;
import es.agonzalez.multiagent.app.core.workflows.chat.models.ChatResult;
import es.agonzalez.multiagent.app.core.workflows.recipe.RecipeWorkflow;
import es.agonzalez.multiagent.app.core.workflows.recipe.models.RecipeRequest;
import es.agonzalez.multiagent.app.core.workflows.recipe.models.RecipeResponse;
import es.agonzalez.multiagent.app.dtos.AIRequest;
import es.agonzalez.multiagent.app.dtos.AIResponse;

@Component
public class WorkflowRunner {
    private static final Logger LOGGER = LoggerFactory.getLogger(WorkflowRunner.class);

    @Autowired
    private MetricsService metrics;
    @Autowired
    private IntentDetector detector;
    @Autowired
    private ChatWorkflow chatWorkflow;
    @Autowired
    private RecipeWorkflow recipeWorkflow;
    @Autowired
    private ResponseMapper responseMapper;
    public AIResponse applyWorkflow(AIRequest request) 
    {
        metrics.incMessages();
        String intent = getIntent(request);
        if(request.getUserId() != null) {
            MDC.put("userId", request.getUserId());
        }
        if(intent != null) {
            MDC.put("intent", intent);
        }
    
        if(intent == null)
        {
            LOGGER.error("Intent not valid in request");
            return AIResponse.error(getAgent(intent), "Request not valid", "invalid_intent");
        }
        try {
            return run(request, intent);
        } catch (LlmException le) {
            metrics.incErrors();
            LOGGER.error("LLM operation failed: {}", le.reason(), le);
            return AIResponse.error(getAgent(intent), le.getMessage(), le.reason());
        } catch (Exception e) {
            metrics.incErrors();
            LOGGER.error("Operation failed", e);
            return AIResponse.error(getAgent(intent), e.getMessage(), "unknown");
        } finally {
            // model se añade en GenerateStep y se elimina por filtro al final del request
            MDC.remove("intent");
            MDC.remove("userId");
        }
    }

    private AIResponse run(AIRequest request, String intent) 
    {
        switch(intent) {
            case "chat" -> {
                return getChatOperation(request, intent);
            }
            case "recipe_request" -> {
                return getRecipeOperation(request, intent);
            }
            default -> {
                return AIResponse.error(getAgent(intent), "Operation not found", "unknown_operation");
            }
        }
    }

    


    private AIResponse getRecipeOperation(AIRequest request, String intent) {
        RecipeRequest in = new RecipeRequest(request.getText(), request.getParams());
        RecipeResponse out = recipeWorkflow.run(in);
        if(!"ok".equals(out.status())){
            metrics.incErrors();
        }

        return responseMapper.mapRecipeResult(out, request, intent, getAgent(intent));
    }

    private AIResponse getChatOperation(AIRequest request, String intent) {
        String username = "";
        if(request.getParams() != null &&
         !request.getParams().isEmpty()
         && request.getParams().containsKey("username")
         && request.getParams().get("username") instanceof String us) {
            username = us;
        }

        ChatInput input = new ChatInput(request.getUserId(), username, request.getText(), intent);
        ChatResult output =  chatWorkflow.run(input);
        if(!"ok".equals(output.status()))
            metrics.incErrors();

        return responseMapper.mapChatResult(output, request, intent, getAgent(intent));
    }

    private String getIntent(AIRequest r) {
        return detector.isValidIntent(r.getIntent()) ? r.getIntent() : detector.detect(r.getText());
    }

    private String getAgent(String intent) {
        String a = detector.agent(intent);
        return a == null ? "Unknown agent" : a;
    }


}
