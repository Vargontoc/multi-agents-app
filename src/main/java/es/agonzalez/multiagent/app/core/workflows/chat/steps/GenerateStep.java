package es.agonzalez.multiagent.app.core.workflows.chat.steps;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import es.agonzalez.multiagent.app.core.LlmClient;
import es.agonzalez.multiagent.app.core.ModelRegistry;
import es.agonzalez.multiagent.app.core.models.LlmResponse;
import es.agonzalez.multiagent.app.core.models.Message;
import es.agonzalez.multiagent.app.core.selectors.ModelSelectors;
import es.agonzalez.multiagent.app.core.workflows.Step;
import es.agonzalez.multiagent.app.core.workflows.chat.models.ChatInput;
import es.agonzalez.multiagent.app.core.workflows.chat.models.ChatResult;
import es.agonzalez.multiagent.app.memory.MemoryService;

public class GenerateStep implements Step<ChatInput, ChatResult> {
    
    private final LlmClient client;
    private final ModelRegistry models;
    private final MemoryService memory;
    private final ModelSelectors selector;
    public GenerateStep(LlmClient client, MemoryService memory, ModelRegistry models, ModelSelectors selector) {
        this.client = client;
        this.memory = memory;
        this.models = models;
        this.selector = selector;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Optional<ChatResult> apply(ChatInput input, Map<String, Object> context) {
        try {
            var history = (List<String>)context.getOrDefault("history", List.of());
            var summary = (String) context.getOrDefault("summary", "");

            var messages = new ArrayList<Message>();
            
                        var sys = """
                    Eres un bot de Twitch: amable, breve (máx 320 chars) y sin inventar.
                    Si la pregunta no es clara pide SOLO una aclaración.
                    No repitas el enunciado del usuario
                    """;
            messages.add(Message.system(sys));
            if(!summary.isBlank()) {
                messages.add(Message.system("Resument previo del usuario:\n" + summary));
            }
            for(var line : last(history, 16)) {
                var parts = line.split("\t", 3);
                if(parts.length < 3) continue;
                var role = parts[1];
                var text = parts[2];
                messages.add(new Message(role.equals("assistant") ? "assistant" : "user", text));
            }

            messages.add(Message.user(input.text()));

            String model = selector.pick("Agent.Chat", input.userId());
            Instant start = Instant.now();
            LlmResponse resp = client.chat(model, messages, models.defaults());
            long latency = Duration.between(start, Instant.now()).toMillis();

            String answer = cap(resp.contet(), 320);

            memory.appendTurn(input.userId(), "user", input.text());
            memory.appendTurn(input.userId(), "assistant", answer);

            context.put("latencyMs", latency);
            context.put("model", model);
            context.put("answer", answer);
            context.put("turnCountBefore", history.size());
            
            return Optional.empty();


        } catch (IOException e) {
            
            return Optional.of(ChatResult.ok("Estoy teniendo problemas para pensar. Prueba otra vez en unos segundos.", Map.of("degraded", true, "error", e.getClass().getSimpleName())));
        }
    }

    private static List<String> last(List<String> all, int k) {
        if(all == null || all.isEmpty()) return  List.of();
        return all.size() <= k ? all : all.subList(all.size() - k, all.size());
    }

    private static String cap(String s, int max) {
        if(s == null) return "";
        if(s.length()  <= max) return s;
        return s.substring(0, Math.max(0, max - 1)) + "...";
    }
}
