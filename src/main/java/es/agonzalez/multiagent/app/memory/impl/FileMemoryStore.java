package es.agonzalez.multiagent.app.memory.impl;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import es.agonzalez.multiagent.app.memory.MemoryStore;
import jakarta.annotation.PostConstruct;

@Component
public class FileMemoryStore  implements MemoryStore{
    @Value("${multiagent.datadir}")
    private String dataDir;
    @Value("${multiagent.max-history-lines}")
    private int maxLines;
    private Path basedir;

    @PostConstruct
    public void init() throws  IOException {
        basedir = Paths.get(dataDir, "history");
        Files.createDirectories(basedir);    
    }

    @Override
    public List<String> load(String userId) throws IOException {
        Path p = pathOf(userId);
        if(!Files.exists(p)) return Collections.emptyList();
        return Files.readAllLines(p, StandardCharsets.UTF_8);
    }

    @Override
    public void append(String userId, String role, String text) throws IOException {
        Path p = pathOf(userId);
        String safe = sanitizeSingleLine(text);
        String line = "%s\t%s\t%s%n".formatted(Instant.now().toString(), role, safe);
        Files.writeString(p, line, StandardCharsets.UTF_8,
        StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    @Override
    public void rotateIfNeeded(String userId) throws IOException {
        Path p = pathOf(userId);
        if (!Files.exists(p)) return;
        long lines = Files.lines(p).count();
        if (lines > maxLines) {
            String rotatedName = fileName(userId).replace(".txt", "-" + System.currentTimeMillis() + ".log");
            Path rotated = p.resolveSibling(rotatedName);
            Files.move(p, rotated, StandardCopyOption.ATOMIC_MOVE);
            // crea archivo nuevo vacío para continuar histórico
            Files.createFile(p);
        }
    }

    @Override
    public int countLines(String userId) throws IOException {
        Path p = pathOf(userId);
        if (!Files.exists(p)) return 0;
        try (var s = Files.lines(p)) {
            return (int) s.count();
        }
    }


    @Override
    public void clearUser(String userId) throws IOException {
        Path hist = Path.of("./data/history", userId + ".txt" );
        Files.deleteIfExists(hist);

        Path sum = Path.of("./data/summary", userId + ".txt" );
        Files.deleteIfExists(sum);
    }

    private Path pathOf(String userId) {
        return basedir.resolve(fileName(userId));
    }

    private String fileName(String userId) {
        return sanitizeUserId(userId) + ".txt";
    }

    private static String sanitizeUserId(String s) {
        if (s == null || s.isBlank()) return "unknown";
        return s.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private static String sanitizeSingleLine(String s) {
        if (s == null) return "";
        // Quita saltos de línea y tabuladores para no romper el formato TSV
        return s.replaceAll("\\R", " ").replace("\t", " ").trim();
    }


}
