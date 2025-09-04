package es.agonzalez.multiagent.app.core.workflows;

import java.util.Map;
import java.util.Optional;

public interface  Step<I, O> {
    
    Optional<O> apply(I input, Map<String, Object> context);
}
