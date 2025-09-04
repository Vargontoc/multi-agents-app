package es.agonzalez.multiagent.app.config;

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
    private Timer timer;
    

    @PostConstruct
    public void init() 
    {
        this.messages = Counter.builder("multiagent_messages_total").description("Total de mensajes recibidos").register(registry);
        this.errors = Counter.builder("multiagent_errors_total").description("Total de errores de procesamiento").register(registry);

        this.timer = Timer.builder("multiagent_llm_latency_ms").description("Latencia de llamadas al LLM (ms)").publishPercentileHistogram().register(registry);

    }

    public void incMessages() { messages.increment(); }
    public void incErrors() { errors.increment(); }
    public <T> T timLlm(Supplier<T> supplier) { return timer.record(supplier); }
    

}
