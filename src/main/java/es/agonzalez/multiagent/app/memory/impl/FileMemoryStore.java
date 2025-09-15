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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import es.agonzalez.multiagent.app.memory.MemoryStore;
import jakarta.annotation.PostConstruct;

@Component
public class FileMemoryStore  implements MemoryStore{
    private static final Logger log = LoggerFactory.getLogger(FileMemoryStore.class);
    @Autowired
    private es.agonzalez.multiagent.app.config.AppProperties props;
    private String dataDir;
    private int maxLines;
    private Path basedir;
    // Locks por usuario para evitar condiciones de carrera en rotación y escritura
    private final ConcurrentHashMap<String, ReentrantLock> userLocks = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() throws  IOException {
        this.dataDir = props.getDatadir();
        this.maxLines = props.getMaxHistoryLines();
        basedir = Paths.get(dataDir, "history");
        Files.createDirectories(basedir);
    }

    @Override
    public List<String> load(String userId) throws IOException {
        Path p = pathOf(userId);
        if(!Files.exists(p)) return Collections.emptyList();
        ReentrantLock lock = lockFor(userId);
        lock.lock();
        try {
            return Files.readAllLines(p, StandardCharsets.UTF_8);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void append(String userId, String role, String text) throws IOException {
        Path p = pathOf(userId);
        String safe = sanitizeSingleLine(text);
        int maxLen = props.getMaxLineLength();
        if (safe.length() > maxLen) {
            String original = safe;
            safe = safe.substring(0, Math.max(0, maxLen - 20)) + "...[truncated]";
            log.debug("Truncado input largo userId={} role={} originalLength={} newLength={}", userId, role, original.length(), safe.length());
        }
        String line = "%s\t%s\t%s%n".formatted(Instant.now().toString(), role, safe);
        ReentrantLock lock = lockFor(userId);
        lock.lock();
        try {
            Files.writeString(p, line, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            log.warn("Fallo al escribir historial userId={}", userId, e);
            throw e;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void rotateIfNeeded(String userId) throws IOException {
        Path p = pathOf(userId);
        if (!Files.exists(p)) return;
        ReentrantLock lock = lockFor(userId);
        lock.lock();
        try (var stream = Files.lines(p)) {
            long lines = stream.count();
            if (lines > maxLines) {
                String rotatedName = fileName(userId).replace(".txt", "-" + System.currentTimeMillis() + ".log");
                Path rotated = p.resolveSibling(rotatedName);
                boolean rotatedOk = false;
                try {
                    Files.move(p, rotated, StandardCopyOption.ATOMIC_MOVE);
                    rotatedOk = true;
                } catch (IOException atomicEx) {
                    log.debug("ATOMIC_MOVE falló para userId={}, intentando move estándar: {}", userId, atomicEx.getMessage());
                    try {
                        Files.move(p, rotated); // fallback no atómico
                        rotatedOk = true;
                    } catch (IOException fallbackEx) {
                        log.error("Fallback move también falló userId={} error={}", userId, fallbackEx.toString());
                        throw fallbackEx;
                    }
                }
                if(rotatedOk) {
                    try {
                        Files.createFile(p);
                    } catch (IOException recreateEx) {
                        log.error("Historial rotado pero no se pudo recrear archivo vacío userId={} error={}", userId, recreateEx.toString());
                        throw recreateEx;
                    }
                    log.debug("Rotado historial userId={} -> {} (lineas={})", userId, rotated.getFileName(), lines);
                }
            }
        } catch (IOException e) {
            log.error("Error rotando historial userId={}", userId, e);
            throw e;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public int countLines(String userId) throws IOException {
        Path p = pathOf(userId);
        if (!Files.exists(p)) return 0;
        ReentrantLock lock = lockFor(userId);
        lock.lock();
        try (var s = Files.lines(p)) {
            return (int) s.count();
        } catch (IOException e) {
            log.debug("No se pudo contar líneas userId={}", userId, e);
            throw e;
        } finally {
            lock.unlock();
        }
    }


    @Override
    public void clearUser(String userId) throws IOException {
        ReentrantLock lock = lockFor(userId);
        lock.lock();
        try {
            // Historial principal (ruta correcta)
            Path hist = pathOf(userId);
            boolean deletedMain = Files.deleteIfExists(hist);

            // Ruta legacy incorrecta (history/history) creada por versión previa: limpiar si existe
            Path legacyHist = basedir.resolve("history").resolve(userId + ".txt");
            boolean deletedLegacyHist = Files.deleteIfExists(legacyHist);

            // Directorios de resumen: soporta typo antiguo 'summery' y el correcto 'summary'
            Path summaryDir = Paths.get(props.getDatadir(), "summary");
            Path summaryTxt = summaryDir.resolve(userId + ".txt");
            boolean deletedSummary = Files.deleteIfExists(summaryTxt);
            Path legacySummaryDir = Paths.get(props.getDatadir(), "summery");
            Path legacySummaryTxt = legacySummaryDir.resolve(userId + ".txt");
            boolean deletedLegacySummary = Files.deleteIfExists(legacySummaryTxt);

            log.info("Cleanup userId={} deleted(main={}, legacyHist={}, summary={}, legacySummary={}) remaining(mainExists={}, summaryExists={})",
                userId,
                deletedMain, deletedLegacyHist, deletedSummary, deletedLegacySummary,
                Files.exists(hist), Files.exists(summaryTxt));
        } catch (IOException e) {
            log.warn("Error limpiando historial userId={}", userId, e);
            throw e;
        } finally {
            lock.unlock();
        }
    }

    private Path pathOf(String userId) {
        return basedir.resolve(fileName(userId));
    }

    private ReentrantLock lockFor(String userId) {
        String key = sanitizeUserId(userId == null ? "unknown" : userId);
        return userLocks.computeIfAbsent(key, k -> new ReentrantLock());
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
