package es.agonzalez.multiagent.app.core.llm.exceptions;

public class LlmValidationException extends LlmException {
    public LlmValidationException(String message) { super("validation", message); }
}
