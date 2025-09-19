package es.agonzalez.multiagent.app.core.llm.exceptions;

/**
 * Excepción base para errores relacionados con el proveedor LLM.
 * Incluye un reason code estable para permitir fallback semántico y telemetría.
 */
public abstract class LlmException extends RuntimeException {
    private final String reason;
    protected LlmException(String reason, String message) { super(message); this.reason = reason; }
    protected LlmException(String reason, String message, Throwable cause) { super(message, cause); this.reason = reason; }
    public String reason() { return reason; }
}
