# NoteLand 2.0 — Phase 1: Knowledge Management (NoteLand Brain)

**Document Identifier:** NL-DOC-019  
**Status:** Completed & Verified  
**Target Codebase:** NoteLand (Kotlin + Jetpack Compose + Material 3 + Room + Coroutines, `com.example` / `com.aistudio.noteland.gzugkw`)  
**Verification Date:** November 17, 2026  
**Debug APK Build:** Passing (`compile_applet`)  
**Phase 1 Automated Test Suite:** 11/11 Passing (`com.example.Phase1KnowledgeBrainTest`, 0 failed, 0 errors, 0 skipped)  
**Full Repository Test Suite:** 157/157 Passing across 12 test suites (`gradle :app:testDebugUnitTest`)

---

## 1. Executive Summary

Phase 1 implemented the **NoteLand Brain** offline-first personal knowledge management system inside the existing NoteLand architecture without altering the application ID, package namespace, or pre-existing note/folder/tag IDs. The implementation introduced normalized Room entities (`MIGRATION_3_4`), a dedicated parsing/linking/graph/search engine (`KnowledgeBrainEngine`), a responsive Compose hub (`BrainScreen`), inline editor intelligence (`NoteEditorScreen`), universal search across notes/tags/links/folders (`SearchScreen`), encrypted v4 backup round-tripping, and strict privacy isolation preventing locked, hidden, archived, or trashed notes from leaking into links, graph nodes, version history, templates, or search.

---

## 2. Database Schema & Non-Destructive `MIGRATION_3_4`

Upgraded `AppDatabase` from **Version 3** to **Version 4** (later extended cleanly to Version 5 in Phase 2) via non-destructive `MIGRATION_3_4`, adding **3 normalized tables**, foreign keys, and query indices while preserving all existing v3 notes, folders, tags, attachments, and settings:

### 2.1 `note_links` (`NoteLinkEntity`)
- **Primary Key:** `id` (`TEXT`)
- **Columns:** `sourceNoteId`, `targetNoteId` (nullable for unresolved/pending notes), `targetTitle`, `normalizedTargetTitle`, `anchorHeading`, `displayAlias`, `contextSnippet`, `createdAtUtc`, `updatedAtUtc`
- **Foreign Keys:**
  - `sourceNoteId -> notes(id)` (`ON DELETE CASCADE`)
  - `targetNoteId -> notes(id)` (`ON DELETE SET NULL`)
- **Indices:**
  - `index_note_links_sourceNoteId`
  - `index_note_links_targetNoteId`
  - `index_note_links_normalizedTargetTitle`
  - Unique `index_note_links_sourceNoteId_normalizedTargetTitle` (`sourceNoteId`, `normalizedTargetTitle`)

### 2.2 `note_templates` (`NoteTemplateEntity`)
- **Primary Key:** `id` (`TEXT`)
- **Columns:** `name`, `description`, `category`, `titlePattern`, `contentTemplate`, `defaultTagsCsv`, `defaultColorHex`, `checklistTemplateJson`, `isBuiltIn`, `usageCount`, `createdAtUtc`, `updatedAtUtc`
- **Indices:**
  - Unique `index_note_templates_name`
  - `index_note_templates_category_updatedAtUtc`
  - `index_note_templates_isBuiltIn`

### 2.3 `note_versions` (`NoteVersionEntity`)
- **Primary Key:** `id` (`TEXT`)
- **Columns:** `noteId`, `versionNumber`, `title`, `content`, `tagsCsv`, `checklistJson`, `changeSummary`, `wordCount`, `createdAtUtc`
- **Foreign Key:** `noteId -> notes(id)` (`ON DELETE CASCADE`)
- **Indices:**
  - `index_note_versions_noteId_createdAtUtc`
  - `index_note_versions_noteId_versionNumber`

### 2.4 Upgrade Seeding (`seedInitialDataDirect`)
- Idempotently seeds 5 built-in knowledge templates on both fresh installs and v3 -> v4 upgrades without duplicating rows or overwriting user-created templates.

---

## 3. Core Knowledge Brain Capabilities Implemented

### 3.1 Wiki Links (`[[Note Name]]`) & Automatic Resolution
- `KnowledgeBrainEngine.extractWikiLinkSpans` parses `[[Note Name]]`, `[[Note Name|Alias]]`, `[[Note Name#Section]]`, and `[[Note Name#Section|Alias]]` spans across markdown note bodies and checklist items while ignoring fenced code blocks (` ```...``` `).
- `resolveTargetNote` matches titles using whitespace-collapsed, case-insensitive normalization (`normalizeTitleKey`) with an unambiguous alphanumeric fallback (`plainAlphaNumKey`), excluding self-links and non-eligible notes.
- Saving, renaming, un-deleting, or un-locking a note synchronizes outgoing `NoteLinkEntity` rows and repairs dangling incoming `targetNoteId` references across eligible notes (`NoteRepository`).

