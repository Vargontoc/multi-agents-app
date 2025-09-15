package es.agonzalez.multiagent.app.core.workflows.recipe;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.context.MessageSource;

import es.agonzalez.multiagent.app.core.workflows.Step;
import es.agonzalez.multiagent.app.core.workflows.Workflow;
import es.agonzalez.multiagent.app.core.workflows.recipe.models.RecipeRequest;
import es.agonzalez.multiagent.app.core.workflows.recipe.models.RecipeResponse;

public class RecipeWorkflow implements Workflow<RecipeRequest, RecipeResponse> {

        private final List<Step<RecipeRequest, RecipeResponse>> steps;
        private final MessageSource messages;

        public RecipeWorkflow(List<Step<RecipeRequest, RecipeResponse>> steps, MessageSource messages) {
            this.steps = steps;
            this.messages = messages;
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
            var msg = messages.getMessage("workflow.recipe.no_result", null, Locale.getDefault());
            return  RecipeResponse.error(msg);
        }
}
