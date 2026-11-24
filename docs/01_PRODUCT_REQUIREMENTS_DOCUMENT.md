# NoteLand — Product Requirements Document (PRD)

## 1. Executive Summary
**NoteLand** is an offline-first Personal Knowledge Management (PKM) and visual productivity application engineered natively for Android using Kotlin and Jetpack Compose. NoteLand unites structured notes, rich markdown editing, an infinite freeform drawing canvas, an interactive sticky notes wall, local image annotation, and an offline PDF reader into a unified workspace.

## 2. Product Objectives
- **Zero-Cloud Requirement**: 100% functionality without internet access, external cloud dependencies, or mandatory accounts.
- **Data Durability & Ownership**: All notes, media, drawings, and metadata reside locally in SQLite (Room) and encrypted app storage.
- **Hardware-Grade Security**: AES-256-GCM encryption with Android Keystore backed keys for private and sensitive notes.
- **Visual & Conceptual Freedom**: Seamlessly bridge hierarchical text documentation (folders, tags, rich text) and spatial visual thinking (canvases, sticky walls, image/PDF annotations).

## 3. User Personas
1. **The Knowledge Worker & Researcher**: Requires categorized hierarchical notes, fast debounced search, tags, extractive text summarization, checklist extraction, and local PDF reading with bookmarks.
2. **The Visual Thinker & Designer**: Needs freeform canvas drawing, mind maps, arrows, geometric connectors, sticky note boards, and non-destructive image annotations.
3. **The Privacy-Conscious User**: Demands authenticated encryption at rest, PIN/biometrics protection, failed attempt lockouts, separation of unlisted vs encrypted data, and password-protected offline backups.

## 4. Key Functional Requirements
- **F-01 Note Management**: Full CRUD, duplicate, pin, favorite, archive, soft-delete, and permanent purge.
- **F-02 Rich Text Editor**: Headings (H1/H2), bold, italic, underline, strikethrough, quote, code, bullet/numbered lists, interactive checklists, text highlights, color accents, and inline image attachments with debounced autosave.
- **F-03 Sticky Notes Wall**: 2D coordinate board with free positioning, drag-and-drop, pastel color palette, persistent dimensions, and accessible directional nudge controls.
- **F-04 Infinite Canvas**: Multi-canvas workspace, pan, zoom, freehand pen, highlighter, eraser, geometric shapes (rectangle, circle, line, arrow, connector), text tool, image insertion, undo/redo, and layer z-index manipulation.
- **F-05 Local Media Workspace**: Android Photo Picker integration, zoomable image viewer, non-destructive annotation layer (pen, shapes, text labels), and PDF reader with bookmarks and comments.
- **F-06 Advanced Search**: 300ms debounced search over titles, contents, tags, folders, and attachment names, with query history and sorting filters.
- **F-07 Reminders & Notifications**: Local reminders with repeat intervals (daily, weekly, monthly), snooze, rescheduling, and Android 13+ notification permission support.
- **F-08 Security Vault**: AES-256-GCM hardware-backed vault for locked notes, 4-digit PIN, biometric authentication, failed attempt lockout timer (30s after 5 failures), and strict separation from unlisted hidden notes.
- **F-09 Offline Backup & Restore**: Standalone ZIP archive containing `manifest.json`, `database.json`, media attachments, SHA-256 integrity checksums, optional PBKDF2/AES-GCM encryption, pre-restore preview, and safe `MERGE` vs `REPLACE` strategies.
