package es.agonzalez.multiagent.app.core.workflows.chat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import es.agonzalez.multiagent.app.core.workflows.Step;
import es.agonzalez.multiagent.app.core.workflows.Workflow;
import es.agonzalez.multiagent.app.core.workflows.chat.models.ChatInput;
import es.agonzalez.multiagent.app.core.workflows.chat.models.ChatResult;

public class ChatWorkflow implements Workflow<ChatInput, ChatResult> {
    
    private final List<Step<ChatInput,ChatResult>> steps;

    public ChatWorkflow(List<Step<ChatInput, ChatResult>> steps) {
        this.steps = steps;
    }

    @Override
    public ChatResult run(ChatInput input) {
        Map<String, Object> context = new HashMap<>();
        for(var s: steps) {
            var maybe = s.apply(input, context);
            if(maybe.isPresent()) {
                return maybe.get();
            }
        }

        return ChatResult.error("Flujo terminó sin resultado");
    }

    
}
