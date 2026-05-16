# OllamaFX — AI Context File
## Generated 2026-05-14 | Java 17 | JavaFX 17.0.6 | Gradle 8.5 | Single Module

---

## 1. PROJECT OVERVIEW
**OllamaFX** is a desktop GUI client for [Ollama](https://ollama.com) — a local LLM runtime. It provides chat, model management, RAG knowledge base, and system integration features. Built as a fat JAR (~155 MB) with ShadowJar bundling cross-platform JavaFX natives (linux, win, mac, mac-aarch64).

| Property | Value |
|----------|-------|
| Version | 0.5.1 |
| Group | com.org.ollamafx |
| Main Class | com.org.ollamafx.Main |
| Build | Gradle 8.5 (Groovy DSL) |
| Module System | None (no module-info.java) |
| IDE | Eclipse + Buildship |
| CI | GitHub Actions (JDK 21 Temurin, ubuntu-latest) |
| Min JRE | Java 17 |

---

## 2. ARCHITECTURE PATTERN: MVC-lite + Singleton Managers

```
┌─────────────────────────────────────────────────────┐
│ App.java (JavaFX Application)                       │
│  ├─ CachedThreadPool (daemon threads)               │
│  ├─ Theme: CupertinoLight / CupertinoDark (AtlantaFX)│
│  ├─ i18n: ResourceBundle (EN/ES)                    │
│  ├─ Splash vs MainUI routing                        │
│  └─ Global exception handler                        │
├─────────────────────────────────────────────────────┤
│ Main.java → App.main(args)  (macOS dock name setup) │
└─────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────┐
│ CONTROLLERS (14) ← FXML-backed, Singleton access to  │
│                   Managers via getInstance()         │
│  MainController ─── Sidebar + View Routing (StackPane)│
│  ChatController ─── Chat, Streaming, RAG, Multimodal │
│  HomeController ─── Dashboard, Carousels, Updates    │
│  SplashController ── Library cache crawl progress    │
│  SettingsController ─ Connection/Theme/Language/HW  │
│  ...+9 more                                          │
├──────────────────────────────────────────────────────┤
│ MANAGERS (13) ── Business Logic, all Singletons      │
│  OllamaManager ─── API calls (ollama4j + raw HTTP)   │
│  ModelManager ──── Model state, classification       │
│  ChatManager ───── Chat CRUD, JSON persistence       │
│  ChatCollectionManager ─ Folders, Smart Collections  │
│  TrashManager ──── Soft-delete w/ 30-day cleanup     │
│  RagManager ────── Local RAG (LangChain4j + Lucene)  │
│  ModelLibraryManager ─ Ollama.com crawler/scraper    │
│  ConfigManager ─── Java Preferences API              │
│  HardwareManager ── OSHI hardware detection          │
│  OllamaServiceManager ─ ollama daemon lifecycle      │
│  ...+3 cache managers                                │
├──────────────────────────────────────────────────────┤
│ MODELS (13) ── Jackson + JavaFX Properties           │
│  OllamaModel (central), ChatSession, ChatMessage,     │
│  ChatNode, ChatFolder, SmartCollection, TrashItem,   │
│  RagCollection, RagDocumentItem, RagResult, etc.     │
├──────────────────────────────────────────────────────┤
│ UI COMPONENTS (9) ── Custom JavaFX widgets            │
│  ChatTreeCell, MarkdownOutput, CodeBlockCard,         │
│  ModelCard, ImagePreviewStrip, TrashView, etc.       │
├──────────────────────────────────────────────────────┤
│ SERVICES (3) ── Cross-cutting                         │
│  UpdateManagerService (self-update ZIP + scripts)    │
│  GitHubUpdateService (release check)                  │
│  MarkdownService (chat export)                        │
├──────────────────────────────────────────────────────┤
│ UTILS (3)                                            │
│  Utils, ImageUtils, SecurityUtils                    │
└──────────────────────────────────────────────────────┘
```

**No Dependency Injection framework** — Controllers receive Manager references via manual setters (e.g., `controller.setModelManager(modelManager)`). All Managers are accessed via `getInstance()`.

---

## 3. KEY DEPENDENCIES & THEIR ROLES

| Dependency | Version | Purpose |
|------------|---------|---------|
| ollama4j | 1.0.100 | Official Ollama Java client (list models, pull, chat) |
| RichTextFX | 0.11.0 | Code/text area (used for input) |
| AtlantaFX | 2.0.1 | CSS theme framework; BBCodeParser for markdown rendering |
| Jackson | 2.15.2 | JSON serialization (chat persistence, caches) |
| jsoup | 1.17.2 | HTML scraping (ollama.com/library) |
| commonmark | 0.21.0 | Markdown parsing → BBCode |
| flexmark | 0.64.8 | Extended Markdown parsing (native viewer) |
| LangChain4j | 1.9.1 | RAG: OllamaEmbeddingModel + LuceneEmbeddingStore + PDFBox |
| OSHI | 6.4.0 | Hardware detection (RAM, VRAM, CPU) |
| Ikonli | 12.3.1 | Icon packs (Feather, Material2, FontAwesome5) |
| SLF4J | 2.0.12 | Logging (nop implementation) |
| ShadowJar | 8.1.1 | Fat JAR packaging |
| JavaFX | 17.0.6 | UI framework (controls, fxml, web, media, graphics, base) |

---

## 4. FILE-BY-FILE ARCHITECTURE MAP

### 4.1 Entry Points
| File | Lines | Role |
|------|-------|------|
| `Main.java` | 8 | Public entry; sets `apple.awt.application.name`, delegates to `App.main()` |
| `App.java` | 238 | JavaFX Application: CachedThreadPool, theme init (Cupertino), i18n (ConfigManager→ResourceBundle), splash/main UI routing (`ModelLibraryManager.UpdateStatus`), global exception handler, `reloadUI()`, `reloadFromSplash()` |

### 4.2 Controllers (com.org.ollamafx.controller)
| File | Lines | FXML | Role |
|------|-------|------|------|
| `MainController.java` | 913 | main_view.fxml | App shell: sidebar (TreeView of chats/folders/smart collections), center StackPane routing, Ollama service status bar with pulse animation, theme toggle, sidebar SVG icons |
| `ChatController.java` | 1383 | chat_view.fxml | Chat engine: streaming responses via `OllamaManager.askModelStream()`, message bubbles (user/assistant), Markdown rendering, RAG context injection, multimodal image attachments (drag-drop/paste), model selector, parameter sliders (temp, topP, topK, numCtx, seed), presets (Writer/Dev/Lawyer/Doctor/Student), chat export to MD, document indexing to RAG |
| `HomeController.java` | 531 | home_view.fxml | Dashboard: model carousels (Recommended/Popular/New via HBox + ModelCard), recent chats list, update checking (`GitHubUpdateService` + `UpdateManagerService`), donation links (PayPal/BuyMeACoffee), theme toggle |
| `SplashController.java` | 167 | splash_view.fxml | First-run library crawl: monitors `ModelLibraryManager` progress, parses status strings (e.g., "Procesando (X/Y): Name"), shows RingProgressIndicator, launches main UI on completion |
| `SettingsController.java` | ~200 | settings_view.fxml | Settings: Ollama host URL, theme toggle, language selector, hardware info display, "Refresh Library" button, API timeout config |
| `AvailableModelsController.java` | ~220 | available_models_view.fxml | Model library browser: search, sort (A-Z/Z-A), lazy loading, ModelCard grid |
| `LocalModelsController.java` | ~120 | local_models_view.fxml | Installed models table (Name/Tag/Size/Date), uninstall |
| `ModelDetailController.java` | ~400 | model_detail_view.fxml | Model detail: tags table, README preview (WebView or text), install/uninstall per tag |
| `RagLibraryController.java` | ~450 | rag_library_view.fxml | Knowledge base management: collections list, document list, add files (PDF/TXT/MD), delete, indexing progress |
| `AboutController.java` | ~100 | about_view.fxml | App version, description, GitHub link, license |
| `MarkdownViewerController.java` | ~150 | markdown_viewer.fxml | External Markdown file viewer (flexmark) |
| `DownloadPopupController.java` | ~50 | download_popup.fxml | Model download progress dialog |
| `HardwareExplanationController.java` | ~30 | hardware_explanation_popup.fxml | Static help popup |

### 4.3 Managers (com.org.ollamafx.manager)
| File | Lines | Singleton? | Key Responsibility |
|------|-------|------------|-------------------|
| `OllamaManager.java` | 545 | Yes | Core Ollama communication: `updateClient()` (sets host), `getLocalModels()` (ollama4j), `getAvailableBaseModels()`, `getLibraryModels()` (jsoup scrape by sort), `scrapeModelDetails()` (per-model detail + tags scraping), `pullModel()` (ProcessBuilder `ollama pull` with progress parsing), `deleteModel()` (ProcessBuilder `ollama rm`), `askModelStream()` (raw HTTP POST to /api/chat with streaming JSON lines, manual Content-Type + auth, supports multimodal images, system prompt, request options), `cancelCurrentRequest()` (closes InputStream) |
| `ModelManager.java` | 455 | Yes | Model state: `localModels`, `availableModels`, `popularModels`, `newModels`, `recommendedModels` (all ObservableList), `installedModelsCache` (Set for O(1) lookup), `classifyModel()` (RAM/VRAM-based RECOMMENDED/CAUTION/INCOMPATIBLE), `parseModelSizeBytes()`, `extractParameterCount()` (regex "8b"/"70b"), `generateRecommendations()` (top 12 from popular+new that fit VRAM), `getModelDetails()` (cache-only, no scraping) |
| `ChatManager.java` | 153 | Yes | Chat CRUD: `chatSessions` (ObservableList), `sortedSessions` (SortedList by pinned→creationDate), `createChat()`, `deleteChat()` (→TrashManager), `renameChat()`, `togglePin()`, `saveChat()`/`loadChats()` (JSON to ~/.OllamaFX/chats/{uuid}.json), property listeners auto-save |
| `ChatCollectionManager.java` | 321 | Yes | Organization: `folders` (ObservableList<ChatFolder>), `smartCollections`, `chatFolderMap` (Map for lookup), CRUD for folders/smart collections, move chat between folders, color tagging, persistence to ~/.OllamaFX/collections.json and smart_collections.json |
| `TrashManager.java` | 289 | Yes | Soft-delete: `trashChat()`/`trashFolder()` (moves to trash list, removes from active), `restoreChat()`/`restoreFolder()` (returns to original folder), `permanentlyDeleteChat()`/`permanentlyDeleteFolder()`, `emptyTrash()`, auto-clean items >30 days, persistence to ~/.OllamaFX/trash.json |
| `RagManager.java` | 579 | Yes | Local RAG pipeline: lazy `initialize()` (OllamaEmbeddingModel(nomic-embed-text) + LuceneEmbeddingStore on ~/.ollamafx/storage/vectors), `indexDocument()` (async: parse PDF/TXT/MD → split by paragraph → embed → store in Lucene), `queryContext()` (embed query → Lucene search with score≥0.2, optional collection filter), `buildAugmentedPrompt()` (injects context + language directive), collection CRUD, document metadata persistence |
| `ModelLibraryManager.java` | 321 | Yes | Library crawler: `UpdateStatus` enum (UP_TO_DATE/OUTDATED_SOFT/MOUTDATED_HARD based on cache age), two-phase `updateLibraryFull()` (Phase 1: `discoverAllModelNames()` scrapes all pages of ollama.com/library?sort=popular; Phase 2: loops each model calling `OllamaManager.scrapeModelDetails()`, classifies, saves to `ModelDetailsCacheManager`), progress tracking with `currentProgress`/`currentStatus` fields |
| `ConfigManager.java` | 68 | Yes | Settings via `java.util.prefs.Preferences`: ollama host (default `http://127.0.0.1:11434`), theme (dark/light), API timeout (default 120s), language (default "es") |
| `HardwareManager.java` | 123 | Static | Hardware via OSHI: `getStats()` returns `HardwareStats` (totalRamBytes, availableRamBytes, totalVramBytes, isUnifiedMemory), Apple Silicon detection (vendor/apple), VRAM via GraphicsCard enumeration |
| `OllamaServiceManager.java` | ~80 | Yes | Ollama daemon: `isInstalled()` (ollama --version), `isRunning()` (HTTP health check to host), `startOllama()` (ProcessBuilder `ollama serve`), `stopOllama()` (kill process or API) |
| `LibraryCacheManager.java` | ~100 | Yes | JSON cache for model library at ~/.ollamafx/library_cache.json |
| `ModelDetailsCacheManager.java` | ~80 | Yes | JSON cache for per-model details at ~/.ollamafx/details_cache.json |
| `GitHubStatsService.java` | ~90 | No | GitHub API: fetch repo stars, forks, latest release info |

### 4.4 Models (com.org.ollamafx.model)
| File | Lines | Key Fields | Notes |
|------|-------|------------|-------|
| `OllamaModel.java` | 187 | name, description, pullCount, tag, size, lastUpdated, compatibilityStatus (RECOMMENDED/CAUTION/INCOMPATIBLE), contextLength, inputType, badges (List<String>), readmeContent | Jackson @JsonCreator with JavaFX Properties; multiple constructors for different use cases; `@JsonIgnoreProperties(ignoreUnknown=true)` |
| `ChatSession.java` | 184 | id (UUID), name, modelName, pinned, creationDate, messages (List<ChatMessage>), temperature (0.7), systemPrompt, seed (-1), numCtx (4096), topK (40), topP (0.9), ragCollectionIds | Mixed Jackson + JavaFX Properties; default constructor for Jackson |
| `ChatMessage.java` | ~70 | role (user/assistant), content, images (List<String> base64) | |
| `ChatNode.java` | ~50 | Type enum (CHAT/FOLDER/SMART_COLLECTION), wraps ChatSession/ChatFolder/SmartCollection | TreeView item wrapper |
| `ChatFolder.java` | ~50 | id, name, color (#HEX), expanded, chatIds (List<String>) | |
| `SmartCollection.java` | ~50 | name, criteria (KEYWORD/MODEL/DATE), value, icon, expanded | |
| `RagCollection.java` | ~40 | id, name | |
| `RagDocumentItem.java` | ~70 | fileName, filePath, collectionId, status (READY/INDEXING/ERROR), progress, errorMessage | |
| `RagResult.java` | ~30 | content, fileName, pageNumber, score | |
| `TrashItem.java` | ~60 | type (CHAT/FOLDER), chat, folder, originalFolderId, deletedAt | |
| `LibraryCache.java` | ~40 | allModels, popularModels, newModels, lastUpdated | |
| `ModelDetailsCache.java` | ~20 | Map<String, ModelDetailsEntry> | |
| `ModelDetailsEntry.java` | ~20 | tags (List<OllamaModel>) | |

### 4.5 UI Components (com.org.ollamafx.ui)
| File | Lines | Extends | Role |
|------|-------|---------|------|
| `ChatTreeCell.java` | 359 | TreeCell<ChatNode> | Custom cell renderer: context menus per node type (folder/chat/smart collection), drag-and-drop (chat→folder), SVG feather-style icons, inline color picker for folders |
| `MarkdownOutput.java` | 187 | VBox | Streaming markdown renderer: commonmark parse → `MarkdownToBBCodeVisitor` → AtlantaFX `BBCodeParser.createLayout()`, diff-based incremental sync (BlockData list), separates CODE (CodeBlockCard) from PROSE |
| `CodeBlockCard.java` | ~70 | VBox | Syntax-highlighted code block with language label and copy button |
| `ModelCard.java` | ~130 | VBox | Model display card: name, description, badges, compatibility badge (color-coded), install/uninstall buttons |
| `ImagePreviewStrip.java` | ~180 | HBox | Horizontal image thumbnail strip: add/remove images, base64 encoding, empty property for binding |
| `TrashView.java` | 243 | VBox | Trash bin UI: list of TrashItems, restore/permanently delete buttons, empty trash |
| `FxDialog.java` | ~180 | — | Static utility: `showInputDialog()` (text input), `showConfirmDialog()` (yes/no) |
| `SmartCollectionDialog.java` | ~90 | — | Smart collection creation/edit dialog (name, criteria dropdown, value, icon) |
| `MarkdownToBBCodeVisitor.java` | ~90 | AbstractVisitor | Commonmark AST node visitor converting to BBCode strings (bold, italic, headings, lists, links, etc.) |

### 4.6 Services (com.org.ollamafx.service)
| File | Lines | Role |
|------|-------|------|
| `UpdateManagerService.java` | 286 | Self-update: `downloadUpdate()` (HTTP GET with progress), `extractUpdate()` (ZIP → update/new/ stripping root folder), `backupCurrentVersion()`, `applyAndRestart()` (launches apply_update shell script + System.exit(0)), `deleteFolder()` (recursive) |
| `GitHubUpdateService.java` | 120 | GitHub Releases API: `fetchLatestRelease()` (CompletableFuture), version comparison |
| `MarkdownService.java` | 60 | `exportChatToMarkdown()` (ChatSession → .md file) |

### 4.7 Utilities (com.org.ollamafx.util)
| File | Lines | Key Methods |
|------|-------|------------|
| `Utils.java` | 162 | `parseSize()`, `formatSize()`, `showError()`, `parseDownloadCount()`, `parseRelativeDate()`, `getOllamaExecutable()` (resolves /opt/homebrew/bin/ollama, /usr/local/bin/ollama, fallback "ollama") |
| `ImageUtils.java` | 72 | `isValidImageFile()`, `isSupportedFormat()`, Base64 encoding utilities |
| `SecurityUtils.java` | 38 | `isValidModelName()` (regex validation for shell-safe names) |

---

## 5. DATA FLOW: KEY OPERATIONS

### 5.1 Chat Streaming Flow
```
User types message + Enter
  → ChatController.sendMessage()
  → Capture images from ImagePreviewStrip (base64)
  → Add user message to ChatSession + UI
  → If RAG enabled: RagManager.queryContext(text, 5, selectedCollections)
      → Embed query via nomic-embed-text
      → Lucene cosine similarity search (minScore 0.2)
      → Build augmented prompt with context
  → OllamaManager.askModelStream(modelName, effectivePrompt, images, options, systemPrompt, handler)
      → Build JSON payload manually (messages[], options{}, stream:true)
      → HTTP POST to {host}/api/chat
      → Read response InputStream line-by-line
      → Parse each line as JSON, extract message.content
      → Accumulate full content, call handler.accept(fullText)
  → Handler updates ChatMessage.content + UI via MarkdownOutput.updateContent()
  → UI throttled to ~30fps (UI_UPDATE_INTERVAL_MS = 30ms)
  → On completion: save ChatSession via ChatManager.saveChats()
  → Add source citation pills if RAG was used
```

### 5.2 Model Library Update (Splash Screen)
```
App.start() checks cache status
  → If OUTDATED_HARD: show SplashController
  → SplashController.initialize()
  → Background thread: ModelLibraryManager.updateLibraryFull()
  → Phase 1: discoverAllModelNames()
      → Loop pages of ollama.com/library?sort=popular&page={n}
      → Jsoup scrape <a href="/library/..."> → Set<String> of model names
  → Phase 2: For each model name
      → OllamaManager.scrapeModelDetails(name)
          → Jsoup GET ollama.com/library/{name}
          → Extract: description, pull count, badges, README, tag rows
          → Returns List<OllamaModel> (one per tag)
      → ModelManager.classifyModel(tag) for each tag
          → OSHI hardware stats: RAM, VRAM, Apple Silicon detection
          → Compare model size vs VRAM/RAM limits
          → Set compatibility status
      → ModelDetailsCacheManager.saveDetails(name, tags)
  → Save LibraryCache (allModels, popularModels, newModels, lastUpdated)
  → On completion: App.reloadUI() → show main view
```

### 5.3 RAG Document Ingestion
```
User attaches file (drag-drop or file chooser)
  → ChatController.indexDocumentToRag(file)
  → Determine collection (folder name or "General")
  → RagManager.indexDocument(file, RagDocumentItem)
      → Parse: PDFBox (PDF) or TextDocumentParser (TXT/MD)
      → Split into paragraphs (DocumentByParagraphSplitter, 500 tokens, 50 overlap)
      → Add metadata to each segment (file_name, file_path, collection_id)
      → Embed all segments via OllamaEmbeddingModel (nomic-embed-text)
      → Store in LuceneEmbeddingStore with UUIDs
      → Update document status to READY
      → Save metadata to JSON
```

### 5.4 Self-Update Flow
```
HomeController.checkForUpdates()
  → GitHubUpdateService.fetchLatestRelease()
  → Compare with app.version from app.properties
  → If newer: show update dialog
  → User confirms: UpdateManagerService.downloadUpdate(url, progressCallback)
      → HTTP GET zip, track Content-Length progress
  → extractUpdate() → unzip to update/new/ (strip root wrapper folder)
  → backupCurrentVersion() → copy current files to update/old/
  → applyAndRestart() → launch apply_update.sh/.bat/.command → System.exit(0)
  → Script: replaces files, restarts the app
```

---

## 6. PERSISTENCE LAYOUT (~/.OllamaFX/)
```
~/.OllamaFX/
├── chats/                    # One JSON per chat session
│   └── {uuid}.json
├── collections.json          # ChatFolder list
├── smart_collections.json    # SmartCollection list  
├── trash.json                # TrashItem list
├── library_cache.json        # LibraryCache (scraped models)
├── details_cache.json        # ModelDetailsEntry per model
└── (Java Preferences)         # ConfigManager: host, theme, language, timeout

~/.ollamafx/
└── storage/
    └── vectors/              # Apache Lucene index
        └── rag_documents.json # Metadata (collections + documents)
```

---

## 7. THREADING MODEL
- **Main JavaFX Thread**: UI updates, FXML loading, property change listeners
- **App.getExecutorService()**: CachedThreadPool (daemon), used by controllers for background tasks (model loading, downloads)
- **Ollama streaming**: Background thread reading InputStream line-by-line
- **RAG indexing**: Fixed thread pool (2 threads, daemon, "rag-indexer")
- **Status polling**: Timeline (5s interval) + background thread for Ollama health check
- **Splash crawl**: Dedicated thread for library update
- **Update download**: CompletableFuture.runAsync

**Rule**: All UI updates MUST use `Platform.runLater()`.

---

## 8. CONFIGURATION
| Key | Default | Storage |
|-----|---------|---------|
| ollama_host | http://127.0.0.1:11434 | Java Preferences |
| app_theme | dark | Java Preferences |
| api_timeout_seconds | 120 | Java Preferences |
| app_language | es | Java Preferences |

---

## 9. FXML VIEW → CONTROLLER MAP
| FXML | Controller | Size |
|------|------------|------|
| main_view.fxml | MainController | 9.5 KB |
| chat_view.fxml | ChatController | 14.7 KB |
| home_view.fxml | HomeController | 13.4 KB |
| settings_view.fxml | SettingsController | 5.1 KB |
| available_models_view.fxml | AvailableModelsController | 4.2 KB |
| model_detail_view.fxml | ModelDetailController | 4.4 KB |
| rag_library_view.fxml | RagLibraryController | 4.1 KB |
| local_models_view.fxml | LocalModelsController | 1.9 KB |
| about_view.fxml | AboutController | 9.0 KB |
| splash_view.fxml | SplashController | 2.1 KB |
| markdown_viewer.fxml | MarkdownViewerController | 1.8 KB |
| download_popup.fxml | DownloadPopupController | 1.6 KB |
| hardware_explanation_popup.fxml | HardwareExplanationController | 3.9 KB |

---

## 10. CSS THEMES
- `ollama_active.css` (57 KB) — Main stylesheet, imported by all views
- `home.css` — Dashboard-specific styles
- `about.css` — About dialog styles
- `splash.css` — Splash screen styles
- `markdown_viewer.css` — Markdown viewer styles
- AtlantaFX themes: CupertinoLight, CupertinoDark, PrimerDark

---

## 11. I18N COVERAGE
- `messages.properties` (default, 145 lines) — Mostly English short strings + some Spanish
- `messages_en.properties` (11 KB) — Complete English
- `messages_es.properties` (14 KB) — Complete Spanish
- Keys used via `App.getBundle()` or injected via `loader.setResources()`
- **Known gaps**: Some strings in ChatTreeCell, SmartCollectionDialog are hardcoded in English

---

## 12. SECURITY NOTES
- `SecurityUtils.isValidModelName()` validates model name/tag before passing to ProcessBuilder (shell injection prevention)
- Ollama host URL validated in SettingsController before saving
- No API keys or secrets in source code
- HTTP connections to localhost (no TLS for Ollama API)
- User input sanitization in seed field (numeric only)

---

## 13. KNOWN TECHNICAL DEBT
1. **No tests** — Zero unit or integration tests
2. **Mixed logging** — `java.util.logging.Logger` + `System.err.println` + `System.out.println`
3. **Incomplete i18n** — Some strings hardcoded in English
4. **Duplicate parsing logic** — `parsePullCount`, `parseRelativeDate`, `parseModelSizeBytes` duplicated across ModelManager, ModelLibraryManager, Utils
5. **ChatController is 1383 lines** — Too large, mixes UI, streaming, RAG, multimodal logic
6. **Singleton abuse** — All Managers are Singletons, no DI, hard to test
7. **No abstraction/interface layer** — Controllers tightly coupled to concrete Manager classes
8. **Raw HTTP streaming** — OllamaManager builds JSON payloads manually instead of using ollama4j's built-in chat streaming
9. **Blocking HTTP calls** — Used in some places where async would be better
10. **No structured error handling** — Relies on try/catch + printStackTrace

---

## 14. BUILD & RUN
```bash
./gradlew build              # Compile
./gradlew shadowJar          # Build fat JAR → build/libs/OllamaFX.jar
./gradlew packageDistribution # Create release structure in releases/OllamaFX/
./gradlew zipDistribution    # Create releases/OllamaFX-{version}.zip
./gradlew run                # Run from IDE
```
