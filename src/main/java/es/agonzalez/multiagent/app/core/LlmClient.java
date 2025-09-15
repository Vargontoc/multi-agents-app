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
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import es.agonzalez.multiagent.app.core.models.LlmResponse;
import es.agonzalez.multiagent.app.core.models.Message;
import es.agonzalez.multiagent.app.core.models.dto.OllamaChatResponse;

@Component
public class LlmClient {
    private String baseUrl;
    private int timeoutMs;
    private final ObjectMapper om = new ObjectMapper();
    private HttpClient http;

    @Autowired
    private es.agonzalez.multiagent.app.config.AppProperties props;

    private HttpClient getClient() {
        if(http == null){
            this.baseUrl = props.getLlm().getUrl();
            this.timeoutMs = (int) props.getLlm().getTimeoutMs();
            this.baseUrl = baseUrl == null ? "" : baseUrl.replaceAll("[\\r\\n]", "").replaceAll("/+$(?!/)", "");
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

        try {
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
            if(resp.statusCode() /100 != 2) {
                throw new RuntimeException("LLM HTTP " + resp.statusCode() + ": " + resp.body());
            }
            OllamaChatResponse json = om.readValue(resp.body(), OllamaChatResponse.class);
            int prompt = json.promptCount();
            int completion = json.completionCount();
            String content = json.contentOrEmpty(generative);
            return new LlmResponse(content, prompt, completion);


        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Error llamando al LLM: " + e.getMessage(), e);
        }
    }

 

    // Eliminado getNumber auxiliar: ahora manejado por DTO tipado

    @io.github.resilience4j.retry.annotation.Retry(name = "llm")
    @io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker(name="llm", fallbackMethod="fallback")
    @io.github.resilience4j.timelimiter.annotation.TimeLimiter(name="llm")
    @org.springframework.scheduling.annotation.Async
    public CompletableFuture<LlmResponse> chatAsync(String model, List<Message> messages, Map<String, Object> params, boolean generative) {return CompletableFuture.supplyAsync(() -> this.chatBlocking(model, messages, params, generative)); }
    private LlmResponse chatBlocking(String model, List<Message> messages, Map<String, Object> params, boolean generative){ return chat(model, messages, params, generative); }
    public CompletableFuture<LlmResponse> fallback(String model, List<Message> messages, Map<String, Object> params, Throwable t) { return CompletableFuture.completedFuture(LlmResponse.of("Ahora mismo estoy ocupado. Intentelo de nuevo más tarde")); } 
}   
