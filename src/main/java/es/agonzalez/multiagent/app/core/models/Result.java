package es.agonzalez.multiagent.app.core.models;

import java.util.Map;

public record Result(
    String status,
    String message,
    Map<String, Object> data
) {}
