package es.agonzalez.multiagent.app.core.models.dto;


/**
 * Representa la respuesta JSON de /api/chat de Ollama (simplificada a campos usados).
 */
public record OllamaChatResponse(
    MessageNode message,
    Integer prompt_eval_count,
    Integer eval_count,
    String response
) {
    public record MessageNode(String role, String content) {}

    public String contentOrEmpty(boolean generative){
        if(generative && response != null) return response;
        if(message != null && message.content() != null) return message.content();
        return "";
    }
    public int promptCount(){ return java.util.Objects.requireNonNullElse(prompt_eval_count, -1); }
    public int completionCount(){ return java.util.Objects.requireNonNullElse(eval_count, -1); }
}
