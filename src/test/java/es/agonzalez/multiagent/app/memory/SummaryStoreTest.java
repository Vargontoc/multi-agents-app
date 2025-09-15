package es.agonzalez.multiagent.app.memory;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import es.agonzalez.multiagent.app.config.AppProperties;

public class SummaryStoreTest {

    @TempDir
    Path tmp;

    private SummaryStore store;
    private AppProperties props;

    @BeforeEach
    void setup() throws Exception {
        props = new AppProperties();
        props.setDatadir(tmp.toString());
        store = new SummaryStore();
        ReflectionTestUtils.setField(store, "props", props);
        store.init();
    }

    @Test
    void saveAndLoad() throws Exception {
        store.save("u1", "Resumen inicial");
        String s = store.load("u1");
        assertEquals("Resumen inicial", s);
    }

    @Test
    void overwriteSummary() throws Exception {
        store.save("u1", "A");
        store.save("u1", "B");
        String s = store.load("u1");
        assertEquals("B", s);
    }
}
