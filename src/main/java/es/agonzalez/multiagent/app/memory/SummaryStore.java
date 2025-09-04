package es.agonzalez.multiagent.app.memory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

@Component
public class SummaryStore {
    @Value("${multiagent.datadir}")
    private String dataDir;
    private Path baseDir;
    
    @PostConstruct
    public void init() throws  IOException {
        baseDir = Paths.get(dataDir, "summary");
        Files.createDirectories(baseDir);    
    }


    public String load(String userId) throws IOException {
        Path p = path(userId);
        if (!Files.exists(p)) return "";
        return Files.readString(p, StandardCharsets.UTF_8);
    }

    public void save(String userId, String summary) throws IOException {
        Path p = path(userId);
        Files.writeString(p, summary == null ? "" : summary.strip(), StandardCharsets.UTF_8,
            StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    private Path path(String userId) {
        String safe = userId == null ? "unknown" : userId.replaceAll("[^a-zA-Z0-9._-]","_");
        return baseDir.resolve(safe + ".md");
    }
}
