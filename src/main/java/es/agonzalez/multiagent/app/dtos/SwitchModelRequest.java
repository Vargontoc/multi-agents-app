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
    @NotBlank(message = "{validation.agent.notBlank}")
    @Pattern(regexp = "^[a-zA-Z0-9_-]{2,40}$", message = "{validation.agent.pattern}")
    String agent,

    @NotBlank(message = "{validation.stable.notBlank}")
    @Pattern(regexp = "^[a-zA-Z0-9._:-]{2,80}$", message = "{validation.stable.pattern}")
    String stable,

    @Pattern(regexp = "^[a-zA-Z0-9._:-]{2,80}$", message = "{validation.canary.pattern}")
    String canary,

    @Min(value = 0, message = "{validation.percent.min}")
    @Max(value = 100, message = "{validation.percent.max}")
    int percent
) {
    @AssertTrue(message = "{validation.percent.canaryRequired}")
    public boolean isCanaryRequiredWhenPercent() {
        if (percent > 0) {
            return canary != null && !canary.isBlank();
        }
        return true;
    }

    @AssertTrue(message = "{validation.percent.canaryDifferent}")
    public boolean isCanaryDifferentFromStable() {
        if (percent > 0 && canary != null && !canary.isBlank()) {
            return !stable.equals(canary);
        }
        return true;
    }
}
