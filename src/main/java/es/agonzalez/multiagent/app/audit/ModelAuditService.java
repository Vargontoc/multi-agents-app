package es.agonzalez.multiagent.app.audit;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import es.agonzalez.multiagent.app.config.AppProperties;
import es.agonzalez.multiagent.app.core.selectors.CanaryConfig;

/**
 * Servicio de auditoría para cambios administrativos de configuración de modelos (stable/canary/percent).
 * Registra eventos JSONL (una línea por evento) en {@code datadir/audit/model-changes.log} y también emite
 * un log estructurado nivel INFO para facilitar ingestión en sistemas centralizados.
 */
@Service
public class ModelAuditService {
    private static final Logger log = LoggerFactory.getLogger(ModelAuditService.class);

    public static final String HEADER_ADMIN_USER = "X-Admin-User";

    @Autowired
    private AppProperties props;
    @Autowired
    private ObjectMapper mapper;

    private final ReentrantLock lock = new ReentrantLock();
    private Path auditFile;

    @jakarta.annotation.PostConstruct
    public void init() {
        try {
            Path dir = Path.of(props.getDatadir(), "audit");
            Files.createDirectories(dir);
            auditFile = dir.resolve("model-changes.log");
        } catch (IOException e) {
            log.error("No se pudo inicializar directorio de auditoría: {}", e.toString());
        }
    }

    public void auditModelSwitch(String agent, CanaryConfig.Entry previous, CanaryConfig.Entry updated,
                                 String requestId, String apiKeyHash, String remoteIp, String adminUser) {
        Map<String,Object> evt = base("model_switch", agent, previous, updated, requestId, apiKeyHash, remoteIp, adminUser);
        write(evt);
    }

    public void auditCanaryRemoval(String agent, CanaryConfig.Entry previous,
                                   String requestId, String apiKeyHash, String remoteIp, String adminUser) {
        // updated pasa a null (config eliminada)
        Map<String,Object> evt = base("canary_remove", agent, previous, null, requestId, apiKeyHash, remoteIp, adminUser);
        write(evt);
    }

    private Map<String,Object> base(String type, String agent, CanaryConfig.Entry prev, CanaryConfig.Entry next,
                                    String requestId, String apiKeyHash, String remoteIp, String adminUser) {
        Map<String,Object> m = new LinkedHashMap<>();
        m.put("@timestamp", Instant.now().toString());
        m.put("type", type);
        m.put("agent", agent);
        if (prev != null) {
            m.put("stable_before", prev.stable());
            m.put("canary_before", prev.canary());
            m.put("percent_before", prev.percentage());
        }
        if (next != null) {
            m.put("stable_after", next.stable());
            m.put("canary_after", next.canary());
            m.put("percent_after", next.percentage());
        }
        m.put("requestId", requestId);
        if (apiKeyHash != null) m.put("apiKeyHash", apiKeyHash);
        if (remoteIp != null) m.put("remoteIp", remoteIp);
        if (adminUser != null && !adminUser.isBlank()) m.put("adminUser", adminUser);
        return m;
    }

    private void write(Map<String,Object> evt) {
        if (auditFile == null) {
            log.warn("Audit file path no inicializado; evento omitido type={} agent={}", evt.get("type"), evt.get("agent"));
            return;
        }
        String json;
        try { json = mapper.writeValueAsString(evt); }
        catch (IOException e) {
            log.error("No se pudo serializar evento de auditoría: {}", e.toString());
            return;
        }
        lock.lock();
        try {
            Files.writeString(auditFile, json + System.lineSeparator(), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            log.error("Fallo escribiendo auditoría en {}: {}", auditFile, e.toString());
        } finally {
            lock.unlock();
        }
        log.info("AUDIT_MODEL_CHANGE {}", json); // Línea estructurada adicional para downstream (prefijo para fácil grep)
    }

    /**
     * Devuelve los últimos N eventos (orden cronológico del archivo conservado). Operación O(N) sobre total de líneas
     * si el archivo es grande; aceptable mientras el volumen sea bajo. Para escalar, considerar un índice o truncado.
     */
    public List<Map<String,Object>> tail(int limit) {
        var result = new ArrayList<Map<String,Object>>();
        if (auditFile == null || !Files.exists(auditFile)) return result;
        List<String> lines;
        try { lines = Files.readAllLines(auditFile, StandardCharsets.UTF_8); }
        catch (IOException e) { log.warn("No se pudieron leer eventos auditoría: {}", e.toString()); return result; }
        int start = Math.max(0, lines.size() - limit);
        for (int i = start; i < lines.size(); i++) {
            String ln = lines.get(i);
            try {
                @SuppressWarnings("unchecked") Map<String,Object> parsed = mapper.readValue(ln, Map.class);
                result.add(parsed);
            } catch (JsonProcessingException | IllegalArgumentException ex) {
                log.debug("Línea de auditoría malformada ignorada index={} error={}", i, ex.toString());
            }
        }
        return result;
    }

    /**
     * Versión con filtros: devuelve hasta 'limit' eventos que cumplan (si se especifican) agent y since.
     * since: ISO-8601 instant (ej: 2025-09-17T19:00:00Z). Si parse falla → se ignora filtro (fail-open) y se loggea debug.
     * Orden de salida: cronológico natural de aparición (igual que tail original) sobre el subconjunto filtrado.
     */
    public List<Map<String,Object>> tailFiltered(int limit, String agent, String sinceIso) {
        var result = new ArrayList<Map<String,Object>>();
        if (auditFile == null || !Files.exists(auditFile)) return result;
        Instant since = null;
        if (sinceIso != null && !sinceIso.isBlank()) {
            try { since = Instant.parse(sinceIso.trim()); }
            catch (Exception ex) { log.debug("Parametro since inválido '{}' ignorado: {}", sinceIso, ex.toString()); }
        }
        List<String> lines;
        try { lines = Files.readAllLines(auditFile, StandardCharsets.UTF_8); }
        catch (IOException e) { log.warn("No se pudieron leer eventos auditoría: {}", e.toString()); return result; }
        // Recorremos desde el final para evitar parsear más de lo necesario; acumulamos reverso y luego invertimos.
        int parsedCount = 0;
        for (int i = lines.size() - 1; i >= 0 && parsedCount < limit * 4; i--) { // heurística: parsear hasta 4x el límite máximo intentando filtrar
            String ln = lines.get(i);
            Map<String,Object> parsed;
            try {
                @SuppressWarnings("unchecked") Map<String,Object> temp = mapper.readValue(ln, Map.class);
                parsed = temp;
            } catch (JsonProcessingException | IllegalArgumentException ex) {
                log.debug("Línea auditoría malformada ignorada index={} error={}", i, ex.toString());
                continue;
            }
            parsedCount++;
            if (agent != null && !agent.isBlank()) {
                Object a = parsed.get("agent");
                if (a == null || !agent.equals(a.toString())) continue;
            }
            if (since != null) {
                Object ts = parsed.get("@timestamp");
                if (ts instanceof String s) {
                    try {
                        Instant eventTs = Instant.parse(s);
                        if (eventTs.isBefore(since)) continue;
                    } catch (Exception ignore) { continue; }
                } else {
                    continue; // sin timestamp no podemos aplicar filtro → se omite
                }
            }
            result.add(parsed);
            if (result.size() == limit) break;
        }
        // Están en orden inverso por lectura desde el final; revertimos
        java.util.Collections.reverse(result);
        return result;
    }
}
