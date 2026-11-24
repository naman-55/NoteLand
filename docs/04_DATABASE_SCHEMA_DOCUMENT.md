# NoteLand — Database Schema Document (Audited & Hardened)

## 1. Overview
NoteLand utilizes an embedded SQLite database managed via Android's **Room Persistence Library (v2.6.1)** with **KSP** code generation. The database operates offline-first with strict foreign key enforcement via runtime `PRAGMA foreign_keys = ON;`, composite indexing on query filters, atomic transactions via `withTransaction`, and migrations.

- **Database Name**: `noteland_database`
- **Current Version**: `3` (Migrated via `MIGRATION_1_2` and `MIGRATION_2_3`)
- **Total Entities**: 13 relational tables
- **Primary Key Strategy**: UUID strings (`UUID.randomUUID().toString()`) across all core entities for offline distributed generation and conflict-free export/import.

---

## 2. Entity Relational Architecture

```
                    ┌──────────────┐
                    │   folders    │
                    └──────┬───────┘
                           │ 1:N (ON DELETE SET NULL)
                           ▼
┌──────────────┐    ┌──────────────┐    ┌──────────────────┐
│     tags     ├────┤  note_tags   ├────┤      notes       │
└──────────────┘    └──────────────┘    └─────────┬────────┘
                    (N:M, CASCADE)                │
             ┌────────────────┬───────────────────┼───────────────────┐
             │ 1:N (CASCADE)  │ 1:N (CASCADE)     │ 1:N (CASCADE)     │ 1:N (CASCADE)
             ▼                ▼                   ▼                   ▼
      ┌─────────────┐  ┌─────────────┐     ┌─────────────┐     ┌─────────────┐
      │ attachments │  │  reminders  │     │ canvas_elem │     │ annotations │
      └──────┬──────┘  └─────────────┘     └─────────────┘     └─────────────┘
             │ 1:N (CASCADE)
             ▼
      ┌─────────────┐
      │ annotations │
      └─────────────┘
```

---

## 3. Schema Definitions

### 3.1 `notes`
The primary table storing document records.
- **Table Name**: `notes`
- **Fields**:
  - `id` (TEXT, Primary Key, UUID string, Not Null)
  - `folderId` (TEXT, Nullable, Foreign Key -> `folders(id)` ON DELETE SET NULL, Indexed)
  - `folderName` (TEXT, Not Null, Default = "")
  - `title` (TEXT, Not Null, Default = ""): In locked notes, stores Base64 AES-256-GCM ciphertext.
  - `content` (TEXT, Not Null, Default = ""): Holds plaintext markdown or Base64 AES-256-GCM ciphertext (`[12-byte IV][Ciphertext + 16-byte Tag]`).
  - `noteType` (TEXT, Not Null): `STANDARD`, `STICKY`, `CANVAS`, `PDF_DOC`. Indexed.
  - `colorHex` (TEXT, Not Null, Default = ""): Hex representation.
  - `tagsCsv` (TEXT, Not Null, Default = ""): Comma-separated tag names.
  - `isPinned` (INTEGER, Not Null, Default = 0): Boolean (0/1). Composite indexed with `updatedAtUtc`.
  - `isArchived` (INTEGER, Not Null, Default = 0): Boolean. Indexed.
  - `isDeleted` (INTEGER, Not Null, Default = 0): Boolean. Indexed.
  - `isLocked` (INTEGER, Not Null, Default = 0): Boolean. Indexed.
  - `isHidden` (INTEGER, Not Null, Default = 0): Boolean. Indexed.
  - `isFavorite` (INTEGER, Not Null, Default = 0): Boolean.
  - `reminderTimestamp` (INTEGER, Nullable): Epoch milliseconds.
  - `canvasDataJson` (TEXT, Not Null, Default = ""): Vector path backup payload.
  - `checklistJson` (TEXT, Not Null, Default = ""): Checkbox items or encrypted Base64 string.
  - `imageUri` (TEXT, Not Null, Default = "")
  - `createdAtUtc` (INTEGER, Not Null, Epoch milliseconds)
  - `updatedAtUtc` (INTEGER, Not Null, Epoch milliseconds, Indexed)
  - `deletedAtUtc` (INTEGER, Nullable, Epoch milliseconds)
