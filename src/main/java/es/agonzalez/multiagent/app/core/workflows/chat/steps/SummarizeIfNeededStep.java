package es.agonzalez.multiagent.app.core.workflows.chat.steps;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import es.agonzalez.multiagent.app.core.workflows.Step;
import es.agonzalez.multiagent.app.core.workflows.chat.models.ChatInput;
import es.agonzalez.multiagent.app.core.workflows.chat.models.ChatResult;
import es.agonzalez.multiagent.app.memory.Summarizer;
import es.agonzalez.multiagent.app.memory.SummaryStore;

public class SummarizeIfNeededStep implements Step<ChatInput, ChatResult> {
    
    private final Summarizer summarizer;
    private  final SummaryStore store;

    public SummarizeIfNeededStep(SummaryStore store, Summarizer summarizer) {
        this.store = store;
        this.summarizer = summarizer;
    }


    @SuppressWarnings("unchecked")
    @Override
    public Optional<ChatResult> apply(ChatInput input, Map<String, Object> context) {
        try {
            
            int turnsBefore = (int)context.getOrDefault("turnsCountBefore", 0);
            int totalTurns = turnsBefore + 2;

            if(summarizer.shouldSummarize(totalTurns)) {
                var history = (List<String>)context.getOrDefault("history", List.of());
                var summary = summarizer.summarize(history);
                store.save(input.userId(), summary);

                context.put("summaryUpdated", true);

            }

        } catch (IOException e) {
            context.put("summaryUpdated", false);
        }

        return Optional.empty();
    }

}
