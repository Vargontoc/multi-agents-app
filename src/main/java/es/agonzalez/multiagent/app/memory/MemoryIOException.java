package es.agonzalez.multiagent.app.memory;

/**
 * Excepción runtime para envolver problemas de IO en la capa de memoria.
 * Permite propagar contexto sin obligar a "throws IOException" por todo el stack
 * y centralizar el manejo en controladores o handlers globales.
 */
public class MemoryIOException extends RuntimeException {
    public MemoryIOException(String message, Throwable cause) { super(message, cause); }
}
