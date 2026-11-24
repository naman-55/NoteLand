# NoteLand — UI/UX Design Document

## 1. Design Philosophy: Material Design 3 (Material You)
NoteLand adheres to Google's **Material Design 3 (M3)** specification. It balances expressive personality with utilitarian efficiency. The design feels native, responsive, tactile, and uncluttered.

### Core Visual Principles
1. **Dynamic Expressiveness**: Seamless support for Android 12+ dynamic tonal palettes (`dynamicLightColorScheme` / `dynamicDarkColorScheme`), deriving UI accents from user wallpaper preferences, backed by a tailored modern Indigo palette (`#4F46E5` primary, `#818CF8` secondary).
2. **Layered Depth & Surfaces**: Elevations are communicated through tonal surface tinting (`surfaceContainerLowest`, `surfaceContainerLow`, `surfaceContainer`, `surfaceContainerHigh`, `surfaceContainerHighest`) rather than heavy drop shadows.
3. **Accessibility First**:
   - Minimum interactive touch targets of **48dp × 48dp**.
   - Contrast ratio exceeding WCAG 2.1 AA requirements (minimum 4.5:1 for normal text).
   - Clear semantic labels on all icons and interactive elements.
   - Distinct focus states and tactile keyboard navigation support.
   - Screen-reader accessible controls for non-standard visual components (e.g. tactile directional nudge buttons for canvas and sticky notes).

---

## 2. Color System & Tokens

### 2.1 Static Color System (Fallback & Default)
| Token Name | Light Value | Dark Value | Purpose |
| :--- | :--- | :--- | :--- |
| `primary` | `#4F46E5` | `#818CF8` | FABs, primary buttons, active tabs, prominent states |
| `onPrimary` | `#FFFFFF` | `#1E1B4B` | Text/icons on primary surfaces |
| `primaryContainer` | `#E0E7FF` | `#312E81` | Selection chips, subtle highlights, active cards |
| `onPrimaryContainer` | `#1E1B4B` | `#E0E7FF` | Text/icons on primaryContainer |
| `secondary` | `#06B6D4` | `#67E8F9` | Accents, badges, secondary indicators |
| `surface` | `#FAFAFA` | `#0F172A` | Background behind main content |
| `onSurface` | `#0F172A` | `#F8FAFC` | Primary text and icons |
| `surfaceVariant` | `#F1F5F9` | `#1E293B` | Card surfaces, search inputs, containers |
| `outline` | `#CBD5E1` | `#334155` | Card borders, subtle dividers |
| `error` | `#EF4444` | `#F87171` | Delete actions, destructive warnings |

### 2.2 Note Color Palette Tokens
Users can assign soft pastel accents to notes and sticky cards:
- Default: Neutral Surface Tint
- Coral / Red: `#FFE4E6` / `#4C0519`
- Amber / Yellow: `#FEF3C7` / `#451A03`
- Emerald / Green: `#D1FAE5` / `#064E3B`
- Sky / Blue: `#E0F2FE` / `#082F49`
- Violet / Purple: `#EDE9FE` / `#2E1065`
- Rose / Pink: `#FCE7F3` / `#500724`

---

## 3. Typography Scale

NoteLand uses the standard Material 3 typographic scale, fully compatible with system font scaling and TalkBack:

| Style Name | Font Size | Line Height | Tracking | Weight | Used For |
| :--- | :--- | :--- | :--- | :--- | :--- |
| `HeadlineLarge` | 32sp | 40sp | 0.0sp | SemiBold | Screen titles, Onboarding headers |
| `HeadlineMedium` | 28sp | 36sp | 0.0sp | Medium | Major section headers, Drawer titles |
| `TitleLarge` | 22sp | 28sp | 0.0sp | Medium | Note editor title input, Dialog titles |
| `TitleMedium` | 16sp | 24sp | 0.15sp | SemiBold | Note card titles, Sticky note headers |
| `BodyLarge` | 16sp | 24sp | 0.5sp | Normal | Rich text note editor body, reader content |
| `BodyMedium` | 14sp | 20sp | 0.25sp | Normal | Note card previews, attachment summaries |
| `LabelLarge` | 14sp | 20sp | 0.1sp | Medium | Button labels, Filter chips, Tab labels |
| `LabelSmall` | 11sp | 16sp | 0.5sp | Medium | Badges, timestamps, reading-time chips |

---

## 4. Key Screen Architectures

### 4.1 Modern Dashboard
- **Header**: Warm, contextual greeting ("Good morning", "Good afternoon", etc.), quick search shortcut bar, and avatar placeholder.
- **Stat Cards**: Grid displaying total notes, sticky cards, pinned items, and active reminders.
- **Recent Workspaces**: Horizontal carousel of recently modified notes with visual color coding.
- **Quick Actions**: One-tap access to New Note, New Sticky, Canvas Whiteboard, and Media Importer.

### 4.2 Rich-Text Note Editor
- **Header**: Top app bar with back navigation, pin toggle, reminder trigger, color picker bottom sheet, and overflow menu (Lock note, Hide note, Export, Word count stats).
- **Title Field**: Borderless `TextField` with `TitleLarge` typography.
- **Folder & Tag Badges**: Dynamic chip group showing assigned folders and classification tags.
- **Formatting Bar**: Floating docked toolbar offering Heading (H1, H2, H3), Bold, Italic, Strikethrough, Code Block, Bullet List, Numbered List, and Interactive Checklist insertion.
- **Attachments Row**: Horizontal thumbnails of attached photos, documents, and PDFs with deletion and viewing affordances.

### 4.3 2D Interactive Sticky Board
- **Infinite 2D Surface**: Dual-axis panning and touch-drag re-positioning.
- **Visual Sticky Cards**: Post-it styled cards rendered with tilt and drop-shadow, with direct inline editing and color customization.
- **Accessibility Nudge Bar**: Discrete directional buttons (Up, Down, Left, Right) allowing users with screen readers or physical motor limitations to position items without touch-drag.

### 4.4 Vector Infinite Canvas
- **Drawing Canvas**: High-frequency touch event processing with anti-aliased path drawing.
- **Tool Palette**: Pen, Highlighter, Shape selector (Rectangle, Circle), Arrow & Connector, Eraser, Text insertion, and Color picker.
- **History Controls**: Full Undo and Redo stack with tactile icons.

### 4.5 Media Annotator & PDF Reader
- **Image Annotator**: Interactive zoom/pan photo viewer with non-destructive vector pen, highlighter, arrow, and text overlays.
- **PDF Reader**: Real-time rendering via Android `PdfRenderer`, page thumbnail grid, page bookmarking, and localized margin notes.

---

## 5. Responsive & Adaptive Layout Guidelines

NoteLand optimizes for multiple form factors via window size class handling:
- **Compact (<600dp)**: Standard vertical mobile phone layout with bottom navigation bar and collapsible drawers.
- **Medium (600dp–840dp)**: Foldables and small tablets; 2-column staggered grid for notes, responsive modal side-sheets.
- **Expanded (>840dp)**: Large tablets and Chromebooks; 3-column masonry grid, permanent navigation rail, dual-pane editor (notes list on the left, active document on the right).
