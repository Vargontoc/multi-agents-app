package es.agonzalez.multiagent.app.memory;

import java.io.IOException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MemoryService {
    private static final Logger log = LoggerFactory.getLogger(MemoryService.class);
    @Autowired
    private MemoryStore store;

    public List<String> loadHistory(String userId) throws IOException { 
        try {
            return store.load(userId); 
        } catch (IOException e) {
            log.warn("No se pudo cargar historial userId={}", userId, e);
            throw e;
        }
    }
    public void appendTurn(String userId, String role, String text) throws IOException {
        try {
            store.append(userId, role, text);
            store.rotateIfNeeded(userId);
        } catch (IOException e) {
            log.warn("No se pudo guardar turno userId={} role={}", userId, role, e);
            throw e;
        }
    }
}
