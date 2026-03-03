package es.agonzalez.multiagent.app.services;

import java.nio.file.Path;
import java.util.List;

import org.springframework.stereotype.Service;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;

@Service
public class DocumentIngestionService {

    private final EmbeddingModel embeddingModel;
    private final EmbeddingStore<TextSegment> embeStore;

    public DocumentIngestionService(EmbeddingModel embeddingModel, EmbeddingStore<TextSegment> embeStore) {
        this.embeddingModel = embeddingModel;
        this.embeStore = embeStore;
    }

    public void ingestDocument(Path filePath) {
        // 1. Load document
        Document document = FileSystemDocumentLoader.loadDocument(filePath);

        // 2. Split document into segments
        DocumentSplitter splitter = DocumentSplitters.recursive(500, 50);
        List<TextSegment> segments = splitter.split(document);

        // 3. Convert to verctors
        List<Embedding> embeddings = embeddingModel.embedAll(segments).content();
        embeStore.addAll(embeddings, segments);

    }
}