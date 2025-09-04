package es.agonzalez.multiagent.app.memory;

import java.io.IOException;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MemoryService {
    @Autowired
    private MemoryStore store;

    public List<String> loadHistory(String userId) throws IOException { return store.load(userId); }
    public void appendTurn(String userId, String role, String text) throws IOException {
        store.append(userId, role, text);
        store.rotateIfNeeded(userId);
    }
}
