package es.agonzalez.multiagent.app.config;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.PostConstruct;

@Component
public class MetricsService {
    @Autowired
    private MeterRegistry registry;

    private Counter messages;
    private Counter errors;
    private Timer timer; // legacy ms timer (kept for backward compatibility)
    // New metric base names
    private static final String LLM_LATENCY = "multiagent_llm_latency"; // histogram seconds
    private static final String LLM_PROMPT_TOKENS = "multiagent_llm_prompt_tokens_total";
    private static final String LLM_COMPLETION_TOKENS = "multiagent_llm_completion_tokens_total";
    private static final String LLM_TOTAL_TOKENS = "multiagent_llm_tokens_total";
    private static final String LLM_ERRORS = "multiagent_llm_errors_total"; // counter per model/intent/reason
    

    @PostConstruct
    public void init() 
    {
        this.messages = Counter.builder("multiagent_messages_total").description("Total de mensajes recibidos").register(registry);
        this.errors = Counter.builder("multiagent_errors_total").description("Total de errores de procesamiento").register(registry);

        this.timer = Timer.builder("multiagent_llm_latency_ms")
            .description("Latencia de llamadas al LLM (ms) (DEPRECATED - usar multiagent_llm_latency)")
            .publishPercentileHistogram().register(registry);

    }

    public void incMessages() { messages.increment(); }
    public void incErrors() { errors.increment(); }
    public <T> T timLlm(Supplier<T> supplier) { return timer.record(supplier); }

    // ---- New LLM metric helpers ----
    public void recordLlmSuccess(String model, String intent, int promptTokens, int completionTokens, long latencyMs) {
        recordLlm(model, intent, "ok", promptTokens, completionTokens, latencyMs);
    }
    /**
     * @deprecated Use recordLlmErrorWithReason(model, intent, reason, latencyMs) instead.
     * El parámetro 'errorType' era confuso - se refería al tipo de error, no al status.
     */
    @Deprecated(since = "2025-09-18", forRemoval = true)
    public void recordLlmError(String model, String intent, String errorType, long latencyMs) {
        recordLlm(model, intent, "error", -1, -1, latencyMs);
        // Para compatibilidad, registrar también como reason si no es null
        if (errorType != null) {
            recordLlmErrorWithReason(model, intent, errorType, 0); // latency ya registrada arriba
        }
    }
    public void recordLlmErrorWithReason(String model, String intent, String reason, long latencyMs) {
        recordLlm(model, intent, "error", -1, -1, latencyMs);
        model = model == null ? "unknown" : model;
        intent = intent == null ? "unknown" : intent;
        reason = (reason == null || reason.isBlank()) ? "unknown" : reason;
        Counter.builder(LLM_ERRORS)
            .description("Errores LLM por modelo/intent/reason")
            .tag("model", model)
            .tag("intent", intent)
            .tag("reason", reason)
            .register(registry)
            .increment();
    }

    private void recordLlm(String model, String intent, String status, int promptTokens, int completionTokens, long latencyMs) {
        model = model == null ? "unknown" : model;
        intent = intent == null ? "unknown" : intent;
        status = status == null ? "unknown" : status;
        Timer.builder(LLM_LATENCY)
            .description("Latencia LLM por modelo/intent/status")
            .tag("model", model)
            .tag("intent", intent)
            .tag("status", status)
            .register(registry)
            .record(latencyMs, TimeUnit.MILLISECONDS);
        if(promptTokens >= 0) {
            Counter.builder(LLM_PROMPT_TOKENS).description("Prompt tokens consumidos")
                .tag("model", model).tag("intent", intent).register(registry).increment(promptTokens);
        }
        if(completionTokens >= 0) {
            Counter.builder(LLM_COMPLETION_TOKENS).description("Completion tokens generados")
                .tag("model", model).tag("intent", intent).register(registry).increment(completionTokens);
        }
        if(promptTokens >= 0 && completionTokens >= 0) {
            Counter.builder(LLM_TOTAL_TOKENS).description("Total tokens (prompt+completion)")
                .tag("model", model).tag("intent", intent).register(registry).increment(promptTokens + completionTokens);
        }
    }

    public es.agonzalez.multiagent.app.core.models.LlmResponse timeAndRecord(String model, String intent, Supplier<es.agonzalez.multiagent.app.core.models.LlmResponse> sup) {
        Instant start = Instant.now();
        try {
            var result = sup.get();
            long latency = Duration.between(start, Instant.now()).toMillis();
            recordLlmSuccess(model, intent, result.promptToken(), result.completionToken(), latency);
            return result;
        } catch(RuntimeException ex) {
            long latency = Duration.between(start, Instant.now()).toMillis();
            recordLlmErrorWithReason(model, intent, "runtime", latency);
            throw ex;
        }
    }
    

}
