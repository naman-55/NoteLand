# Noteland 3.5 — Database Audit & Backward-Compatible Migration Plan

**Document Status:** Phase 1 Technical Audit Deliverable  
**Database Engine:** AndroidX Room (SQLite) — `noteland_database`  
**Current Schema Version:** `5` (`exportSchema = false`, `PRAGMA foreign_keys = ON`)  
**Target Noteland 3.5 Schema Versions:** `6` (Phase 2 Foundation) and `7` (Phase 6 Search & Structured Blocks)  
**Golden Rule:** Zero data loss. All schema upgrades are strictly additive and backward-compatible. Never enable `fallbackToDestructiveMigration()`.

---

## 1. Current Database Inventory (Schema Version 5)

The database is defined in `app/src/main/java/com/example/data/db/AppDatabase.kt` with **22 `@Entity` tables**, **14 DAOs**, and **~151 SQL queries**.

### 1.1 Complete Table & Relationship Matrix

| # | Table Name | Entity Class | Primary Key | Foreign Keys & Delete Actions | Indices | Status / Notes |
|---|---|---|---|---|---|---|
| 1 | `notes` | `NoteEntity` | `id` (String UUID) | `folderId -> folders.id` (`SET_NULL`) | `folderId`, `(isPinned, updatedAtUtc)`, `isArchived`, `isDeleted`, `isLocked`, `isHidden`, `noteType` | **Overloaded** `canvasDataJson` holds 7 unrelated payloads |
| 2 | `folders` | `FolderEntity` | `id` (String UUID) | *None* (`parentId` has no FK) | *None* | **Gap:** Missing index & self-referential FK on `parentId` |
| 3 | `tags` | `TagEntity` | `id` (String UUID) | *None* | `UNIQUE(name)` | Stable |
| 4 | `note_tags` | `NoteTagCrossRef` | `(noteId, tagId)` | `noteId -> notes.id` (`CASCADE`), `tagId -> tags.id` (`CASCADE`) | `noteId`, `tagId` | Stable |
| 5 | `sticky_notes` | `StickyNoteEntity` | `id` (String UUID) | `noteId -> notes.id` (`SET_NULL`) | `noteId`, `(isPinned, orderIndex)` | **Gap:** Spatial coordinates (`x/y/w/h/rotation`) stored in `SharedPreferences` instead of Room |
| 6 | `canvases` | `CanvasEntity` | `id` (String UUID) | *None* | *None* | **Gap:** No `boardKind` (`MIND_MAP` vs `WHITEBOARD`) or `folderId` |
| 7 | `canvas_elements` | `CanvasElementEntity` | `id` (String UUID) | `canvasId -> canvases.id` (`CASCADE`) | `canvasId` | Stores freehand strokes, shapes, `MINDMAP_NODE`, `MINDMAP_CONFIG` |
| 8 | `attachments` | `AttachmentEntity` | `id` (String UUID) | `noteId -> notes.id` (`CASCADE`) | `noteId` | **Gap:** `canvasId` unindexed; no `folderId` for folder-level PDFs; no SAF external URI columns |
| 9 | `annotations` | `AnnotationEntity` | `id` (String UUID) | `attachmentId -> attachments.id` (`CASCADE`) | `(attachmentId, pageNumber)` | Stores PDF highlights, ink paths, callouts, sticky pins |
| 10 | `bookmarks` | `BookmarkEntity` | `id` (String UUID) | *None* (polymorphic `targetType`/`targetId`) | `(targetType, targetId)` | Stable |
| 11 | `reminders` | `ReminderEntity` | `id` (String UUID) | `noteId -> notes.id` (`CASCADE`) | `noteId`, `triggerAtUtc` | Scheduled via `WorkManager` |
| 12 | `app_settings` | `AppSettingEntity` | `key` (String) | *None* | *None* | **Gap:** Not included in `BackupManager` ZIP; fragmented alongside 3 `SharedPreferences` files |
| 13 | `deleted_items` | `DeletedItemEntity` | `id` (String UUID) | *None* | `deletedAtUtc` | Trash restore snapshots |
| 14 | `note_links` | `NoteLinkEntity` | `id` (String UUID) | `sourceNoteId -> notes.id` (`CASCADE`), `targetNoteId -> notes.id` (`SET_NULL`) | `sourceNoteId`, `targetNoteId` | Phase 1 Knowledge Brain backlinks |
| 15 | `note_templates` | `NoteTemplateEntity` | `id` (String UUID) | *None* | `category` | Phase 1 Knowledge Brain templates |
| 16 | `note_versions` | `NoteVersionEntity` | `id` (String UUID) | `noteId -> notes.id` (`CASCADE`) | `(noteId, createdAtUtc)` | Phase 1 Knowledge Brain version history |
| 17 | `kanban_columns` | `KanbanColumnEntity` | `id` (String UUID) | *None* | `position` | Phase 2 Productivity |
| 18 | `tasks` | `TaskEntity` | `id` (String UUID) | `linkedNoteId -> notes.id` (`SET_NULL`), `linkedReminderId -> reminders.id` (`SET_NULL`, unique) | `kanbanColumnId`, `dueAtUtc`, `linkedNoteId`, `UNIQUE(linkedReminderId)` | Phase 2 Productivity |
| 19 | `task_dependencies` | `TaskDependencyEntity` | `(taskId, dependsOnTaskId)` | Both `-> tasks.id` (`CASCADE`) | `taskId`, `dependsOnTaskId` | Phase 2 Productivity |
| 20 | `habits` | `HabitEntity` | `id` (String UUID) | *None* | `isArchived` | Phase 2 Productivity |
| 21 | `habit_logs` | `HabitLogEntity` | `id` (String UUID) | `habitId -> habits.id` (`CASCADE`) | `UNIQUE(habitId, localDateKey)` | Phase 2 Productivity |
| 22 | `focus_sessions` | `FocusSessionEntity` | `id` (String UUID) | `linkedTaskId -> tasks.id` (`SET_NULL`), `linkedNoteId -> notes.id` (`SET_NULL`) | `startedAtUtc`, `linkedTaskId`, `linkedNoteId` | Phase 2 Productivity |

