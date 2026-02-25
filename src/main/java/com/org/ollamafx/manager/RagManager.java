package com.org.ollamafx.manager;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.org.ollamafx.model.RagCollection;
import com.org.ollamafx.model.RagDocumentItem;
import com.org.ollamafx.model.RagResult;

import dev.langchain4j.community.rag.content.retriever.lucene.LuceneEmbeddingStore;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentParser;
import dev.langchain4j.data.document.parser.TextDocumentParser;
import dev.langchain4j.data.document.parser.apache.pdfbox.ApachePdfBoxDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentByParagraphSplitter;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.ollama.OllamaEmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;

/**
 * Singleton manager for the Local RAG pipeline.
 * Handles document ingestion, embedding, vector store, and context retrieval.
 */
public class RagManager {

    private static final Logger LOGGER = Logger.getLogger(RagManager.class.getName());
    private static final String EMBEDDING_MODEL_NAME = "nomic-embed-text";
    private static final String VECTORS_DIR = ".ollamafx/storage/vectors";
    private static final String DOCS_METADATA_FILE = "rag_documents.json";
    private static final String DEFAULT_COLLECTION_NAME = "General";
    private static final int MAX_SEGMENT_TOKENS = 500;
    private static final int MAX_OVERLAP_TOKENS = 50;

    private static RagManager instance;

    private LuceneEmbeddingStore embeddingStore;
    private EmbeddingModel embeddingModel;
    private final ExecutorService indexingExecutor;
    private final ObservableList<RagDocumentItem> documents;
    private final ObservableList<RagCollection> collections;
    private boolean initialized = false;

    private RagManager() {
        indexingExecutor = Executors.newFixedThreadPool(2, r -> {
            Thread t = new Thread(r, "rag-indexer");
            t.setDaemon(true);
            return t;
        });
        documents = FXCollections.observableArrayList();
        collections = FXCollections.observableArrayList();
    }

    public static synchronized RagManager getInstance() {
        if (instance == null) {
            instance = new RagManager();
        }
        return instance;
    }

    /**
     * Initialize the RAG engine: embedding model + Lucene store.
     * Must be called before any indexing or querying.
     * Safe to call multiple times (idempotent).
     */
    public synchronized void initialize() {
        if (initialized) return;

        try {
            String ollamaHost = ConfigManager.getInstance().getOllamaHost();

            // Initialize embedding model via Ollama
            embeddingModel = OllamaEmbeddingModel.builder()
                    .baseUrl(ollamaHost)
                    .modelName(EMBEDDING_MODEL_NAME)
                    .build();

            // Initialize Lucene vector store with persistence
            Path vectorPath = Path.of(System.getProperty("user.home"), VECTORS_DIR);
            File vectorDir = vectorPath.toFile();
            if (!vectorDir.exists()) {
                vectorDir.mkdirs();
            }

            embeddingStore = LuceneEmbeddingStore.builder()
                    .directory(org.apache.lucene.store.FSDirectory.open(vectorPath))
                    .build();

            // Load previously indexed documents from metadata
            loadExistingDocuments();

            initialized = true;
            LOGGER.info("RAG Manager initialized. Vector store at: " + vectorPath);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to initialize RAG Manager", e);
        }
    }

    /**
     * Check if the embedding model (nomic-embed-text) is available locally in Ollama.
     */
    public boolean isEmbeddingModelAvailable() {
        try {
            return ModelManager.getInstance().getLocalModels().stream()
                    .anyMatch(m -> m.getName().equals(EMBEDDING_MODEL_NAME));
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Could not check embedding model availability", e);
            return false;
        }
    }

