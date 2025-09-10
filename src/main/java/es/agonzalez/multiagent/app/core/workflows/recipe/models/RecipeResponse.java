package es.agonzalez.multiagent.app.core.workflows.recipe.models;

import java.util.Map;

public record  RecipeResponse(String status, String message, Map<String, Object> data) {
    public static RecipeResponse ok(String message, Map<String, Object> data) { return new RecipeResponse("ok", message, data); }
    public static RecipeResponse error(String message) { return new RecipeResponse("error", message, Map.of()); }
}
