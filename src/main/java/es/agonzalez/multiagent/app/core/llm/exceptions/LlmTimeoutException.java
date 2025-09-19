package es.agonzalez.multiagent.app.core.llm.exceptions;

public class LlmTimeoutException extends LlmException {
    public LlmTimeoutException(String message, Throwable cause) { super("timeout", message, cause); }
}
