# Noteland 3.5 Security Report

## Authentication and Protection

### 1. Vault Auto-Lock Engine
- Configurable timeout implemented: Immediate, 1, 5, 15, or 30 minutes. 
- Integrated securely with Android `LifecycleEventObserver`. App transition to `ON_STOP` halts exposure; `ON_RESUME` calculates elapsed time and actively forces the user to the Lock Screen if the threshold is surpassed.

### 2. External File Sandboxing
- Implemented `ExternalDocumentHandler` to intercept `ACTION_SEND` and `ACTION_VIEW` Intents.
- Prevents Path Traversal attacks by utilizing Android's strict `ContentResolver.openInputStream` pattern.
- Validates payload headers (MIME checks, suffix checks) before allowing files into the NoteLand internal file cache (`context.filesDir`).

### 3. Biometrics
- Standardized AndroidX Biometrics Prompt fallback (`DeviceCredentialReady` vs `BiometricReady`).

### 4. Backup Encryption
- ZIP export backups maintain strict byte-encryption routines protecting locked notes. Restorations require the user to explicitly decrypt.