### 1.2 Existing Migration Chain (`v1 -> v5`)

All existing migrations in `AppDatabase.kt` are non-destructive and must remain untouched:
- **`MIGRATION_1_2`:** Added initial secondary columns/indices during early schema stabilization.
- **`MIGRATION_2_3`:** Added `notes.isHidden INTEGER NOT NULL DEFAULT 0` and index `index_notes_isHidden`.
- **`MIGRATION_3_4`:** Added Phase 1 Knowledge Brain tables (`note_links`, `note_templates`, `note_versions`).
- **`MIGRATION_4_5`:** Added Phase 2 Productivity tables (`kanban_columns`, `tasks`, `task_dependencies`, `habits`, `habit_logs`, `focus_sessions`).

---

## 2. Cross-Layer Data Dependencies

Upgrading Noteland safely requires respecting **four coupled persistence stores** outside Room tables:

1. **App Internal File Storage (`context.filesDir`):**
   - `attachments.filePath` stores local file paths (or relative paths inside `filesDir/attachments/`, `filesDir/audio/`, `filesDir/pdfs/`, `filesDir/backgrounds/`).
   - `notes.canvasDataJson` embeds file paths for voice memos and custom note background images.
   - **Rule:** Never move or rename existing files in `filesDir` during a DB migration. Always support reading both legacy absolute paths and relative paths.
2. **Three Fragmented `SharedPreferences` Files:**
   - `noteland_settings_prefs`: Theme mode, font scale, grid columns, `sticky_board_layouts` (JSON map of sticky ID → `{x, y, width, height, rotation}`), `dashboard_widgets_config`, reminder defaults.
   - `noteland_security_prefs` (EncryptedSharedPreferences / fallback): Vault PIN hash/salt, biometric lock toggle, auto-lock timeout.
   - `noteland_onboarding_prefs` / widget prefs: First-launch flags and Glance widget state.
   - **Rule:** Phase 2 introduces a unified `PreferencesRepository` façade that reads existing keys directly before gradually mirroring non-sensitive settings into `app_settings` / Room.
