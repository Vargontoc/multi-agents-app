package es.agonzalez.multiagent.app.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI multiAgentsOpenAPI() {
        final String securitySchemeName = "ApiKeyAuth";
        return new OpenAPI()
            .info(new Info()
                .title("Multi-Agents Service API")
                .description("Orquestación de workflows multi-agente, memoria y gestión de modelos.")
                .version("v1")
                .contact(new Contact().name("MultiAgents Team"))
                .license(new License().name("Apache 2.0")))
            .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
            .components(new Components()
                .addSecuritySchemes(securitySchemeName, new SecurityScheme()
                    .name("X-API-Key")
                    .type(SecurityScheme.Type.APIKEY)
                    .in(SecurityScheme.In.HEADER)
                    .description("API key requerida en cabecera X-API-Key")));
    }
}
