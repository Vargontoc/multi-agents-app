package es.agonzalez.multiagent.app.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;

/**
 * Propiedades de seguridad (API key) para exponer metadata a tooling y validar presencia.
 */
@Component
@ConfigurationProperties(prefix = "security")
@Validated
public class SecurityProperties {

    /** API key esperada para autenticación de peticiones. */
    @NotBlank(message = "{validation.security.apikey.notBlank}")
    private String apikey;

    public String getApikey() { return apikey; }
    public void setApikey(String apikey) { this.apikey = apikey; }
}
