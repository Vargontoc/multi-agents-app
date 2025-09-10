package es.agonzalez.multiagent.app.core.workflows;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import es.agonzalez.multiagent.app.config.MetricsService;
import es.agonzalez.multiagent.app.core.IntentDetector;
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
    public AIResponse applyWorklow(AIRequest request) 
    {
        metrics.incMessages();
        String intent = getIntent(request);
    
        if(intent == null)
        {
            LOGGER.error("Intent not valid in request");
            return new AIResponse("error", getAgent(intent), "Request not valid", Map.of());
        }
        try {
            return run(request, intent);
        } catch (Exception e) {
            metrics.incErrors();
            LOGGER.error("Operation failed", e);
            return new AIResponse("error", getAgent(intent), e.getMessage(), Map.of());
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
                return new AIResponse("error", getAgent(intent), "Operation not found", Map.of());
            }
        }
    }

    


    private AIResponse getRecipeOperation(AIRequest request, String intent) {
        RecipeRequest in = new RecipeRequest(request.getText(), request.getParams());
        RecipeResponse out = recipeWorkflow.run(in);
        if(!"ok".equals(out.status())){
            metrics.incErrors();
        }

        return new AIResponse(out.status(), getAgent(intent), out.message(), Map.of(
            "userId", "", "intent", intent, "meta", out.data()
        ));

    }

    private AIResponse getChatOperation(AIRequest request, String intent) {
        ChatInput input = new ChatInput(request.getUserId(), request.getText(), intent);
        ChatResult output =  chatWorkflow.run(input);
        if(!"ok".equals(output.status()))   
            metrics.incErrors();
        return new AIResponse(
                output.status(), getAgent(intent), output.message(), Map.of(
                        "userId", request.getUserId(), "intent", intent, "meta", output.data()
                ));
    }

    private String getIntent(AIRequest r) {
        return detector.isValidIntent(r.getIntent()) ? r.getIntent() : detector.detect(r.getText());
    }

    private String getAgent(String intent) {
        String a = detector.agent(intent);
        return a == null ? "Unknown agent" : a;
    }


}
