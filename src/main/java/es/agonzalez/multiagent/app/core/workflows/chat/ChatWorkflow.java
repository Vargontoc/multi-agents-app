package es.agonzalez.multiagent.app.core.workflows.chat;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.context.MessageSource;

import es.agonzalez.multiagent.app.core.workflows.Step;
import es.agonzalez.multiagent.app.core.workflows.Workflow;
import es.agonzalez.multiagent.app.core.workflows.chat.models.ChatInput;
import es.agonzalez.multiagent.app.core.workflows.chat.models.ChatResult;

public class ChatWorkflow implements Workflow<ChatInput, ChatResult> {
    
    private final List<Step<ChatInput,ChatResult>> steps;
    private final MessageSource messages;

    public ChatWorkflow(List<Step<ChatInput, ChatResult>> steps, MessageSource messages) {
        this.steps = steps;
        this.messages = messages;
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

        var msg = messages.getMessage("workflow.chat.no_result", null, Locale.getDefault());
        return ChatResult.error(msg);
    }

    
}
