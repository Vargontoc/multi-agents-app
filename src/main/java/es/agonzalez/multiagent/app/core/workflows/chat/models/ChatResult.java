package es.agonzalez.multiagent.app.core.workflows.chat.models;

import java.util.Map;

public record ChatResult(String status, String message, Map<String, Object> data) {
    public static ChatResult ok(String message, Map<String, Object> data) { return new ChatResult("ok", message, data); }
    public static ChatResult error(String message) { return new ChatResult("error", message, Map.of()); }
}
