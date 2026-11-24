# NoteLand 2.0 — Phase 0: Existing Project Audit and Stabilization Report

**Document Identifier:** NL-DOC-018  
**Status:** Completed & Verified  
**Target Codebase:** NoteLand (Kotlin + Jetpack Compose + Material 3 + Room + Coroutines, `com.example` / `com.aistudio.noteland.gzugkw`)  
**Verification Date:** November 16, 2026  
**Debug APK Build:** Passing (`compile_applet`)  
**Automated Test Suite:** 135/135 Passing (10 test suites, 0 failed, 0 errors, 0 skipped)

---

## 1. Executive Summary

Phase 0 audited the existing offline-first NoteLand codebase across database schema/migrations, note creation and autosave, checklists/to-dos, calendar and reminders, boards and infinite canvas, PDF attachments and annotations, encrypted backup/restore, and Compose navigation/state preservation before introducing NoteLand 2.0 features.

All 6 Phase 0 stabilization priorities and database integrity guarantees were verified and stabilized without changing the package name, application ID, or schema version (preserved non-destructively at Room version 3).

---

## 2. Subsystem Audit Findings & Fixes Implemented

### 2.1 Room Database Entities, DAOs & Non-Destructive Migrations
- **Audit Findings:**
  - `AppDatabase` is at **Version 3** with **13 entities** and **10 DAOs**, non-destructive `MIGRATION_1_2` and `MIGRATION_2_3`, and `PRAGMA foreign_keys = ON` enabled on connection open.
  - **Confirmed Bug (`ON DELETE CASCADE` data loss on parent upserts):** `CanvasDao.saveCanvasWithElements`, `AttachmentDao.saveAttachmentWithAnnotations`, and `OnConflictStrategy.REPLACE` paths used SQLite `INSERT OR REPLACE` on parent entities (`canvases`, `attachments`, `notes`, `folders`, `tags`). Under `PRAGMA foreign_keys = ON`, SQLite executes `REPLACE` on a colliding primary key as `DELETE` followed by `INSERT`, silently triggering `ON DELETE CASCADE` on child rows (`canvas_elements`, `annotations`, `reminders`, `note_tags`) or `SET NULL` on references.
  - `clearAllData()` omitted clearing `note_tags` and `deleted_items` join/tombstone tables.
- **Fixes Applied:**
  - Added direct lookup queries and `@Update` methods across `NoteDao`, `TagDao`, `CanvasDao`, `AttachmentDao`, and `ReminderDao`.
  - Updated `CanvasDao.saveCanvasWithElements` and `AttachmentDao.saveAttachmentWithAnnotations` to check row existence first and invoke `@Update` on existing parents instead of `REPLACE`.
  - Added non-cascading `upsert*Safe` repository methods in `NoteRepository` (including tag-name collision ID remapping across `tags` and `note_tags`) and updated `clearAllData()` to clear `note_tags` and `deleted_items`.

### 2.2 Note Creation, Autosave & Configuration/Process State
- **Audit Findings:**
  - `NoteRepository.saveOrUpdateNote` and `NoteViewModel.saveNoteSerialized` already serialize writes via `editorSaveMutex` and revision checks, preserving `createdAtUtc` on update.
  - **Confirmed Bugs in `NoteEditorScreen.kt`:**
    1. Editing an existing note reconstructed `NoteEntity` with `isHidden = false` hardcoded rather than preserving the hydrated `isHidden` flag.
    2. `LaunchedEffect(noteId)` unconditionally overwrote local draft state on recomposition after configuration changes or process recreation and failed to preserve `tagsList`, `checklistItems`, and `blockOrder` across process death.
- **Fixes Applied:**
  - Added `isHidden` (`rememberSaveable`), `hasHydratedFromDb`, `savedTagsCsv`, `savedChecklistJson`, and `savedBlockOrderCsv` (`rememberSaveable(noteId)`) in `NoteEditorScreen.kt` so unsaved draft state survives configuration changes/process recreation and existing `isHidden`, `isDeleted`, and `deletedAtUtc` flags are preserved on updates.

