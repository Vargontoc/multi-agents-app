package es.agonzalez.multiagent.app.core.workflows.chat.steps;

import java.util.Map;
import java.util.Optional;

import es.agonzalez.multiagent.app.core.workflows.Step;
import es.agonzalez.multiagent.app.core.workflows.chat.models.ChatInput;
import es.agonzalez.multiagent.app.core.workflows.chat.models.ChatResult;

public class SaveResultStep implements Step<ChatInput, ChatResult> {
    

    @Override
    public Optional<ChatResult> apply(ChatInput input, Map<String, Object> context) {
        String answer = (String) context.getOrDefault("answer", "");
        String model = (String)context.get("model");
        long  latency = (long) context.get("latencyMs");

        ChatResult result = ChatResult.ok(answer, Map.of(
            "model", model, "latencyMs", latency
        ));
        context.put("result", result);
        
        return Optional.of(result);
    }
    
}
