package es.agonzalez.multiagent.app.core.workflows.chat.models;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChatInput(String userId, 
@NotBlank(message="{validation.username.notBlank}") @Size(max=500, message="{validation.username.size}")
String username,
String text, String intent) {}
