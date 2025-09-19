package es.agonzalez.multiagent.app.core.llm.exceptions;

public class LlmUnknownException extends LlmException {
    public LlmUnknownException(String message, Throwable cause) { super("unknown", message, cause); }
}