### 2.3 Tasks & Checklists (Priorities 1, 2 & 3)
- **Audit Findings:**
  - **Confirmed Bug (Unstable & colliding checklist item IDs):** Legacy checklist JSON entries without an `"id"` field defaulted to `System.currentTimeMillis().toString()`, producing identical IDs when parsed inside the same millisecond. `encodeChecklistJson` also dropped or normalized `"id"` inconsistently, breaking Compose row identity.
  - **Confirmed Bug (Race & locked ciphertext overwrite):** `NoteViewModel.toggleNoteChecklistItem` mutated stale `NoteEntity` snapshots outside `editorSaveMutex` and could overwrite encrypted `checklistJson` on locked notes.
  - **Confirmed Bug (UI list keying & touch targets):** Edit/View checklist rows in `NoteEditorScreen.kt` iterated by index without `key(item.id)`, and delete icons were below the 48dp accessibility touch target minimum.
- **Fixes Applied:**
  - Updated `SmartNoteUtils.parseChecklistJson` and `encodeChecklistJson` to preserve persistent `"id"` values and assign unique deduplicated fallback IDs (`item_$i`, `item_${i}_$suffix`) for legacy/duplicate JSON.
  - Rewrote `NoteViewModel.toggleNoteChecklistItem` to acquire `editorSaveMutex`, re-fetch the latest row via `getNoteByIdDirect`, abort when `latest.isLocked` is true, and update `latestSavedRevisions`.
  - Keyed checklist rows via `key(item.id)` with dynamic index lookups and standardized checklist delete `IconButton` touch targets to `48.dp`.

### 2.4 Infinite Canvas & Boards (Priority 4)
- **Audit Findings:**
  - **Confirmed Bug (Cold-start hydration race):** `CanvasScreen.kt` looked up metadata from `viewModel.allCanvases` StateFlow (`WhileSubscribed`), which could be empty on cold start before DB emission, defaulting metadata (`title`, `createdAtUtc`, `width`, `height`) and overwriting `createdAtUtc` / `width` / `height` on save.
  - **Confirmed Bug (Legacy `NoteType.CANVAS` notes):** Seeded/legacy canvas notes stored strokes in `notes.canvasDataJson` instead of `canvases` / `canvas_elements`.
  - **Confirmed Bug (Stale element transforms & missed flushes):** Undo/Redo and `TransformSheet` sliders operated on stale item snapshots or deferred writes; drag gestures lacked `onDragCancel` cleanup and lifecycle pause/stop flush.
- **Fixes Applied:**
  - Hydrated `CanvasScreen.kt` on launch using `viewModel.getCanvasByIdDirect(currentId)` with fallback `parseLegacyCanvasNoteStrokes` hydration for legacy `NoteType.CANVAS` notes.
  - Preserved original `createdAtUtc`, `width`, and `height` across board saves and image imports.
  - Updated Undo/Redo and `TransformSheet` controls to read live element state and persist immediately (including `onValueChangeFinished` on sliders), added `onDragCancel` handlers, and added a `LifecycleEventObserver` flush on `ON_PAUSE` / `ON_STOP` / `onDispose`.

### 2.5 Calendar, Reminders & Locked Content Privacy (Priority 5)
- **Audit Findings:**
  - **Confirmed Bug (DST 7-day strip edge case):** `SmartNoteUtils.getWeekDaysStartingMonday` stepped from `startOfDayUtc` using 24-hour / midnight increments without local-noon normalization, creating DST spring-forward/fall-back day drift risk, while `WeekStripSelector` could clip weekday labels and had `< 48.dp` prev/next buttons.
  - **Confirmed Bug (Calendar task trash side-effect):** Deleting a reminder in `CalendarScreen.kt` could soft-delete arbitrary linked user notes even if the note was not a calendar-created task.
  - **Confirmed Privacy Leaks:** Locked notes' reminder titles/subtitles or attachments could surface in `CalendarScreen.kt`, `RemindersScreen.kt`, `MediaScreen.kt`, and `NoteViewModel.safeNotificationTitle` / `CreateReminderDialog`.
- **Fixes Applied:**
  - Normalized `getWeekDaysStartingMonday` to step across local noon (`12:00:00`) via `Calendar.DAY_OF_MONTH` and map each day with `startOfDayUtc`.
  - Enforced `48.dp` touch targets on `calendar_prev_week_btn` / `calendar_next_week_btn` and prevented weekday label clipping in `WeekStripSelector`.
  - Restricted reminder trash side-effects in `CalendarScreen.kt` to calendar-created task notes (`SmartNoteUtils.parseCalendarMeta(linkedNote.canvasDataJson) != null`).
  - Masked locked note titles (`"Locked Note Reminder"`), excluded locked notes/attachments from `CreateReminderDialog` and `MediaScreen`, and gated locked note opening from Calendar/Reminders behind `viewModel.isVaultUnlocked`.

