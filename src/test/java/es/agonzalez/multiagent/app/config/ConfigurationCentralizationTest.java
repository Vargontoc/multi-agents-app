package es.agonzalez.multiagent.app.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifica que la centralización de configuración en @ConfigurationProperties está correctamente implementada.
 * Este test verifica la estructura de las clases de configuración sin cargar el contexto completo de Spring.
 */
class ConfigurationCentralizationTest {

    @Test
    void appProperties_HasCorrectStructure() {
        // Verificar que AppProperties tiene la estructura correcta
        AppProperties props = new AppProperties();

        // Verificar que tiene los setters para todas las propiedades
        props.setDatadir("/test");
        props.setSummarizationEvery(10);
        props.setModelconfig("test.yml");

        // Verificar getters
        assertEquals("/test", props.getDatadir());
        assertEquals(10, props.getSummarizationEvery());
        assertEquals("test.yml", props.getModelconfig());

        // Verificar valores por defecto
        assertEquals(200, props.getMaxHistoryLines());
        assertEquals(4096, props.getMaxLineLength());

        // Verificar estructura anidada LLM
        assertNotNull(props.getLlm());
        props.getLlm().setUrl("http://test:1234");
        props.getLlm().setTimeoutMs(2000);
        assertEquals("http://test:1234", props.getLlm().getUrl());
        assertEquals(2000, props.getLlm().getTimeoutMs());
    }

    @Test
    void securityProperties_HasCorrectStructure() {
        // Verificar que SecurityProperties tiene la estructura correcta
        SecurityProperties props = new SecurityProperties();

        props.setApikey("test-key");
        assertEquals("test-key", props.getApikey());
    }

    @Test
    void appProperties_LlmDefaults() {
        // Verificar valores por defecto de la clase anidada LLM
        AppProperties props = new AppProperties();
        AppProperties.Llm llm = props.getLlm();

        assertEquals(5000, llm.getTimeoutMs()); // valor por defecto
    }

    @Test
    void configurationAnnotations_ArePresent() {
        // Verificar que las anotaciones están presentes
        assertTrue(AppProperties.class.isAnnotationPresent(org.springframework.boot.context.properties.ConfigurationProperties.class));
        assertTrue(AppProperties.class.isAnnotationPresent(org.springframework.stereotype.Component.class));
        assertTrue(AppProperties.class.isAnnotationPresent(org.springframework.validation.annotation.Validated.class));

        assertTrue(SecurityProperties.class.isAnnotationPresent(org.springframework.boot.context.properties.ConfigurationProperties.class));
        assertTrue(SecurityProperties.class.isAnnotationPresent(org.springframework.stereotype.Component.class));
        assertTrue(SecurityProperties.class.isAnnotationPresent(org.springframework.validation.annotation.Validated.class));
    }

    @Test
    void configurationPrefixes_AreCorrect() {
        // Verificar que los prefijos están configurados correctamente
        org.springframework.boot.context.properties.ConfigurationProperties appAnnotation =
            AppProperties.class.getAnnotation(org.springframework.boot.context.properties.ConfigurationProperties.class);
        assertEquals("multiagent", appAnnotation.prefix());

        org.springframework.boot.context.properties.ConfigurationProperties secAnnotation =
            SecurityProperties.class.getAnnotation(org.springframework.boot.context.properties.ConfigurationProperties.class);
        assertEquals("security", secAnnotation.prefix());
    }
}