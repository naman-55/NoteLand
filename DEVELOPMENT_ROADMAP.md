# Noteland 3.5 — 10-Phase Development Roadmap

**Document Status:** Phase 1 Complete (Audit & Architecture Baseline)  
**Baseline Test Suite:** `162 / 162 PASSED` (`gradle :app:testDebugUnitTest`, 14 test suites, 0 failures)  
**Guiding Principles:** Incremental evolution, zero regressions, strictly backward-compatible DB migrations, full preservation of existing user data, routes, and Glance widgets.

---

## Roadmap Overview

| Phase | Title | Primary Goal | DB Version | Risk Level | Status |
|---|---|---|---|---|---|
| **Phase 1** | Technical Audit & Architecture Blueprint | Complete codebase/DB audit, feature mapping, architecture & migration specs, baseline tests | `v5` | None (Read-Only) | **COMPLETE** |
| **Phase 2** | Core Foundation, Schema v6 & Tech Debt Cleanup | Enable Room schema export, ship `MIGRATION_5_6`, introduce `AppContainer` & `PreferencesRepository`, fix minor hygiene defects | `v5 -> v6` | Low–Medium | **READY NEXT** |
| **Phase 3** | 5-Pillar Navigation Shell & Studio Hub | Reorganize bottom navigation to `Home`, `Folders`, `Studio`, `Productivity`, `Settings`; build `StudioHubScreen` while preserving legacy routes | `v6` | Low | Planned |
| **Phase 4** | Advanced Notes Editor Decomposition | Break up 4,816-LOC `NoteEditorScreen.kt` into modular components, wire `note_metadata` dual-read adapter, add `.md`/`.pdf` export | `v6` | Medium–High | Planned |
| **Phase 5** | Customizable Quick Edit Toolbar | Build `EditorToolbarAction` registry, `QuickEditToolbar`, `ToolbarCustomizerSheet`, drag-to-reorder, presets & persistence | `v6` | Low | Planned |
| **Phase 6** | Unified Folders, PDF-in-Folders & FTS Search | Ship `MIGRATION_6_7` (`notes_fts`), decompose `FoldersScreen` & `PdfReaderScreen`, add LRU PDF tile cache & batch folder tools | `v6 -> v7` | Medium | Planned |
| **Phase 7** | Studio Unification (Canvas, Mind Map & Sticky Wall) | Unify `core/ink` & `core/export`, migrate Sticky Wall spatial geometry to Room, add Mind Map ↔ Note outline conversion & folder assignment | `v7` | Medium | Planned |
| **Phase 8** | Unified Productivity & Calendar Workspace | Merge `CalendarScreen` (2,762 LOC) & `ProductivityScreen` into one cohesive 5-tab workspace (`Today`, `Calendar`, `Tasks`, `Habits`, `Focus`) | `v7` | Medium | Planned |
| **Phase 9** | Android External File Integration (SAF & Intents) | Add `ACTION_VIEW`, `ACTION_SEND`, `ACTION_SEND_MULTIPLE`, `ACTION_PROCESS_TEXT` intake flows, persistable SAF links & `CreateDocument` export | `v7` | Medium | Planned |
| **Phase 10** | Settings Overhaul, Backup v7 & Release Hardening | Redesign unified `SettingsScreen`, upgrade `BackupManager` to v7 (with preferences/toolbar), add Storage Inspector & full regression pass | `v7` | Low–Medium | Planned |

---

## Detailed Phase Breakdown

### Phase 1: Technical Audit & Architecture Blueprint *(COMPLETE)*
- **Deliverables Completed:**
  - `CODEBASE_AUDIT.md` — Complete inventory of 61,118 LOC Kotlin codebase, 14 test suites (162/162 passing), critical monoliths, and Android gaps.
  - `FEATURE_MAPPING.md` — Exhaustive mapping of all existing and target features into `Home`, `Folders`, `Studio`, `Productivity`, and `Settings`.
  - `ARCHITECTURE_PLAN.md` — Target modular package hierarchy, `AppContainer` DI graph, `NoteViewModel` decomposition strategy, and subsystem blueprints.
  - `DATABASE_MIGRATION_PLAN.md` — Complete v5 schema audit (22 tables, 14 DAOs) and strictly additive `5 -> 6` and `6 -> 7` migration specs.
  - `DEVELOPMENT_ROADMAP.md` — This 10-phase execution strategy.

---