    /**
     * Index a document asynchronously. Returns a Task for progress binding.
     */
    public Task<Void> indexDocument(File file, RagDocumentItem item) {
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                Platform.runLater(() -> {
                    item.setStatus(RagDocumentItem.Status.INDEXING);
                    item.setProgress(-1); // Indeterminate
                });

                try {
                    // 1. Parse document
                    DocumentParser parser = getParserForFile(file);
                    Document document;
                    try (InputStream is = new FileInputStream(file)) {
                        document = parser.parse(is);
                    }
                    document.metadata().put("file_name", file.getName());
                    document.metadata().put("file_path", file.getAbsolutePath());

                    // 2. Split into segments
                    DocumentByParagraphSplitter splitter = new DocumentByParagraphSplitter(
                            MAX_SEGMENT_TOKENS, MAX_OVERLAP_TOKENS);
                    List<TextSegment> segments = splitter.split(document);

                    // Add file metadata to each segment
                    for (int i = 0; i < segments.size(); i++) {
                        TextSegment seg = segments.get(i);
                        seg.metadata().put("file_name", file.getName());
                        seg.metadata().put("file_path", file.getAbsolutePath());
                        seg.metadata().put("segment_index", String.valueOf(i));
                        seg.metadata().put("collection_id", item.getCollectionId());
                    }

                    if (segments.isEmpty()) {
                        Platform.runLater(() -> {
                            item.setStatus(RagDocumentItem.Status.ERROR);
                            item.setErrorMessage("No content found in document");
                        });
                        return null;
                    }

                    // 3. Embed all segments
                    updateMessage("Embedding " + segments.size() + " segments...");
                    List<Embedding> embeddings = embeddingModel.embedAll(
                            segments).content();

                    // 4. Store in Lucene
                    List<String> ids = segments.stream()
                            .map(s -> UUID.randomUUID().toString())
                            .collect(Collectors.toList());
                    embeddingStore.addAll(ids, embeddings, segments);

                    Platform.runLater(() -> {
                        item.setStatus(RagDocumentItem.Status.READY);
                        item.setProgress(1.0);
                        saveDocumentMetadata();
                    });

                    LOGGER.info("Indexed document: " + file.getName() +
                            " (" + segments.size() + " segments)");

                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "Failed to index: " + file.getName(), e);
                    Platform.runLater(() -> {
                        item.setStatus(RagDocumentItem.Status.ERROR);
                        item.setErrorMessage(e.getMessage());
                    });
                }
                return null;
            }
        };

        indexingExecutor.submit(task);
        return task;
    }

    /**
     * Query the vector store for relevant context given a user query.
     * Searches all documents (no collection filtering).
     */
    public List<RagResult> queryContext(String userQuery, int topK) {
        return queryContext(userQuery, topK, null);
    }

    /**
     * Query the vector store for relevant context, filtered by collection IDs.
     * @param collectionIds set of collection IDs to filter by, or null/empty for all
     */
    public List<RagResult> queryContext(String userQuery, int topK, Set<String> collectionIds) {
        List<RagResult> results = new ArrayList<>();

        if (!initialized || embeddingStore == null || embeddingModel == null) {
            LOGGER.warning("RAG not initialized, returning empty context");
            return results;
        }

        try {
            Embedding queryEmbedding = embeddingModel.embed(userQuery).content();

            // Request more results if filtering, to ensure enough results after filtering
            int requestCount = (collectionIds != null && !collectionIds.isEmpty()) ? topK * 3 : topK;

            EmbeddingSearchRequest request = EmbeddingSearchRequest.builder()
                    .queryEmbedding(queryEmbedding)
                    .maxResults(requestCount)
                    .minScore(0.2) // Low threshold to capture cross-language semantic matches
                    .build();

            EmbeddingSearchResult<TextSegment> searchResult = embeddingStore.search(request);

            for (EmbeddingMatch<TextSegment> match : searchResult.matches()) {
                TextSegment segment = match.embedded();
                String segCollectionId = segment.metadata().getString("collection_id");

                // Filter by collection if specified
                if (collectionIds != null && !collectionIds.isEmpty()) {
                    if (segCollectionId == null || !collectionIds.contains(segCollectionId)) {
                        continue;
                    }
                }

                String fileName = segment.metadata().getString("file_name");
                String pageStr = segment.metadata().getString("page_number");
                int pageNumber = 0;
                if (pageStr != null) {
                    try { pageNumber = Integer.parseInt(pageStr); } catch (NumberFormatException ignored) {}
                }

                String contentText = segment.text();
                LOGGER.info(String.format("RAG match [score=%.3f, file=%s, collection=%s]: %s",
                        match.score(), fileName, segCollectionId,
                        contentText.length() > 120 ? contentText.substring(0, 120) + "..." : contentText));

                results.add(new RagResult(
                        contentText,
                        fileName != null ? fileName : "Unknown",
                        pageNumber,
                        match.score()
                ));

                if (results.size() >= topK) break;
            }

            LOGGER.info("RAG query returned " + results.size() + " results for: " +
                    (userQuery.length() > 50 ? userQuery.substring(0, 50) + "..." : userQuery));

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "RAG query failed", e);
        }

        return results;
    }

    /**
     * Build the augmented prompt using retrieved context and the RAG template.
     */
    public String buildAugmentedPrompt(String userMessage, List<RagResult> results) {
        if (results.isEmpty()) {
            return userMessage;
        }

        StringBuilder context = new StringBuilder();
        for (RagResult r : results) {
            context.append("--- ").append(r.getFileName());
            if (r.getPageNumber() > 0) {
                context.append(" (page ").append(r.getPageNumber()).append(")");
            }
            context.append(" ---\n");
            context.append(r.getContent()).append("\n\n");
        }

        String augmentedPrompt = "Answer the following question using ONLY the context provided below.\n" +
                "If the context is in a different language than the question, translate your answer " +
                "to match the question's language.\n\n" +
                "CONTEXT:\n" + context.toString().trim() + "\n\n" +
                "QUESTION: " + userMessage;

        LOGGER.info("Augmented prompt length: " + augmentedPrompt.length() + " chars");
        return augmentedPrompt;
    }

    /**
     * Delete all vectors associated with a given file name.
     */
    public void deleteDocument(String fileName) {
        try {
            // Remove from the observable list
            documents.removeIf(d -> d.getFileName().equals(fileName));
            saveDocumentMetadata();

            // Remove from Lucene by re-building (Lucene doesn't support metadata-based delete easily)
            // For now, we mark it removed from the UI. Full Lucene cleanup requires index rebuild.
            LOGGER.info("Removed document from library: " + fileName);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to delete document: " + fileName, e);
        }
    }

    /**
     * Get the observable list of documents for UI binding.
     */
    public ObservableList<RagDocumentItem> getDocuments() {
        return documents;
    }

    /**
     * Get the observable list of collections for UI binding.
     */
    public ObservableList<RagCollection> getCollections() {
        return collections;
    }

    /**
     * Create a new collection. Returns the created collection.
     */
    public RagCollection createCollection(String name) {
        RagCollection collection = new RagCollection(name);
        collections.add(collection);
        saveDocumentMetadata();
        LOGGER.info("Created collection: " + name + " (" + collection.getId() + ")");
        return collection;
    }

    /**
     * Delete a collection and all its documents from the UI list.
     * Note: Lucene vectors remain but are orphaned (won't match queries with collection filter).
     */
    public void deleteCollection(String collectionId) {
        documents.removeIf(d -> collectionId.equals(d.getCollectionId()));
        collections.removeIf(c -> c.getId().equals(collectionId));
        saveDocumentMetadata();
        LOGGER.info("Deleted collection: " + collectionId);
    }

    /**
     * Rename a collection.
     */
    public void renameCollection(String collectionId, String newName) {
        collections.stream()
                .filter(c -> c.getId().equals(collectionId))
                .findFirst()
                .ifPresent(c -> {
                    c.setName(newName);
                    saveDocumentMetadata();
                    LOGGER.info("Renamed collection " + collectionId + " to: " + newName);
                });
    }

    /**
     * Get the default collection, creating it if necessary.
     */
    public RagCollection getOrCreateDefaultCollection() {
        return collections.stream()
                .filter(c -> DEFAULT_COLLECTION_NAME.equals(c.getName()))
                .findFirst()
                .orElseGet(() -> createCollection(DEFAULT_COLLECTION_NAME));
    }

    /**
     * Get documents filtered by collection ID.
     */
    public List<RagDocumentItem> getDocumentsByCollection(String collectionId) {
        return documents.stream()
                .filter(d -> collectionId.equals(d.getCollectionId()))
                .collect(Collectors.toList());
    }

    /**
     * Get the total number of indexed documents with READY status.
     */
    public long getReadyDocumentCount() {
        return documents.stream()
                .filter(d -> d.getStatus() == RagDocumentItem.Status.READY)
                .count();
    }

    /**
     * Check if a file is already indexed.
     */
    public boolean isDocumentIndexed(String fileName) {
        return documents.stream()
                .anyMatch(d -> d.getFileName().equals(fileName));
    }

    /**
     * Determine the appropriate parser based on file extension.
     */
    private DocumentParser getParserForFile(File file) {
        String name = file.getName().toLowerCase();
        if (name.endsWith(".pdf")) {
            return new ApachePdfBoxDocumentParser();
        }
        // TextDocumentParser handles .txt, .md, and other text formats
        return new TextDocumentParser();
    }

    /**
     * Load document metadata from JSON file on startup.
     * Restores the Knowledge Base UI list from the last session.
     */
    private void loadExistingDocuments() {
        try {
            Path metadataPath = Path.of(System.getProperty("user.home"), VECTORS_DIR, DOCS_METADATA_FILE);
            File metadataFile = metadataPath.toFile();
            if (!metadataFile.exists()) {
                // First run: create default collection
                createCollection(DEFAULT_COLLECTION_NAME);
                return;
            }

            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> data = mapper.readValue(metadataFile,
                    new TypeReference<Map<String, Object>>() {});

            // Load collections
            @SuppressWarnings("unchecked")
            List<Map<String, String>> collectionEntries = (List<Map<String, String>>) data.getOrDefault("collections", Collections.emptyList());
            for (Map<String, String> entry : collectionEntries) {
                String id = entry.get("id");
                String name = entry.get("name");
                if (id != null && name != null) {
                    collections.add(new RagCollection(id, name));
                }
            }

            // Load documents
            @SuppressWarnings("unchecked")
            List<Map<String, String>> docEntries = (List<Map<String, String>>) data.getOrDefault("documents", Collections.emptyList());
            for (Map<String, String> entry : docEntries) {
                String fileName = entry.get("fileName");
                String filePath = entry.get("filePath");
                String collectionId = entry.getOrDefault("collectionId", "");
                String statusStr = entry.getOrDefault("status", "READY");

                if (fileName != null && filePath != null) {
                    RagDocumentItem item = new RagDocumentItem(fileName, filePath, collectionId);
                    try {
                        item.setStatus(RagDocumentItem.Status.valueOf(statusStr));
                    } catch (IllegalArgumentException e) {
                        item.setStatus(RagDocumentItem.Status.READY);
                    }
                    item.setProgress(1.0);
                    documents.add(item);
                }
            }

            // Ensure default collection exists
            if (collections.isEmpty()) {
                createCollection(DEFAULT_COLLECTION_NAME);
            }

            // Migrate orphan documents (no collectionId) to default collection
            String defaultId = getOrCreateDefaultCollection().getId();
            for (RagDocumentItem doc : documents) {
                if (doc.getCollectionId() == null || doc.getCollectionId().isEmpty()) {
                    doc.setCollectionId(defaultId);
                }
            }

            LOGGER.info("Loaded " + collections.size() + " collections and " + documents.size() + " documents");
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Could not load document metadata", e);
            if (collections.isEmpty()) {
                createCollection(DEFAULT_COLLECTION_NAME);
            }
        }
    }

    /**
     * Save collections and document metadata to JSON file.
     */
    private void saveDocumentMetadata() {
        try {
            Path metadataPath = Path.of(System.getProperty("user.home"), VECTORS_DIR, DOCS_METADATA_FILE);
            File parentDir = metadataPath.getParent().toFile();
            if (!parentDir.exists()) {
                parentDir.mkdirs();
            }

            List<Map<String, String>> collectionEntries = collections.stream()
                    .map(c -> Map.of("id", c.getId(), "name", c.getName()))
                    .collect(Collectors.toList());

            List<Map<String, String>> docEntries = documents.stream()
                    .filter(d -> d.getStatus() == RagDocumentItem.Status.READY)
                    .map(d -> Map.of(
                            "fileName", d.getFileName(),
                            "filePath", d.getFilePath(),
                            "collectionId", d.getCollectionId() != null ? d.getCollectionId() : "",
                            "status", d.getStatus().name()
                    ))
                    .collect(Collectors.toList());

            Map<String, Object> data = Map.of(
                    "collections", collectionEntries,
                    "documents", docEntries
            );

            ObjectMapper mapper = new ObjectMapper();
            mapper.writerWithDefaultPrettyPrinter().writeValue(metadataPath.toFile(), data);

            LOGGER.info("Saved " + collectionEntries.size() + " collections and " + docEntries.size() + " documents");
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Could not save document metadata", e);
        }
    }

    /**
     * Supported file extensions for RAG ingestion.
     */
    public static boolean isSupportedFile(File file) {
        String name = file.getName().toLowerCase();
        return name.endsWith(".txt") || name.endsWith(".md") || name.endsWith(".pdf");
    }

    /**
     * Shutdown RAG resources gracefully.
     */
    public void shutdown() {
        try {
            if (indexingExecutor != null) {
                indexingExecutor.shutdown();
                if (!indexingExecutor.awaitTermination(2, TimeUnit.SECONDS)) {
                    indexingExecutor.shutdownNow();
                }
            }
            // LuceneEmbeddingStore handles its own close on JVM shutdown
            LOGGER.info("RAG Manager shut down.");
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error during RAG shutdown", e);
        }
    }
}
