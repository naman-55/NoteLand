# NoteLand — Installation Guide

## 1. System Requirements

### Hardware Requirements
- **Architecture**: ARM64-v8a, armeabi-v7a, x86_64
- **RAM**: Minimum 2GB (4GB+ recommended for large PDF rendering and extensive canvas documents)
- **Free Storage**: 50MB for application binary; additional storage proportional to imported photos and PDFs.

### Software Requirements
- **Operating System**: Android 7.0 (API Level 24) or newer.
- **Recommended**: Android 12+ (API Level 31+) for Material You dynamic color theming and modern splash screen API.

---

## 2. Installation Options

### Option A: Sideloading the APK via ADB (Developers & Testers)
1. Ensure **USB Debugging** is enabled on your Android target device:
   - Go to **Settings** > **About Phone**.
   - Tap **Build Number** 7 times to enable Developer Options.
   - Go to **Settings** > **Developer Options** > Enable **USB Debugging**.
2. Connect your device via USB or WiFi ADB.
3. Verify connection in your terminal:
   ```bash
   adb devices
   ```
4. Install the NoteLand APK directly:
   ```bash
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```
5. Launch NoteLand from your device home screen or via terminal:
   ```bash
   adb shell am start -n com.aistudio.noteland.pkmx/com.example.MainActivity
   ```

---

### Option B: Direct Device Sideloading (End Users)
1. Download `noteland-v1.0.0.apk` onto your Android device (via browser download, USB transfer, or local network share).
2. Open the device's **Files** or **Downloads** app.
3. Tap on the `.apk` file.
4. If prompted with *"For your security, your phone is not allowed to install unknown apps from this source"*:
   - Tap **Settings** on the prompt.
   - Toggle **Allow from this source** to ON.
   - Return to the installer and tap **Install**.
5. Once installation finishes, tap **Open** to launch NoteLand.

---

## 3. Post-Installation & Permissions Setup

NoteLand is architected according to the Principle of Least Privilege:
- **No Internet Permission**: NoteLand does not request `<uses-permission android:name="android.permission.INTERNET" />`, guaranteeing offline privacy.
- **Zero Storage Permissions**: NoteLand uses the modern Android Photo Picker (`ActivityResultContracts.PickVisualMedia`) and Android Storage Access Framework (SAF), requiring zero broad media permissions (`READ_EXTERNAL_STORAGE`).
- **Notification Permission (Android 13+)**:
  - When you create your first reminder, Android will prompt: *"Allow NoteLand to send you notifications?"*
  - Tap **Allow** to enable timely reminder alerts.
- **Biometric Authentication (Optional)**:
  - If you wish to use fingerprint or face unlock for your private vault, ensure biometric credentials are registered in Android system settings.
