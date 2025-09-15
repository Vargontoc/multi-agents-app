package es.agonzalez.multiagent.app.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

@Component
@ConfigurationProperties(prefix = "multiagent")
@Validated
public class AppProperties {

    @NotBlank(message = "multiagent.datadir es obligatorio")
    private String datadir;

    @Min(value = 10, message = "multiagent.max-history-lines debe ser >= 10")
    private int maxHistoryLines = 200;

    @Min(value = 1, message = "multiagent.summarization-every debe ser >= 1")
    private int summarizationEvery = 10;

    @Min(value = 200, message = "multiagent.max-line-length debe ser >= 200")
    private int maxLineLength = 4096;

    @NotBlank(message = "multiagent.modelconfig es obligatorio")
    private String modelconfig;

    @Valid
    private Llm llm = new Llm();

    // Normalización movida al setter para evitar método @PostConstruct innecesario (reduce warnings)

    public String getDatadir() { return datadir; }
    public void setDatadir(String datadir) {
        if (datadir != null) {
            datadir = datadir.replaceAll("[\\r\\n]", "").replaceAll("/+$(?!/)", "");
        }
        this.datadir = datadir;
    }
    public int getMaxHistoryLines() { return maxHistoryLines; }
    public void setMaxHistoryLines(int maxHistoryLines) { this.maxHistoryLines = maxHistoryLines; }
    public int getSummarizationEvery() { return summarizationEvery; }
    public void setSummarizationEvery(int summarizationEvery) { this.summarizationEvery = summarizationEvery; }
    public int getMaxLineLength() { return maxLineLength; }
    public void setMaxLineLength(int maxLineLength) { this.maxLineLength = maxLineLength; }
    public String getModelconfig() { return modelconfig; }
    public void setModelconfig(String modelconfig) { this.modelconfig = modelconfig; }
    public Llm getLlm() { return llm; }
    public void setLlm(Llm llm) { this.llm = llm; }

    @Validated
    public static class Llm {
        @NotBlank(message = "multiagent.llm.url es obligatorio")
        private String url;
        @Min(value = 100, message = "multiagent.llm.timeout-ms debe ser >= 100")
        private long timeoutMs = 5000;

        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
        public long getTimeoutMs() { return timeoutMs; }
        public void setTimeoutMs(long timeoutMs) { this.timeoutMs = timeoutMs; }
    }
}
