package es.agonzalez.multiagent.app.dtos;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Petición para configurar modelo estable y (opcionalmente) canary.
 * Reglas:
 * - agent y stable no vacíos y con patrón permitido.
 * - percent en [0,100].
 * - Si percent > 0 => canary obligatorio y no vacío.
 * - Si canary presente pero percent=0, se acepta (significa canary definido pero 0% tráfico) para facilitar preconfiguración.
 * - Si canary presente y stable==canary se rechaza (no tiene sentido canary idéntico al estable cuando percent>0).
 */
public record SwitchModelRequest(
    @NotBlank(message = "agent es obligatorio")
    @Pattern(regexp = "^[a-zA-Z0-9_-]{2,40}$", message = "agent formato inválido")
    String agent,

    @NotBlank(message = "stable es obligatorio")
    @Pattern(regexp = "^[a-zA-Z0-9._:-]{2,80}$", message = "stable formato inválido")
    String stable,

    @Pattern(regexp = "^[a-zA-Z0-9._:-]{2,80}$", message = "canary formato inválido")
    String canary,

    @Min(value = 0, message = "percent debe ser >= 0")
    @Max(value = 100, message = "percent debe ser <= 100")
    int percent
) {
    @AssertTrue(message = "percent>0 requiere canary no vacío")
    public boolean isCanaryRequiredWhenPercent() {
        if (percent > 0) {
            return canary != null && !canary.isBlank();
        }
        return true;
    }

    @AssertTrue(message = "Si percent>0 canary debe ser distinto de stable")
    public boolean isCanaryDifferentFromStable() {
        if (percent > 0 && canary != null && !canary.isBlank()) {
            return !stable.equals(canary);
        }
        return true;
    }
}
