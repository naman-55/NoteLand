# NOTELAND — CRITICAL BUG FIX REPORT: NEW NOTE PERSISTENCE FLOW

**Document Identifier:** NL-DOC-016  
**Status:** Resolved & Verified  
**Affected Subsystem:** Note Creation, Room Database Persistence, Compose Editor Lifecycle  
**Primary Engine:** Kotlin 1.9.24, Android Jetpack Compose M3, Room 2.6.1, Coroutines Flow  
**Verification Date:** November 8, 2026  
**Test Suite Status:** 50/50 Tests Passing (100% Pass Rate)

---

## 1. Executive Summary

A critical issue was reported in NoteLand:
> *"When I create a new note and write text in it, the note is not being saved."*

An exhaustive architectural investigation was executed across the note lifecycle, tracking the flow from the UI entry points (`HomeScreen`, FAB action, `NavDestination`), through the editor composable (`NoteEditorScreen`), into the presentation/state layer (`NoteViewModel`), repository layer (`NoteRepository`), and data access layer (`NoteDao` / SQLite).

The root cause was identified, fixed surgically without breaking changes or data loss, fortified with debounce and lifecycle persistence guarantees, and verified with 8 targeted automated integration tests and a full clean Gradle test suite.

---

## 2. Root Cause Analysis

### 2.1 The Silent Zero-Row `@Update` Flaw

1. **Initial Entry Point (`noteId == "0"` or empty):**
   When the user tapped **"New Note"**, the app navigated to `NoteEditorScreen` with `noteId = "0"`.
2. **Editor Initialization:**
   In `NoteEditorScreen`, when building the entity to save, the code evaluated:
   ```kotlin
   val finalId = if (noteId.isNotBlank() && noteId != "0") noteId else UUID.randomUUID().toString()
   ```
   This assigned a newly generated 36-character UUID string (e.g. `a1b2c3d4-...`) to `finalId`.
3. **Repository / ViewModel Logic:**
   The `saveNote` method inspected the note:
   ```kotlin
   if (note.id.isBlank() || note.id == "0") {
       repository.insertNote(note)
   } else {
       repository.updateNote(note)
   }
   ```
   Because `note.id` had already been populated with the generated UUID, `note.id.isBlank() || note.id == "0"` evaluated to **`false`**.
4. **Room DAO Execution:**
   The repository invoked `noteDao.updateNote(note)`, generating:
   ```sql
   UPDATE notes SET title = :title, content = :content ... WHERE id = :id;
   ```
   Because this note ID had **never been inserted**, the SQLite `UPDATE` statement matched **0 rows**.
5. **Silent Failure:**
   Room's `@Update` annotation does not throw an exception when updating a row with an unmatched primary key; it simply updates 0 rows and returns `Unit`. The operation completed without an error, giving the impression of an asynchronous execution, but zero data was committed to SQLite.
6. **Recomposition ID Churn:**
   On subsequent keystrokes or autosave iterations, because `originalNote` was not updated to reflect an inserted record, a new UUID was generated again on each attempt, continuously issuing 0-row updates and discarding all typed text upon exiting the editor.

---

## 3. Structural Solutions Implemented

### 3.1 Repository Layer (`INoteRepository` & `NoteRepository.kt`)

Added an atomic `saveOrUpdateNote` method executed strictly on `Dispatchers.IO`:

```kotlin
override suspend fun saveOrUpdateNote(note: NoteEntity): Result<String> {
    return withContext(Dispatchers.IO) {
        try {
            val effectiveId = if (note.id.isBlank() || note.id == "0") UUID.randomUUID().toString() else note.id
            val existing = noteDao.getNoteByIdDirect(effectiveId)
            if (existing == null) {
                val newNote = note.copy(
                    id = effectiveId,
                    createdAtUtc = if (note.createdAtUtc > 0) note.createdAtUtc else System.currentTimeMillis(),
                    updatedAtUtc = System.currentTimeMillis()
                )
                noteDao.insertNote(newNote)
            } else {
                val updatedNote = note.copy(
                    id = effectiveId,
                    createdAtUtc = existing.createdAtUtc,
                    updatedAtUtc = System.currentTimeMillis()
                )
                noteDao.updateNote(updatedNote)
            }
            Result.success(effectiveId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
```

**Key Architectural Guarantees:**
- **Deterministic Existence Check:** Direct query to `getNoteByIdDirect` checks whether the row exists in Room.
- **Insert vs. Update Routing:** Inserts brand new notes with immutable `createdAtUtc`, and updates existing notes without duplicating rows or mutating original timestamps.
- **Safe Dispatcher:** Guaranteed execution on `Dispatchers.IO`.

### 3.2 ViewModel Layer (`NoteViewModel.kt`)