### Phase 2: Core Foundation, Schema v6 & Tech Debt Cleanup *(NEXT)*
- **Objective:** Establish rock-solid architectural and persistence foundations without changing visible UX workflows.
- **Key Deliverables:**
  1. **Room Schema Export + `MIGRATION_5_6`:**
     - Add `room.schemaLocation` KSP arg in `app/build.gradle.kts`, set `exportSchema = true`, commit baseline `5.json` and new `6.json`.
     - Implement idempotent `MIGRATION_5_6` (`note_metadata` table, `sticky_notes` spatial columns, `canvases.boardKind`/`folderId`, `attachments.folderId`/SAF columns, missing indices).
     - Add `DatabaseSafetyManager` pre-migration WAL backup snapshot.
  2. **Manual DI & Unified Preferences:**
     - Create `app/src/main/java/com/example/core/di/AppContainer.kt` providing `AppDatabase`, repositories, `SecurityManager`, `BackupManager`, and `PreferencesRepository`.
     - Create `PreferencesRepository` unifying `noteland_settings_prefs` and `app_settings` behind reactive `StateFlow`s.
  3. **Targeted Hygiene Fixes:**
     - Remove `!!` unsafe assertion in `CanvasScreen.kt:640`.
     - Replace deprecated `Icons.Filled.TrendingUp` (`ProgressReportsTab.kt:397`) and `Icons.Filled.Label` (`MindMapBottomPanels.kt:516`) with `Icons.AutoMirrored` equivalents.
     - Replace silent hardcoded Vault default PIN `"1234"` in `SecurityManager.kt` with an explicit first-time Vault PIN creation check (preserving existing user PIN hashes).
- **Files Modified / Created:**
  - *Modified:* `app/build.gradle.kts`, `AppDatabase.kt`, `StickyNoteEntity.kt`, `CanvasEntity.kt`, `AttachmentEntity.kt`, `FolderEntity.kt`, `CanvasScreen.kt`, `ProgressReportsTab.kt`, `MindMapBottomPanels.kt`, `SecurityManager.kt`, `NoteLandApplication.kt` (or `MainActivity.kt` initialization).
  - *Created:* `NoteMetadataEntity.kt`, `NoteMetadataDao.kt`, `core/di/AppContainer.kt`, `core/preferences/PreferencesRepository.kt`, `core/db/DatabaseSafetyManager.kt`, `DatabaseMigrationTest.kt`.
- **Verification Gate:** `compile_applet` succeeds + all 162 existing tests + new `DatabaseMigrationTest` (`5 -> 6`) pass.
- **Rollback Strategy:** Purely additive schema & wrapper classes; revert commit cleanly with zero user data impact.

---

### Phase 3: 5-Pillar Navigation Shell & Studio Hub
- **Objective:** Transition app-level navigation to the 5 Noteland 3.5 pillars (`Home`, `Folders`, `Studio`, `Productivity`, `Settings`) while keeping every deep-link route and widget action intact.
- **Key Deliverables:**
  1. Update `FloatingGlassBottomBar.kt` (and add adaptive `NavigationRail` for `Medium`/`Expanded` window sizes) with the 5 top-level pillars.
  2. Create `StudioHubScreen.kt` unifying **Mind Maps**, **Whiteboard Canvases**, **Sticky Wall**, and **Templates / Quick Sketch** in one visual gallery with search, filter by `boardKind`, and creation FAB.
  3. Wire `Settings` pillar entry point (initially hosting existing `BackupSettingsScreen` + grouped settings navigation list, expanded in Phase 10).
  4. Consolidate `Calendar` and `Productivity` access under the `Productivity` tab while retaining `NavDestination.Calendar` route compatibility for widget/notification deep links.
  5. Enhance `HomeScreen.kt` with quick resume row (recent note, recent mind map, recent PDF) and cleaner quick-capture actions.
- **Files Modified / Created:**
  - *Modified:* `NavDestination.kt`, `FloatingGlassBottomBar.kt`, `MainActivity.kt`, `HomeScreen.kt`.
  - *Created:* `ui/screens/StudioHubScreen.kt`, `ui/viewmodel/StudioViewModel.kt`, `ui/components/AdaptiveNavScaffold.kt`.
- **Verification Gate:** All 162+ tests pass + new Robolectric navigation tests verifying all 5 bottom tabs, `StudioHubScreen` transitions, and legacy deep-link routes.

---

