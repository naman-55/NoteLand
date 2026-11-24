# Noteland 3.5 — Feature Mapping Matrix

This document maps every existing and planned Noteland capability into the new **5-pillar Noteland 3.5 Information Architecture**:

> **Bottom Navigation / Navigation Rail Pillars:**  
> `Home` · `Folders` · `Studio` · `Productivity` · `Settings`

**Status Legend:**
- **Exists (Stable):** Production-ready, retain behavior and APIs.
- **Needs Improvement:** Working today, requires refactoring, UX unification, or architectural hardening for 3.5.
- **Needs Development:** Missing or stubbed; must be built in upcoming roadmap phases.

---

## 1. HOME (`Screen.Home`)
*Role: Personal command center, instant capture, smart discovery, and quick access to recent & starred work.*

| Feature | Current Location | 3.5 Target Status | Action Plan |
|---|---|---|---|
| Greeting Header & Quick Capture ("New Note") | `HomeScreen.kt` | Exists (Stable) | Keep; add quick-create menu (Note, Checklist, Mind Map, Sticky, Scan/PDF) |
| Global Search Bar & Search Screen | `HomeScreen.kt`, `SearchScreen.kt` | Needs Improvement | Back with SQLite FTS4/FTS5 for instant sub-20ms search across notes, PDFs, tasks |
| Customizable Home Dashboard Widgets | `DashboardWidgetsSection.kt` | Needs Improvement | Move config from `SharedPreferences` to unified Settings/Preferences store + Backup |
| Calendar & Tasks Summary Hero Cards | `HomeScreen.kt` | Exists (Stable) | Link directly into unified `Productivity` pillar tabs |
| Recent Notes Staggered Grid / List | `HomeScreen.kt`, `NoteCard.kt` | Exists (Stable) | Optimize memoized preview rendering; avoid JSON parsing in item composition |
| Favourites / Starred Notes | `FavoritesScreen.kt` (currently bottom tab 3) | Needs Improvement | Relocate from primary bottom bar to Home quick-filter chip & Smart Collection while preserving `Screen.Favorites` route for backward compatibility |
| Knowledge Brain (Graph, Backlinks, Templates) | `BrainScreen.kt` (Home shortcut) | Needs Improvement | Keep fast access from Home + integrate template gallery cleanly into note creation |
| Daily Diary One-Tap Entry | `NoteViewModel.openOrCreateDailyDiary` | Exists (Stable) | Surface in Home quick strip + Calendar day sheet |
| Quick-Jump Recent Studio Boards & PDFs | Scattered | **Needs Development** | Add optional "Continue Creating" strip for recent Mind Maps, Sticky Walls, and PDFs |
| Incoming Android Share/Open Inbox Banner | None | **Needs Development** | Handle `ACTION_SEND` / `ACTION_VIEW` intents cleanly into Home/Editor/PDF Reader |

---

## 2. FOLDERS (`Screen.Folders`)
*Role: Unified file, folder, notebook, tag, and document (PDF/Media) organization system.*

| Feature | Current Location | 3.5 Target Status | Action Plan |
|---|---|---|---|
| Nested Hierarchical Folders & Breadcrumbs | `FoldersScreen.kt`, `HierarchicalFolderComponents.kt` | Needs Improvement | Add SQLite self-FK & index on `folders.parentId`; decompose 2,144-LOC file |
| Folder Custom Colors & Icons | `FoldersScreen.kt`, `FolderEntity.kt` | Exists (Stable) | Preserve existing presets; add folder cover/sort preference |
| In-Folder Filters (All, Pinned, Simple, To-Do) | `FoldersScreen.kt` | Needs Improvement | Extend filters to include `PDFs`, `Boards`, and `Audio` inside folders |
| Tag Manager & Multi-Tag Filtering | `HomeScreen.kt`, `TagDao.kt` | Needs Improvement | Unify tag management inside Folders/Collections hub |
| Smart Collections (Favourites, Locked Vault, Archive, Recycle Bin) | Scattered (`VaultScreen`, `ArchiveScreen`, `RecycleBinScreen`, `FavoritesScreen`) | Needs Improvement | Surface as pinned Smart Collections at top of Folders for 1-tap discoverability |
| PDF Documents Inside Folders | `PdfToolsScreen.kt`, `MediaScreen.kt` (disconnected from folders) | **Needs Development** | Add nullable `folderId` on `attachments` so PDFs & images live directly inside user folders |
| PDF Reader & Annotation Engine | `PdfReaderScreen.kt`, `PdfReaderComponents.kt`, `PdfTextHighlightEngine.kt` | Needs Improvement | Modularize 2,110+2,148 LOC files; add bookmark outline sidebar & export-flattened PDF |
| PDF Office Tools (Merge, Split, Compress, Image-to-PDF) | `PdfToolsScreen.kt` | Needs Improvement | Connect directly into Folder actions & external `ACTION_VIEW` PDF launches |
| External Folder / SAF File Linking | None (`OpenDocument` copies only) | **Needs Development** | Add SAF persistable URI reference support + "Import vs. Link" option for external PDFs/Markdown |
| Batch Operations (Multi-Select Move, Tag, Delete, Export) | Partial | **Needs Development** | Add consistent multi-select toolbar inside Folders |

