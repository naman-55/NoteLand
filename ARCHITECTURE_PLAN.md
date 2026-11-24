# Noteland 3.5 — Modular Architecture Plan

## 1. Architectural Principles

Noteland 3.5 evolves the existing single-module Android app into a **clean, feature-modularized MVVM + Domain Services architecture** *without* breaking existing package boundaries (`com.example.*`), Gradle stability, or test suites.

1. **Strangler-Fig Refactoring (Zero Big-Bang Rewrites):** Every monolith (`NoteEditorScreen`, `NoteViewModel`, `NoteRepository`, `CalendarScreen`, `FoldersScreen`, `PdfReaderScreen`) is decomposed incrementally inside clear feature packages while maintaining compatibility facades until all callers migrate.
2. **Strict File Size Discipline:** Target **< 500 LOC per file** (following the clean 4-file pattern already proven in `ui/screens/mindmap/`).
3. **Unidirectional Data Flow (UDF):** Feature ViewModels expose immutable `StateFlow<FeatureUiState>` + sealed `FeatureIntent/Action` events; Composables remain stateless where practical.
4. **Offline-First & Zero Unnecessary Dependencies:** Reuse existing AndroidX stack (`Room 2.7`, `Compose M3`, `Navigation Compose`, `WorkManager 2.10.1`, `Glance 1.1.1`, `Coil 2.7`, `Biometric`). Only enable already-cataloged `androidx-datastore-preferences` (if needed for typed settings) or keep unified Room `app_settings` + typed `SettingsRepository` to avoid dependency bloat.
5. **Play-Policy & Security First:** Zero broad storage permissions (`PickVisualMedia`, `OpenDocument`, `CreateDocument`, `FileProvider`), AES-256-GCM Keystore vault protection, no `!!` assertions.

---

## 2. Target Package & Module Layout

```text
app/src/main/java/com/example/
├── core/
│   ├── designsystem/       # Unified Theme, Color tokens, Shapes, Shared Pickers, CommonUiStates
│   ├── preferences/        # Unified PreferencesRepository (replaces fragmented SharedPreferences)
│   ├── export/             # Shared ExportService (PNG, PDF, Markdown, ShareSheet via FileProvider)
│   ├── ink/                # Unified InkEngine & Canvas viewport math (shared by Editor, PDF, Image, Studio)
│   └── files/              # ExternalFileIntakeService (ACTION_VIEW, ACTION_SEND, SAF persistable URIs)
├── data/
│   ├── db/                 # AppDatabase (v5 -> v7), DAOs, Migration registry + exported schema JSONs
│   ├── model/              # Room @Entity definitions (cleanly separated from UI models)
│   ├── repository/         # Domain-scoped repositories:
│   │                       #   NotesRepo, FoldersRepo, StudioRepo, ProductivityRepo, SettingsRepo, MediaRepo
│   │                       #   + NoteRepository facade preserving backward compatibility
│   └── storage/            # AttachmentStorageManager, BackupManager (modularized into Export/Import/Crypto)
├── feature/
│   ├── home/               # HomeScreen, HomeViewModel, DashboardWidgets, QuickCapture
│   ├── editor/             # Modular Advanced Notes Editor + Customizable Quick Edit Toolbar
│   ├── folders/            # FoldersScreen, FolderViewModel, SmartCollections, BatchActions
│   ├── pdf/                # PdfReader, PdfAnnotationEngine, PdfTools, PdfOutlineSheet
│   ├── studio/             # StudioHubScreen, MindMap (existing mindmap/), Whiteboard, StickyWall, ImageAnnotator
│   ├── productivity/       # Unified ProductivityHubScreen (Calendar, Tasks/Kanban, Habits, Focus, Reports)
│   ├── brain/              # Knowledge Graph, Backlinks, Templates, Version History
│   └── settings/           # SettingsScreen, ToolbarCustomizer, SecuritySettings, StorageInspector, BackupUI
├── security/               # SecurityManager, BiometricAuthHelper
├── widget/                 # Glance AppWidgets (DailyTasks, Reminders, Collections)
└── worker/                 # ProductivityWorkers (Pomodoro, Reminders, Habit Nudges)
```

---

## 3. State Management Decomposition (Breaking Up `NoteViewModel`)

`NoteViewModel.kt` (2,420 LOC) is currently a single god object. In Noteland 3.5:

