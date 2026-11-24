# NoteLand — Future Scope & Roadmap

This document outlines architectural and functional expansions planned for future major releases of NoteLand while maintaining its core commitments to privacy, offline-first reliability, and battery efficiency.

---

## 1. Roadmap Milestones

### Milestone 1: Peer-to-Peer Encrypted Synchronization (v1.1.0)
- **Zero-Cloud P2P Sync**: Implement local Wi-Fi / Bluetooth Low Energy (BLE) direct device-to-device synchronization (via Android Nearby Connections API) without routing data through intermediary cloud servers.
- **End-to-End Encryption (E2EE)**: Exchange ephemeral keys using ECDH (Elliptic Curve Diffie-Hellman) for zero-trust transfer of notes between personal devices (e.g., Android phone to tablet).
- **CRDT (Conflict-Free Replicated Data Types)**: Integrate state-based CRDTs (such as Yjs / Automerge algorithms) for automated merge resolution when editing notes offline across multiple devices.

---

### Milestone 2: Markdown & PKM Knowledge Graph (v1.2.0)
- **Bidirectional Wiki Links (`[[Note Title]]`)**: Support obsidian-style cross-referencing between notes with real-time autocompletion.
- **Interactive Knowledge Graph View**: Render a 2D force-directed graph (via Canvas physics simulation) visualizing clusters of interconnected concepts, tags, and folders.
- **Backlink Indexing**: Automated discovery and display of unlinked references and incoming note links in an expandable editor pane.

---

### Milestone 3: Advanced Media & Stylus Integration (v1.3.0)
- **Stylus & S-Pen Low-Latency API**: Utilize Android's `androidx.input.motionprediction` and low-latency graphics buffer pipelines for sub-10ms stylus latency on Samsung Galaxy Tab and Google Pixel Tablet.
- **Pressure & Tilt Sensitivity**: Dynamic brush stroke thickness based on stylus physical pressure and tilt angles.
- **Audio Voice Memos**: Native on-device audio recording with embedded wave visualization directly inside rich-text notes.

---

### Milestone 4: On-Device Hardware-Accelerated ML (v1.4.0)
- **On-Device OCR via ML Kit**: Offline text extraction from photographed whiteboards, receipts, and book pages with zero cloud reliance.
- **Smart Handwriting Recognition**: Convert freehand canvas writing into editable rich text using Google ML Kit Digital Ink Recognition.
- **Local Small Language Model (SLM)**: Integrate Android 15's on-device Gemini Nano via AICore for zero-network semantic summarization and auto-tagging on flagship NPU-equipped devices.

---

### Milestone 5: WebDAV / Nextcloud Self-Hosted Backup Sync (v1.5.0)
- **User-Controlled Cloud Storage**: Enable automated scheduled exports to user-owned WebDAV, Nextcloud, or Synology NAS servers.
- **Client-Side Encrypted Backups**: Automatic AES-256-GCM encryption of backup archives using user's master key before uploading to remote WebDAV endpoints.
