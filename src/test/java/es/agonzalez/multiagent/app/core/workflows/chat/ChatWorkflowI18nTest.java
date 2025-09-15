package es.agonzalez.multiagent.app.core.workflows.chat;

import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.MessageSource;

import es.agonzalez.multiagent.app.core.workflows.Step;
import es.agonzalez.multiagent.app.core.workflows.chat.models.ChatInput;
import es.agonzalez.multiagent.app.core.workflows.chat.models.ChatResult;

@SpringBootTest
class ChatWorkflowI18nTest {

    @Autowired
    MessageSource messageSource;

    static class NoopStep implements Step<ChatInput, ChatResult> {
        @Override public java.util.Optional<ChatResult> apply(ChatInput input, java.util.Map<String,Object> ctx){
            return java.util.Optional.empty();
        }
    }

    @Test
    void returnsSpanishByDefault() {
        var wf = new ChatWorkflow(List.of(new NoopStep()), messageSource);
    var res = wf.run(new ChatInput("u1", "user1", "hola", "chat"));
    assertEquals("error", res.status());
    assertEquals("Flujo terminó sin resultado", res.message());
    }

    @Test
    void returnsEnglishWhenLocaleChanged() {
        Locale.setDefault(Locale.ENGLISH);
        try {
            var wf = new ChatWorkflow(List.of(new NoopStep()), messageSource);
            var res = wf.run(new ChatInput("u1", "user1", "hi", "chat"));
            assertEquals("error", res.status());
            assertEquals("Flow ended without result", res.message());
        } finally {
            Locale.setDefault(Locale.forLanguageTag("es"));
        }
    }
}