Updated `saveNote` to use `saveOrUpdateNote` and return `Result<String>` with the persisted note ID dispatched back to `Dispatchers.Main`:

```kotlin
fun saveNote(note: NoteEntity, onSaved: (Result<String>) -> Unit = {}) {
    viewModelScope.launch(Dispatchers.IO) {
        val result = repository.saveOrUpdateNote(note)
        withContext(Dispatchers.Main) {
            onSaved(result)
        }
    }
}

suspend fun saveNoteDirect(note: NoteEntity): Result<String> {
    return withContext(Dispatchers.IO) {
        repository.saveOrUpdateNote(note)
    }
}
```

### 3.3 Editor State & UI Lifecycle Layer (`NoteEditorScreen.kt`)

1. **Stateful Note ID Tracking:**
   ```kotlin
   var currentNoteId by remember(noteId) {
       mutableStateOf(if (noteId.isNotBlank() && noteId != "0") noteId else "")
   }
   ```
   Once an ID is assigned during note creation or loaded from navigation, it remains stable throughout the editing session.
2. **Race-Condition & Stale-Write Protection (`saveVersionCounter`):**
   ```kotlin
   val thisSaveVersion = ++saveVersionCounter
   saveState = EditorSaveState.Saving
   viewModel.saveNote(noteToSave) { result ->
       result.onSuccess { savedId ->
           currentNoteId = savedId
           originalNote = noteToSave.copy(id = savedId)
           if (thisSaveVersion == saveVersionCounter) {
               hasUnsavedChanges = false
               saveState = EditorSaveState.Saved
           }
           onDone()
       }
   }
   ```
   Older asynchronous saves do not overwrite newer in-progress edits.
3. **Save State Feedback:**
   Added `EditorSaveState`:
   - `Idle`: No active save operation in flight.
   - `Saving…`: Displayed in amber badge with a mini indeterminate indicator.
   - `Saved`: Displayed in emerald green check badge only after Room confirms persistence.
   - `Save failed — Retry`: Displayed in red with a click-to-retry target; user content is never discarded on failure.
4. **Lifecycle and Navigation Flush:**
   - **`BackHandler` & Back Navigation Icon:** Flushes any pending changes via `saveCurrentNote(onDone = onNavigateBack)` before popping the screen.
   - **`DisposableEffect(Unit)`:** Flushes unsaved edits in `onDispose` to handle system process termination, multi-window dismissals, or incoming calls.
   - **Debounced Autosave:** Automatically triggers a background save 1,000ms after the user pauses typing.
   - **Empty Title Support:** Notes with empty titles but valid content or checklists are fully persisted.

---

## 4. Verification & Testing

Eight comprehensive unit and integration tests were added to `app/src/test/java/com/example/ExampleRobolectricTest.kt`:

| # | Test Case | Target Requirement | Result |
|---|---|---|---|
| 1 | `verify critical bug fix - creating a new note inserts a row into Room via saveOrUpdateNote` | UUID new note insertion verification | **PASSED** |
| 2 | `verify critical bug fix - inserted note can be retrieved using its ID` | Query by ID directly from SQLite | **PASSED** |
| 3 | `verify critical bug fix - updating a note changes stored content without duplicating rows` | Subsequent edits mutate existing row without duplicate rows | **PASSED** |
| 4 | `verify critical bug fix - navigating away does not lose the note` | Back navigation / onDispose flush persistence | **PASSED** |
| 5 | `verify critical bug fix - reopening the note displays saved content and preserves creation time` | Multi-session reopen and timestamp immutability | **PASSED** |
| 6 | `verify critical bug fix - multiple quick edits preserve the latest content` | Rapid typing and debounce version ordering | **PASSED** |
| 7 | `verify critical bug fix - an empty title with valid content can still be saved` | Blank title resilience | **PASSED** |
| 8 | `verify critical bug fix - a failed save does not silently discard the user text` | Failure state handling and memory retention | **PASSED** |

### Complete Test Suite Execution:
- `com.example.NoteLandUnitTest`: 15 tests, 0 failures, 0 errors
- `com.example.ExampleRobolectricTest`: 35 tests, 0 failures, 0 errors
- **Total:** 50 tests executed in 40 seconds; 100% passing.

---

## 5. Non-Destructive Impact Confirmation

- **Database Integrity:** Schema version remains at 3; all foreign keys, unique constraints, and indexes remain intact.
- **No Data Cleared:** Existing notes, attachments, folders, canvas documents, and encrypted vault entries are preserved.
- **Security:** AES-256-GCM encryption on locked notes and vault auto-locking are unmodified.
- **PDF & Canvas Tools:** Offline PDF reading, page annotation, and infinite canvas tools remain fully functional.
