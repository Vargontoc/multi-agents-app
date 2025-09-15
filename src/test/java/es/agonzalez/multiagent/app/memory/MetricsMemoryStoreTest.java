package es.agonzalez.multiagent.app.memory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;

import es.agonzalez.multiagent.app.config.AppProperties;
import es.agonzalez.multiagent.app.memory.impl.FileMemoryStore;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
public class MetricsMemoryStoreTest {

    @TempDir
    Path tmp;

    @Autowired
    private FileMemoryStore store; // use real bean to get metrics wiring

    @Autowired
    private AppProperties props; // default bean

    @BeforeEach
    public void reconfigure() throws Exception {
        // Redefinir límites para forzar truncado y rotación más fácilmente
        props.setDatadir(tmp.toString());
        props.setMaxHistoryLines(3);
        props.setMaxLineLength(30);
        // Re-init store (simulate new settings)
        store.init();
    }

    @Autowired
    private MockMvc mvc;

    @Test
    void metricsIncrementOnAppendTruncateAndRotate() throws Exception {
        final String apiKey = "secret123"; // default fallback
        // Generar truncado: línea larga
        store.append("uMetrics", "user", "x".repeat(200));
        // Generar suficientes líneas para rotar
        store.append("uMetrics", "user", "a");
        store.append("uMetrics", "user", "b");
        store.append("uMetrics", "user", "c");
        store.rotateIfNeeded("uMetrics");

        var res = mvc.perform(withKey(get("/actuator/metrics/memory.lines.truncated"), apiKey))
            .andReturn().getResponse();
        assertThat(res.getStatus()).isEqualTo(200);
        assertThat(res.getContentAsString()).contains("memory.lines.truncated");

        var rot = mvc.perform(withKey(get("/actuator/metrics/memory.history.rotations"), apiKey))
            .andReturn().getResponse();
        assertThat(rot.getStatus()).isEqualTo(200);
        assertThat(rot.getContentAsString()).contains("memory.history.rotations");

        var appended = mvc.perform(withKey(get("/actuator/metrics/memory.lines.appended"), apiKey))
            .andReturn().getResponse();
        assertThat(appended.getStatus()).isEqualTo(200);
        assertThat(appended.getContentAsString()).contains("memory.lines.appended");

        var dist = mvc.perform(withKey(get("/actuator/metrics/memory.line.length"), apiKey))
            .andReturn().getResponse();
        assertThat(dist.getStatus()).isEqualTo(200);
        assertThat(dist.getContentAsString()).contains("memory.line.length");
    }

    private static MockHttpServletRequestBuilder withKey(MockHttpServletRequestBuilder b, String key) {
        return b.header("X-API-Key", key);
    }
}
