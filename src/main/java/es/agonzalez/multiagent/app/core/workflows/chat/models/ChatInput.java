package es.agonzalez.multiagent.app.core.workflows.chat.models;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChatInput(String userId, 
@NotBlank @Size(max=500)
String username,
String text, String intent) {}
