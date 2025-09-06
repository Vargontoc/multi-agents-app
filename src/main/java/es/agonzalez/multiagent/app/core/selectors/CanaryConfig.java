package es.agonzalez.multiagent.app.core.selectors;

import java.util.Map;

public record  CanaryConfig(Map<String, Entry> porAgent) {
    public record Entry(String stable, String canary, int percentage) {}
}
