package es.agonzalez.multiagent.app.core.llm.exceptions;

public class LlmProviderException extends LlmException {
    private final int statusCode;
    public LlmProviderException(int statusCode, String body) { super("provider_error", "LLM HTTP " + statusCode + ": " + body); this.statusCode = statusCode; }
    public int statusCode() { return statusCode; }
}
