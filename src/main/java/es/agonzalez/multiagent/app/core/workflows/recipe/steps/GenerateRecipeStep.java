package es.agonzalez.multiagent.app.core.workflows.recipe.steps;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.StringJoiner;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import es.agonzalez.multiagent.app.core.LlmClient;
import es.agonzalez.multiagent.app.core.ModelRegistry;
import es.agonzalez.multiagent.app.core.models.LlmResponse;
import es.agonzalez.multiagent.app.core.models.Message;
import es.agonzalez.multiagent.app.core.selectors.ModelSelectors;
import es.agonzalez.multiagent.app.core.workflows.Step;
import es.agonzalez.multiagent.app.core.workflows.recipe.models.RecipeRequest;
import es.agonzalez.multiagent.app.core.workflows.recipe.models.RecipeResponse;

@Component
public class GenerateRecipeStep implements  Step<RecipeRequest, RecipeResponse> 
{
    private final ModelSelectors selectors;
    private final ModelRegistry models;
    private final LlmClient client;
    public GenerateRecipeStep(LlmClient client, ModelRegistry models, ModelSelectors selectors) {
        this.selectors = selectors;
        this.client = client;
        this.models  = models;
    }
    
    @Override
    public Optional<RecipeResponse> apply(RecipeRequest input, Map<String, Object> context) 
    {
        var messages = new ArrayList<Message>();
        var props = models.defaults();
        
        boolean generative = false;

        if(input.data() != null && !input.data().isEmpty() && input.data().containsKey("ingredients")) 
        {
            generative = true;
            context.put(org.springframework.http.HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON.toString());
            String ingredients = readList(input.data(),"ingredients");
            String diet = readDiet(input.data());
            String allergns = readList(input.data(), "allergens");
            String excludes = readList(input.data(), "excludes");
            
            messages.add(Message.system("""
                Eres un asistente culinario experto.
                Genera UNA SOLA receta adecuda a los ingredientes disponibles y preferencias.
                RESPONDE EXCLUSIVAMENTE con un JSON válido, sin texto adicional.
                
                Esquema JSON de salida:
                {
                    "title": string,
                    "summary": string,
                    "steps": string[], // cada paso una breve explicacion
                    "tags": string[] // 3-6 etiquetas
                    }
                    
                    Requisitos:
                    - Ingredientes disponibles: [%s]
                    - Dieta: %s
                    - Excluir ingredientes: [%s]
                    - Alergias (no incluir): [%s]
                    - Prioriza sencillez (<= 6 pasos) y tiempos razonables.
                    - Si faltan ingredientes críticos, propón sustitutos comunes.
                    
                    Devuelve solo el JSON indicado.
                    """.formatted(ingredients, diet, excludes, allergns)));

            
        }


        if(input.text() != null && !input.text().isBlank() && !generative) 
        {
            context.put(org.springframework.http.HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN.toString());
            messages.add(Message.system("""
                Eres un asistente culinario experto. Genera UNA SOLA receta con la informacion que te diga el usuario.
                Si con la información que recibes no sabes dí simplemente "Vaya, parece que me pides algo fuera de mis posibilidades". 
                    No inventes de nada, cualquier contradiccion o dato que no entiendas decir la frase anterior: "Vaya, parece que me pides algo fuera de mis posibilidades"
                    Responde en el idioma que viene la petición y no excedas los 320 caracteres.
                    """));
                    
                String inp = input.text().startsWith("!recipe") ? input.text().replace("!recipe", "") : input.text();
                messages.add(Message.user(inp));
                
        }
        
        if(messages.isEmpty())
            return Optional.of(RecipeResponse.error("Lo siento, las instrucciones bo las he entendido."));
        

        String model = selectors.pick("Agent.Recipe", null);
        Instant start = Instant.now();

        LlmResponse resp = client.chat(model, messages, props, generative);
        long latency = Duration.between(start, Instant.now()).toMillis();
        String answer = resp.contet();

        context.put("latencyMs", latency);
        context.put("model", model);
        context.put("answer", answer);

        return Optional.empty();

    }


    private String readList(Map<String, Object> map, String property) {
        if(map.containsKey(property) && map.get(property) instanceof List<?> list) {
            List<String> ing = list.stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .toList();

            StringJoiner sj = new StringJoiner(", ");
            ing.forEach(s -> sj.add('"' + s.replace("\"", "'") + '"'));
            return sj.toString();
        }
        return "";
    }

    private String readDiet(Map<String, Object> map) 
    {
        if(map.containsKey("diet") && map.get("diet") instanceof String s)
            return s;
        return "";    
    }
    
        
}
