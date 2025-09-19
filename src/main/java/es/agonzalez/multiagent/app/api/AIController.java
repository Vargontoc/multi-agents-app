package es.agonzalez.multiagent.app.api;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import es.agonzalez.multiagent.app.core.workflows.WorkflowRunner;
import es.agonzalez.multiagent.app.dtos.AIRequest;
import es.agonzalez.multiagent.app.dtos.AIResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;


@RestController
@RequestMapping("/api/v1")
@Tag(name = "Chat & AI", description = "Endpoints de interacción con agentes IA")
public class AIController {
    @Autowired
    private WorkflowRunner runner;

    @PostMapping("/ai")
    @Operation(summary = "Ejecuta un workflow IA", description = "Procesa un input del usuario y devuelve respuesta del agente / modelo.")
    public ResponseEntity<AIResponse> chat(@Valid @RequestBody AIRequest req) {
    return ResponseEntity.ok().body(runner.applyWorkflow(req));
    }
    
}
