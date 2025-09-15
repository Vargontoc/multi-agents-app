package es.agonzalez.multiagent.app.memory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class MemoryCleaner {

    private static final Logger log = LoggerFactory.getLogger(MemoryCleaner.class);

    private final Path historyDir = Path.of("./data/history");
    private final Path summaryDir = Path.of("./data/summary");

    private final long ttlMs = TimeUnit.DAYS.toMillis(3);

    @Scheduled(fixedDelay = 6 * 60 * 60 * 1000) // cada 6h
    public void clean() {
        long start = System.currentTimeMillis();
        int[] deleted = new int[]{0};
        try {
            deleted[0] += cleanDir(historyDir);
            deleted[0] += cleanDir(summaryDir);
            log.debug("MemoryCleaner ejecutado. Archivos eliminados: {} en {} ms", deleted[0], System.currentTimeMillis()-start);
        } catch (IOException e) {
            log.warn("Error limpiando memorias", e);
        }
    }

    private int cleanDir(Path dir) throws  IOException{
        if (!Files.exists(dir)) return 0;
        int[] count = new int[]{0};
        try (var stream = Files.list(dir)) {
            stream.filter(Files::isRegularFile).forEach(p -> {
                try {
                    var last = Files.getLastModifiedTime(p).toInstant();
                    if (Instant.now().minusMillis(ttlMs).isAfter(last)) {
                        if (Files.deleteIfExists(p)) {
                            count[0]++;
                        }
                    }
                } catch (IOException ignored) {}
            });
        }
        return count[0];
    }
}
