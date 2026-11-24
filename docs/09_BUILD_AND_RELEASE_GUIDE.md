# NoteLand — Build and Release Guide

## 1. Prerequisites
- **JDK**: Java Development Kit 17 (OpenJDK 17 recommended)
- **Android SDK**: Build Tools 36.0.0, Platform SDK 36 (Android 16)
- **Gradle**: Gradle 8.11.1+ (managed via root Gradle toolchain)
- **KSP Version**: 2.0.21-1.0.28 (strictly aligned with Kotlin 2.0.21)

---

## 2. Directory Structure & Key Files
- `app/build.gradle.kts`: Application level build script, dependencies, and signing configs.
- `gradle/libs.versions.toml`: Centralized Version Catalog.
- `metadata.json`: AI Studio platform application descriptor.

---

## 3. Assembling the Application

### 3.1 Debug Build
To compile and assemble the debug variant with active debug logs:
```bash
gradle :app:assembleDebug
```
**Artifact Location**:
`app/build/outputs/apk/debug/app-debug.apk`

---

### 3.2 Running Verification & Tests
Always run JVM unit and Robolectric tests prior to generating production artifacts:
```bash
gradle :app:testDebugUnitTest
```
Ensure 100% test pass rate across `ExampleRobolectricTest` and `NoteLandUnitTest`.

---

### 3.3 Release Build (Signed APK)
1. Set up signing credentials securely via environment variables (never commit keystores to version control):
   ```bash
   export NOTELAND_KEYSTORE_PATH="/secure/path/release.keystore"
   export NOTELAND_KEYSTORE_PASSWORD="yourKeystorePassword"
   export NOTELAND_KEY_ALIAS="noteland_key"
   export NOTELAND_KEY_PASSWORD="yourKeyPassword"
   ```
2. Build the production release APK:
   ```bash
   gradle :app:assembleRelease
   ```
**Artifact Location**:
`app/build/outputs/apk/release/app-release.apk`

---

### 3.4 Android App Bundle (AAB) for Google Play Store Distribution
Google Play requires publishing in the `.aab` format to enable dynamic feature splitting and optimized delivery:
```bash
gradle :app:bundleRelease
```
**Artifact Location**:
`app/build/outputs/bundle/release/app-release.aab`

---

## 4. Release Checklist

| Checkpoint | Requirement | Status |
| :--- | :--- | :--- |
| **Version Code** | Integer incremented (e.g., `1`) | Verified |
| **Version Name** | Semantic versioning string (e.g., `"1.0.0"`) | Verified |
| **Application ID** | Unique reverse domain (`com.aistudio.noteland.pkmx`) | Verified |
| **Min SDK** | Set to API 24 (Android 7.0) | Verified |
| **Target SDK** | Set to API 36 (Android 16) | Verified |
| **Adaptive Icon** | Valid mipmap vectors & drawables | Verified |
| **Splash Screen** | Android 12+ SplashScreen theme with window background | Verified |
| **Permissions** | Zero broad storage, zero internet permissions | Verified |
| **ProGuard / R8** | Enabled for release builds with keep rules for Room and Kotlinx Serialization | Configured |