1. **Phase-by-Phase Extraction of Feature ViewModels** (using lightweight manual `AppContainer` constructor injection — no heavy Hilt migration required):
   - `AppContainer(application)` holds singletons: `AppDatabase`, scoped repositories (`NotesRepository`, `FoldersRepository`, `StudioRepository`, `ProductivityRepository`, `MediaRepository`, `SettingsRepository`), `SecurityManager`, `BackupManager`, `AttachmentStorageManager`, `ExportService`, `ExternalFileIntakeService`.
   - Scoped ViewModels created via `ViewModelProvider.Factory`:
     - `HomeViewModel` (feed, greeting, dashboard widgets, search trigger)
     - `NoteEditorViewModel` (single-note editing, debounced serialized autosave, undo/redo, toolbar config, checklists, backlinks, versions)
     - `FoldersViewModel` (folder tree, folder contents including PDFs, batch selection)
     - `StudioViewModel` (Mind Maps, Whiteboards, Sticky Walls, Image Markup list & templates)
     - `ProductivityViewModel` (Calendar, Kanban/Tasks, Habits, Pomodoro, Reports)
     - `SettingsViewModel` (Theme, Appearance, Quick Toolbar customization, Security/Vault, Backup, Storage Inspector)
2. **Backward Compatibility Strategy:** Retain `NoteViewModel` as a thin delegating facade (or keep its public API intact for existing tests like `Phase0StabilityTest`, `Phase1KnowledgeBrainTest`, `Phase2ProductivityTest`) while new/upgraded screens bind to focused ViewModels.

---

## 4. Navigation Architecture (5 Pillars + Type-Safe Routes)

### 4.1 Primary Navigation Shell (`PrimaryNavItems`)
Upgrade `FloatingGlassBottomBar` (phone) and `NavigationRail` (tablet/foldable) to the **5 Noteland 3.5 Pillars**:
1. **Home** (`Screen.Home`) — Overview, Search, Recent Notes, Smart Quick-Capture
2. **Folders** (`Screen.Folders`) — Nested Folders, PDFs/Docs, Smart Collections (Favourites, Vault, Archive, Trash), Tags
3. **Studio** (`Screen.Studio`) — Mind Maps, Whiteboards, Sticky Walls, Image Annotator, PDF Markup Hub
4. **Productivity** (`Screen.Productivity`) — Calendar, Tasks & Kanban, Habits, Focus Timer, Reports
5. **Settings** (`Screen.Settings`) — Appearance, Toolbar Customization, Security, Backups, Storage, External Files

### 4.2 Routing & Compatibility Rules
- Preserve legacy string route constants (`Screen.Favorites`, `Screen.Canvas`, `Screen.StickyBoard`, `Screen.Calendar`, `Screen.Tasks`, `Screen.PdfTools`, `Screen.Brain`, `Screen.Utilities`) as alias/deep-link targets so **Glance Widgets**, **Notification PendingIntents**, and **existing Robolectric tests** never break.
- Replace manual `URLEncoder`/`URLDecoder` parameter passing with opaque ID-based navigation (`pdf_reader/{attachmentId}`, `image_annotator/{attachmentId}`) fetching paths safely from Room rather than passing raw file paths inside route URIs.

---

## 5. Modular Architecture by Subsystem

### 5.1 Advanced Notes Editor (`feature/editor/`)
Decompose the 4,816-LOC `NoteEditorScreen.kt` into cohesive components:
- `NoteEditorScreen.kt` (~300 LOC): Top-level scaffold, lifecycle save hooks, `BackHandler`, sheet host.
- `NoteEditorViewModel.kt`: Manages `NoteEditorUiState`, `Mutex`-serialized debounced autosave, version snapshotting, wiki-link resolution, and undo/redo history.
- `NoteDocumentModel.kt`: Decouples structured note blocks (Markdown body, Checklists, Callouts, Code Blocks, Tables, Attachments, Ink, Voice) from `canvasDataJson` via dedicated Room metadata table (`note_metadata` / `note_blocks`).
- `EditorTopBar.kt`: Compact header with pin, favorite, lock, read/edit mode toggle, undo/redo, and overflow inspector.
- `EditorContentSurface.kt`: Texture background + title + rich text / markdown preview + interactive checklist + inline attachments + handwriting overlay.
- `EditorInspectorSheet.kt`: Tabbed bottom sheet for Backlinks, Version History, Word Stats, Reminder, Paper Texture, and Export (`.md`/`.pdf`/`.txt`).
- `EditorAudioController.kt`: Isolated audio recording/playback state machine (eliminating `MediaRecorder`/`MediaPlayer` lifecycle coupling from Composables).

