package es.agonzalez.multiagent.app.memory;

import java.io.IOException;
import java.util.List;

public interface  MemoryStore {
    List<String> load(String userId) throws  IOException;
    void append(String userId, String role, String text) throws  IOException;
    void rotateIfNeeded(String userId) throws  IOException;
    int countLines(String userId) throws IOException;
    void clearUser(String userId) throws IOException;
}
