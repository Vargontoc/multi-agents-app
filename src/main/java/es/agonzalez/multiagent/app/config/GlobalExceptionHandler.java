package es.agonzalez.multiagent.app.config;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        return ResponseEntity.badRequest().body(Map.of(
            "status", "error",
            "error", "validation",
            "details", ex.getBindingResult().getAllErrors().stream().map(e -> e.getDefaultMessage()).toList()
        ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
            "status", "error",
            "error", ex.getClass().getSimpleName(),
            "message", ex.getMessage()
        ));
    }
}
