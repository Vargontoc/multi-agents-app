package es.agonzalez.multiagent.app.memory;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import es.agonzalez.multiagent.app.config.AppProperties;
import es.agonzalez.multiagent.app.memory.impl.FileMemoryStore;

/**
 * Verifica que load() ahora sólo mantiene las últimas maxHistoryLines líneas
 * evitando cargar todo el archivo masivo en memoria.
 */
public class FileMemoryStoreTailLoadTest {

    @TempDir
    Path tmp;

    private FileMemoryStore store;
    private AppProperties props;

    @BeforeEach
    @SuppressWarnings("unused") // Usado por el ciclo de vida de JUnit
    void setup() throws Exception {
        props = new AppProperties();
        props.setDatadir(tmp.toString());
        props.setMaxHistoryLines(50); // límite para tail
        // config mínima LLM
        AppProperties.Llm llm = new AppProperties.Llm();
        llm.setUrl("http://localhost");
        llm.setTimeoutMs(5000);
        props.setLlm(llm);

        store = new FileMemoryStore();
        ReflectionTestUtils.setField(store, "props", props);
        store.init();
    }

    @Test
    void loadReturnsOnlyTail() throws Exception {
        // Escribimos 200 líneas (> 50)
        IntStream.range(0, 200).forEach(i -> {
            try {
                store.append("userTail", "user", "line-" + i);
            } catch (java.io.IOException e) { //noinspection CatchMayIgnoreException - se re-lanza envuelto
                throw new RuntimeException(e); // Escalamos porque el test no puede continuar sin escribir
            }
        });
        List<String> lines = store.load("userTail");
        assertEquals(50, lines.size(), "Debe devolver sólo las últimas 50");
        // La primera de la lista debe corresponder a la línea 150 original
        assertTrue(lines.get(0).contains("line-150"), "Esperado comienzo tail en line-150");
        assertTrue(lines.get(49).contains("line-199"), "Esperado final tail en line-199");
    }
}
