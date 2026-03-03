package es.agonzalez.multiagent.app.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import dev.langchain4j.model.ollama.OllamaChatModel;

@Configuration
public class OllamaConfig {
    @Value("${langchain4j.ollama.url}")
    private String url;
    @Value("${langchain4j.ollama.model}")
    private String model;

    @Bean
    public OllamaChatModel chatLanguageModel() {
        return OllamaChatModel.builder()
                .baseUrl(url)
                .modelName(model)
                .timeout(java.time.Duration.ofSeconds(60))
                .build();
    }
}
