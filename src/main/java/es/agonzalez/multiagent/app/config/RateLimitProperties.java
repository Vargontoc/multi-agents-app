package es.agonzalez.multiagent.app.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Propiedades configurables para rate limiting.
 * Ejemplo en application.properties:
 * ratelimit.enabled=true
 * ratelimit.capacity=50
 * ratelimit.refill.tokens=50
 * ratelimit.refill.period=60s
 */
@Component
@ConfigurationProperties(prefix = "ratelimit")
public class RateLimitProperties {
    /** Habilita o deshabilita el filtro */
    private boolean enabled = true;
    /** Capacidad máxima del bucket */
    private int capacity = 100;
    /** Tokens añadidos cada periodo de refill */
    private int refillTokens = 100;
    /** Periodo de refill (parseado por Duration.parse si incluye sufijos) */
    private String refillPeriod = "60s"; // formato amigable (lo convertiremos con DurationStyle)
    /** Lista de paths excluidos (coma separada) */
    private String excludePaths = "/actuator/health,/v3/api-docs,/swagger-ui,/swagger-ui.html";

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public int getCapacity() { return capacity; }
    public void setCapacity(int capacity) { this.capacity = capacity; }
    public int getRefillTokens() { return refillTokens; }
    public void setRefillTokens(int refillTokens) { this.refillTokens = refillTokens; }
    public String getRefillPeriod() { return refillPeriod; }
    public void setRefillPeriod(String refillPeriod) { this.refillPeriod = refillPeriod; }
    public String getExcludePaths() { return excludePaths; }
    public void setExcludePaths(String excludePaths) { this.excludePaths = excludePaths; }
}
