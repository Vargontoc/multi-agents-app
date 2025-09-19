package es.agonzalez.multiagent.app.core;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import es.agonzalez.multiagent.app.util.Sanitizers;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import es.agonzalez.multiagent.app.core.models.LlmResponse;
import es.agonzalez.multiagent.app.core.llm.exceptions.*;
import es.agonzalez.multiagent.app.core.models.Message;
import es.agonzalez.multiagent.app.core.models.dto.OllamaChatResponse;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import jakarta.annotation.PostConstruct;

@Component
public class LlmClient {
    private String baseUrl;
    private int timeoutMs;
    private final ObjectMapper om;
    private HttpClient http;

    @Autowired
    private es.agonzalez.multiagent.app.config.AppProperties props;

    @Autowired
    private Tracer tracer;

    private final MessageSource messages;

    public LlmClient(ObjectMapper om, MessageSource messages) {
        this.om = om;
        this.messages = messages;
    }
    
    @PostConstruct
    public HttpClient getClient() {
        if(http == null){
            this.baseUrl = Sanitizers.normalizePathLike(props.getLlm().getUrl());
            this.timeoutMs = (int) props.getLlm().getTimeoutMs();
            http = HttpClient.newBuilder().connectTimeout(Duration.ofMillis(timeoutMs)).build();
        }
        return http;
    }

    public LlmResponse chat(String model, List<Message> messages, Map<String, Object> params, boolean generative) {
        String call = generative ? "/api/generate" : "/api/chat";
        // Normalizar para evitar NPE posteriores
        List<Message> safeMessages = (messages == null) ? List.of() : messages;
        if (generative && safeMessages.isEmpty()) {
            return new LlmResponse("", -1, -1);
        }

        // Crear span para tracing de llamada LLM
        Span span = tracer.nextSpan()
            .name("llm.request")
            .tag("llm.model", model)
            .tag("llm.endpoint", call)
            .tag("llm.generative", String.valueOf(generative))
            .tag("llm.message_count", String.valueOf(safeMessages.size()));

        try (var ignored = tracer.withSpan(span.start())) {
            var payload = new HashMap<String, Object>();
            payload.put("model", model);
            payload.putIfAbsent("stream", false);
            if(params != null) payload.putAll(params);

            if(generative) {
                payload.put("prompt", safeMessages.get(0).content());            
            } else {
                payload.put("messages", safeMessages.stream().map(m -> Map.of(
                    "role", m.role(),
                    "content", m.content()
                )).toList());
            }


            
            var req = HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + call))
            .timeout(Duration.ofMillis(timeoutMs))
            .header(HttpHeaders.CONTENT_TYPE, "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(om.writeValueAsString(payload)))
            .build();

            var resp = getClient().send(req, HttpResponse.BodyHandlers.ofString());
            int statusCode = resp.statusCode();
            if(statusCode < 200 || statusCode >= 300) {
                span.tag("error", "true")
                    .tag("http.status_code", String.valueOf(statusCode))
                    .event("llm.provider.error");
                throw new LlmProviderException(statusCode, resp.body());
            }
            OllamaChatResponse json = om.readValue(resp.body(), OllamaChatResponse.class);
            int prompt = json.promptCount();
            int completion = json.completionCount();
            String content = json.contentOrEmpty(generative);

            // Añadir métricas de tokens al span
            span.tag("llm.prompt_tokens", String.valueOf(prompt))
                .tag("llm.completion_tokens", String.valueOf(completion))
                .tag("llm.total_tokens", String.valueOf(prompt + completion))
                .tag("http.status_code", String.valueOf(statusCode));

            return new LlmResponse(content, prompt, completion);

        } catch (IOException e) {
            span.tag("error", "true").event("llm.io.error");
            throw new LlmUnknownException("io_error: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            span.tag("error", "true").event("llm.timeout.error");
            Thread.currentThread().interrupt();
            throw new LlmTimeoutException("interrupted", e);
        } finally {
            span.end();
        }
    }

 

    // Eliminado getNumber auxiliar: ahora manejado por DTO tipado

    @io.github.resilience4j.retry.annotation.Retry(name = "llm")
    @io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker(name="llm", fallbackMethod="fallback")
    @io.github.resilience4j.timelimiter.annotation.TimeLimiter(name="llm")
    @org.springframework.scheduling.annotation.Async
    public CompletableFuture<LlmResponse> chatAsync(String model, List<Message> messages, Map<String, Object> params, boolean generative) {return CompletableFuture.supplyAsync(() -> this.chatBlocking(model, messages, params, generative)); }
    private LlmResponse chatBlocking(String model, List<Message> messages, Map<String, Object> params, boolean generative){ return chat(model, messages, params, generative); }
    public CompletableFuture<LlmResponse> fallback(String model, List<Message> messages, Map<String, Object> params, Throwable t) { 
        var locale = LocaleContextHolder.getLocale();
        String msg = this.messages.getMessage("llm.fallback.busy", null, "Busy, please retry later", locale);
        return CompletableFuture.completedFuture(LlmResponse.of(msg)); 
    } 
}   