### 3.2 Backlinks Panel & Related Notes Engine
- `NoteEditorScreen` View Mode renders interactive **Backlinks** (`backlink_item_<sourceId>`) displaying source note titles, dates, and captured line-level `contextSnippet` previews.
- `KnowledgeBrainEngine.findRelatedNotes` scores candidate unlocked/visible notes using:
  - Direct wiki link connections (incoming/outgoing)
  - 2-hop graph neighbor paths
  - Shared normalized tags
  - Shared folder membership
- Edit Mode provides live `[[...` wiki-link autocomplete suggestions (`wiki_link_autocomplete_bar`, `wiki_suggestion_<id>`) for rapid cross-note linking and click-to-open or create-on-tap (`openOrCreateWikiLink`).

### 3.3 Interactive Knowledge Graph & Responsive List View (`BrainScreen`)
- **Brain Section (`BrainScreen.kt`):** Dedicated 4-tab hub (**Graph**, **List**, **Journal**, **Templates**) accessible from Home Workspace & navigation routes, supporting adaptive layouts (`WindowWidthSizeClass` / dual-pane inspector alongside graph canvas on wide displays and bottom inspector sheet on compact displays).
- **Graph Canvas (`knowledge_graph_canvas`):** Deterministic circular + force-directed spring relaxation layout computed in `KnowledgeBrainEngine`, supporting smooth drag pan, pinch-to-zoom, explicit zoom in/out/reset controls (`graph_zoom_in`, `graph_reset_view`), orphan-node filtering (`graph_toggle_orphans`), node selection highlighting adjacent edges, quick open navigation (`brain_open_selected_note`), and helpful empty states.
- **List View (`brain_tab_list`):** Sortable/filterable directory (`brain_list_item_<id>`) displaying incoming/outgoing degree counts, tags, and unresolved target indicators.

### 3.4 Daily Journal (Date-Based Deterministic Entries)
- `KnowledgeBrainEngine.journalDateKeyFromMillis`, `millisFromJournalDateKey`, `isDailyJournalNote`, and `extractJournalDateKey` pair `canvasDataJson` metadata (`SmartNoteUtils.withJournalDateKey`) with `"Diary - MMM d, yyyy"` / `"Diary"` conventions.
- `NoteViewModel.openOrCreateJournalForDate(dateKey)` guarantees deterministic date-based lookup and reuses existing journal IDs instead of duplicating entries for the same date, expanding the **Daily Reflection** built-in template when creating a new day's entry.

### 3.5 Reusable Note Templates & Placeholder Expansion
- Seeds 5 protected built-in templates (`isBuiltIn = true` cannot be deleted):
  1. `template_builtin_daily_journal` — **Daily Reflection**
  2. `template_builtin_zettelkasten` — **Zettelkasten Concept**
  3. `template_builtin_meeting_notes` — **Meeting Brief**
  4. `template_builtin_book_notes` — **Book & Reading Notes**
  5. `template_builtin_project_plan` — **Project Blueprint**
- Supports dynamic placeholder expansion (`{{date}}`, `{{iso_date}}`, `{{day}}`, `{{time}}`, `{{title}}`), custom template creation/deletion, "Save Note as Template" (explicitly blocked on locked notes), and atomic `usageCount` tracking.

### 3.6 Note Version History & Non-Destructive Restoration
- Every meaningful content, title, tag, or checklist change automatically snapshots a `NoteVersionEntity` with incrementing `versionNumber`, `wordCount`, and `changeSummary`, deduplicating identical saves and pruning older snapshots beyond **25 revisions per note**.
- Users can browse revision diffs/previews inside `NoteEditorScreen` (`more_menu_version_history`) and restore any retained revision (`restore_version_<id>`), preserving the note's original `id` and immutable `createdAtUtc` while appending a `"Restored from vN"` version entry.

### 3.7 Universal Search (`SearchScreen` + `KnowledgeBrainEngine`)
- Multi-signal weighted search across note titles (+10), tags/`#tag` tokens (+7), outgoing/incoming wiki link titles (+6 each), folder names (+5), note body content (+5), and checklist items (+4), plus dedicated filter chips (`ALL`, `NOTES`, `TAGS`, `LINKS`, etc.) and reason badges (e.g., `Links to [[...]]`, `Linked from [[...]]`).

---

## 4. Security & Visibility Enforcement

