package es.agonzalez.multiagent.app.api;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import es.agonzalez.multiagent.app.core.ModelRegistry;
import es.agonzalez.multiagent.app.core.selectors.CanaryConfig;
import es.agonzalez.multiagent.app.core.selectors.ModelSelectors;



@RestController
@RequestMapping("/admin")
public class AdminController {
    @Value("${multiagent.llm.url}")
    private String uri;

    @Autowired
    private ModelRegistry registry;
    @Autowired
    private ModelSelectors selector;

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> checkHealth() {
        try {
            var url = URI.create(uri + "/api/tags");
            var client = HttpClient.newHttpClient();
            var res = client.send(HttpRequest.newBuilder(url).GET().build(), HttpResponse.BodyHandlers.ofString());
            return ResponseEntity.ok().body(Map.of(
                "status", res.statusCode(),
                "body", res.body()
            ));
        } catch (InterruptedException | IOException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    

    @GetMapping("/models")
    public ResponseEntity<Map<String, Object>> listModels() {
        return ResponseEntity.ok().body(Map.of(
            "agents", registry.currentAgents(),
            "defaults", registry.defaults(),
            "canary", selector.getCanary()
        ));
    }

    @PostMapping("/models/switch")
    public ResponseEntity<Map<String, Object>> getMethodName(@RequestBody Map<String, Object> body) {
        String agent = String.valueOf(body.get("agent"));
        String stable = String.valueOf(body.get("stable"));
        String canary = String.valueOf(body.get("canary"));
        int percent = Integer.parseInt(String.valueOf(body.getOrDefault("percent", 0)));

        var current = selector.getCanary().porAgent();
        var newMap = new HashMap<>(current);

        newMap.put(agent, new CanaryConfig.Entry(stable, canary, percent));
        selector.setCanaryConfig(new CanaryConfig(newMap));

        return ResponseEntity.ok().body(Map.of(
            "status", "ok",
            "agent", agent,
            "stable", stable,
            "canary", canary,
            "percent", percent
        ));
    }

    @DeleteMapping("/models/canary")
    public ResponseEntity<Map<String, Object>> clear(@RequestParam("agent") String agent) {
        var current = selector.getCanary().porAgent();
        var newMap = new HashMap<>(current);

        newMap.remove(agent);
        selector.setCanaryConfig(new CanaryConfig(newMap));

        return ResponseEntity.ok().body(Map.of(
            "status", "ok", "agent", agent
        ));
    }

    
}
