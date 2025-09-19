package es.agonzalez.multiagent.app.core.persistence;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class AtomicOverridesStoreTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("Persistencia y lectura válida con checksum")
    void writeAndRead() {
        Path file = tempDir.resolve("overrides.json");
        AtomicOverridesStore.writeAtomic(file, Map.of("agentA", "model1", "agentB", "model2"));
        var loaded = AtomicOverridesStore.readValidated(file);
        assertThat(loaded).containsEntry("agentA", "model1").containsEntry("agentB", "model2");
    }

    @Test
    @DisplayName("Detecta corrupción (checksum mismatch) y devuelve vacío")
    void detectCorruption() throws IOException {
        Path file = tempDir.resolve("overrides.json");
        AtomicOverridesStore.writeAtomic(file, Map.of("agentA", "model1"));
        // Corromper segunda línea (payload JSON)
        var lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        lines.set(1, "{\"agentA\":\"otro\"}");
        Files.write(file, String.join("\n", lines).getBytes(StandardCharsets.UTF_8));
        var loaded = AtomicOverridesStore.readValidated(file);
        assertThat(loaded).isEmpty();
    }

    @Test
    @DisplayName("Soporta formato legacy sin checksum (JSON simple)")
    void legacyFormat() throws IOException {
        Path file = tempDir.resolve("overrides.json");
        Files.writeString(file, "{\"agentC\":\"modelX\"}", StandardCharsets.UTF_8);
        var loaded = AtomicOverridesStore.readValidated(file);
        assertThat(loaded).containsEntry("agentC", "modelX");
    }

    @Test
    @DisplayName("Archivo vacío o inexistente -> mapa vacío")
    void emptyOrMissing() throws IOException {
        Path file = tempDir.resolve("missing.json");
        assertThat(AtomicOverridesStore.readValidated(file)).isEmpty();
        Path empty = tempDir.resolve("empty.json");
        Files.writeString(empty, "", StandardCharsets.UTF_8);
        assertThat(AtomicOverridesStore.readValidated(empty)).isEmpty();
    }
}
