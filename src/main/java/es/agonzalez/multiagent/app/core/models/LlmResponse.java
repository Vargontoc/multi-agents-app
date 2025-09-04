package es.agonzalez.multiagent.app.core.models;

public record LlmResponse(
    String contet,
    int promptToken,
    int completionToken
) {
    public static LlmResponse of(String msg) { return new LlmResponse(msg, -1, -1); }
}
