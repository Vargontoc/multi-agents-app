package es.agonzalez.multiagent.app.memory;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import es.agonzalez.multiagent.app.config.AppProperties;
import es.agonzalez.multiagent.app.memory.impl.FileMemoryStore;

/**
 * Verifica que las entradas demasiado largas se truncan respetando el sufijo
 * ...[truncated] y que la longitud final del mensaje no excede el límite.
 */
public class FileMemoryStoreTruncationTest {

    @TempDir
    java.nio.file.Path tmp;

    private FileMemoryStore store;
    private AppProperties props;

    @BeforeEach
    public void setup() throws Exception {
        props = new AppProperties();
        props.setDatadir(tmp.toString());
        props.setMaxHistoryLines(50); // no forzamos rotación aquí
        props.setMaxLineLength(60);   // límite pequeño para probar truncado
        // propiedades mínimas LLM para pasar validación si se usan en otros componentes
        AppProperties.Llm llm = new AppProperties.Llm();
        llm.setUrl("http://localhost");
        llm.setTimeoutMs(5000);
        props.setLlm(llm);

        store = new FileMemoryStore();
        ReflectionTestUtils.setField(store, "props", props);
        store.init();
    }

    @Test
    void longLineIsTruncated() throws Exception {
        String longText = "x".repeat(200) + " FIN"; // claramente > 60
        store.append("userA", "user", longText);
        List<String> lines = store.load("userA");
        assertEquals(1, lines.size());
        String stored = lines.get(0);
        // El formato es: timestamp\trole\tcontenido
        String[] parts = stored.split("\t", 3);
        assertEquals(3, parts.length, "Formato TSV inesperado");
        String rawContent = parts[2];
        assertTrue(rawContent.endsWith("...[truncated]"), "Debe terminar con sufijo de truncado");
        String contentNoNl = rawContent.replace("\r", "").replace("\n", "");
        int observedLen = contentNoNl.length();
        assertTrue(observedLen <= props.getMaxLineLength(),
            "Contenido excede límite: len=" + observedLen + " limit=" + props.getMaxLineLength());
        assertTrue(contentNoNl.startsWith("x"), "Debe empezar por los datos originales");
    }

    @Test
    void shortLineNotTruncated() throws Exception {
        String shortText = "hola";
        store.append("userB", "assistant", shortText);
        String stored = store.load("userB").get(0);
    String content = stored.split("\t", 3)[2].replace("\r", "").replace("\n", "");
    assertEquals(shortText, content);
    }
}