---

## 3. STUDIO (`Screen.Studio` — *New Primary Tab*)
*Role: Visual thinking, creative expression, whiteboarding, mind mapping, sticky walls, and media markup.*

| Feature | Current Location | 3.5 Target Status | Action Plan |
|---|---|---|---|
| **Studio Unified Landing Hub** | None (currently scattered across Home cards) | **Needs Development** | Build `StudioHubScreen` with tabs/sections for Mind Maps, Whiteboards, Sticky Walls, PDF Markup, and Image Markup + "Create New" CTA |
| Mind Map Studio (5-Tab Emerald Dock, 6 Layouts, Themes, PNG Export) | `CanvasScreen.kt`, `mindmap/*` | Exists (Recently Upgraded) | Extract reusable canvas gesture/export services; add Mind Map ↔ Note outline conversion |
| Freehand Infinite Whiteboard / Canvas | Shared with `CanvasScreen.kt` | Needs Improvement | Allow choosing Blank Whiteboard vs. Mind Map template on board creation; split freehand toolbar cleanly |
| Sticky Wall (Draggable, Resizable, Rotatable Cards) | `StickyBoardScreen.kt` | Needs Improvement | Migrate geometry (`x, y, w, h, rotation, boardId`) from `SharedPreferences` into Room `sticky_notes` columns; support multiple sticky walls |
| Image Annotator & Markup | `ImageAnnotatorScreen.kt`, `MediaScreen.kt` | Needs Improvement | Share unified ink engine & color picker with Canvas and PDF Reader |
| Handwriting Ink Layer inside Notes | `HandwritingOverlay.kt`, `HandwritingInkEngine.kt` | Needs Improvement | Extract shared ink renderer to eliminate duplication across Note Editor, Image Annotator, and PDF Reader |
| Studio Templates (Brainstorm, SWOT, Org Chart, Kanban Sticky Wall) | Starter radial map only | **Needs Development** | Add visual template gallery inside Studio Hub |
| High-Res Export (PNG / PDF / Markdown Outline) | PNG export in `MindMapCanvasRenderer.kt` | Needs Improvement | Standardize via shared `ExportService` supporting PNG, SVG/PDF, and Markdown outline |

---

## 4. PRODUCTIVITY (`Screen.Productivity` — *Promoted to Primary Tab*)
*Role: Unified time management, calendar scheduling, task execution, habits, focus timer, and analytics.*

| Feature | Current Location | 3.5 Target Status | Action Plan |
|---|---|---|---|
| **Unified Productivity Hub Shell** | `ProductivityScreen.kt` (sub-route) + `CalendarScreen.kt` (separate route) | Needs Improvement | Promote to Primary Tab #4 combining **Calendar**, **Tasks/Kanban**, **Habits**, **Focus**, and **Reports** under one coherent navigation flow |
| Calendar (Month / Week / Day Timeline) | `CalendarScreen.kt` (2,762 LOC) | Needs Improvement | Decompose monolith into Month/Week/Timeline/Agenda composables; unify event & task creation |
| Tasks & Kanban Board (WIP limits, subtasks, dependencies, recurrence) | `KanbanBoardTab.kt`, `KanbanDialogs.kt`, `ProductivityEngine.kt` | Needs Improvement | Merge duplicate task views between `CalendarScreen` (tab 1) and `ProductivityScreen` (tab 0); add quick Today/Upcoming list toggle |
| Reminders & Exact Alarm Notifications | `RemindersScreen.kt`, `ReminderNotificationManager.kt` | Exists (Stable) | Keep WorkManager/AlarmManager scheduling; unify reminder creation inside Task & Note editors |
| Habit Tracker & Streaks | `HabitsTab.kt`, `HabitModels.kt` | Exists (Stable) | Add Home/Calendar habit check-in strip and weekly heatmap polish |
| Pomodoro Focus Timer & Background Worker | `PomodoroFocusTab.kt`, `ProductivityWorkers.kt` | Exists (Stable) | Keep WorkManager `2.10.1` timer & deep-linking; link completed sessions automatically to task/note time logs |
| Daily & Weekly Progress Reports | `ProgressReportsTab.kt` | Needs Improvement | Fix deprecated `TrendingUp` icon; add exportable weekly review note generator |
| Glance Home Screen Widgets (Tasks, Reminders, Collections) | `widget/*` | Exists (Stable) | Keep intact; ensure widget routes map cleanly to updated 3.5 primary tabs |