3. **Backup & Restore Format (`BackupManager.kt`, `CURRENT_SCHEMA_VERSION = 5`):**
   - Exports a ZIP archive containing `backup.json` (20 domain tables + `stickyBoardLayouts` + `dashboardWidgetsConfig`) plus binary attachment files under `files/`.
   - **Gap:** Currently omits `app_settings`, user preferences (theme, quick toolbar configuration, editor font scale), and `deleted_items`.
   - **Rule:** Upgrading `BackupManager` to schema v6/v7 **must** remain 100% capable of importing legacy v1–v5 ZIP backups seamlessly.
4. **Home Screen Widgets & Background Workers (`Glance` + `WorkManager`):**
   - `QuickNotesWidget`, `StickyBoardWidget`, and `ReminderWorker` query `AppDatabase` directly via DAOs.
   - **Rule:** Any schema change must preserve existing `NoteEntity`, `StickyNoteEntity`, and `ReminderEntity` column names and types so widgets and scheduled reminders continue working immediately after an app update.

---

## 3. Identified Database & Persistence Bottlenecks

| Severity | Issue | Impact | Remediation Phase |
|---|---|---|---|
| **Critical** | **`notes.canvasDataJson` payload overloading** (7 distinct JSON structures: legacy strokes, calendar event metadata, daily journal date keys, voice clips, handwriting ink strokes, background image/texture, block order) | Fragile string parsing in `SmartNoteUtils.kt`; impossible to query journal dates or audio notes cleanly in SQL; high risk of partial JSON overwrites | **Phase 2 (Schema v6)** + **Phase 4 (Dual-Read Adapter)** |
| **High** | **`exportSchema = false` in `AppDatabase.kt`** | No compile-time schema JSON history (`app/schemas/`); cannot run Room `MigrationTestHelper` automated migration verification | **Phase 2 (Immediate)** |
| **High** | **No full-text search index (`FTS4`/`FTS5`)** | Searching notes performs `LIKE '%query%'` full-table scans across `title` and `content`, degrading latency as notes scale past 500+ entries | **Phase 6 (Schema v7)** |
| **Medium** | **Sticky Wall spatial geometry stored in `SharedPreferences`** (`sticky_board_layouts`) | Split-brain state between Room `sticky_notes` and `SharedPreferences`; race conditions on undo/delete; cannot support multiple Sticky Walls cleanly | **Phase 2 (Schema v6)** + **Phase 7 (Backfill & Cutover)** |
| **Medium** | **Missing `folderId` & SAF external link fields on `attachments`** | PDFs must be artificially attached to a dummy note or copied into `filesDir`; cannot link external Android storage PDFs/Markdown files via SAF persistable URIs | **Phase 2 (Schema v6)** + **Phase 6/9 (UI & SAF)** |
| **Medium** | **Missing `boardKind` & `folderId` on `canvases`** | Mind Maps and Blank Whiteboards are indistinguishable at the query level without parsing `canvas_elements` for `MINDMAP_NODE`; canvases cannot be organized into folders | **Phase 2 (Schema v6)** + **Phase 7 (Studio Unification)** |
| **Low** | **Missing index on `folders(parentId)` and `attachments(canvasId)`** | Recursive folder tree traversal and canvas-attachment lookups do full table scans | **Phase 2 (Schema v6)** |
| **Low** | **`BackupManager` omits `app_settings` and toolbar/theme preferences** | Restoring a backup resets user theme, editor preferences, and custom Quick Edit Toolbar layout | **Phase 10 (Backup v7)** |

---

## 4. Backward-Compatible Migration Plan

