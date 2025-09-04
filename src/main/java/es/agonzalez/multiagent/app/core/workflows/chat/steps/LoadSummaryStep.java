package es.agonzalez.multiagent.app.core.workflows.chat.steps;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

import es.agonzalez.multiagent.app.core.workflows.Step;
import es.agonzalez.multiagent.app.core.workflows.chat.models.ChatInput;
import es.agonzalez.multiagent.app.core.workflows.chat.models.ChatResult;
import es.agonzalez.multiagent.app.memory.SummaryStore;

public class LoadSummaryStep implements Step<ChatInput, ChatResult> {
    private final SummaryStore store;

    public LoadSummaryStep(SummaryStore store) {
        this.store = store;
    }


    @Override
    public Optional<ChatResult> apply(ChatInput input, Map<String, Object> context) {
        try{    
            String summary = store.load(input.userId());
            context.put("summary", summary == null ? "" : summary);
            return Optional.empty();
        }catch(IOException e) {
            context.put("summary", "");
            return Optional.empty();
        }
    }

}
