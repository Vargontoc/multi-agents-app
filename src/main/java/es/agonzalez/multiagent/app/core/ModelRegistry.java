package es.agonzalez.multiagent.app.core;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import jakarta.annotation.PostConstruct;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

@Component
public class ModelRegistry {

    /** YAML parser para el fichero de modelos */
    private final ObjectMapper yaml = new ObjectMapper(new YAMLFactory());

    /**
     * Mapa actual agente->modelo. Se reemplaza atómicamente (volatile) en cada override para dar
     * snapshot inmutable a lectores sin necesidad de locks de lectura.
     */
    private volatile Map<String, Object> agentToModel = Collections.emptyMap();

    /** Valores por defecto cargados del YAML (inmutables una vez cargados) */
    private volatile Map<String, Object> defaults = Collections.emptyMap();

    /** Overrides aplicados en runtime (no persistidos). */
    private final Map<String, Object> overridesMutable = new ConcurrentHashMap<>();
    private volatile Map<String, Object> overridesView = Collections.emptyMap();

    @Autowired
    private es.agonzalez.multiagent.app.config.AppProperties props;

    private Path overridesFile;

    public ModelRegistry(@Value("${multiagent.modelconfig}") Resource config) {
        load(config);
    }


    @SuppressWarnings("unchecked")
    private void load(Resource config) {
        try {
            Map<String, Object> root = yaml.readValue(config.getInputStream(), Map.class);
            Map<String, Object> agents = (Map<String, Object>) root.getOrDefault("agents", Collections.emptyMap());
            Map<String, Object> defs = (Map<String, Object>) root.getOrDefault("defaults", Collections.emptyMap());
            // snapshots inmutables iniciales
            this.agentToModel = Collections.unmodifiableMap(new HashMap<>(agents));
            this.defaults = Collections.unmodifiableMap(new HashMap<>(defs));
            this.overridesView = Collections.unmodifiableMap(new HashMap<>()); // vacío inicial
        } catch (IOException e) {
            throw new IllegalStateException("No pude cargar models.yml: " +  e.getMessage(), e);
        }
    }

    @PostConstruct
    @SuppressWarnings("unchecked")
    void initOverrides() {
        try {
            Path modelsDir = Paths.get(props.getDatadir(), "models");
            Files.createDirectories(modelsDir);
            overridesFile = modelsDir.resolve("overrides.json");
            if (Files.exists(overridesFile)) {
                // Cargar overrides persistidos y aplicarlos sobre snapshot inicial
                Map<String,Object> persisted = yaml.readValue(Files.readString(overridesFile, StandardCharsets.UTF_8), Map.class);
                if (!persisted.isEmpty()) {
                    overridesMutable.putAll(persisted);
                    overridesView = Collections.unmodifiableMap(new HashMap<>(overridesMutable));
                    var newMap = new HashMap<String,Object>(agentToModel);
                    newMap.putAll(persisted);
                    agentToModel = Collections.unmodifiableMap(newMap);
                }
            }
        } catch (IOException ex) {
            // No detener la aplicación por fallo de carga de overrides; loggear y continuar
            System.err.println("[ModelRegistry] No se pudieron cargar overrides persistidos: " + ex.getMessage());
        }
    }

    /**
     * Aplica un override sólo en memoria. No se persiste en disco deliberadamente.
     * Thread-safety: método sincronizado para crear un nuevo snapshot inmutable visible para lectores.
     */
    public synchronized void overrideInMemory(String agent, String model) {
        var newMap = new HashMap<String, Object>(agentToModel);
        newMap.put(agent, model);
        agentToModel = Collections.unmodifiableMap(newMap);

        overridesMutable.put(agent, model);
        // generar nueva vista inmutable de overrides (snapshot) para quien la exponga públicamente
        overridesView = Collections.unmodifiableMap(new HashMap<>(overridesMutable));
        // persistir en disco (best-effort)
        persistOverrides();
    }

    // Renombrado: método previamente llamado modelForÇ (carácter no ASCII) para evitar problemas de tooling
    public String modelFor(String name) {
        return (String) agentToModel.getOrDefault(name, "llama3.2:3b");
    }

    public Map<String, Object> defaults(){ return defaults; }
    public Map<String, Object> currentAgents(){ return agentToModel; }
    /** Overrides activos en memoria (snapshot inmutable). */
    public Map<String, Object> overrides(){ return overridesView; }

    private void persistOverrides() {
        if (overridesFile == null) return; // aún no inicializado
        try {
            String json = yaml.writeValueAsString(overridesMutable);
            Files.writeString(overridesFile, json, StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.println("[ModelRegistry] Fallo al persistir overrides: " + e.getMessage());
        }
    }
}
