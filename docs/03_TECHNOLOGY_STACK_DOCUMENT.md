# NoteLand — Technology Stack Document

## 1. Core Platform & Runtime
- **Language**: Kotlin 2.0.21
- **Target OS**: Android 7.0+ (minSdk 24, targetSdk 36, compileSdk 36)
- **Runtime Environment**: Native Android JVM / ART
- **Build System**: Gradle 8.11.1 (Kotlin DSL - `.gradle.kts`) with AGP 8.8.0

## 2. Jetpack & UI Frameworks
- **UI Toolkit**: Jetpack Compose (BOM 2024.09.00)
- **Design System**: Material Design 3 (`androidx.compose.material3:material3`)
- **Navigation**: Jetpack Navigation Compose (`androidx.navigation:navigation-compose:2.8.1`)
- **Lifecycle & ViewModel**: `androidx.lifecycle:lifecycle-viewmodel-compose:2.8.5`, `lifecycle-runtime-compose`
- **Activity Integration**: `androidx.activity:activity-compose:1.9.2` (Edge-to-Edge window insets)

## 3. Data Storage & Concurrency
- **Database Engine**: SQLite via AndroidX Room (`androidx.room:room-runtime:2.6.1`, `room-ktx:2.6.1`)
- **Annotation Processor**: Kotlin Symbol Processing (KSP 2.0.21-1.0.28)
- **Concurrency**: Kotlin Coroutines (`kotlinx-coroutines-android:1.8.1`)
- **Reactive Streams**: Kotlin `Flow` & `StateFlow`
- **Data Serialization**: Android `org.json` and standard Java Streams

## 4. Hardware, Security & Media
- **Cryptography Engine**: `javax.crypto.Cipher` (AES/GCM/NoPadding, 256-bit key, 128-bit tag, 12-byte IV)
- **Key Store**: `AndroidKeyStore` provider
- **Password KDF**: `PBKDF2WithHmacSHA256` (100,000 iterations, 32-byte salt)
- **Device Security**: Android `KeyguardManager` / `BiometricManager`
- **Archive Engine**: `java.util.zip.ZipInputStream` / `ZipOutputStream`
- **Image Decoding & Vector Graphics**: Android `BitmapFactory`, Compose Canvas `DrawScope`
- **PDF Engine**: Android Native `android.graphics.pdf.PdfRenderer` (zero remote cloud API)
- **System Services**: `AlarmManager`, `NotificationManager`, `BroadcastReceiver`

## 5. Testing Frameworks
- **Unit Testing**: JUnit 4 (`junit:junit:4.13.2`)
- **JVM Android Simulation**: Robolectric (`org.robolectric:robolectric:4.14`)
- **Test Coroutines**: `kotlinx-coroutines-test:1.8.1`
- **AndroidX Test Core**: `androidx.test:core:1.6.1`, `androidx.test.ext:junit:1.2.1`