### Phase 4: Advanced Notes Editor Decomposition
- **Objective:** Break down the 4,816-LOC `NoteEditorScreen.kt` monolith into clean, maintainable modules and decouple `notes.canvasDataJson`.
- **Key Deliverables:**
  1. Extract `EditorViewModel.kt` + `EditorUiState` from `NoteViewModel.kt` (keeping delegating wrappers in `NoteViewModel` until Phase 8 completes).
  2. Split `NoteEditorScreen.kt` into focused modules under `ui/editor/`:
     - `EditorTopBar.kt` (title, undo/redo, lock/pin, version history, export menu)
     - `EditorBodyCanvas.kt` (rich text / checklist / code / callout / divider / table blocks + inline ink overlay)
     - `EditorAudioSection.kt` (voice memo recorder & playback cards)
     - `EditorBacklinksPanel.kt` (`[[wiki-link]]` autocomplete & backlinks drawer)
     - `EditorBackgroundSheet.kt` (paper templates, colors, custom backgrounds)
     - `EditorExportService.kt` (Markdown `.md`, plain text `.txt`, and printable Android `PdfDocument` `.pdf` export)
  3. Wire `NoteMetadataAdapter` for dual-read / write to `note_metadata` (preserving legacy `notes.canvasDataJson` read compatibility).
- **Files Modified / Created:**
  - *Modified:* `NoteEditorScreen.kt` (reduced from 4,816 LOC to <500 LOC coordinator), `NoteViewModel.kt`, `SmartNoteUtils.kt`.
  - *Created:* `ui/editor/*` modules, `data/adapter/NoteMetadataAdapter.kt`, `EditorDecompositionTest.kt`.
- **Verification Gate:** `EditorAndThemeStabilizationTest` (15 tests), `Phase1KnowledgeBrainTest` (11 tests), plus new unit tests for `NoteMetadataAdapter` and `.md`/`.pdf` export.

---

### Phase 5: Customizable Quick Edit Toolbar
- **Objective:** Replace the static `RichFormatBar.kt` with a user-customizable, reorderable Quick Edit Toolbar integrated with Settings and Editor presets.
- **Key Deliverables:**
  1. Define typed `EditorToolbarAction` registry (Bold, Italic, Underline, Strikethrough, Highlight, Heading 1–3, Bullet List, Numbered List, Checklist, Quote, Code Block, Callout, Divider, Link, Image, Audio, Drawing, Undo, Redo, Indent/Outdent, Word Count).
  2. Build `QuickEditToolbar.kt` (IME-aware floating/docked bar, 48dp accessible touch targets, overflow menu for secondary actions).
  3. Build `ToolbarCustomizerSheet.kt` allowing users to toggle visible tools, drag-to-reorder priority slots, switch presets (`Writing`, `Study`, `Minimal`, `Markdown`), or reset to defaults.
  4. Persist active layout via `PreferencesRepository` (and sync to `editor_toolbar_presets` in Phase 6).
- **Files Modified / Created:**
  - *Modified:* `RichFormatBar.kt` (delegates or replaced cleanly), `NoteEditorScreen.kt`, `PreferencesRepository.kt`.
  - *Created:* `ui/editor/toolbar/EditorToolbarAction.kt`, `QuickEditToolbar.kt`, `ToolbarCustomizerSheet.kt`, `QuickToolbarTest.kt`.
- **Verification Gate:** Robolectric UI tests verifying action execution, reordering persistence across config changes, and preset switching.

---

### Phase 6: Unified Folders, PDF-in-Folders & FTS Search
- **Objective:** Upgrade file/folder organization, enable folder-owned PDFs, optimize PDF rendering memory, and introduce SQLite FTS4 instant search.
- **Key Deliverables:**
  1. **Room `MIGRATION_6_7`:** Add `notes_fts` (`@Fts4(contentEntity = NoteEntity::class)`) and `editor_toolbar_presets` table; commit `7.json` and migration test (`5 -> 6 -> 7`).
  2. **Decompose `FoldersScreen.kt` (2,144 LOC):** Split into `FoldersViewModel`, breadcrumb bar, nested folder tree/grid, multi-select batch toolbar (move, tag, archive, delete), and unified mixed-item view (Notes + PDFs + Canvases inside a folder).
  3. **Decompose `PdfReaderScreen.kt` (2,110 LOC) & `PdfReaderComponents.kt` (2,148 LOC):** Extract `PdfRenderCache` (bounded `LruCache<String, Bitmap>` with explicit memory cap and lifecycle recycling) and split annotation toolbar, outline/bookmarks drawer, and page viewport.
  4. **Search Upgrade:** Route `SearchScreen` and `HomeScreen` search through `NoteFtsDao` with prefix matching and vault/trash privacy filtering.
- **Files Modified / Created:**
  - *Modified:* `AppDatabase.kt`, `NoteDao.kt`, `AttachmentDao.kt`, `FoldersScreen.kt`, `PdfReaderScreen.kt`, `PdfReaderComponents.kt`, `SearchScreen.kt`.
  - *Created:* `NoteFtsEntity.kt`, `EditorToolbarPresetEntity.kt`, `ui/folders/*`, `ui/pdf/PdfRenderCache.kt`, `FtsSearchAndMigrationTest.kt`.
