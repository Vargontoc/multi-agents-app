package es.agonzalez.multiagent.app.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import es.agonzalez.multiagent.app.util.Sanitizers;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

@Component
@ConfigurationProperties(prefix = "multiagent")
@Validated
public class AppProperties {

    @NotBlank(message = "{validation.datadir.notBlank}")
    private String datadir;

    @Min(value = 10, message = "{validation.maxHistoryLines.min}")
    private int maxHistoryLines = 200;

    @Min(value = 1, message = "{validation.summarizationEvery.min}")
    private int summarizationEvery = 10;

    @Min(value = 200, message = "{validation.maxLineLength.min}")
    private int maxLineLength = 4096;

    @NotBlank(message = "{validation.modelconfig.notBlank}")
    private String modelconfig;

    @Valid
    private Llm llm = new Llm();

    // Normalización movida al setter para evitar método @PostConstruct innecesario (reduce warnings)

    public String getDatadir() { return datadir; }
    public void setDatadir(String datadir) {
        this.datadir = Sanitizers.normalizePathLike(datadir);
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
    @NotBlank(message = "{validation.llm.url.notBlank}")
        private String url;
    @Min(value = 100, message = "{validation.llm.timeoutMs.min}")
        private long timeoutMs = 5000;

        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
        public long getTimeoutMs() { return timeoutMs; }
        public void setTimeoutMs(long timeoutMs) { this.timeoutMs = timeoutMs; }
    }
}
