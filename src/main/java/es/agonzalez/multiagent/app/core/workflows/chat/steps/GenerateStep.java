package es.agonzalez.multiagent.app.core.workflows.chat.steps;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import es.agonzalez.multiagent.app.core.LlmClient;
import es.agonzalez.multiagent.app.config.MetricsService;
import es.agonzalez.multiagent.app.core.ModelRegistry;
import es.agonzalez.multiagent.app.core.models.LlmResponse;
import es.agonzalez.multiagent.app.core.llm.exceptions.LlmException;
import es.agonzalez.multiagent.app.core.models.Message;
import es.agonzalez.multiagent.app.core.selectors.ModelSelectors;
import es.agonzalez.multiagent.app.core.workflows.Step;
import es.agonzalez.multiagent.app.core.workflows.chat.models.ChatInput;
import es.agonzalez.multiagent.app.core.workflows.chat.models.ChatResult;
import es.agonzalez.multiagent.app.memory.MemoryService;
import org.slf4j.MDC;

public class GenerateStep implements Step<ChatInput, ChatResult> {
    
    private final LlmClient client;
    private final ModelRegistry models;
    private final MemoryService memory;
    private final ModelSelectors selector;
    private final MetricsService metrics;
    public GenerateStep(LlmClient client, MemoryService memory, ModelRegistry models, ModelSelectors selector, MetricsService metrics) {
        this.client = client;
        this.memory = memory;
        this.models = models;
        this.selector = selector;
        this.metrics = metrics;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Optional<ChatResult> apply(ChatInput input, Map<String, Object> context) {
        try {
            var history = (List<String>)context.getOrDefault("history", List.of());
            var summary = (String) context.getOrDefault("summary", "");

            String sysPrompt = """
                    Eres un bot amable, breve (máx 320 chars) y sin inventar.
                    Si la pregunta no es clara pide SOLO una aclaración.
                    No repitas el enunciado del usuario
                    """;
            if(input.username() != null && !input.username().isBlank()) {
                sysPrompt = """
                        Eres un Bot que se llama Botty, encargado de tener una conversacion lo mas natural posible con la siguiente persona %s, adaptate a su forma de hablar y en el idioma en que te hable. Frases cortas
                        no mas de 300 320 caracteres. Si no le entiendes algo preguntale, 
                        """.formatted(input.username());
            }

            var messages = new ArrayList<Message>();
            
                     
            messages.add(Message.system(sysPrompt));
            if(!summary.isBlank()) {
                messages.add(Message.system("Resumen previo del usuario:\n" + summary));
            }
            for(var line : last(history, 16)) {
                var parts = line.split("\t", 3);
                if(parts.length < 3) continue;
                var role = parts[1];
                var text = parts[2];
                messages.add(new Message(role.equals("assistant") ? "assistant" : "user", text));
            }

            String inp = input.text().startsWith("!ai") ? input.text().replace("!ai", "") : input.text();
            messages.add(Message.user(inp));

            String model = selector.pick("Agent.Chat", input.userId());
            MDC.put("model", model);
            Instant start = Instant.now();
            var props = models.defaults();
            LlmResponse resp;
            try {
                resp = client.chat(model, messages, props, false);
                long latency = Duration.between(start, Instant.now()).toMillis();
                metrics.recordLlmSuccess(model, input.intent(), resp.promptToken(), resp.completionToken(), latency);
            } catch (LlmException lex) {
                long latency = Duration.between(start, Instant.now()).toMillis();
                metrics.recordLlmErrorWithReason(model, input.intent(), lex.reason(), latency);
                throw lex;
            } catch (RuntimeException rte) {
                long latency = Duration.between(start, Instant.now()).toMillis();
                metrics.recordLlmErrorWithReason(model, input.intent(), "runtime", latency);
                throw rte;
            }

            String answer = cap(resp.content(), 320);

            memory.appendTurn(input.userId(), "user", input.text());
            memory.appendTurn(input.userId(), "assistant", answer);

            context.put("latencyMs", context.getOrDefault("latencyMs", -1));
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
