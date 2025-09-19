package es.agonzalez.multiagent.app.dtos;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AIResponse(
    String status,
    String agent,
    String message,
    Map<String, Object> data,
    String reason
) {
    public static AIResponse ok(String agent, String message, Map<String,Object> data){
        return new AIResponse("ok", agent, message, data, null);
    }
    public static AIResponse error(String agent, String message, String reason){
        return new AIResponse("error", agent, message, Map.of(), reason);
    }
}

