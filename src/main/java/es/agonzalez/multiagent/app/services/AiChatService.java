package es.agonzalez.multiagent.app.services;

import org.springframework.stereotype.Service;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.spring.AiService;
import dev.langchain4j.store.embedding.EmbeddingStore;
import es.agonzalez.multiagent.app.audit.AiObservabilityInterceptor;
import es.agonzalez.multiagent.app.tools.SqlAgentTools;

@Service
public class AiChatService {

    private final ContentRetriever contentRetriever;
    private final AiObservabilityInterceptor aiObservabilityInterceptor;
    private final AssistantAssistant assistant;

    public AiChatService(OllamaChatModel chatModel, EmbeddingStore<TextSegment> embeddingStore,
            EmbeddingModel embeddingModel, SqlAgentTools sqlAgentTools,
            AiObservabilityInterceptor aiObservabilityInterceptor) {
        this.contentRetriever = EmbeddingStoreContentRetriever.builder().embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel).maxResults(3).minScore(0.7).build();
        this.aiObservabilityInterceptor = aiObservabilityInterceptor;
        this.assistant = AiServices.builder(AssistantAssistant.class)
                .chatModel(chatModel)
                .contentRetriever(contentRetriever)
                .chatMemory(MessageWindowChatMemory.withMaxMessages(10))
                .tools(sqlAgentTools)
                .build();
    }

    public String askQuestion(String question) {
        long start = System.currentTimeMillis();

        String response = assistant.answer(question);

        long duration = System.currentTimeMillis() - start;
        aiObservabilityInterceptor.logInteraction(question, response, "SQL_EXTRACTED_FROM_CONTEXT", duration);
        return response;
    }

    @AiService
    interface AssistantAssistant {
        @SystemMessage("Eres un asistente experto en la empresa. Responde de forma clara y concisa. Antes de response consulta la base de datos con las herramientas disponibles.")
        String answer(String query);
    }
}
