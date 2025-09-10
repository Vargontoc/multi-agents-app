package es.agonzalez.multiagent.app.core.workflows.recipe.models;

import java.util.Map;

public record  RecipeRequest(
    String text,
    Map<String, Object> data
) {}
