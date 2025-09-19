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
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
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
    // Contador incremental de líneas por usuario (persistido en fichero .meta)
    private final ConcurrentHashMap<String, Integer> userLineCounts = new ConcurrentHashMap<>();

    // Métricas
    @Autowired(required = false)
    private MeterRegistry meterRegistry;
    private Counter truncationsCounter;
    private Counter rotationsCounter;
    private Counter appendsCounter;
    private Counter recountsCounter; // número de recuentos completos (fallback) para medir eficiencia
    private DistributionSummary lineLengthSummary;

    @PostConstruct
    public void init() throws  IOException {
        this.dataDir = props.getDatadir();
        this.maxLines = props.getMaxHistoryLines();
        basedir = Paths.get(dataDir, "history");
        Files.createDirectories(basedir);
        // Cargar metadatos de conteo existentes (best-effort)
        try (var paths = Files.list(basedir)) {
            paths.filter(p -> p.getFileName().toString().endsWith(".meta"))
                .forEach(meta -> {
                    try {
                        String content = Files.readString(meta, StandardCharsets.UTF_8).trim();
                        if (!content.isEmpty()) {
                            int idx = content.indexOf(':');
                            if (idx > 0) {
                                String checksum = content.substring(0, idx);
                                String numStr = content.substring(idx+1);
                                // Validar checksum simple (sha-256 del número) para detectar corrupción
                                // Implementamos hash local (SHA-256 del número) para validar la metadata
                                java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
                                String expected = java.util.HexFormat.of().formatHex(md.digest(numStr.getBytes(StandardCharsets.UTF_8)));
                                if (expected.equalsIgnoreCase(checksum)) {
                                    int count = Integer.parseInt(numStr);
                                    String user = meta.getFileName().toString().replace(".meta", "");
                                    userLineCounts.put(user, count);
                                }
                            }
                        }
                    } catch (java.io.IOException | NumberFormatException | java.security.NoSuchAlgorithmException ex) {
                        log.debug("Ignorando metadata corrupta {}: {}", meta.getFileName(), ex.getMessage());
                    }
                });
        } catch (IOException ignore) {}
        if (meterRegistry != null && truncationsCounter == null) {
            truncationsCounter = Counter.builder("memory.lines.truncated")
                .description("Número de líneas truncadas por exceder max-line-length")
                .register(meterRegistry);
            rotationsCounter = Counter.builder("memory.history.rotations")
                .description("Número de rotaciones de archivos de historial")
                .register(meterRegistry);
            appendsCounter = Counter.builder("memory.lines.appended")
                .description("Número de líneas añadidas al historial")
                .register(meterRegistry);
            recountsCounter = Counter.builder("memory.line.recounts")
                .description("Número de recuentos completos de líneas realizados (fallback por ausencia de metadata)")
                .register(meterRegistry);
            lineLengthSummary = DistributionSummary.builder("memory.line.length")
                .baseUnit("chars")
                .description("Distribución de longitudes de líneas (ya saneadas y posiblemente truncadas)")
                .publishPercentileHistogram()
                .register(meterRegistry);
            // Gauge de usuarios con metadata cargada (tamaño del mapa de contadores)
            io.micrometer.core.instrument.Gauge.builder("memory.users.tracked", userLineCounts, java.util.Map::size)
                .description("Número de usuarios con contador de líneas en memoria")
                .register(meterRegistry);
        }
    }

    @Override
    public List<String> load(String userId) throws IOException {
        Path p = pathOf(userId);
        if(!Files.exists(p)) return Collections.emptyList();
        ReentrantLock lock = lockFor(userId);
        lock.lock();
        try {
            int limit = Math.max(1, maxLines);
            java.util.ArrayDeque<String> deque = new java.util.ArrayDeque<>(limit);
            try (var stream = Files.lines(p, StandardCharsets.UTF_8)) {
                stream.forEach(line -> {
                    if (deque.size() == limit) {
                        deque.pollFirst(); // descartamos la más antigua para mantener sólo las últimas
                    }
                    deque.addLast(line);
                });
            }
            return java.util.List.copyOf(deque);
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
            if (truncationsCounter != null) truncationsCounter.increment();
        }
        String line = "%s\t%s\t%s%n".formatted(Instant.now().toString(), role, safe);
        ReentrantLock lock = lockFor(userId);
        lock.lock();
        try {
            Files.writeString(p, line, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            if (appendsCounter != null) appendsCounter.increment();
            if (lineLengthSummary != null) lineLengthSummary.record(safe.length());
            incrementLineCountUnsafe(userId, 1);
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
                        // Tras rotar, reseteamos contador a 0 y persistimos
                        userLineCounts.put(userId, 0);
                        persistMeta(userId, 0);
                    } catch (IOException recreateEx) {
                        log.error("Historial rotado pero no se pudo recrear archivo vacío userId={} error={}", userId, recreateEx.toString());
                        throw recreateEx;
                    }
                    log.debug("Rotado historial userId={} -> {} (lineas={})", userId, rotated.getFileName(), lines);
                    if (rotationsCounter != null) rotationsCounter.increment();
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
        // Fast path usando contador incremental persistido
        Integer cached = userLineCounts.get(sanitizeUserId(userId));
        if (cached != null) return cached;
        // Fallback: cálculo completo (primer acceso tras migración / ausencia meta)
        Path p = pathOf(userId);
        if (!Files.exists(p)) return 0;
        ReentrantLock lock = lockFor(userId);
        lock.lock();
        try (var s = Files.lines(p)) {
            int counted = (int) s.count();
            userLineCounts.put(sanitizeUserId(userId), counted);
            persistMeta(userId, counted);
            if (recountsCounter != null) recountsCounter.increment();
            return counted;
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
            userLineCounts.remove(sanitizeUserId(userId));
            deleteMeta(userId);
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
        // Improvement 10: we now expect controllers/DTO validation to enforce non-blank userId.
        // Null safety kept (maps to "unknown"), but blank should not reach here after validation.
        if (s == null) return "unknown"; // legacy safeguard
        String trimmed = s.trim();
        if (trimmed.isEmpty()) return "unknown"; // fallback kept to avoid NPEs if validation missed some path
        return trimmed.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private void incrementLineCountUnsafe(String userId, int delta) {
        String key = sanitizeUserId(userId);
        int updated = userLineCounts.merge(key, delta, Integer::sum);
        persistMeta(key, updated);
    }

    private void persistMeta(String userId, int count) {
        Path meta = basedir.resolve(sanitizeUserId(userId) + ".meta");
        try {
            // checksum simple sha256 del número textual
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            String numStr = Integer.toString(count);
            String checksum = java.util.HexFormat.of().formatHex(md.digest(numStr.getBytes(StandardCharsets.UTF_8)));
            String content = checksum + ":" + numStr + "\n";
            Path tmp = meta.resolveSibling(meta.getFileName().toString() + ".tmp");
            Files.writeString(tmp, content, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            try (var ch = java.nio.channels.FileChannel.open(tmp, StandardOpenOption.WRITE)) { ch.force(true); }
            try {
                Files.move(tmp, meta, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException atomicEx) {
                Files.move(tmp, meta, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException | java.security.NoSuchAlgorithmException ex) {
            log.debug("No se pudo persistir meta userId={} : {}", userId, ex.getMessage());
        }
    }

    private void deleteMeta(String userId) {
        Path meta = basedir.resolve(sanitizeUserId(userId) + ".meta");
        try { Files.deleteIfExists(meta); } catch (IOException ignore) {}
    }

    private static String sanitizeSingleLine(String s) {
        if (s == null) return "";
        // Quita saltos de línea y tabuladores para no romper el formato TSV
        return s.replaceAll("\\R", " ").replace("\t", " ").trim();
    }


}
