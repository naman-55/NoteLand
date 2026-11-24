# NoteLand — Known Limitations (Audited & Hardened)

While NoteLand delivers a comprehensive, highly responsive, and robust offline knowledge management experience, the following architectural and hardware boundaries are explicitly documented:

---

## 1. Operating System & Hardware Boundaries

### 1.1 Local Storage Size Limits
- NoteLand saves all notes, vector strokes, and attachments directly into the application's internal sandbox (`context.filesDir`).
- The maximum storage capacity is bounded strictly by the physical flash storage available on the user's Android device. If a device has insufficient space, importing large multi-gigabyte video or PDF files may fail with an `IOException: No space left on device`. NoteLand handles this gracefully with error snackbars.

### 1.2 Android PDF Rendering (Memory Constraints)
- NoteLand utilizes the built-in Android `android.graphics.pdf.PdfRenderer`.
- For exceptionally large PDF documents (e.g., 500+ scanned pages with high-resolution bitmaps), memory usage scales with rendered bitmap resolution. NoteLand recycles page bitmaps immediately upon scrolling, but older 32-bit devices with strict 192MB per-app JVM heaps may experience performance degradation during rapid page-turning.

### 1.3 Exact Alarms & OEM Battery Optimization
- While NoteLand uses `AlarmManager.setExactAndAllowWhileIdle()` for reminder alerts, aggressive OEM battery management implementations (such as Xiaomi MIUI/HyperOS, Huawei EMUI, Samsung OneUI background killing) may delay local notifications unless the user exempts NoteLand from battery restrictions in system settings.

---

## 2. Security & Database Design Boundaries

### 2.1 Full-Text Search in Locked Notes (Deliberate Security Design)
- By deliberate security design, **Locked Notes are excluded from SQLite full-text search index tables and normal search queries**.
- Because `title` and `content` are stored as AES-256-GCM ciphertext in the SQLite database, SQL pattern matching (`LIKE '%query%'`) cannot match encrypted text without first decrypting it into memory. This is a foundational privacy principle to prevent leakage of sensitive information through search caches, index files, or memory dumps.

### 2.2 Vector Canvas Performance on Low-End Devices
- The infinite vector canvas re-renders paths via Compose `Canvas`.
- Documents containing more than 2,000 distinct freehand stroke segments may experience frame drops on entry-level Android devices without GPU hardware acceleration enabled. NoteLand optimizes this through path simplification, but complex CAD-level illustrations are outside the design scope of NoteLand's note-taking whiteboard.

### 2.3 Cloud Sync & Collaboration
- NoteLand is deliberately architected as an **offline-first, zero-cloud application**.
- There is no real-time multi-user collaborative editing, cloud web sync, or WebSocket relay. Users must transfer backup ZIP files manually to share or migrate data between devices.