### 4.1 Step 0 (Prerequisite in Phase 2): Enable Schema Export & Snapshot v5
Before altering a single column:
1. Configure KSP in `app/build.gradle.kts` with `ksp { arg("room.schemaLocation", "$projectDir/schemas") }`.
2. Flip `exportSchema = true` in `AppDatabase.kt` while keeping `version = 5`.
3. Compile once to generate the canonical baseline `app/schemas/com.example.data.db.AppDatabase/5.json` and commit it as the immutable v5 reference.

### 4.2 Migration `5 -> 6` (Phase 2 — Foundation & Unification)

Strictly **additive** DDL. Zero existing columns are renamed, narrowed, or dropped. All existing rows receive safe defaults.

#### A. New Table: `note_metadata` (Decoupling `notes.canvasDataJson`)
```sql
CREATE TABLE IF NOT EXISTS `note_metadata` (
    `noteId` TEXT NOT NULL,
    `journalDateKey` TEXT DEFAULT NULL,
    `calendarStartUtc` INTEGER DEFAULT NULL,
    `calendarEndUtc` INTEGER DEFAULT NULL,
    `calendarAllDay` INTEGER NOT NULL DEFAULT 0,
    `backgroundStyleJson` TEXT DEFAULT NULL,
    `voiceClipsJson` TEXT DEFAULT NULL,
    `inkOverlayJson` TEXT DEFAULT NULL,
    `blockSchemaVersion` INTEGER NOT NULL DEFAULT 1,
    `updatedAtUtc` INTEGER NOT NULL DEFAULT 0,
    PRIMARY KEY(`noteId`),
    FOREIGN KEY(`noteId`) REFERENCES `notes`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS `index_note_metadata_journalDateKey` ON `note_metadata` (`journalDateKey`);
CREATE INDEX IF NOT EXISTS `index_note_metadata_calendarStartUtc` ON `note_metadata` (`calendarStartUtc`);
```
- **Dual-Read / Lazy Backfill Strategy:**
  - `notes.canvasDataJson` is **preserved intact** (read-only fallback) throughout Phases 2–9.
  - When a note is opened or saved in Phase 4+, `NoteMetadataAdapter` reads from `note_metadata` first; if absent, it parses legacy `notes.canvasDataJson` via `SmartNoteUtils` and lazily persists the structured row to `note_metadata`.
  - Background one-time backfill worker (`NoteMetadataBackfillWorker`) indexes `journalDateKey` and `calendarStartUtc` for existing notes so Calendar queries stay instant.

#### B. Additive Columns on `sticky_notes` (Unified Room Spatial Geometry)
```sql
ALTER TABLE `sticky_notes` ADD COLUMN `wallId` TEXT NOT NULL DEFAULT 'default';
ALTER TABLE `sticky_notes` ADD COLUMN `posX` REAL DEFAULT NULL;
ALTER TABLE `sticky_notes` ADD COLUMN `posY` REAL DEFAULT NULL;
ALTER TABLE `sticky_notes` ADD COLUMN `widthDp` REAL DEFAULT NULL;
ALTER TABLE `sticky_notes` ADD COLUMN `heightDp` REAL DEFAULT NULL;
ALTER TABLE `sticky_notes` ADD COLUMN `rotationDeg` REAL NOT NULL DEFAULT 0.0;
CREATE INDEX IF NOT EXISTS `index_sticky_notes_wallId` ON `sticky_notes` (`wallId`);
```
- **One-Time Safe Backfill (Phase 7):** On first launch after upgrade, if `sticky_board_layouts` exists in `SharedPreferences` and `sticky_notes.posX IS NULL`, copy coordinates into Room inside a single transaction, then set `sticky_layout_migrated_v6 = true` (leaving the legacy preference key intact for rollback safety).

