package es.agonzalez.multiagent.app.dtos;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record SwitchModelRequest(
    @NotBlank(message = "agent es obligatorio") String agent,
    @NotBlank(message = "stable es obligatorio") String stable,
    String canary,
    @Min(value = 0, message = "percent debe ser >= 0") @Max(value = 100, message = "percent debe ser <= 100") int percent
) {}