### 5.2 Customizable Quick Edit Toolbar (`feature/editor/toolbar/`)
Replace the hardcoded `RichFormatBar.kt` with a data-driven, customizable toolbar engine:
- **`EditorToolbarAction` Registry (Enum / Sealed Registry):**
  - Formatting: `BOLD`, `ITALIC`, `UNDERLINE`, `STRIKETHROUGH`, `HIGHLIGHT`, `HEADING`, `BULLET_LIST`, `NUMBERED_LIST`, `CHECKLIST`, `QUOTE`, `CODE_BLOCK`, `TABLE`, `DIVIDER`, `CALLOUT`
  - Insertion & Capture: `WIKI_LINK`, `IMAGE`, `VOICE_RECORD`, `VOICE_DICTATE`, `HANDWRITING`, `COLOR_PALETTE`, `TIMESTAMP`, `TEMPLATE`
  - Editing: `UNDO`, `REDO`, `INDENT`, `OUTDENT`, `FIND_IN_NOTE`
- **`ToolbarConfig` Persistence Model (JSON in `app_settings` key `quick_toolbar_config_v1`):**
  - `pinnedActionIds: List<String>` (ordered primary dock actions, 5–12 items)
  - `hiddenActionIds: Set<String>` (disabled actions)
  - `compactMode: Boolean`, `showWordCountPill: Boolean`, `hapticFeedback: Boolean`
- **`QuickEditToolbar.kt`:** Renders pinned actions in a horizontally scrollable or docked bar with a trailing `+ / Customize` trigger; automatically respects `imePadding()` + `navigationBarsPadding()`.
- **`ToolbarCustomizerSheet.kt`:** Live preview + drag/move-up-down reorder + toggle visibility + "Reset to Default" presets (*Writer*, *Student*, *Developer*, *Minimal*).

### 5.3 Calendar & Tasks (`feature/productivity/`)
Unify `CalendarScreen.kt` (2,762 LOC) and `ProductivityScreen.kt` into one coherent subsystem:
- `ProductivityHubScreen.kt`: Persistent segmented top switcher: **Calendar** | **Tasks** | **Habits** | **Focus** | **Reports**.
- `CalendarTab.kt`: Split into `MonthGridView`, `WeekStripView`, `DayTimelineView`, and `CalendarAgendaList` (<350 LOC each).
- `TasksTab.kt`: Unified task workspace with **List (Today / Upcoming / All)** and **Kanban Board** toggle — eliminating the current duplicate task UI between `CalendarScreen` and `ProductivityScreen`.
- Single source of truth: Tasks (`tasks` table) and Reminders (`reminders` table) remain synchronized via `ProductivityRepository` with `WorkManager` + `AlarmManager` notifications.

### 5.4 Folder & PDF Management (`feature/folders/`, `feature/pdf/`)
- **Folder Tree Engine:** Backed by `FolderHierarchyUtils` + Room self-FK on `folders.parentId`. Decompose `FoldersScreen.kt` into `FolderExplorerView`, `FolderDetailView`, `SmartCollectionsStrip`, and `FolderDialogs`.
- **Unified Folder Contents:** A folder now displays Notes, **PDF Documents** (`attachments.folderId`), and **Studio Boards** (`canvases.folderId`) with type filter chips.
- **PDF Subsystem Decomposition:**
  - `PdfDocumentEngine.kt`: Thread-safe `PdfRenderer` wrapper with LRU bitmap tile cache (`LruCache<String, Bitmap>`) to eliminate OOM spikes on high-DPI PDFs.
  - `PdfReaderScreen.kt` (<450 LOC): Viewport + gesture controller + tool dock.
  - `PdfAnnotationController.kt`: Uses shared `core/ink/` engine for strokes, highlights, text notes, and undo/redo.
  - `PdfToolsService.kt`: Background coroutine workers for Merge, Split, Compress, Image-to-PDF, and Flattened Annotation Export.

### 5.5 Canvas, Mind Map & Sticky Wall (`feature/studio/`)
- **Studio Hub (`StudioHubScreen.kt`):** Unified gallery of Mind Maps, Freehand Whiteboards, Sticky Walls, and Annotated Images with template picker and instant search.
- **Mind Map & Whiteboard (`ui/screens/mindmap/` + `CanvasScreen.kt`):** Already well-modularized; add `boardKind` (`MIND_MAP` vs. `WHITEBOARD`) on `CanvasEntity` so Blank Whiteboards do not auto-seed starter Mind Map nodes while Mind Maps do.
- **Sticky Wall (`StickyBoardScreen.kt`):** Move card coordinates (`posX`, `posY`, `widthDp`, `heightDp`, `rotationDeg`, `wallId`) into Room `sticky_notes` columns so layout is atomic, searchable, and included cleanly in SQLite transactions and backups.
- **Shared Ink & Export Core (`core/ink/`, `core/export/`):** Eliminate duplicated color pickers, stroke smoothing, and `FileProvider` share intents across Canvas, Sticky Wall, Image Annotator, PDF Reader, and Note Editor.

