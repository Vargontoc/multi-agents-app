package es.agonzalez.multiagent.app.memory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

@Component
public class SummaryStore {
    private static final Logger log = LoggerFactory.getLogger(SummaryStore.class);
    @Autowired
    private es.agonzalez.multiagent.app.config.AppProperties props;
    private String dataDir;
    private Path baseDir;
    private final ConcurrentHashMap<String, ReentrantLock> locks = new ConcurrentHashMap<>();
    
    @PostConstruct
    public void init() throws  IOException {
        this.dataDir = props.getDatadir();
        baseDir = Paths.get(dataDir, "summary");
        Files.createDirectories(baseDir);
    }


    public String load(String userId) throws IOException {
        Path pTxt = pathTxt(userId);
        Path pLegacyMd = pathMd(userId);
        ReentrantLock lock = lockFor(userId);
        lock.lock();
        try {
            if (Files.exists(pTxt)) {
                return Files.readString(pTxt, StandardCharsets.UTF_8);
            }
            if (Files.exists(pLegacyMd)) {
                // Migración: renombrar a .txt
                try {
                    Files.move(pLegacyMd, pTxt);
                    log.info("Migrado summary .md -> .txt userId={}", userId);
                    return Files.readString(pTxt, StandardCharsets.UTF_8);
                } catch (IOException moveEx) {
                    log.warn("No se pudo migrar summary .md a .txt userId={} error={}", userId, moveEx.toString());
                    return Files.readString(pLegacyMd, StandardCharsets.UTF_8);
                }
            }
            return "";
        } finally {
            lock.unlock();
        }
    }

    public void save(String userId, String summary) throws IOException {
        Path p = pathTxt(userId);
        ReentrantLock lock = lockFor(userId);
        lock.lock();
        try {
            Files.writeString(p, summary == null ? "" : summary.strip(), StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            log.debug("Resumen actualizado userId={} bytes={}", userId, summary == null ? 0 : summary.length());
        } finally {
            lock.unlock();
        }
    }

    private Path pathMd(String userId) {
        String safe = userId == null ? "unknown" : userId.replaceAll("[^a-zA-Z0-9._-]","_");
        return baseDir.resolve(safe + ".md");
    }

    private Path pathTxt(String userId) {
        String safe = userId == null ? "unknown" : userId.replaceAll("[^a-zA-Z0-9._-]","_");
        return baseDir.resolve(safe + ".txt");
    }

    private ReentrantLock lockFor(String userId) {
        String key = userId == null ? "unknown" : userId.replaceAll("[^a-zA-Z0-9._-]","_");
        return locks.computeIfAbsent(key, k -> new ReentrantLock());
    }
}
