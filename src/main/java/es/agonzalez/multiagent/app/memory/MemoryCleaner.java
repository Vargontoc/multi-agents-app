package es.agonzalez.multiagent.app.memory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class MemoryCleaner {

    private final Path historyDir = Path.of("./data/history");
    private final Path summaryDir = Path.of("./data/summary");

    private final long ttlMs = TimeUnit.DAYS.toMillis(3);

    @Scheduled(fixedDelay = 6 * 60 * 60 * 1000) // cada 6h
    public void clean() {
        try {
            cleanDir(historyDir);
            cleanDir(summaryDir);
        } catch (IOException e) {
            System.err.println("Error limpiando memorias: " + e.getMessage());
        }
    }

    private void cleanDir(Path dir) throws  IOException{
        if (!Files.exists(dir)) return;
        try (var stream = Files.list(dir)) {
            stream.filter(Files::isRegularFile).forEach(p -> {
                try {
                var last = Files.getLastModifiedTime(p).toInstant();
                if (Instant.now().minusMillis(ttlMs).isAfter(last)) {
                    Files.deleteIfExists(p);
                }
                } catch (IOException ignored) {}
            });
        }
    }
}
