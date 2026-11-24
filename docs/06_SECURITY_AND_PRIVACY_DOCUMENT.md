# NoteLand — Security and Privacy Document (Audited & Hardened)

## 1. Executive Summary & Philosophy
NoteLand is architected around a **Zero-Trust, Local-First, Zero-Telemetry** security model. User notes, attachments, drawings, and metadata belong exclusively to the user on their physical hardware.

- **Zero Cloud Network Traffic**: Zero internet permissions declared in `AndroidManifest.xml`.
- **Zero Third-Party Telemetry**: No crash analytics, tracking SDKs, or external AI API calls.
- **Hardware-Backed Cryptography**: Authenticated encryption via AES-256-GCM backed by Android Keystore.
- **Standardized Key Derivation**: PBKDF2 with HMAC-SHA256 (100,000 iterations).

> **Disclaimer**: This document details the cryptographic safeguards and defensive implementation architectures incorporated into NoteLand. NoteLand does not claim formal third-party government or enterprise security certifications (e.g. FIPS 140-3, Common Criteria).

---

## 2. Threat Modeling & Hardened Defenses

| Threat Vector | Attack Scenario | Implemented Safeguard | Verification |
| :--- | :--- | :--- | :--- |
| **Physical Storage Extraction** | Attacker accesses raw SQLite file via root/ADB backup | Both `notes.title` and `notes.content` (and `checklistJson`) are stored as Base64 AES-256-GCM ciphertext. Zero plaintext in columns or SQLite indexes. | Verified via raw SQLite query test in `ExampleRobolectricTest` |
| **Bit-Flipping / Ciphertext Tampering** | Attacker manipulates bytes in ciphertext | AES-256-GCM enforces a 128-bit authentication tag. Tampered ciphertext immediately throws `GeneralSecurityException`/`AEADBadTagException` and rejects decryption. | Tested and verified |
| **PIN Brute-Force Guessing** | Rapid PIN trial attacks | 100,000 iterations of PBKDF2WithHmacSHA256 + exponential throttling + hard 30-second lockout after 5 consecutive failed attempts. | Tested and verified |
| **Search Leakage** | Searching keywords to reveal locked records | Search queries in `NoteDao.searchNotes` enforce `WHERE isLocked = 0 AND isHidden = 0`. Locked records are completely unindexed. Search history recording is suppressed when vault is unlocked. | Tested and verified |
| **Notification Drawer Leakage** | Scheduled reminders on locked notes displaying private titles | `ReminderReceiver` queries the note's `isLocked` flag; if locked, replaces title with `"🔒 Locked Note Reminder"` and content with `"Unlock NoteLand to view private reminder."` | Tested and verified |
| **Backgrounding Leakage** | App left open in background while unlocked | `MainActivity` monitors lifecycle `ON_STOP` and `ON_RESUME`. If auto-lock timeout expires or is set to immediate, the vault locks immediately and clears decrypted memory state. | Tested and verified |
| **Backup Archive Tampering** | Attacker modifies file entries inside export ZIP | SHA-256 checksums are verified per entry against `manifest.json`. Restore rejects altered entries, checksum mismatches, or unsupported schema versions. | Tested and verified |
| **Restore Crash / Interruption** | Restoration interrupted by process death | Restores execute inside atomic database transactions (`withTransaction`). Partial writes are completely rolled back to the prior valid state. | Tested and verified |

---

## 3. Cryptographic Implementation Details

### 3.1 Authenticated Encryption: AES-256-GCM
- **Cipher**: `AES/GCM/NoPadding`
- **Key Length**: 256 bits (32 bytes)
- **IV / Nonce**: 12 bytes (96 bits) randomly generated for **every single encryption invocation** via Android cryptographic provider.
- **Payload Format**: Base64 encoding of:
  `[12-byte IV][Ciphertext + 16-byte Authentication Tag]`
- **Authentication Tag**: 128 bits (16 bytes) generated and validated during `doFinal()`.

### 3.2 Standardized Password KDF: PBKDF2-HMAC-SHA256
- **Algorithm**: `PBKDF2WithHmacSHA256`
- **Iterations**: 100,000 rounds
- **Salt**: 16 cryptographically secure random bytes generated via `SecureRandom` per vault PIN setup.
- **Prefix Identifier**: Stored hashes use `PBKDF2$<base64_hash>` format.
- **Legacy Migration**: Legacy 5,000-round SHA-256 hashes are automatically verified and upgraded to PBKDF2 on first successful authentication.

### 3.3 Android Keystore Key Lifecycle
- **Key Store**: `AndroidKeyStore`
- **Key Alias**: `NoteLandVaultKey_v2`
- **Purpose**: `KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT`
- **Block Mode**: `KeyProperties.BLOCK_MODE_GCM`
- **Padding**: `KeyProperties.ENCRYPTION_PADDING_NONE`
- **Hardware Isolation**: Supported hardware-backed secure element / TEE (Trusted Execution Environment). Includes software fallback key generation for emulator/JVM unit testing environments.

---

## 4. Separation of Concepts: "Locked" vs "Hidden"

```
┌────────────────────────────────────────────────────────────────────────┐
│                                ALL NOTES                               │
├───────────────────────────────────┬────────────────────────────────────┤
│           PUBLIC NOTES            │            VAULT NOTES             │
│  - Displayed in notes list        ├──────────────────┬─────────────────┤
│  - Indexed in search queries      │   HIDDEN NOTES   │  LOCKED NOTES   │
│  - Plaintext at rest              │  - Unlisted      │  - Encrypted    │
│                                   │  - No PIN needed │  - Requires PIN │
│                                   │  - Plaintext     │  - AES-256-GCM  │
└───────────────────────────────────┴──────────────────┴─────────────────┘
```

1. **Hidden Notes (`isHidden = 1`)**:
   - Designed for visual decluttering.
   - Excluded from main notes list and default search results.
   - Stored in plaintext in SQLite (not encrypted).
2. **Locked Notes (`isLocked = 1`)**:
   - Designed for sensitive personal data.
   - **Both `title` and `content` are stored as AES-256-GCM ciphertext in SQLite.**
   - Excluded from notifications, previews, recents, and search indexing.
   - Requires biometric or PBKDF2 PIN authentication.
