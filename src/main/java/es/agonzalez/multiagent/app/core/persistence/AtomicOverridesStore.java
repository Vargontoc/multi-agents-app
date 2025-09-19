package es.agonzalez.multiagent.app.core.persistence;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Utilidad de persistencia atómica y verificada por checksum para los overrides de modelos.
 * Formato del fichero (UTF-8):
 *   Línea 1: "sha256:<hex>" (checksum del payload JSON de la línea 2)
 *   Línea 2: JSON compacto con el mapa de overrides {"agent":"model",...}
 *   (Termina opcionalmente en newline final)
 *
 * Operación de escritura (writeAtomic):
 * 1. Serializa mapa a JSON compacto.
 * 2. Calcula SHA-256 sobre los bytes UTF-8 del JSON.
 * 3. Escribe a fichero temporal (mismo directorio) con fsync.
 * 4. Renombra atómicamente sobre el destino (fallback a move normal si ATOMIC_MOVE no soportado).
 *
 * Operación de lectura (readValidated):
 * 1. Si no existe: devuelve mapa vacío.
 * 2. Lee todas las líneas. Si tiene >=2 y la primera empieza por "sha256:", valida checksum.
 * 3. Si checksum válido → parsea JSON (línea 2) y devuelve mapa inmutable.
 * 4. Si checksum inválido / JSON corrupto → log caller debe decidir (aquí devolvemos vacío).
 * 5. Backward compatibility: si el fichero NO contiene prefijo sha256, se intenta parsear el contenido completo como JSON (formato legacy sin checksum).
 */
public final class AtomicOverridesStore {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AtomicOverridesStore.class);
    private static final String CHECKSUM_PREFIX = "sha256:";
    private static final ObjectMapper JSON = new ObjectMapper();

    private AtomicOverridesStore() {}

    public static void writeAtomic(Path file, Map<String, Object> overrides) {
        Objects.requireNonNull(file, "file");
        Objects.requireNonNull(overrides, "overrides");
        String json;
        try {
            json = JSON.writeValueAsString(overrides);
        } catch (JsonProcessingException e) {
            // No debería ocurrir (Map<String,Object> simple) pero si pasa no persistimos.
            log.error("No se pudo serializar overrides a JSON: {}", e.getMessage());
            return;
        }
        String checksum = sha256Hex(json.getBytes(StandardCharsets.UTF_8));
        String content = CHECKSUM_PREFIX + checksum + "\n" + json + "\n"; // newline final para ediciones manuales seguras

        Path tmp = file.resolveSibling(file.getFileName().toString() + ".tmp");
        try {
            // Escribir bytes en temporal (truncando si existe)
            Files.writeString(tmp, content, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
            // fsync para durabilidad
            try (FileChannel ch = FileChannel.open(tmp, StandardOpenOption.WRITE)) {
                ch.force(true);
            }
            // Intentar movimiento atómico
            try {
                Files.move(tmp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException atomicEx) {
                // Fallback a move no atómico (p.ej. FS que no soporta ATOMIC_MOVE en Windows)
                Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException io) {
            log.error("Fallo persistiendo overrides atómicos: {}", io.getMessage());
            // Intentar limpieza del temporal si queda
            try { Files.deleteIfExists(tmp); } catch (IOException ignore) {}
        }
    }

    @SuppressWarnings("unchecked")
    public static Map<String,Object> readValidated(Path file) {
        Objects.requireNonNull(file, "file");
        if (!Files.exists(file)) return Map.of();
        try {
            List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
            if (lines.isEmpty()) return Map.of();
            if (lines.size() >= 2 && lines.get(0).startsWith(CHECKSUM_PREFIX)) {
                String declared = lines.get(0).substring(CHECKSUM_PREFIX.length()).trim();
                String json = lines.get(1).trim();
                String actual = sha256Hex(json.getBytes(StandardCharsets.UTF_8));
                if (!declared.equalsIgnoreCase(actual)) {
                    log.warn("Checksum overrides inválido: esperado={} calculado={}", declared, actual);
                    return Map.of();
                }
                if (json.isEmpty() || json.equals("{}")) return Map.of();
                return (Map<String,Object>) JSON.readValue(json, Map.class);
            }
            // Formato legacy: intentar parsear todo el fichero como JSON
            String whole = String.join("\n", lines).trim();
            if (whole.isEmpty()) return Map.of();
            return (Map<String,Object>) JSON.readValue(whole, Map.class);
        } catch (IOException e) {
            log.warn("No se pudieron leer overrides: {}", e.getMessage());
            return Map.of();
        }
    }

    private static String sha256Hex(byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(data));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
    }
}
