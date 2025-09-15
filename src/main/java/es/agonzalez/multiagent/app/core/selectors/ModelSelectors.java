package es.agonzalez.multiagent.app.core.selectors;

import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import es.agonzalez.multiagent.app.core.ModelRegistry;

@Component
public class ModelSelectors {
    @Autowired
    private ModelRegistry registry;
    private final AtomicReference<CanaryConfig> canaryRef = new AtomicReference<>(new CanaryConfig(Map.of()));


    public CanaryConfig getCanary(){ return canaryRef.get(); }
    public void setCanaryConfig(CanaryConfig config) { canaryRef.set(config); }

    public String pick(String agentName, String userIdOrNull) {
        var cfg = canaryRef.get().porAgent().get(agentName);
    if(cfg == null || cfg.percentage() <= 0 || cfg.canary() == null || cfg.canary().isBlank()) return registry.modelFor(agentName);

        int bucket = (userIdOrNull == null || userIdOrNull.isBlank()) ?
            ThreadLocalRandom.current().nextInt(100) : Math.floorMod(userIdOrNull.hashCode(), 100);
        return (bucket <= cfg.percentage()) ? cfg.canary() : cfg.stable();
    }
}