---

## 5. SETTINGS (`Screen.Settings`)
*Role: Personalization, editor/toolbar customization, security & vault controls, storage, backups, and external integrations.*

| Feature | Current Location | 3.5 Target Status | Action Plan |
|---|---|---|---|
| Workspace Profile (Name & Email) | `BackupSettingsScreen.kt` | Exists (Stable) | Keep local profile customization |
| Appearance (Light / Dark / Ivory, Dynamic Color, Font Size) | `BackupSettingsScreen.kt`, `Theme.kt` | Needs Improvement | Add accent color overrides, compact/comfortable density toggle, and font family selector |
| **Customizable Quick Edit Toolbar Settings** | None | **Needs Development** | Build drag-to-reorder & toggle UI for the Note Editor Quick Edit Toolbar (persisted in unified preferences + backup) |
| **Editor Preferences** (Default Note Type, Paper Texture, Auto-Save, Spell/Markdown Mode) | Partial (font size only) | **Needs Development** | Centralize editor defaults in Settings |
| Security & Vault (PIN, Biometric, Auto-Lock Timeout) | `BackupSettingsScreen.kt`, `SecurityManager.kt` | Needs Improvement | Replace silent default PIN `"1234"` with explicit first-time PIN onboarding wizard (while keeping existing PINs valid) |
| Encrypted ZIP Backup & Restore (`MERGE` / `REPLACE`) | `BackupManager.kt`, `BackupDialogs.kt` | Needs Improvement | Include `app_settings`, toolbar layout, and preferences in backup ZIP; add scheduled local backup reminder |
| Storage & Cache Inspector | None | **Needs Development** | Display breakdown of DB, attachments, PDFs, audio, and cache with safe orphan-file cleanup |
| External File Integration Preferences | None | **Needs Development** | Configure default import behavior for external PDFs/Markdown (Copy into Vault vs. SAF Link) and default target folder |
| Notifications & Power Optimization Guidance | `BackupSettingsScreen.kt`, `PowerOptimizationUtils.kt` | Exists (Stable) | Preserve exact alarm permission and battery optimization helpers |

---

## 6. Cross-Cutting Feature: Advanced Notes Editor (`Screen.NoteEditor`)
*Accessible from Home, Folders, Productivity, Studio, Search, Widgets, and External Intents.*

| Capability | Current Status | 3.5 Upgrade Target |
|---|---|---|
| Markdown & Rich Text Formatting | Exists (`NoteRichText.kt`, `MarkdownPreviewPane.kt`) | Add block-aware editing (Callouts, Tables, Code blocks with language tag, Dividers, Collapsible toggles) |
| Quick Edit Formatting Toolbar | Hardcoded (`RichFormatBar.kt`) | **Customizable Quick Edit Toolbar**: user-ordered pinned actions, overflow drawer, context-aware mode (Text vs. Checklist vs. Ink) |
| Interactive Checklists | Exists (`checklistJson`) | Add drag-to-reorder, indent subtasks, convert checklist items directly to Productivity Tasks |
| Handwriting Ink & Paper Textures | Exists | Keep & decouple into standalone `editor/` sub-module using shared ink engine |
| Voice Dictation & Audio Recorder | Exists | Keep & extract audio playback service cleanly out of `NoteEditorScreen.kt` |
| Wiki-Links (`[[Note]]`), Backlinks & Version History | Exists (`Phase 1 Knowledge Brain`) | Keep & surface cleanly in modular Editor Inspector bottom sheet |
| Import / Export Note (`.md`, `.txt`, `.pdf`, `.html`) | Partial (plain text share only) | **Needs Development**: Full Markdown & formatted PDF export + incoming `.md`/`.txt` import |

---

## 7. Summary Counts

- **Exists (Stable):** 16 feature areas
- **Needs Improvement / Refactoring:** 22 feature areas
- **Needs New Development:** 12 feature areas
- **Backward Compatibility Commitment:** All 21 existing navigation routes, 3 Glance widgets, and all existing Room tables remain functional throughout the transition.
