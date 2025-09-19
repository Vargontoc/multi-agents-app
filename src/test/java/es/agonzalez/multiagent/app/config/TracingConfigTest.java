package es.agonzalez.multiagent.app.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test unitario para verificar la estructura de configuración de tracing.
 */
class TracingConfigTest {

    @Test
    void tracingProperties_HasCorrectStructure() {
        // Verificar que TracingProperties tiene la estructura correcta
        TracingConfig.TracingProperties props = new TracingConfig.TracingProperties();

        // Verificar valores por defecto
        assertEquals("multi-agents-service", props.getServiceName());
        assertEquals("1.0.0", props.getServiceVersion());
        assertFalse(props.isExportEnabled()); // disabled by default

        // Verificar setters
        props.setServiceName("test-service");
        props.setServiceVersion("2.0.0");
        props.setExportEnabled(true);

        assertEquals("test-service", props.getServiceName());
        assertEquals("2.0.0", props.getServiceVersion());
        assertTrue(props.isExportEnabled());

        // Verificar propiedades OTLP anidadas
        assertNotNull(props.getOtlp());
        assertEquals("http://localhost:4317", props.getOtlp().getEndpoint());

        props.getOtlp().setEndpoint("http://custom:4317");
        assertEquals("http://custom:4317", props.getOtlp().getEndpoint());
    }

    @Test
    void tracingConfig_HasCorrectAnnotations() {
        // Verificar que las anotaciones están presentes
        assertTrue(TracingConfig.class.isAnnotationPresent(org.springframework.context.annotation.Configuration.class));

        assertTrue(TracingConfig.TracingProperties.class.isAnnotationPresent(
            org.springframework.boot.context.properties.ConfigurationProperties.class));
        assertTrue(TracingConfig.TracingProperties.class.isAnnotationPresent(
            org.springframework.validation.annotation.Validated.class));

        // Verificar prefix
        org.springframework.boot.context.properties.ConfigurationProperties annotation =
            TracingConfig.TracingProperties.class.getAnnotation(
                org.springframework.boot.context.properties.ConfigurationProperties.class);
        assertEquals("management.tracing", annotation.prefix());
    }

    @Test
    void tracingProperties_ValidatesServiceName() {
        // Verificar que tiene anotación de validación
        try {
            var field = TracingConfig.TracingProperties.class.getDeclaredField("serviceName");
            assertTrue(field.isAnnotationPresent(jakarta.validation.constraints.NotBlank.class));
        } catch (NoSuchFieldException e) {
            fail("Field serviceName should exist");
        }
    }

    @Test
    void otlpProperties_HasCorrectDefaults() {
        TracingConfig.TracingProperties.Otlp otlp = new TracingConfig.TracingProperties.Otlp();

        assertEquals("http://localhost:4317", otlp.getEndpoint());

        otlp.setEndpoint("http://jaeger:14268/api/traces");
        assertEquals("http://jaeger:14268/api/traces", otlp.getEndpoint());
    }
}