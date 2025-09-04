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

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import es.agonzalez.multiagent.app.core.models.LlmResponse;
import es.agonzalez.multiagent.app.core.models.Message;

@Component
public class LlmClient {
    @Value("${multiagent.llm.url}")
    private String baseUrl;
    @Value("${multiagent.llm.timeout-ms}")
    private int timeoutMs;
    private final ObjectMapper om = new ObjectMapper();
    private HttpClient http;

    private HttpClient getClient() {
        if(http == null)
            http = HttpClient.newBuilder().connectTimeout(Duration.ofMillis(timeoutMs)).build();
        return http;
    }

    public LlmResponse chat(String model, List<Message> messages, Map<String, Object> params) {
        try {
            var payload = new HashMap<String, Object>();
            payload.put("model", model);
            payload.put("messages", messages.stream().map(m -> Map.of(
                "role", m.role(),
                "content", m.content()
            )).toList());
            if(params != null) payload.putAll(params);
            payload.putIfAbsent("stream", false);

            var req = HttpRequest.newBuilder().uri(URI.create(baseUrl + "/api/chat"))
            .timeout(Duration.ofMillis(timeoutMs))
            .header(HttpHeaders.CONTENT_TYPE, "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(om.writeValueAsString(payload))).build();

            var resp = getClient().send(req, HttpResponse.BodyHandlers.ofString());
            if(resp.statusCode() /100 != 2) {
                throw new RuntimeException("LLM HTTP " + resp.statusCode() + ": " + resp.body());
            }

            Map<?, ?> json = om.readValue(resp.body(), Map.class);
            Map<?, ?> message = (Map<?, ?>) json.get("message");
            String content = message != null ? String.valueOf(message.get("content")) : "";
            int prompt = getNumber(json, "prompt_eval_count", -1).intValue();
            int completion = getNumber(json, "eval_count", -1).intValue();
            return new LlmResponse(content, prompt, completion);

        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Error llamando al LLM: " + e.getMessage(), e);
        }
    }

    private Number getNumber(Map<?, ?> map, String prop, Number defaultValue) {
        if(map.containsKey(prop) && map.get(prop) instanceof Number n) {
            return n;
        }
        return defaultValue;
    } 
}   
