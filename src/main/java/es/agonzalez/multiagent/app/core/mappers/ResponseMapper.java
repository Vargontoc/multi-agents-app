package es.agonzalez.multiagent.app.core.mappers;

import java.util.Map;

import org.springframework.stereotype.Component;

import es.agonzalez.multiagent.app.core.workflows.chat.models.ChatResult;
import es.agonzalez.multiagent.app.core.workflows.recipe.models.RecipeResponse;
import es.agonzalez.multiagent.app.dtos.AIRequest;
import es.agonzalez.multiagent.app.dtos.AIResponse;

/**
 * Responsable del mapeo entre resultados de workflows internos y respuestas de API.
 * Separa la lógica de transformación de datos de la orquestación de workflows.
 */
@Component
public class ResponseMapper {

    /**
     * Mapea resultado de ChatWorkflow a AIResponse.
     */
    public AIResponse mapChatResult(ChatResult result, AIRequest request, String intent, String agent) {
        return new AIResponse(
            result.status(),
            agent,
            result.message(),
            Map.of(
                "userId", request.getUserId(),
                "intent", intent,
                "meta", result.data()
            ),
            result.status().equals("ok") ? null : extractReason(result)
        );
    }

    /**
     * Mapea resultado de RecipeWorkflow a AIResponse.
     */
    public AIResponse mapRecipeResult(RecipeResponse result, AIRequest request, String intent, String agent) {
        return new AIResponse(
            result.status(),
            agent,
            result.message(),
            Map.of(
                "userId", request.getUserId(),
                "intent", intent,
                "meta", result.data()
            ),
            result.status().equals("ok") ? null : extractReason(result)
        );
    }

    /**
     * Extrae reason de error desde resultado de workflow.
     * Por ahora usa lógica simple, puede extenderse para mapeos más complejos.
     */
    private String extractReason(ChatResult result) {
        // Lógica futura: mapear tipos específicos de error del workflow a reasons
        return result.status().equals("ok") ? null : "workflow_error";
    }

    /**
     * Extrae reason de error desde resultado de workflow.
     * Por ahora usa lógica simple, puede extenderse para mapeos más complejos.
     */
    private String extractReason(RecipeResponse result) {
        // Lógica futura: mapear tipos específicos de error del workflow a reasons
        return result.status().equals("ok") ? null : "workflow_error";
    }
}