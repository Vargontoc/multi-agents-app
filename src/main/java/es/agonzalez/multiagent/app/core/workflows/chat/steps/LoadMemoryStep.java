package es.agonzalez.multiagent.app.core.workflows.chat.steps;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

import es.agonzalez.multiagent.app.core.workflows.Step;
import es.agonzalez.multiagent.app.core.workflows.chat.models.ChatInput;
import es.agonzalez.multiagent.app.core.workflows.chat.models.ChatResult;
import es.agonzalez.multiagent.app.memory.MemoryService;

public class LoadMemoryStep implements Step<ChatInput, ChatResult> {

    public final MemoryService memory;
    public LoadMemoryStep(MemoryService memory) { this.memory = memory; }

    @Override
    public Optional<ChatResult> apply(ChatInput input, Map<String, Object> context) {
        try
        {
            var history = memory.loadHistory(input.userId());
            context.put("history", history);
            return Optional.empty();
        }catch(IOException e) {
            return Optional.of(ChatResult.error("No se pudo cargar historial:" + e.getMessage()));
        }
    }
}
