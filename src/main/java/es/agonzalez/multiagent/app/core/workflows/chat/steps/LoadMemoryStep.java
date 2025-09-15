package es.agonzalez.multiagent.app.core.workflows.chat.steps;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.springframework.context.MessageSource;

import es.agonzalez.multiagent.app.core.workflows.Step;
import es.agonzalez.multiagent.app.core.workflows.chat.models.ChatInput;
import es.agonzalez.multiagent.app.core.workflows.chat.models.ChatResult;
import es.agonzalez.multiagent.app.memory.MemoryService;

public class LoadMemoryStep implements Step<ChatInput, ChatResult> {

    public final MemoryService memory;
    private final MessageSource messages;
    public LoadMemoryStep(MemoryService memory, MessageSource messages) { 
        this.memory = memory; 
        this.messages = messages;
    }

    @Override
    public Optional<ChatResult> apply(ChatInput input, Map<String, Object> context) {
        try
        {
            var history = memory.loadHistory(input.userId());
            context.put("history", history);
            return Optional.empty();
        }catch(IOException e) {
            var msg = messages.getMessage("workflow.memory.load_error", new Object[]{e.getMessage()}, Locale.getDefault());
            return Optional.of(ChatResult.error(msg));
        }
    }
}