#### C. Additive Columns on `canvases` (Mind Map vs. Whiteboard & Folder Ownership)
```sql
ALTER TABLE `canvases` ADD COLUMN `boardKind` TEXT NOT NULL DEFAULT 'WHITEBOARD';
ALTER TABLE `canvases` ADD COLUMN `folderId` TEXT DEFAULT NULL;
CREATE INDEX IF NOT EXISTS `index_canvases_boardKind` ON `canvases` (`boardKind`);
CREATE INDEX IF NOT EXISTS `index_canvases_folderId` ON `canvases` (`folderId`);
```
- **Deterministic SQL Backfill inside `MIGRATION_5_6`:**
  ```sql
  UPDATE `canvases`
  SET `boardKind` = 'MIND_MAP'
  WHERE `id` IN (
      SELECT DISTINCT `canvasId` FROM `canvas_elements`
      WHERE `elementType` IN ('MINDMAP_NODE', 'MINDMAP_CONFIG')
  );
  ```
  Existing mind maps are automatically classified as `'MIND_MAP'` with zero user intervention.

#### D. Additive Columns on `attachments` (Folder-Owned PDFs & Android SAF Links)
```sql
ALTER TABLE `attachments` ADD COLUMN `folderId` TEXT DEFAULT NULL;
ALTER TABLE `attachments` ADD COLUMN `externalUri` TEXT DEFAULT NULL;
ALTER TABLE `attachments` ADD COLUMN `isExternalLinked` INTEGER NOT NULL DEFAULT 0;
ALTER TABLE `attachments` ADD COLUMN `lastSyncedUtc` INTEGER DEFAULT NULL;
CREATE INDEX IF NOT EXISTS `index_attachments_folderId` ON `attachments` (`folderId`);
CREATE INDEX IF NOT EXISTS `index_attachments_canvasId` ON `attachments` (`canvasId`);
```
- Note: SQLite `ALTER TABLE` cannot add a foreign key constraint to an existing table without rebuilding it; to eliminate table-rebuild risk in v6, `folderId` on `canvases` and `attachments` is indexed and enforced via repository transactional cleanup (`SET_NULL` on folder deletion) rather than risky table recreation.

#### E. Missing Index on `folders`
```sql
CREATE INDEX IF NOT EXISTS `index_folders_parentId` ON `folders` (`parentId`);
```

#### F. Idempotent Column-Check Helper Pattern
All `ALTER TABLE ... ADD COLUMN` statements inside `MIGRATION_5_6` will use a shared Kotlin helper `addColumnIfMissing(db, table, column, definition)` querying `PRAGMA table_info(table)` first, guaranteeing idempotency even if a partial migration occurred on an interrupted device.

---

### 4.3 Migration `6 -> 7` (Phase 6 — Full-Text Search & Toolbar Presets)

Strictly additive DDL supporting instant offline search and customizable editor toolbars:

#### A. SQLite FTS4 Virtual Table (`notes_fts`)
Room supports `@Fts4(contentEntity = NoteEntity::class)` cleanly on all Android API levels (`minSdk 24+`).
```sql
CREATE VIRTUAL TABLE IF NOT EXISTS `notes_fts` USING FTS4(
    `title`,
    `content`,
    content=`notes`
);
-- Sync triggers automatically generated by Room for external-content FTS4:
-- room_fts_content_sync_notes_fts_BEFORE_UPDATE / BEFORE_DELETE / AFTER_UPDATE / AFTER_INSERT
-- Initial index population for all pre-existing notes:
INSERT INTO `notes_fts`(`notes_fts`) VALUES('rebuild');
```
- **Performance Impact:** Cuts search query time on 2,000+ notes from O(N) full-text scan to O(log N) inverted index lookup, supporting prefix matching (`query*`) and ranked snippets.
- **Privacy Guarantee:** Search DAOs always `JOIN notes ON notes.rowid = notes_fts.docid` with `WHERE notes.isDeleted = 0 AND notes.isHidden = 0` (and exclude locked vault notes unless unlocked).

