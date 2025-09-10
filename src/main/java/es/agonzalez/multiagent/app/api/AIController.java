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


@RestController
@RequestMapping("/api/v1")
public class AIController {
    @Autowired
    private WorkflowRunner runner;

    @PostMapping("/ai")
    public ResponseEntity<AIResponse> chat(@RequestBody AIRequest req) {
            return ResponseEntity.ok().body(runner.applyWorklow(req));
    }
    
}