### 5.6 Productivity Tools (`feature/productivity/`)
- **Habits:** Keep `HabitsTab.kt` & `ProductivityEngine.computeHabitStreak`; add quick check-in action from Home and Calendar.
- **Pomodoro Focus:** Keep `PomodoroFocusTab.kt` + `PomodoroCompletionWorker` (WorkManager 2.10.1); add task/note session attribution summary in Reports.
- **Reports:** Upgrade `ProgressReportsTab.kt` with weekly streak heatmap, focus-time breakdown by category, and one-tap "Export Weekly Review to Note".

### 5.7 Android External File Integration (`core/files/`)
Strict Play-Policy compliance using **Storage Access Framework (SAF)** and **Intent Filters** (zero broad storage permissions):
- **`AndroidManifest.xml` Intent Filters on `MainActivity` (or lightweight `FileIntakeActivity` alias):**
  - `ACTION_VIEW` for `application/pdf`, `text/markdown`, `text/plain`, `image/*`
  - `ACTION_SEND` & `ACTION_SEND_MULTIPLE` for `text/plain`, `application/pdf`, `image/*`
  - `ACTION_PROCESS_TEXT` (read-only) for clipping highlighted text from any app straight into a new Noteland note
- **`ExternalFileIntakeService.kt`:**
  - Validates MIME type, file size limits (preventing OOM on malformed streams), and URI read grants.
  - Supports two safe modes:
    1. **Import Copy (Default):** Copies into app-private `AttachmentStorageManager` and creates an `AttachmentEntity` / `NoteEntity` in the user's chosen folder.
    2. **SAF Persistable Link (Optional for PDFs/Docs):** Calls `contentResolver.takePersistableUriPermission(uri, FLAG_GRANT_READ_URI_PERMISSION)` and tracks `externalUri` + `isExternalLinked` in `attachments`, with graceful fallback UI if the user revokes/moves the external file.
- **Export (`ActivityResultContracts.CreateDocument`):** Lets users save exported PDFs, Markdown files, PNG Mind Maps, and encrypted Backup ZIPs to any external folder via SAF without needing storage permissions.

### 5.8 Settings & Customization (`feature/settings/`, `core/preferences/`)
- **`PreferencesRepository`:** Single typed reactive source of truth backed by Room `app_settings` (automatically included in DB transactions & `BackupManager` ZIPs!), with one-time non-destructive migration of existing `noteland_settings_prefs` keys.
- **Organized Settings Sections:**
  1. **Workspace & Profile**
  2. **Appearance & Typography** (Theme, Dynamic Color, Font Scale, Card Density)
  3. **Editor & Quick Toolbar** (Quick Edit Toolbar Customizer, Default Paper Texture, Default Note Mode)
  4. **Security & Privacy Vault** (Explicit PIN Setup/Change, Biometric Toggle, Auto-Lock Timer, Screen Capture Protection toggle)
  5. **Files & External Integrations** (Default Import Folder, PDF Auto-Save, Import vs. SAF Link behavior)
  6. **Backup, Restore & Storage** (Encrypted ZIP Backup/Restore including Settings, Storage Breakdown & Orphan Cache Cleaner)
  7. **Notifications & System** (Reminders, Exact Alarm Status, Battery Optimization Helper)

---

## 6. Development Safety, Backup & Rollback Strategy

### 6.1 Mandatory Pre-Phase Safety Checklist
1. **Immutable Contracts:** Never change `applicationId` (`com.aistudio.noteland.yjaavj`), `namespace` (`com.example`), `debug.keystore`, `signingConfigs`, or existing Room column names without a tested migration.
2. **Database Backup Before Migration:** In `BackupManager`, support automatic pre-upgrade safety snapshots (written to `filesDir/backups/auto_pre_v{N}.zip`) prior to destructive maintenance or user-initiated restores.
3. **Feature Flags in `PreferencesRepository`:** Gate major UI rollouts (e.g., 5-tab navigation shell, customizable toolbar, FTS search) behind clean internal toggles during incremental phase commits so partial phases never break main navigation.
4. **Rollback Procedure:**
   - Because all schema changes are **strictly additive** (new nullable/defaulted columns and new tables), rolling back application UI code never corrupts existing user rows.
   - `AppDatabase` NEVER uses `.fallbackToDestructiveMigration()`.
5. **Continuous Verification Gate per Phase:**
   - `compile_applet` must succeed cleanly with zero new warnings.
   - `gradle :app:testDebugUnitTest` must pass 100% (baseline 162 tests + newly added phase tests).
   - Every Room migration (`5->6`, `6->7`) must ship with a dedicated Robolectric migration test verifying zero data loss across notes, folders, attachments, mind maps, stickies, tasks, habits, and settings.
