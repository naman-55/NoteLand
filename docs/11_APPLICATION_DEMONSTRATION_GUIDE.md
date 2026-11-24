# NoteLand — Application Demonstration Guide

This guide provides a scripted, end-to-end demonstration script walking through every key user journey in NoteLand.

---

## Demonstration Script

### Scene 1: App Launch & Material You Aesthetics
- **Action**: Launch NoteLand.
- **Visuals**:
  - Clean animated splash screen displays the modern Indigo NoteLand logo.
  - Landing on the **Dashboard** reveals personalized greeting, quick stat cards, recent workspaces, and actionable launchpads.
- **Narrative**: *"NoteLand launches instantly with zero network requests. The interface embraces Material Design 3 with dynamic color theming that adapts to the user's system wallpaper."*

### Scene 2: Creating a Rich-Text Note with Checklists
- **Action**: Tap `+` FAB on Dashboard. Type title: *"Project Horizon Launch Plan"*.
- **Action**: Type markdown body and use the docked formatting bar to insert an H2 heading, bold text, code block, and interactive checklist items (`- [ ] Finalize UX`, `- [ ] Pass security audit`, `- [ ] Publish build`).
- **Action**: Tap the checkbox next to *Finalize UX*.
- **Visuals**: The item dynamically checks off with strikethrough animation.
- **Narrative**: *"The rich-text editor combines the speed of Markdown with tactile visual controls. All changes persist automatically to the local SQLite database."*

### Scene 3: Folders, Tags & Categorization
- **Action**: Tap the Folder icon in the top bar. Create a new folder named *"Work / Launches"*.
- **Action**: Tap the Tag icon and assign tags `#launch` and `#critical`.
- **Visuals**: Color-coded chips render neatly under the note title.

### Scene 4: Offline Smart Utilities in Action
- **Action**: Tap the **⚡ (Smart Tools)** icon on the note toolbar.
- **Visuals**: A bottom sheet reveals real-time word count (142 words), reading time (<1 min), extractive summary, suggested alternative titles, and duplicate analysis.
- **Narrative**: *"All text intelligence runs 100% deterministically on-device without any third-party AI APIs or internet permissions."*

### Scene 5: Spatial Corkboard (Sticky Notes)
- **Action**: Navigate to the **Sticky Board** tab.
- **Action**: Tap **Add Sticky**. Type *"Quick brainstorm: Call Dave regarding logistics"*. Select yellow tint.
- **Action**: Drag the card across the board. Then tap the directional nudge buttons to position it precisely.
- **Visuals**: Smooth 60fps drag-and-drop movement. Coordinate state is retained across app restarts.

### Scene 6: Infinite Vector Canvas
- **Action**: Switch to the **Canvas** tab.
- **Action**: Select the **Pen** tool and sketch a flowchart. Select **Shapes** and draw a rectangle and circle. Connect them with an **Arrow**.
- **Action**: Tap **Undo**, then **Redo**.
- **Visuals**: High-precision vector paths render crisply with zero rasterization artifacts.

### Scene 7: PDF Hub, Office Tools & Comprehensive PDF Reader
- **Action**: Open Navigation Drawer or Quick Create FAB > select **PDF Hub & Office Tools**.
- **Visuals**:
  - Displays the Recents Carousel with document preview cards (*Quick Start Guide.pdf*, *What's New.pdf*).
  - Four vibrant gradient Office Tools: **Scanner & OCR**, **Convert PDF**, **Assign Signers**, and **File Sharing**.
  - Three Document Tools: **Create PDFs**, **Smart AI Text Tools**, and **Compress PDF**.
- **Action**: Tap *Quick Start Guide.pdf* to launch the full-screen PDF Reader.
- **Visuals & Interaction**:
  - Page 1 renders the complete reference layout with vector landscape illustration.
  - Right-hand vertical floating annotation toolbar provides immediate access to **Undo/Redo**, **Sticky Notes**, **Highlighters** (multi-color), **Strikethrough**, **Underlines**, **Handwriting / Pen**, **Shapes** (Rectangle, Circle, Arrow, 5-Pointed Star ⭐), **Text Box**, **Stamps / Signatures**, and **Eraser**.
  - Top bar features **Reading Stats Dialog (ⓘ)**, **Text-to-Speech (TTS) Read Aloud** with speech playback, **In-Document Search**, and **Reading Themes** (Daylight, Night Mode, Warm Sepia, Mint Green).
  - Bottom bar provides page navigation, bookmark ribbon toggle, and thumbnail grid drawer.
- **Narrative**: *"NoteLand delivers an enterprise-grade offline PDF studio on Android, keeping all annotations indexed locally in SQLite without altering the original file."*

### Scene 8: Fast Offline Search & Filtering
- **Action**: Navigate to the Search screen.
- **Action**: Type *"Launch"*.
- **Visuals**: Search results update immediately with debounce. Filter chips allow narrowing by tag (`#critical`) or folder (`Work`).

### Scene 9: Security Vault: Locked vs Hidden Notes
- **Action**: Open Note Settings > Set Master PIN to `1234`.
- **Action**: Open a private note: select **Lock Note**.
- **Visuals**: Note is encrypted with AES-256-GCM. Attempting to view it requires PIN or biometric entry.
- **Action**: Open another note: select **Hide Note**.
- **Visuals**: Note disappears from the public list and normal search results, but remains unencrypted in the separate Hidden Vault.

### Scene 10: Complete Offline Backup & Restore
- **Action**: Go to Settings > Backup & Restore > Tap **Export Backup**.
- **Visuals**: Creates `noteland-backup-2026.zip` with complete SHA-256 manifest.
- **Action**: Simulate a restore preview: verify checksums and inspect items before deciding to Merge or Replace.
