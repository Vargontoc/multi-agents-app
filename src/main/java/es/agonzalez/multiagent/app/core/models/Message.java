package es.agonzalez.multiagent.app.core.models;

public record Message(
    String role,
    String content
) {
    public static Message system(String msg) { return new Message("system", msg); }
    public static Message user(String msg) { return new Message("user", msg); }
    public static Message assistant(String msg) { return new Message("assistant", msg); }    
}
