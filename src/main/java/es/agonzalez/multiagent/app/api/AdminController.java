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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import es.agonzalez.multiagent.app.audit.ModelAuditService;
import es.agonzalez.multiagent.app.config.AppProperties;
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

    @Autowired
    private AppProperties appProperties;
    @Autowired
    private ModelRegistry registry;
    @Autowired
    private ModelSelectors selector;
    @Autowired
    private ModelAuditService audit;

    @GetMapping("/health")
    @Operation(summary = "Health del backend LLM", description = "Verifica acceso al endpoint remoto de modelos.")
    public ResponseEntity<Map<String, Object>> checkHealth() {
        try {
            String llmUrl = appProperties.getLlm().getUrl();
            var url = URI.create(llmUrl + "/api/tags");
            var client = HttpClient.newHttpClient();
            var res = client.send(HttpRequest.newBuilder(url).GET().build(), HttpResponse.BodyHandlers.ofString());
            return ResponseEntity.ok().body(Map.of(
                "status", res.statusCode(),
                "body", res.body()
            ));
        } catch (InterruptedException | IOException e) {
            log.warn("Fallo health-check contra LLM endpoint={} error={}", appProperties.getLlm().getUrl(), e.toString());
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
    public ResponseEntity<Map<String, Object>> switchModel(@jakarta.validation.Valid @RequestBody SwitchModelRequest req, jakarta.servlet.http.HttpServletRequest servletRequest) {
        String agent = req.agent();
        String stable = req.stable();
        String canary = (req.canary() == null || req.canary().isBlank()) ? null : req.canary();
        int percent = req.percent();

        // Validaciones de percent/canary ya aplicadas vía Bean Validation en SwitchModelRequest
        var currentCfg = selector.getCanary();
        var current = currentCfg.porAgent();
        var previous = current.get(agent);
        var newMap = new HashMap<>(current);
        var updatedEntry = new CanaryConfig.Entry(stable, canary, percent);
        newMap.put(agent, updatedEntry);
        selector.setCanaryConfig(new CanaryConfig(newMap));

        // Auditoría
        String requestId = org.slf4j.MDC.get("requestId");
        String apiKeyHash = org.slf4j.MDC.get("apiKeyHash");
        String remoteIp = servletRequest.getRemoteAddr();
        String adminUser = servletRequest.getHeader(ModelAuditService.HEADER_ADMIN_USER); // opcional
        audit.auditModelSwitch(agent, previous, updatedEntry, requestId, apiKeyHash, remoteIp, adminUser);
    // Usar HashMap porque Map.of no admite valores null (canary puede ser null cuando se desactiva)
    var resp = new HashMap<String,Object>();
    resp.put("status", "ok");
    resp.put("agent", agent);
    resp.put("stable", stable);
    resp.put("canary", canary); // puede ser null y Jackson lo serializa como null
    resp.put("percent", percent);
    return ResponseEntity.ok().body(resp);
    }

    @DeleteMapping("/models/canary")
    @Operation(summary = "Elimina canary de un agente", description = "Quita configuración canary dejando sólo el modelo estable.")
    public ResponseEntity<Map<String, Object>> removeCanary(@RequestParam("agent") String agent, jakarta.servlet.http.HttpServletRequest servletRequest) {
        var currentCfg = selector.getCanary();
        var current = currentCfg.porAgent();
        var previous = current.get(agent);
        var newMap = new HashMap<>(current);

        newMap.remove(agent); // eliminación completa del entry para el agente
        selector.setCanaryConfig(new CanaryConfig(newMap));

        // Auditoría (solo si había algo que eliminar)
        if (previous != null) {
            String requestId = org.slf4j.MDC.get("requestId");
            String apiKeyHash = org.slf4j.MDC.get("apiKeyHash");
            String remoteIp = servletRequest.getRemoteAddr();
            String adminUser = servletRequest.getHeader(ModelAuditService.HEADER_ADMIN_USER);
            audit.auditCanaryRemoval(agent, previous, requestId, apiKeyHash, remoteIp, adminUser);
        }

        return ResponseEntity.ok().body(Map.of(
                "status", "ok",
                "agent", agent,
                "action", "canary-removed"
            ));
    }

    @GetMapping("/model-audit")
    @Operation(summary = "Eventos de auditoría filtrables", description = "Devuelve los eventos más recientes de auditoría (model_switch / canary_remove) con filtros opcionales. Parámetros: limit (1-500), agent (exact match) y since (ISO-8601 instant, ej: 2025-09-17T19:00:00Z). Si since es inválido se ignora (fail-open).")
    public ResponseEntity<Map<String,Object>> tailAudit(
            @RequestParam(name = "limit", defaultValue = "50") int limit,
            @RequestParam(name = "agent", required = false) String agent,
            @RequestParam(name = "since", required = false) String since
        ) {
        if (limit < 1) limit = 1;
        if (limit > 500) limit = 500; // cota de seguridad
        var events = (agent == null && since == null) ? audit.tail(limit) : audit.tailFiltered(limit, agent, since);
        return ResponseEntity.ok(Map.of(
                "status", "ok",
                "count", events.size(),
                "events", events,
                "appliedFilters", Map.of(
                    "agent", agent == null ? "" : agent,
                    "since", since == null ? "" : since
                )
            ));
    }

    
}