- **Indices**:
  - `index_notes_folderId` on (`folderId`)
  - `index_notes_isPinned_updatedAtUtc` on (`isPinned`, `updatedAtUtc`)
  - `index_notes_isArchived` on (`isArchived`)
  - `index_notes_isDeleted` on (`isDeleted`)
  - `index_notes_isLocked` on (`isLocked`)
  - `index_notes_isHidden` on (`isHidden`)
  - `index_notes_noteType` on (`noteType`)

### 3.2 `folders`
Hierarchical tree storage for organizing workspaces.
- **Table Name**: `folders`
- **Fields**:
  - `id` (TEXT, Primary Key, UUID string, Not Null)
  - `parentId` (TEXT, Nullable, Self-referencing folder tree)
  - `name` (TEXT, Not Null)
  - `colorHex` (TEXT, Not Null, Default = "#3B82F6")
  - `iconName` (TEXT, Not Null, Default = "folder")
  - `createdAtUtc` (INTEGER, Not Null)
  - `updatedAtUtc` (INTEGER, Not Null)

### 3.3 `tags` & `note_tags`
Categorical tagging with many-to-many junction table.
- **Table Name**: `tags`
  - `id` (TEXT, Primary Key, UUID string, Not Null)
  - `name` (TEXT, Not Null, Unique)
  - `colorHex` (TEXT, Not Null)
  - `createdAtUtc` (INTEGER, Not Null)
  - **Unique Index**: `index_tags_name` on (`name`)
- **Table Name**: `note_tags`
  - **Composite Primary Key**: (`noteId`, `tagId`)
  - `noteId` (TEXT, Foreign Key -> `notes(id)` ON DELETE CASCADE, Indexed)
  - `tagId` (TEXT, Foreign Key -> `tags(id)` ON DELETE CASCADE, Indexed)
  - `assignedAtUtc` (INTEGER, Not Null)
  - **Indices**: `index_note_tags_noteId`, `index_note_tags_tagId`

### 3.4 `attachments`
Metadata for local files stored in application sandbox (`context.filesDir/attachments/`).
- **Table Name**: `attachments`
- **Fields**:
  - `id` (TEXT, Primary Key, UUID string, Not Null)
  - `noteId` (TEXT, Nullable, Foreign Key -> `notes(id)` ON DELETE CASCADE, Indexed)
  - `canvasId` (TEXT, Nullable)
  - `fileName` (TEXT, Not Null)
  - `fileExtension` (TEXT, Not Null)
  - `relativePath` (TEXT, Not Null): Path relative to internal storage root.
  - `mimeType` (TEXT, Not Null, Indexed)
  - `sizeBytes` (INTEGER, Not Null)
  - `createdAtUtc` (INTEGER, Not Null)
- **Indices**:
  - `index_attachments_noteId` on (`noteId`)
  - `index_attachments_mimeType` on (`mimeType`)

### 3.5 `annotations`
Non-destructive drawing and comment layers pinned to attachments.
- **Table Name**: `annotations`
- **Fields**:
  - `id` (TEXT, Primary Key, UUID string, Not Null)
  - `attachmentId` (TEXT, Not Null, Foreign Key -> `attachments(id)` ON DELETE CASCADE, Indexed)
  - `annotationType` (TEXT, Not Null): `HIGHLIGHT`, `DRAWING`, `STAMP`, `TEXT_NOTE`
  - `pageNumber` (INTEGER, Not Null, Default = 1)
  - `coordinatesJson` (TEXT, Not Null): Vector bounding box or path coordinates.
  - `content` (TEXT, Not Null, Default = "")
  - `colorHex` (TEXT, Not Null, Default = "#FDE047")
  - `createdAtUtc` (INTEGER, Not Null)
