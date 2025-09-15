package es.agonzalez.multiagent.app.memory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.annotation.PostConstruct;

@Component
public class MemoryFlagsStore {
    
    @Value("${multiagent.datadir}")
    private String dataDir;
    private Path basedir;
    private final ObjectMapper om;
    public MemoryFlagsStore(ObjectMapper om) { this.om = om; }

    @PostConstruct
    public void init() throws  IOException {
        basedir = Paths.get(dataDir, "flags");
        Files.createDirectories(basedir);    
    }

    private Path path(String userId) {
        String safe = userId.replaceAll("[^a-zA-Z0-9._-]", "_");
        return basedir.resolve(safe + ".json");
    }

    public boolean isEnabled(String userId) {
        try {
        Path p = path(userId);
        if (!Files.exists(p)) return true; // por defecto activado
        Map<?,?> map = om.readValue(Files.readString(p), Map.class);
        Object v = map.get("enabled");
        return v == null || Boolean.parseBoolean(v.toString());
        } catch (IOException e) {
        return true;
        }
    }

    public void setEnabled(String userId, boolean enabled) throws IOException {
        Path p = path(userId);
        Map<String,Object> map = new HashMap<>();
        map.put("enabled", enabled);
        Files.writeString(p, om.writeValueAsString(map), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    public void clear(String userId) throws IOException {
        Path p = path(userId);
        Files.deleteIfExists(p);
    }

}
