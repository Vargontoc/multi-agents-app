package es.agonzalez.multiagent.app.core.workflows.chat;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.MessageSource;

import es.agonzalez.multiagent.app.core.workflows.chat.models.ChatInput;
import es.agonzalez.multiagent.app.memory.MemoryService;

@SpringBootTest
class LoadMemoryStepI18nTest {

    @Autowired
    MessageSource messageSource;

    private MemoryService failingMemoryService() throws IOException {
        var m = mock(MemoryService.class);
        when(m.loadHistory(anyString())).thenThrow(new IOException("IOX"));
        return m;
    }

    @Test
    void spanishMessage() {
        Locale.setDefault(Locale.forLanguageTag("es"));
        var step = new es.agonzalez.multiagent.app.core.workflows.chat.steps.LoadMemoryStep(uncheckedFailingMemory(), messageSource);
        var wf = new ChatWorkflow(List.of(step), messageSource);
        var res = wf.run(new ChatInput("u1","user1","hola","chat"));
        assertEquals("error", res.status());
        assertTrue(res.message().startsWith("No se pudo cargar historial:"));
    }

    @Test
    void englishMessage() {
        Locale.setDefault(Locale.ENGLISH);
        var step = new es.agonzalez.multiagent.app.core.workflows.chat.steps.LoadMemoryStep(uncheckedFailingMemory(), messageSource);
        var wf = new ChatWorkflow(List.of(step), messageSource);
        var res = wf.run(new ChatInput("u1","user1","hi","chat"));
        assertEquals("error", res.status());
        assertTrue(res.message().startsWith("Could not load history:"));
        Locale.setDefault(Locale.forLanguageTag("es"));
    }

    private MemoryService uncheckedFailingMemory() {
        try {
            return failingMemoryService();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
