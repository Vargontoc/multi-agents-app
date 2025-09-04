package es.agonzalez.multiagent.app.dtos;

import java.util.Map;

public record AIResponse(
    String status,
    String agent,
    String message,
    Map<String, Object> data
) {
    
}
