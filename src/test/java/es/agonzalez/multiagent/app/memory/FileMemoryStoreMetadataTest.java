package es.agonzalez.multiagent.app.memory;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import es.agonzalez.multiagent.app.config.AppProperties;
import es.agonzalez.multiagent.app.memory.impl.FileMemoryStore;

/**
 * Tests para la persistencia incremental del contador de líneas (.meta) por usuario.
 */
public class FileMemoryStoreMetadataTest {

    @TempDir
    Path tmp;

    FileMemoryStore store;
    AppProperties props;

    @BeforeEach
    public void setup() throws Exception {
        props = new AppProperties();
        props.setDatadir(tmp.toString());
        props.setMaxHistoryLines(100);
        AppProperties.Llm llm = new AppProperties.Llm();
        llm.setUrl("http://localhost");
        llm.setTimeoutMs(5000);
        props.setLlm(llm);
        store = new FileMemoryStore();
        ReflectionTestUtils.setField(store, "props", props);
        store.init();
    }

    @Test
    @DisplayName("Meta se crea y se reutiliza tras reinicio (sin recount)")
    void metaPersistsAndAvoidsRecount() throws Exception {
        for (int i=0;i<10;i++) store.append("user1", "user", "line"+i);
        int linesFirst = store.countLines("user1");
        assertThat(linesFirst).isEqualTo(10);
        Path meta = tmp.resolve("history").resolve("user1.meta");
        assertThat(meta).exists();
        String metaContent = Files.readString(meta).trim();
        assertThat(metaContent).contains(":10");

        // Simular reinicio: nueva instancia reutiliza metadata
        FileMemoryStore store2 = new FileMemoryStore();
        ReflectionTestUtils.setField(store2, "props", props);
        store2.init();
        int linesSecond = store2.countLines("user1");
        assertThat(linesSecond).isEqualTo(10);
    }

    @Test
    @DisplayName("Corrupción de metadata provoca recount y regeneración")
    void corruptedMetaTriggersRecount() throws Exception {
        for (int i=0;i<5;i++) store.append("user2", "user", "x"+i);
        int baseline = store.countLines("user2");
        assertThat(baseline).isEqualTo(5);
        Path meta = tmp.resolve("history").resolve("user2.meta");
        // Corromper: modificar checksum manteniendo número
        String corrupted = """
                           deadbeef:5
                           """;
        Files.writeString(meta, corrupted);
        FileMemoryStore store2 = new FileMemoryStore();
        ReflectionTestUtils.setField(store2, "props", props);
        store2.init();
        int recounted = store2.countLines("user2");
        assertThat(recounted).isEqualTo(5);
        // Metadata regenerada debe tener checksum válido diferente a deadbeef
        String newContent = Files.readString(meta).trim();
        assertThat(newContent).doesNotStartWith("deadbeef:");
    }
}