A central eligibility gate (`KnowledgeBrainEngine.isNoteKnowledgeEligible`) enforces `!isDeleted && !isLocked && !isHidden && !isArchived` consistently across all Brain subsystems:
1. **Wiki Links & Graph:** Locked, hidden, archived, and deleted notes never generate outgoing `note_links` rows, never resolve as incoming link targets, and never render as nodes/labels/previews in the Knowledge Graph.
2. **Version History:** Plaintext `note_versions` snapshots are skipped for locked notes; locking a note clears/blocks plaintext revision exposure and "Save as Template" rejects locked notes with `SecurityException`.
3. **Universal Search:** Locked, hidden, archived, and deleted notes (plus their private tags and checklists) are excluded from `universalSearchNotes` and `universalSearchTags`.
4. **Backup v4 Round-Trip:** `BackupManager` serializes `note_links`, `note_templates`, and `note_versions` safely inside encrypted ZIP/JSON backups without exposing locked note plaintext.

---

## 5. Files Created & Modified in Phase 1

### Created Production Files
- `app/src/main/java/com/example/data/model/NoteLinkEntity.kt`
- `app/src/main/java/com/example/data/model/NoteTemplateEntity.kt`
- `app/src/main/java/com/example/data/model/NoteVersionEntity.kt`
- `app/src/main/java/com/example/data/db/NoteLinkDao.kt`
- `app/src/main/java/com/example/data/db/NoteTemplateDao.kt`
- `app/src/main/java/com/example/data/db/NoteVersionDao.kt`
- `app/src/main/java/com/example/util/KnowledgeBrainEngine.kt`
- `app/src/main/java/com/example/ui/screens/BrainScreen.kt`

### Modified Production Files
- `app/src/main/java/com/example/data/db/AppDatabase.kt` (`MIGRATION_3_4`, v4 entities/DAOs, template seeding)
- `app/src/main/java/com/example/data/repository/NoteRepository.kt` (link sync, version capture/pruning/restore, template CRUD)
- `app/src/main/java/com/example/ui/viewmodel/NoteViewModel.kt` (Brain StateFlows, graph/journal/template/version/link actions)
- `app/src/main/java/com/example/ui/screens/NoteEditorScreen.kt` (Backlinks, Related Notes, `[[` autocomplete, Version History dialog, Save as Template)
- `app/src/main/java/com/example/ui/screens/SearchScreen.kt` (Universal search filters, tag counts, link match badges)
- `app/src/main/java/com/example/ui/screens/HomeScreen.kt` & navigation wiring in `MainActivity.kt`
- `app/src/main/java/com/example/util/BackupManager.kt` & `app/src/main/java/com/example/util/SmartNoteUtils.kt`

### Created Test & Documentation Files
- `app/src/test/java/com/example/Phase1KnowledgeBrainTest.kt`
- `docs/19_PHASE_1_KNOWLEDGE_BRAIN.md`

---

## 6. Verification Results (Actual Executed Outputs)

### 6.1 `Phase1KnowledgeBrainTest` (11/11 Passing)
Verified inside full `gradle :app:testDebugUnitTest --console=plain`:
1. `wiki link parser handles standard, aliased, anchored links and ignores fenced code` — **PASSED**
2. `automatic link detection resolves forward references, backlinks, and related notes` — **PASSED**
3. `safe MIGRATION_3_4 preserves existing v3 data, builds indexed Brain tables, and seeds templates` — **PASSED**
4. `daily journal opens or creates deterministic date-based entries without duplicates` — **PASSED**
5. `templates expand placeholders, create notes, increment usage, and protect built-ins` — **PASSED**
6. `version history deduplicates unchanged saves, prunes at 25, and restores older revision cleanly` — **PASSED**
7. `locked, hidden, archived, and deleted notes never leak into links, graph, versions, search, or templates` — **PASSED**
8. `backup v4 preserves wiki links, custom templates, and version history without leaking locked notes` — **PASSED**
9. `BrainScreen renders Graph, List, Journal, Templates tabs and interactive zoom & inspector` — **PASSED**
10. `SearchScreen universal search supports Tags and Links filters while hiding locked notes` — **PASSED**
11. `NoteEditorScreen displays Backlinks, Related Notes, Wiki Autocomplete, and Version History restore` — **PASSED**

### 6.2 Debug APK Compilation
- `compile_applet`: `Build succeeded - the applet is compiled`

---

## 7. Known Limitations

1. **JVM / Robolectric Verification Environment:** Tests run under Robolectric (SDK 36) on the JVM rather than physical device instrumentation (`adb` is unavailable in the build environment).
2. **Graph Scalability Ceiling:** Force-directed relaxation runs synchronously inside derived state on eligible note sets; very large vaults (thousands of densely interlinked notes) may benefit from progressive layout throttling in future optimization passes.
3. **Locked Note Content Exclusion:** By design, encrypted/locked notes are excluded from Brain indexing, graph rendering, search, and plaintext revision history until unlocked or converted to standard notes.