- **Verification Gate:** `PdfAnnotationEditingTest` (9), `PdfTextHighlightEngineTest` (4), `DatabaseMigrationTest` (`6 -> 7`), and FTS search latency/privacy tests pass.

---

### Phase 7: Studio Unification (Canvas, Mind Map & Sticky Wall)
- **Objective:** Consolidate duplicated rendering/export engines across Canvas, Mind Map, and Sticky Wall, and complete Sticky Wall spatial migration to Room.
- **Key Deliverables:**
  1. **Shared Engines:** Extract `core/ink/` (stroke smoothing, eraser hit-testing, color/width presets) and `core/export/` (high-DPI PNG & PDF bitmap/vector export) used by Canvas, Mind Map, and PDF annotations.
  2. **Mind Map ↔ Note Integration:** Add one-tap "Export Mind Map to Structured Note Outline" and "Generate Mind Map from Note Headings". Allow assigning any canvas/mind map to a Folder (`canvases.folderId`).
  3. **Sticky Wall Room Cutover:** Perform one-time transactional backfill of `sticky_board_layouts` (`SharedPreferences`) into `sticky_notes` (`wallId, posX, posY, widthDp, heightDp, rotationDeg`), enabling multiple named Sticky Walls and reliable undo/backup.
  4. **Decompose `CanvasScreen.kt` (1,514 LOC) & `StickyBoardScreen.kt` (1,479 LOC):** Extract `CanvasViewModel` and `StickyWallViewModel` from `NoteViewModel`.
- **Files Modified / Created:**
  - *Modified:* `CanvasScreen.kt`, `MindMapScreen.kt`, `StickyBoardScreen.kt`, `StudioHubScreen.kt`, `NoteViewModel.kt`.
  - *Created:* `core/ink/*`, `core/export/*`, `ui/studio/MindMapOutlineConverter.kt`, `ui/viewmodel/StickyWallViewModel.kt`, `StickyMigrationTest.kt`.
- **Verification Gate:** `MindMapStudioTest` (3), `StickyBoardResizeTest` (3), and new outline conversion + Room layout backfill tests pass.

---

### Phase 8: Unified Productivity & Calendar Workspace
- **Objective:** Unify `CalendarScreen` (2,762 LOC) and `ProductivityScreen` into a single cohesive 5-tab workspace under the `Productivity` pillar and complete `NoteViewModel` decomposition.
- **Key Deliverables:**
  1. Decompose `CalendarScreen.kt` into reusable Month Grid, Week Timeline, Day Agenda, and Event/Journal Sheet components backed by indexed `note_metadata.journalDateKey` and `calendarStartUtc`.
  2. Merge into unified `ProductivityWorkspaceScreen` with 5 switchable sub-views: **Today**, **Calendar**, **Tasks (List + Kanban)**, **Habits**, and **Focus (Pomodoro + Analytics)**.
  3. Extract `ProductivityViewModel.kt` and `CalendarViewModel.kt`, retiring legacy bloated methods from `NoteViewModel.kt` (target `<600` LOC).
  4. Tighten bi-directional sync between Tasks, Reminders (`WorkManager`), Daily Journal Notes, and Focus Sessions.
- **Files Modified / Created:**
  - *Modified:* `CalendarScreen.kt`, `ProductivityScreen.kt`, `NoteViewModel.kt`, `MainActivity.kt`.
  - *Created:* `ui/productivity/ProductivityWorkspaceScreen.kt`, `ui/calendar/*`, `ui/viewmodel/ProductivityViewModel.kt`, `ui/viewmodel/CalendarViewModel.kt`.
- **Verification Gate:** `Phase2ProductivityTest` (11), `WorkManagerAndWidgetRegressionTest` (2), plus unified calendar/task sync tests pass.

---

