package es.agonzalez.multiagent.app.core;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

@Component
public class ModelRegistry {
    
    private final ObjectMapper yaml = new ObjectMapper(new YAMLFactory());
    private volatile Map<String, Object> agentToModel = Collections.emptyMap();
    private volatile Map<String, Object> defaults = Collections.emptyMap();
    private volatile Map<String, Object> overrides = Collections.emptyMap();

    public ModelRegistry(@Value("${multiagent.modelconfig}") Resource config) {
        load(config);
    }


    @SuppressWarnings("unchecked")
    private void load(Resource config) {
        try {
            Map<String, Object> root = yaml.readValue(config.getInputStream(), Map.class);
            this.agentToModel = (Map<String,  Object>) root.getOrDefault("agents", Collections.emptyMap());
            this.defaults = (Map<String,  Object>) root.getOrDefault("defaults", Collections.emptyMap());
        } catch (IOException e) {
            throw new IllegalStateException("No pude cargar models.yml: " +  e.getMessage(), e);
        }
    }

    public synchronized  void overrideInMemory(String agent, String model) {
        var newMap = new HashMap<String, Object>(agentToModel);
        newMap.put(agent, model);
        agentToModel = Collections.unmodifiableMap(newMap);

        var over = new HashMap<String, Object>(overrides);
        over.put(agent, model);
        overrides = Collections.unmodifiableMap(over);

    }

    public String modelForÇ(String name) {
        return (String)agentToModel.getOrDefault(name, "llama3.2:3b");
    }

    public Map<String, Object> defaults(){ return defaults; }
    public Map<String, Object> currentAgents(){ return agentToModel; }
}
