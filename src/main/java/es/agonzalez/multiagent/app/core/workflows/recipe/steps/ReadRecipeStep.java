package es.agonzalez.multiagent.app.core.workflows.recipe.steps;

import java.util.Map;
import java.util.Optional;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import es.agonzalez.multiagent.app.core.workflows.Step;
import es.agonzalez.multiagent.app.core.workflows.recipe.models.RecipeRequest;
import es.agonzalez.multiagent.app.core.workflows.recipe.models.RecipeResponse;

@Component
public class ReadRecipeStep implements Step<RecipeRequest, RecipeResponse> {
    private final ObjectMapper om;
    public ReadRecipeStep(ObjectMapper om) { this.om = om; }

    @Override
    public Optional<RecipeResponse> apply(RecipeRequest input, Map<String, Object> context) {
        if(context != null && context.containsKey(HttpHeaders.CONTENT_TYPE) && context.get(HttpHeaders.CONTENT_TYPE) instanceof String type) {
            
            String answer = (String)context.get("answer");
            String model = (String)context.get("model");
            long  latency = (long) context.get("latencyMs");

            if(MediaType.APPLICATION_JSON.toString().equals(type)) 
            {   
                try {
                    om.readTree(answer);
                    
                }catch(JsonProcessingException e) {
                    return Optional.of(RecipeResponse.error("Mmm, parece que no he hecho bien mi trabajo"));
                }
            }
            
            return Optional.of(RecipeResponse.ok(answer, Map.of(
                "model", model, "latencyMs", latency
            )));
        }

        return Optional.of(RecipeResponse.error("Mmm, parece que te dado una receta mal descrita"));
    }
    
}
