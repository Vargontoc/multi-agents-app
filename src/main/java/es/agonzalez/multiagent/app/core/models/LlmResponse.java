package es.agonzalez.multiagent.app.core.models;

public record LlmResponse(
    String content,
    int promptToken,
    int completionToken
) {
    public static LlmResponse of(String msg) { return new LlmResponse(msg, -1, -1); }
}