- **Indices**:
  - `index_annotations_attachmentId` on (`attachmentId`)
  - `index_annotations_attachmentId_pageNumber` on (`attachmentId`, `pageNumber`)

### 3.6 `canvases` & `canvas_elements`
Infinite vector whiteboard drawings and stroke hierarchies.
- **Table Name**: `canvases`
  - `id` (TEXT, Primary Key, UUID string, Not Null)
  - `title` (TEXT, Not Null)
  - `backgroundColorHex` (TEXT, Not Null)
  - `width` (REAL, Not Null)
  - `height` (REAL, Not Null)
  - `createdAtUtc` (INTEGER, Not Null)
  - `updatedAtUtc` (INTEGER, Not Null)
- **Table Name**: `canvas_elements`
  - `id` (TEXT, Primary Key, UUID string, Not Null)
  - `canvasId` (TEXT, Not Null, Foreign Key -> `canvases(id)` ON DELETE CASCADE, Indexed)
  - `elementType` (TEXT, Not Null): `STROKE`, `SHAPE`, `TEXT`, `IMAGE`
  - `elementDataJson` (TEXT, Not Null): Vector points and visual properties.
  - `zIndex` (INTEGER, Not Null, Default = 0)
  - `createdAtUtc` (INTEGER, Not Null)
  - **Indices**: `index_canvas_elements_canvasId`, `index_canvas_elements_canvasId_zIndex`

### 3.7 `sticky_notes`
2D spatial corkboard cards.
- **Table Name**: `sticky_notes`
- **Fields**:
  - `id` (TEXT, Primary Key, UUID string, Not Null)
  - `noteId` (TEXT, Nullable, Foreign Key -> `notes(id)` ON DELETE SET NULL, Indexed)
  - `title` (TEXT, Not Null, Default = "")
  - `content` (TEXT, Not Null, Default = "")
  - `colorHex` (TEXT, Not Null, Default = "#FEF3C7")
  - `orderIndex` (INTEGER, Not Null, Default = 0)
  - `isPinned` (INTEGER, Not Null, Default = 0)
  - `createdAtUtc` (INTEGER, Not Null)
  - `updatedAtUtc` (INTEGER, Not Null)
- **Indices**:
  - `index_sticky_notes_noteId` on (`noteId`)
  - `index_sticky_notes_isPinned_orderIndex` on (`isPinned`, `orderIndex`)

### 3.8 `reminders`
Local time alerts coupled to Android `AlarmManager`.
- **Table Name**: `reminders`
- **Fields**:
  - `id` (TEXT, Primary Key, UUID string, Not Null)
  - `noteId` (TEXT, Not Null, Foreign Key -> `notes(id)` ON DELETE CASCADE, Indexed)
  - `scheduledTimeUtc` (INTEGER, Not Null, Indexed)
  - `repeatInterval` (TEXT, Nullable): `DAILY`, `WEEKLY`, `MONTHLY`, `null`
  - `isCompleted` (INTEGER, Not Null, Default = 0, Indexed)
  - `createdAtUtc` (INTEGER, Not Null)
- **Indices**:
  - `index_reminders_noteId` on (`noteId`)
  - `index_reminders_scheduledTimeUtc` on (`scheduledTimeUtc`)
  - `index_reminders_isCompleted` on (`isCompleted`)

### 3.9 `bookmarks`, `app_settings`, `deleted_items`
- **`bookmarks`**: Reading progress markers with `index_bookmarks_targetType_targetId`.
- **`app_settings`**: `key` (TEXT, PK), `value` (TEXT), `updatedAtUtc` (INTEGER).
- **`deleted_items`**: Audit log of soft-deleted records with `index_deleted_items_originalTable`.

---

## 4. Runtime Integrity Verification (PRAGMAs)

On database initialization:
1. `PRAGMA foreign_keys = ON;` is executed on every database connection via `RoomDatabase.Callback.onOpen()`.
2. Verified via automated test:
   - `PRAGMA integrity_check;` returns `"ok"`.
   - `PRAGMA foreign_key_check;` returns `0` violations.
