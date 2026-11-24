# NoteLand — System Architecture Document

## 1. Architectural Pattern
NoteLand adheres to modern **Android Clean Architecture** and **MVVM (Model-View-ViewModel)** guidelines using Kotlin Coroutines and reactive Kotlin `Flow` / `StateFlow`:

```
┌─────────────────────────────────────────────────────────────────┐
│                       Presentation Layer                        │
│  - Jetpack Compose UI (Material 3, Edge-to-Edge, Window Insets)  │
│  - Screens: Home, Dashboard, Editor, StickyBoard, Canvas, etc.  │
│  - Components: NoteCard, RichFormatBar, PinDialog, Sheets       │
└────────────────────────────────┬────────────────────────────────┘
                                 │ Observes StateFlow / Dispatches Events
┌────────────────────────────────▼────────────────────────────────┐
│                        ViewModel Layer                          │
│  - NoteViewModel (AndroidViewModel, CoroutineScope)             │
│  - Search, Filter, Sort & Vault Lockout State Management        │
│  - Reactive Flow Combinations (combine, stateIn)                │
└───────┬───────────────────────┬───────────────────────┬─────────┘
        │                       │                       │
┌───────▼──────────────┐ ┌──────▼──────────────┐ ┌──────▼─────────┐
│   SecurityManager    │ │    BackupManager     │ │ ReminderNotif │
│  - AES-256-GCM       │ │  - ZIP Packaging    │ │  - AlarmMgr   │
│  - Android Keystore  │ │  - SHA-256 Hashes   │ │  - Broadcast  │
│  - PBKDF2 (100k)     │ │  - Merge / Replace  │ │    Receiver   │
└───────┬──────────────┘ └──────┬──────────────┘ └────────────────┘
        │                       │
┌───────▼───────────────────────▼─────────────────────────────────┐
│                       Repository Layer                          │
│  - INoteRepository / NoteRepository                             │
│  - Coordinates SQLite DAOs and Attachment File Storage          │
└────────────────────────────────┬────────────────────────────────┘
                                 │
┌────────────────────────────────▼────────────────────────────────┐
│                        Data Layer                               │
│  - Room Database (AppDatabase v3, 13 SQLite tables)             │
│  - DAOs: NoteDao, FolderDao, TagDao, StickyNoteDao, CanvasDao,   │
│          AttachmentDao, BookmarkDao, ReminderDao                │
│  - Local App Storage: `context.filesDir/attachments/`           │
└─────────────────────────────────────────────────────────────────┘
```

## 2. Layer Responsibilities
1. **Presentation Layer**: Exclusively declarative Jetpack Compose. Utilizes M3 dynamic colors, responsive layouts (`BoxWithConstraints` with navigation rail on >=600dp wide screens), and accessible 48dp touch targets.
2. **ViewModel Layer**: Exposes read-only `StateFlow` streams. Encapsulates business logic, search debounce, autosave debounce, and state persistence.
3. **Security Subsystem**: `SecurityManager` interfaces with `AndroidKeyStore` to generate and manage non-exportable 256-bit AES keys without hardcoded material.
4. **Data Layer**: Room SQLite with versioned migrations (`MIGRATION_1_2`, `MIGRATION_2_3`), transactional queries, and raw file storage for binary attachments.