### Phase 9: Android External File Integration (SAF & Intents)
- **Objective:** Make Noteland a first-class Android citizen capable of opening, importing, linking, and exporting external files via Storage Access Framework (SAF) and system intents while complying strictly with Google Play policies.
- **Key Deliverables:**
  1. **Manifest Intent Filters (`AndroidManifest.xml`):**
     - `ACTION_VIEW` for `application/pdf`, `text/markdown`, `text/plain`.
     - `ACTION_SEND` & `ACTION_SEND_MULTIPLE` for shared text, URLs, images, and PDFs (routing to a lightweight Quick Capture / Import bottom sheet).
     - `ACTION_PROCESS_TEXT` ("Save to Noteland" from Android text selection menu).
  2. **SAF Persistable File Linking (`ExternalFileManager.kt`):**
     - Use `ActivityResultContracts.OpenDocument` + `takePersistableUriPermission` to either **Import Copy** (into `filesDir`) or **Link External File** (`attachments.externalUri`, `isExternalLinked = true`) with graceful broken-link recovery UI.
     - Use zero-permission `ActivityResultContracts.PickVisualMedia` for images (full Google Play Photo Picker compliance—no broad storage permissions).
     - Use `ActivityResultContracts.CreateDocument` for exporting Notes (`.md`, `.pdf`), Canvases (`.png`, `.pdf`), and Full Backups (`.zip`).
- **Files Modified / Created:**
  - *Modified:* `AndroidManifest.xml`, `MainActivity.kt`, `FoldersScreen.kt`, `PdfReaderScreen.kt`.
  - *Created:* `core/files/ExternalFileManager.kt`, `core/files/IntentIntakeRouter.kt`, `ui/components/ImportShareSheet.kt`, `ExternalIntentIntegrationTest.kt`.
- **Verification Gate:** Robolectric intent resolution tests (`ACTION_VIEW`, `ACTION_SEND`, `ACTION_PROCESS_TEXT`), SAF broken-link fallback tests, and Play Policy permission verification pass.

---

### Phase 10: Settings Overhaul, Backup v7 & Release Hardening
- **Objective:** Deliver a comprehensive, polished Settings hub, upgrade Backup/Restore to v7, add Storage Management, and execute full regression hardening.
- **Key Deliverables:**
  1. **Unified `SettingsScreen.kt`:** Organized into 6 clear sections:
     - *Appearance & Theme* (Light/Dark/System, Dynamic Color, accent presets, font scale)
     - *Editor & Quick Toolbar* (default note type, auto-save, Quick Edit Toolbar customizer shortcut)
     - *Privacy & Vault Security* (explicit PIN setup/change, biometric unlock, auto-lock timer, hidden notes toggle)
     - *Backup, Restore & Export* (Manual ZIP export/import, pre-restore preview, legacy v1–v5 compatibility)
     - *Storage & Diagnostics* (Storage Inspector breaking down DB size, PDFs, audio, images, cached thumbnails, orphaned file cleanup)
     - *About & Shortcuts* (app version, schema version, gesture/markdown cheat sheet)
  2. **BackupManager v7:** Include `note_metadata`, `app_settings`, `editor_toolbar_presets`, and non-sensitive `PreferencesRepository` snapshot while preserving verified v1–v5 import compatibility.
  3. **Hardening & Profiling:** Verify ProGuard/R8 rules (`proguard-rules.pro`), audit Compose recomposition hotspots, run full `gradle :app:testDebugUnitTest` and `lint_applet` final quality gate.
- **Files Modified / Created:**
  - *Modified:* `BackupSettingsScreen.kt` (evolved into modular `ui/settings/*`), `BackupManager.kt`, `proguard-rules.pro`, `res/values/strings.xml`.
  - *Created:* `ui/settings/SettingsScreen.kt`, `ui/settings/StorageInspectorScreen.kt`, `BackupV7CompatibilityTest.kt`.
- **Verification Gate:** 100% pass across all unit & Robolectric suites, zero lint errors, verified clean release build (`compile_applet`).

---

## Phase 2 Implementation Readiness Checklist

| Readiness Criterion | Status | Evidence / Notes |
|---|---|---|
| Full codebase & dependency audit complete | **PASS** | `CODEBASE_AUDIT.md` (61,118 LOC mapped, Gradle & Manifest inspected) |
| All existing features mapped to 5-pillar model | **PASS** | `FEATURE_MAPPING.md` |
| Modular target architecture & DI plan approved | **PASS** | `ARCHITECTURE_PLAN.md` |
| Backward-compatible Room migration spec ready | **PASS** | `DATABASE_MIGRATION_PLAN.md` (`5 -> 6` strictly additive, zero data loss) |
| Baseline unit & Robolectric tests passing | **PASS** | **162 / 162 PASSED** across 14 test suites (`gradle :app:testDebugUnitTest`) |
| Blocking dependencies or missing SDKs | **NONE** | All required libraries (Room, KSP, WorkManager, Glance, Coroutines, Serialization, Coil, Material3) are already configured |
| Required first action in Phase 2 | **READY** | Enable `room.schemaLocation` + `exportSchema = true`, freeze `5.json`, then implement `MIGRATION_5_6` + `AppContainer` |
