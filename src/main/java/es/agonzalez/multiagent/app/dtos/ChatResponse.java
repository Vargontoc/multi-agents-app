package es.agonzalez.multiagent.app.dtos;

import java.time.LocalDateTime;

public record ChatResponse(String answer, LocalDateTime timestamp) {
}
