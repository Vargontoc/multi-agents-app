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
    public int promptCount(){ return prompt_eval_count == null ? -1 : prompt_eval_count.intValue(); }
    public int completionCount(){ return eval_count == null ? -1 : eval_count.intValue(); }
}
