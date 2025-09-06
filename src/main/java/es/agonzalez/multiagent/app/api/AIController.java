package es.agonzalez.multiagent.app.api;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import es.agonzalez.multiagent.app.config.MetricsService;
import es.agonzalez.multiagent.app.core.IntentDetector;
import es.agonzalez.multiagent.app.core.workflows.chat.ChatWorkflow;
import es.agonzalez.multiagent.app.core.workflows.chat.models.ChatInput;
import es.agonzalez.multiagent.app.core.workflows.chat.models.ChatResult;
import es.agonzalez.multiagent.app.dtos.AIRequest;
import es.agonzalez.multiagent.app.dtos.AIResponse;


@RestController
@RequestMapping("/api/v1")
public class AIController {
    @Autowired
    private MetricsService metrics;
    @Autowired
    private IntentDetector intentDetector;
    @Autowired
    private ChatWorkflow chatFlow;

    @PostMapping("/chat")
    public ResponseEntity<AIResponse> chat(@RequestBody AIRequest req) {
        try {
            metrics.incMessages();
            String intent = (req.getIntent() != null && !req.getIntent().isBlank()) ? req.getIntent() : intentDetector.detect(req.getText());
            ChatInput input = new ChatInput(req.getUserId(), req.getText(), intent);
            ChatResult result = chatFlow.run(input);
            if(!"ok".equals(result.status())) metrics.incErrors();
            return ResponseEntity.ok().body(new AIResponse(
                result.status(), "Agent.Chat", result.message(), Map.of(
                    "userId", req.getUserId(), "intent", intent, "meta", result.data()
                )
            ));

        } catch (Exception e) {
            metrics.incErrors();
            throw e;
        }
    }
    
}
