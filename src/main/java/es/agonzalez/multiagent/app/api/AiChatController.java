package es.agonzalez.multiagent.app.api;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import es.agonzalez.multiagent.app.dtos.ChatRequest;
import es.agonzalez.multiagent.app.dtos.ChatResponse;
import es.agonzalez.multiagent.app.services.AiChatService;
import es.agonzalez.multiagent.app.services.DocumentIngestionService;

@RestController
@RequestMapping("/api/ai")
@CrossOrigin(origins = "*")
public class AiChatController {

    private final AiChatService aiChatService;
    private final DocumentIngestionService documentIngestService;

    public AiChatController(AiChatService aiChatService, DocumentIngestionService documentIngestService) {
        this.aiChatService = aiChatService;
        this.documentIngestService = documentIngestService;
    }

    @PostMapping("/ask")
    public ResponseEntity<ChatResponse> chat(@RequestBody ChatRequest request) {
        if (request.query() == null || request.query().isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        String answer = aiChatService.askQuestion(request.query());
        return ResponseEntity.ok(new ChatResponse(answer, LocalDateTime.now()));

    }

    @PostMapping("/ingest")
    public ResponseEntity<String> ingestDocument(@RequestParam MultipartFile document) {
        try {
            Path tmp = Files.createTempFile("upload", document.getOriginalFilename());
            document.transferTo(tmp);

            documentIngestService.ingestDocument(tmp);
            return ResponseEntity.ok("Document ingested successfully on database");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error ingesting document");
        }
    }
}
