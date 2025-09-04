package es.agonzalez.multiagent.app.memory;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import es.agonzalez.multiagent.app.core.LlmClient;
import es.agonzalez.multiagent.app.core.ModelRegistry;
import es.agonzalez.multiagent.app.core.models.Message;

@Component
public class Summarizer {
    @Value("${multiagent.summarization-every}")
    private  int every;
    @Autowired
    private LlmClient client;
    @Autowired
    private ModelRegistry registry;

    public boolean shouldSummarize(int totalTurns) { return totalTurns > 0 && totalTurns % every == 0; }

    public String summarize(List<String> historyLines) {
        var recent = historyLines.size() > 120 ? historyLines.subList(historyLines.size() - 120, historyLines.size()) : historyLines;
        String conversation = recent.stream().map(l -> {
            var parts = l.split("\t", 3);
            if(parts.length < 3) return "";
            var role = parts[1];
            var text = parts[2];
            return (role.equals("assistant")) ? "Asistente: " : "Usuario: " + text;
        }).filter(s -> !s.isBlank()).reduce("", (a, b) -> a + b + "\n");

        var sys = """
                Resume la conversación en 3-5 viñetas, neutras y sin inventar.
                No incluyas datos sensibles; máximo 400 caracteres.
                """;

        var resp = client.chat(registry.modelForÇ("Agent.Chat"), List.of(
        Message.system(sys),    
        Message.user(conversation)), registry.defaults());

        var s = resp.contet() == null ? "" : resp.contet().strip();
        return s.length()    <= 500 ? s : s.substring(0,499) + "...";
    }

}
