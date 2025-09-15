package es.agonzalez.multiagent.app.api;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import es.agonzalez.multiagent.app.dtos.SwitchModelRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;



@RestController
@RequestMapping("/admin")
@Tag(name = "Administración", description = "Gestión de modelos y health checks")
public class AdminController {
    private static final Logger log = LoggerFactory.getLogger(AdminController.class);

    @Value("${multiagent.llm.url}")
    private String uri;

    @Autowired
    private ModelRegistry registry;
    @Autowired
    private ModelSelectors selector;

    @GetMapping("/health")
    @Operation(summary = "Health del backend LLM", description = "Verifica acceso al endpoint remoto de modelos.")
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
            log.warn("Fallo health-check contra LLM endpoint={} error={}", uri, e.toString());
            return ResponseEntity.badRequest().body(Map.of("status","error","error", e.getClass().getSimpleName(), "message", e.getMessage()));
        }
    }
    

    @GetMapping("/models")
    @Operation(summary = "Lista modelos conocidos", description = "Devuelve mapeo de agentes a modelos y overrides actuales.")
    public ResponseEntity<Map<String, Object>> listModels() {
        return ResponseEntity.ok().body(Map.of(
            "agents", registry.currentAgents(),
            "defaults", registry.defaults(),
            "canary", selector.getCanary()
        ));
    }

    /**
     * Endpoint de solo lectura para inspeccionar la configuración canary actual.
     * Devuelve, por cada agente configurado, el modelo estable, el canario (si existe)
     * y el porcentaje de tráfico dirigido al canario.
     */
    @GetMapping("/models/canary")
    @Operation(summary = "Config canary actual", description = "Devuelve configuración canary por agente (stable, canary, percentage).")
    public ResponseEntity<Map<String, Object>> canaryConfig() {
        var cfg = selector.getCanary();
        return ResponseEntity.ok(Map.of(
            "status", "ok",
            "canary", cfg.porAgent()
        ));
    }

    @PostMapping("/models/switch")
    @Operation(summary = "Actualiza configuración de un agente", description = "Define stable, canary y porcentaje para dirigir tráfico.")
    public ResponseEntity<Map<String, Object>> switchModel(@jakarta.validation.Valid @RequestBody SwitchModelRequest req) {
        String agent = req.agent();
        String stable = req.stable();
        String canary = (req.canary() == null || req.canary().isBlank()) ? null : req.canary();
        int percent = req.percent();

        // Validaciones de percent/canary ya aplicadas vía Bean Validation en SwitchModelRequest

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
    @Operation(summary = "Elimina canary de un agente", description = "Quita configuración canary dejando sólo el modelo estable.")
    public ResponseEntity<Map<String, Object>> removeCanary(@RequestParam("agent") String agent) {
        var current = selector.getCanary().porAgent();
        var newMap = new HashMap<>(current);

        newMap.remove(agent);
        selector.setCanaryConfig(new CanaryConfig(newMap));

        return ResponseEntity.ok().body(Map.of(
            "status", "ok",
            "agent", agent,
            "action", "canary-removed"
        ));
    }

    
}