#### B. Customizable Toolbar Presets Table (`editor_toolbar_presets`)
```sql
CREATE TABLE IF NOT EXISTS `editor_toolbar_presets` (
    `id` TEXT NOT NULL,
    `name` TEXT NOT NULL,
    `actionsJson` TEXT NOT NULL,
    `isActive` INTEGER NOT NULL DEFAULT 0,
    `isBuiltIn` INTEGER NOT NULL DEFAULT 0,
    `updatedAtUtc` INTEGER NOT NULL DEFAULT 0,
    PRIMARY KEY(`id`)
);
```
- Stores built-in presets (`Writing`, `Study`, `Minimal`, `Markdown`) plus user-created quick edit toolbar layouts, backed up automatically by `BackupManager` v7.

---

## 5. BackupManager Compatibility Matrix (`v5 -> v7`)

| Backup Version | Exported Contents | Import Compatibility |
|---|---|---|
| **v1 – v5 (Legacy)** | Up to 20 domain tables + `stickyBoardLayouts` + `dashboardWidgetsConfig` + `files/` | **100% Supported** — Missing v6/v7 fields deserialize with safe defaults; `boardKind` inferred from `canvasElements`; `notes_fts` rebuilt automatically post-restore. |
| **v6 (Phase 2–5)** | All v5 + `noteMetadata` + `sticky_notes` spatial fields + `canvases.boardKind/folderId` + `attachments.folderId/externalUri` | **100% Supported** (SAF `externalUri` permissions are flagged as re-linkable on new devices since Android URI grants are device-local). |
| **v7 (Phase 6–10)** | All v6 + `appSettings` + `editorToolbarPresets` + non-sensitive `PreferencesRepository` snapshot (excludes Vault PIN hash for security) | **Full Fidelity Restore** across reinstalls and devices. |

---

## 6. Pre-Migration Safety, Verification & Rollback Strategy

### 6.1 Pre-Migration Automated Safety Snapshot
Before executing any production migration (`5->6` or `6->7`), `AppDatabase` / `DatabaseSafetyManager` will:
1. Verify available disk space (`filesDir.usableSpace > dbFile.length() * 3`).
2. Copy `noteland_database` (after checkpointing WAL) to `filesDir/db_backups/noteland_v{oldVersion}_pre_migration.db` (retaining the latest 2 snapshots).
3. Run the Room migration inside SQLite's atomic transaction (`PRAGMA foreign_keys = ON` verified post-migration with `PRAGMA foreign_key_check`).

### 6.2 Mandatory Automated Migration Test Suite (Robolectric)
Every schema version bump **must** pass a dedicated Robolectric test suite (`DatabaseMigrationTest.kt`) before merging:
1. **Fresh Install Test:** Open v6 / v7 database cleanly and verify all 22+ tables and indices exist.
2. **Legacy Upgrade Test (`1 -> 5 -> 6 -> 7`):** Seed realistic fixture rows at v5 (notes with all 7 `canvasDataJson` payload types, nested folders, mind map canvases, whiteboard canvases, PDF attachments + annotations, sticky notes, reminders, tasks, habits, focus sessions, note links), execute `MIGRATION_5_6` and `MIGRATION_6_7`, and assert:
   - Row counts across all 22 legacy tables are strictly identical.
   - `PRAGMA foreign_key_check` returns 0 rows.
   - Mind map canvases are accurately backfilled to `boardKind = 'MIND_MAP'`.
   - `notes_fts` returns exact matches for pre-existing notes after `'rebuild'`.
3. **Backup Restore Regression Test:** Import a frozen v5 backup ZIP fixture into a v6/v7 database and assert 100% data integrity.

### 6.3 Rollback Procedure
- Because `MIGRATION_5_6` and `MIGRATION_6_7` are strictly additive and leave `notes.canvasDataJson` and legacy `SharedPreferences` keys untouched, any feature regression in Phases 2–9 can be rolled back in application code (reverting UI/repository commits or toggling a feature flag) without requiring a destructive database downgrade.
- If a migration bug is caught during development testing, restore the pre-migration snapshot from `filesDir/db_backups/` and fix the idempotent SQL migration script before release.
