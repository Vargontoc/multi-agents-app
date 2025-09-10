package es.agonzalez.multiagent.app.core.workflows.recipe;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import es.agonzalez.multiagent.app.core.workflows.Step;
import es.agonzalez.multiagent.app.core.workflows.Workflow;
import es.agonzalez.multiagent.app.core.workflows.recipe.models.RecipeRequest;
import es.agonzalez.multiagent.app.core.workflows.recipe.models.RecipeResponse;

public class RecipeWorkflow implements Workflow<RecipeRequest, RecipeResponse> {

        private final List<Step<RecipeRequest, RecipeResponse>> steps;

        public RecipeWorkflow(List<Step<RecipeRequest, RecipeResponse>> steps) {
            this.steps = steps;
        }


        @Override
        public RecipeResponse run(RecipeRequest input) {
            Map<String, Object> context = new HashMap<>();
            for(var s: steps) {
                var maybe = s.apply(input, context);
                if(maybe.isPresent()) {
                    return maybe.get();
                }
            }
            return  RecipeResponse.error("Flujo terminó sin resultado");
        }
}
