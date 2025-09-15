package es.agonzalez.multiagent.app.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import es.agonzalez.multiagent.app.core.LlmClient;
import es.agonzalez.multiagent.app.core.ModelRegistry;
import es.agonzalez.multiagent.app.core.selectors.ModelSelectors;
import es.agonzalez.multiagent.app.core.workflows.chat.ChatWorkflow;
import es.agonzalez.multiagent.app.core.workflows.chat.steps.GenerateStep;
import es.agonzalez.multiagent.app.core.workflows.chat.steps.LoadMemoryStep;
import es.agonzalez.multiagent.app.core.workflows.chat.steps.LoadSummaryStep;
import es.agonzalez.multiagent.app.core.workflows.chat.steps.SaveResultStep;
import es.agonzalez.multiagent.app.core.workflows.chat.steps.SummarizeIfNeededStep;
import es.agonzalez.multiagent.app.core.workflows.recipe.RecipeWorkflow;
import es.agonzalez.multiagent.app.core.workflows.recipe.steps.GenerateRecipeStep;
import es.agonzalez.multiagent.app.core.workflows.recipe.steps.ReadRecipeStep;
import com.fasterxml.jackson.databind.ObjectMapper;
import es.agonzalez.multiagent.app.memory.MemoryService;
import es.agonzalez.multiagent.app.memory.Summarizer;
import es.agonzalez.multiagent.app.memory.SummaryStore;

@Configuration
public class WorkflowConfig {
    @Autowired
    private LlmClient client;
    @Autowired
    private ModelRegistry registry;
    @Autowired
    private MemoryService memory;
    @Autowired
    private SummaryStore summary;
    @Autowired
    private Summarizer summarizer;
    @Autowired
    private ModelSelectors selectors;
    
    @Bean
    public ChatWorkflow chatWorkflow() {
        return new ChatWorkflow(List.of(
            new LoadMemoryStep(memory),
            new LoadSummaryStep(summary),
            new GenerateStep(client, memory, registry, selectors),
            new SummarizeIfNeededStep(summary, summarizer),
            new SaveResultStep()
        ));
    }

    @Bean
    public RecipeWorkflow recipeWorkflow(ObjectMapper om) {
        return new RecipeWorkflow(List.of(
            new GenerateRecipeStep(client, registry, selectors),
            new ReadRecipeStep(om)
        ));
    }
}
