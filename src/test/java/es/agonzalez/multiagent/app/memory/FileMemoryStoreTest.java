package es.agonzalez.multiagent.app.memory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import es.agonzalez.multiagent.app.config.AppProperties;
import es.agonzalez.multiagent.app.memory.impl.FileMemoryStore;

public class FileMemoryStoreTest {

    @TempDir
    Path tmp;

    private FileMemoryStore store;
    private AppProperties props;

    @BeforeEach
    public void setup() throws Exception {
        props = new AppProperties();
        props.setDatadir(tmp.toString());
        props.setMaxHistoryLines(5); // forzamos rotación rápida
        store = new FileMemoryStore();
        ReflectionTestUtils.setField(store, "props", props);
        store.init();
    }

    @Test
    void appendAndLoad() throws Exception {
        store.append("u1", "user", "hola");
        store.append("u1", "assistant", "respuesta");
        List<String> lines = store.load("u1");
        assertEquals(2, lines.size());
        assertTrue(lines.get(0).contains("user"));
    }

    @Test
    void rotateWhenExceeds() throws Exception {
        for (int i = 0; i < 7; i++) {
            store.append("u2", "user", "msg"+i);
        }
        store.rotateIfNeeded("u2");
        // debe existir el archivo nuevo vacío
        int count = store.countLines("u2");
        assertTrue(count <= 5);
        // debe haber un archivo rotado con sufijo .log
        boolean rotated = Files.list(tmp.resolve("history"))
            .anyMatch(p -> p.getFileName().toString().contains(".log"));
        assertTrue(rotated, "No se encontró archivo rotado");
    }

    @Test
    void clearUserRemovesHistory() throws Exception {
        store.append("u3", "user", "hola");
        store.clearUser("u3");
        // countLines crea path distinto si se ha limpiado; validamos que la carga devuelve lista vacía
        assertTrue(store.load("u3").isEmpty());
    }

    @Test
    void concurrentAppends() throws Exception {
        int threads = 10;
        int perThread = 20;
        var exec = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(threads);
        IntStream.range(0, threads).forEach(t -> exec.submit(() -> {
            try {
                for (int i=0;i<perThread;i++) {
                    store.append("conc", "user", "m"+t+"-"+i);
                }
            } catch (IOException e) { fail(e); }
            finally { latch.countDown(); }
        }));
        latch.await(5, TimeUnit.SECONDS);
        exec.shutdownNow();
        int lines = store.countLines("conc");
        assertEquals(threads*perThread, lines);
    }
}