### 2.6 PDF Reader, Annotations & Attachment Lifecycle
- **Audit Findings:**
  - Offline `PdfRenderer` + `AttachmentEntity` / `AnnotationEntity` storage and edit flows already pass targeted highlight/annotation regressions, while parent `attachments` row updates/merges risked cascading child `annotations` deletion via SQLite `REPLACE`.
- **Fixes Applied:**
  - Replaced parent `REPLACE` writes with existence-checked `@Update` in `AttachmentDao.saveAttachmentWithAnnotations` and non-cascading `upsertAttachmentSafe` / `upsertAnnotationSafe` in `NoteRepository`, ensuring PDF annotations survive updates and backup merges.

### 2.7 Backup, Encryption & Restore (Priority 6)
- **Audit Findings:**
  - **Confirmed Bugs (`BackupManager.kt`):**
    1. ZIP and JSON backups omitted `NoteEntity.deletedAtUtc`, `StickyNoteEntity.noteId`, `CanvasEntity.width/height`, `note_tags` join records, several `createdAtUtc`/`updatedAtUtc` timestamps, `sticky_board_layouts` SharedPreferences, and `note_backgrounds/` image files.
    2. `restoreFromJson` only restored `notes` via `REPLACE`, ignoring other tables/assets and bypassing the transaction-safe merge pipeline.
    3. `restoreFromZip` (`MERGE` / `REPLACE`) used `REPLACE` inserts on parent tables, triggering `ON DELETE CASCADE` against pre-existing local child records on colliding IDs.
- **Fixes Applied:**
  - Extended ZIP & JSON export (`createBackupZip`, `saveBackupToFile`) to serialize all 12 core data collections (including `note_tags`, full timestamps, `deletedAtUtc`, `StickyNoteEntity.noteId`, `CanvasEntity.width/height`), `sticky_board_layouts` SharedPreferences, and `note_backgrounds/` assets with SHA-256 manifest verification and AES password encryption.
  - Unified `restoreFromZip` and `restoreFromJson` onto a single transactional `restoreDatabasePayload(root, strategy)` pipeline using non-cascading `upsert*Safe` methods, path-traversal-safe restoration of `note_backgrounds/` into `context.filesDir`, and `NoteViewModel.refreshStickyLayouts()`.

---

## 3. Files Modified & Created in Phase 0

### Modified Production Files
- `app/src/main/java/com/example/util/SmartNoteUtils.kt`
- `app/src/main/java/com/example/util/BackupManager.kt`
- `app/src/main/java/com/example/data/db/NoteDao.kt`
- `app/src/main/java/com/example/data/db/TagDao.kt`
- `app/src/main/java/com/example/data/db/CanvasDao.kt`
- `app/src/main/java/com/example/data/db/AttachmentDao.kt`
- `app/src/main/java/com/example/data/db/ReminderDao.kt`
- `app/src/main/java/com/example/data/repository/NoteRepository.kt`
- `app/src/main/java/com/example/ui/viewmodel/NoteViewModel.kt`
- `app/src/main/java/com/example/ui/screens/NoteEditorScreen.kt`
- `app/src/main/java/com/example/ui/screens/CanvasScreen.kt`
- `app/src/main/java/com/example/ui/screens/CalendarScreen.kt`
- `app/src/main/java/com/example/ui/screens/RemindersScreen.kt`
- `app/src/main/java/com/example/ui/screens/MediaScreen.kt`

### Created Test & Documentation Files
- `app/src/test/java/com/example/Phase0StabilityTest.kt`
- `docs/18_PHASE_0_STABILITY_REPORT.md`

---

## 4. Verification Results (Actual Executed Outputs)

### 4.1 Unit, Database Integrity & Robolectric UI Tests (`gradle :app:testDebugUnitTest`)
- **Command:** `gradle :app:testDebugUnitTest`
- **Outcome:** `BUILD SUCCESSFUL` — **135/135 tests passed (0 failed, 0 errors, 0 skipped)**

| Test Suite | Executed | Passed | Failed | Errors | Skipped |
|---|---:|---:|---:|---:|---:|
| `com.example.Phase0StabilityTest` | 10 | 10 | 0 | 0 | 0 |
| `com.example.ExampleRobolectricTest` | 53 | 53 | 0 | 0 | 0 |
| `com.example.NoteLandUnitTest` | 18 | 18 | 0 | 0 | 0 |
| `com.example.StabilizationPassRobolectricTest` | 17 | 17 | 0 | 0 | 0 |
| `com.example.EditorAndThemeStabilizationTest` | 15 | 15 | 0 | 0 | 0 |
| `com.example.PdfAnnotationEditingTest` | 9 | 9 | 0 | 0 | 0 |
| `com.example.MainActivityCrashTest` | 4 | 4 | 0 | 0 | 0 |
| `com.example.PdfTextHighlightEngineTest` | 4 | 4 | 0 | 0 | 0 |
| `com.example.StickyBoardResizeTest` | 3 | 3 | 0 | 0 | 0 |
| `com.example.data.repository.NoteRepositoryTest` | 2 | 2 | 0 | 0 | 0 |
| **Total** | **135** | **135** | **0** | **0** | **0** |

**`Phase0StabilityTest` Coverage (10 Tests):**
1. New note creation, flag preservation (`isHidden`, `isPinned`), `createdAtUtc` immutability, and persistence across DB close/reopen.
2. Checklist JSON stable ID preservation & deduplication of legacy/duplicate IDs.
3. Sequential `toggleNoteChecklistItem` updates and locked-note ciphertext protection.
4. `NoteEditorScreen` UI end-to-end checklist creation, View Mode rendering, navigation back flush, and SQLite verification.
5. `CanvasScreen` UI cold-start direct DB hydration (`createdAtUtc`, `width`, `height`) and zoom controls (`125%` / `100%` reset).
6. Legacy `NoteType.CANVAS` stroke JSON hydration (`parseLegacyCanvasNoteStrokes`).
7. `getWeekDaysStartingMonday` 7-day Mon–Sun invariants across leap days, year boundaries, and US DST Spring Forward / Fall Back transitions.
8. `CalendarScreen` UI 7-day strip rendering and locked-note reminder title masking (`"Locked Note Reminder"`).
9. Encrypted ZIP & JSON backup/restore fidelity (wrong-password rejection without DB loss, `REPLACE`, non-cascading `MERGE`, `note_tags`, `sticky_board_layouts`, `note_backgrounds/`).
10. SQLite `PRAGMA integrity_check` (`"ok"`), `PRAGMA foreign_key_check` (`0` violations), FK `SET_NULL` on folder deletion, tombstone recording, and FK `CASCADE` on permanent note deletion.

### 4.2 Debug APK Build (`compile_applet`)
- **Command:** `compile_applet`
- **Outcome:** `Build succeeded - the applet is compiled`

---

## 5. Known Limitations Documented at End of Phase 0

1. **JVM / Robolectric Verification Environment vs. Physical Device Hardware:** All 135 tests execute under Robolectric (SDK 36) on the local JVM; `adb` and hardware-only flows (hardware TEE Keystore attestation, physical `AlarmManager` delivery under OEM battery savers, biometric prompt hardware, and live microphone/PDF scroll memory pressure on physical 32-bit devices) remain governed by standard Android hardware boundaries documented in `docs/13_KNOWN_LIMITATIONS.md`.
2. **`tagsCsv` vs. `note_tags` Dual Representation:** `NoteRepository.saveOrUpdateNote` persists `tagsCsv` directly on `notes`, while relational `note_tags` rows are maintained separately via tag assignment/backup flows. Full schema normalization/sync between `tagsCsv` and `note_tags` can be addressed safely in a dedicated Phase 1+ migration if needed.
3. **Locked Note Search Boundary:** Locked notes remain intentionally excluded from SQLite plaintext `LIKE` search and reminder previews when the vault is locked.
4. **Legacy Backup `note_backgrounds` Paths:** JSON backups created before Phase 0 did not bundle `note_backgrounds/` binary files; restoring a pre-Phase-0 backup without those files safely preserves note text/metadata while missing external background images fall back to the note's solid/gradient palette.
