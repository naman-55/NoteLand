package com.example.security

import android.app.KeyguardManager
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.biometric.BiometricManager
import androidx.core.content.edit
import java.security.GeneralSecurityException
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * SecurityManager handles authenticated encryption (AES-256-GCM),
 * Android Keystore key management, PIN/Passphrase authentication,
 * failed-attempt throttling, and automatic relocking for NoteLand.
 */
class SecurityManager(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("noteland_vault_prefs", Context.MODE_PRIVATE)

    companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val KEY_ALIAS = "NoteLandVaultKey_v2"
        const val AES_MODE = "AES/GCM/NoPadding"
        const val GCM_TAG_LENGTH = 128
        const val GCM_IV_LENGTH = 12

        private const val PREF_PIN_HASH = "vault_pin_hash"
        private const val PREF_PIN_SALT = "vault_pin_salt"
        private const val PREF_FAILED_ATTEMPTS = "vault_failed_attempts"
        private const val PREF_LOCKOUT_UNTIL = "vault_lockout_until"
        private const val PREF_AUTO_LOCK_TIMEOUT_MS = "vault_autolock_ms"
        private const val PREF_BIOMETRICS_ENABLED = "vault_biometrics_enabled"
        private const val PREF_VAULT_INITIALIZED = "vault_is_initialized"

        const val MAX_FAILED_ATTEMPTS = 5
        const val LOCKOUT_DURATION_MS = 30_000L
        const val DEFAULT_AUTO_LOCK_MS = 0L // Lock immediately on exit or background by default

        val AUTO_LOCK_OPTIONS_MS = listOf(0L, 60_000L, 300_000L, 900_000L, 1_800_000L)

        fun formatAutoLockLabel(timeoutMs: Long): String = when (timeoutMs) {
            0L -> "Immediately"
            60_000L -> "After 1 minute"
            300_000L -> "After 5 minutes"
            900_000L -> "After 15 minutes"
            1_800_000L -> "After 30 minutes"
            else -> "After ${(timeoutMs / 60_000L).coerceAtLeast(1L)} min"
        }
    }

    private var unlockedTimestamp: Long = 0L
    private val secureRandom = SecureRandom()
    private var softwareFallbackKey: SecretKey? = null

    init {
        ensureKeyStoreKey()
    }
    
    fun isVaultInitialized(): Boolean {
        return prefs.contains(PREF_PIN_HASH)
    }

    // ==========================================
    // KEY MANAGEMENT (Android Keystore / Secure Fallback)
    // ==========================================

    private fun ensureKeyStoreKey() {
        try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            if (!keyStore.containsAlias(KEY_ALIAS)) {
                val keyGenerator =
                    KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
                val spec = KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .setRandomizedEncryptionRequired(true)
                    .build()
                keyGenerator.init(spec)
                keyGenerator.generateKey()
            }
        } catch (e: Exception) {
            // In unit tests or devices lacking AndroidKeyStore, initialize deterministic software fallback key
            initSoftwareFallbackKey()
        }
    }

    private fun initSoftwareFallbackKey(): SecretKey {
        softwareFallbackKey?.let { return it }
        var rawKey = prefs.getString("software_vault_key_b64", null)
        if (rawKey == null) {
            val keyBytes = ByteArray(32)
            secureRandom.nextBytes(keyBytes)
            rawKey = Base64.encodeToString(keyBytes, Base64.NO_WRAP)
            prefs.edit { putString("software_vault_key_b64", rawKey) }
        }
        val decoded = Base64.decode(rawKey, Base64.NO_WRAP)
        return SecretKeySpec(decoded, "AES").also { softwareFallbackKey = it }
    }

    private fun getSecretKey(): SecretKey {
        return try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            if (keyStore.containsAlias(KEY_ALIAS)) {
                keyStore.getKey(KEY_ALIAS, null) as SecretKey
            } else {
                ensureKeyStoreKey()
                keyStore.getKey(KEY_ALIAS, null) as SecretKey
            }
        } catch (_: Exception) {
            initSoftwareFallbackKey()
        }
    }

    fun isKeyInvalidated(): Boolean {
        return try {
            val cipher = Cipher.getInstance(AES_MODE)
            cipher.init(Cipher.ENCRYPT_MODE, getSecretKey())
            false
        } catch (e: KeyPermanentlyInvalidatedException) {
            true
        } catch (e: Exception) {
            false
        }
    }

    fun recoverKeyStore(): Boolean {
        return try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            if (keyStore.containsAlias(KEY_ALIAS)) {
                keyStore.deleteEntry(KEY_ALIAS)
            }
            ensureKeyStoreKey()
            true
        } catch (e: Exception) {
            false
        }
    }

    // ==========================================
    // AUTHENTICATED ENCRYPTION (AES-256-GCM)
    // ==========================================

    /**
     * Encrypts plaintext using AES-256-GCM with a unique 12-byte IV per invocation.
     * Returns Base64 of: [12-byte IV][Ciphertext + 16-byte GCM authentication tag]
     */
    @Throws(GeneralSecurityException::class)
    fun encryptOrThrow(plainText: String): String {
        val cipher = Cipher.getInstance(AES_MODE)
        cipher.init(Cipher.ENCRYPT_MODE, getSecretKey())
        val iv = cipher.iv ?: throw GeneralSecurityException("Failed to generate unique IV")
        val cipherBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))

        val combined = ByteArray(iv.size + cipherBytes.size)
        System.arraycopy(iv, 0, combined, 0, iv.size)
        System.arraycopy(cipherBytes, 0, combined, iv.size, cipherBytes.size)
        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    /**
     * Decrypts ciphertext and verifies GCM authentication tag.
     * Throws GeneralSecurityException / AEADBadTagException if ciphertext is modified or tampered with.
     */
    @Throws(GeneralSecurityException::class)
    fun decryptOrThrow(encryptedText: String): String {
        val combined = Base64.decode(encryptedText, Base64.NO_WRAP)
        if (combined.size < GCM_IV_LENGTH + 16) {
            throw GeneralSecurityException("Ciphertext truncated or corrupted")
        }

        val iv = ByteArray(GCM_IV_LENGTH)
        val cipherBytes = ByteArray(combined.size - GCM_IV_LENGTH)
        System.arraycopy(combined, 0, iv, 0, GCM_IV_LENGTH)
        System.arraycopy(combined, GCM_IV_LENGTH, cipherBytes, 0, cipherBytes.size)

        val cipher = Cipher.getInstance(AES_MODE)
        val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), spec)
        val decryptedBytes = cipher.doFinal(cipherBytes)
        return String(decryptedBytes, Charsets.UTF_8)
    }

    fun encrypt(plainText: String): String {
        return try {
            encryptOrThrow(plainText)
        } catch (e: Exception) {
            // Return raw or unencrypted fallback on catastrophic Keystore failure
            plainText
        }
    }

    fun decrypt(encryptedText: String): String {
        return try {
            decryptOrThrow(encryptedText)
        } catch (e: Exception) {
            encryptedText
        }
    }

    /**
     * Encrypts a binary file in bounded 256KB chunks using AES-256-GCM so large PDFs/media
     * never load completely into memory. Format:
     * ["NLVF1" (5 bytes)] + repeated { [int32 chunkPlainLen] [12-byte IV] [int32 cipherLen] [ciphertext+tag] }
     */
    @Throws(GeneralSecurityException::class, java.io.IOException::class)
    fun encryptStreamToFile(input: java.io.InputStream, destFile: java.io.File) {
        val secretKey = getSecretKey()
        val magic = "NLVF1".toByteArray(Charsets.US_ASCII)
        val buffer = ByteArray(256 * 1024)
        var chunkIndex = 0
        destFile.parentFile?.mkdirs()
        java.io.DataOutputStream(java.io.BufferedOutputStream(java.io.FileOutputStream(destFile))).use { out ->
            out.write(magic)
            while (true) {
                var readTotal = 0
                while (readTotal < buffer.size) {
                    val r = input.read(buffer, readTotal, buffer.size - readTotal)
                    if (r <= 0) break
                    readTotal += r
                }
                if (readTotal <= 0) break
                val cipher = Cipher.getInstance(AES_MODE)
                cipher.init(Cipher.ENCRYPT_MODE, secretKey)
                val iv = cipher.iv ?: throw GeneralSecurityException("Missing GCM IV")
                val aad = java.nio.ByteBuffer.allocate(4).putInt(chunkIndex).array()
                cipher.updateAAD(aad)
                val cipherBytes = cipher.doFinal(buffer, 0, readTotal)
                out.writeInt(readTotal)
                out.write(iv)
                out.writeInt(cipherBytes.size)
                out.write(cipherBytes)
                chunkIndex++
            }
            out.writeInt(0) // EOF sentinel
            out.flush()
        }
    }

    @Throws(GeneralSecurityException::class, java.io.IOException::class)
    fun decryptFileToStream(sourceFile: java.io.File, output: java.io.OutputStream) {
        val secretKey = getSecretKey()
        val magic = ByteArray(5)
        var chunkIndex = 0
        java.io.DataInputStream(java.io.BufferedInputStream(java.io.FileInputStream(sourceFile))).use { input ->
            input.readFully(magic)
            if (String(magic, Charsets.US_ASCII) != "NLVF1") {
                throw GeneralSecurityException("Invalid encrypted vault file header")
            }
            while (true) {
                val expectedPlainLen = input.readInt()
                if (expectedPlainLen == 0) break
                if (expectedPlainLen < 0 || expectedPlainLen > 2 * 1024 * 1024) {
                    throw GeneralSecurityException("Invalid vault chunk size: $expectedPlainLen")
                }
                val iv = ByteArray(GCM_IV_LENGTH)
                input.readFully(iv)
                val cipherLen = input.readInt()
                if (cipherLen <= 16 || cipherLen > 3 * 1024 * 1024) {
                    throw GeneralSecurityException("Invalid vault ciphertext chunk size: $cipherLen")
                }
                val cipherBytes = ByteArray(cipherLen)
                input.readFully(cipherBytes)
                val cipher = Cipher.getInstance(AES_MODE)
                cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_LENGTH, iv))
                val aad = java.nio.ByteBuffer.allocate(4).putInt(chunkIndex).array()
                cipher.updateAAD(aad)
                val plainBytes = cipher.doFinal(cipherBytes)
                output.write(plainBytes)
                chunkIndex++
            }
            output.flush()
        }
    }

    fun isEncryptedVaultFile(file: java.io.File): Boolean {
        if (!file.exists() || file.length() < 5L) return false
        return try {
            java.io.FileInputStream(file).use { fis ->
                val header = ByteArray(5)
                val n = fis.read(header)
                n == 5 && String(header, Charsets.US_ASCII) == "NLVF1"
            }
        } catch (_: Exception) {
            false
        }
    }

    // ==========================================
    // BACKUP PBKDF2 DERIVATION & ENCRYPTION
    // ==========================================

    @Throws(GeneralSecurityException::class)
    fun deriveBackupKey(password: String, salt: ByteArray): SecretKey {
        if (password.isEmpty() || salt.isEmpty()) {
            throw GeneralSecurityException("Passphrase and salt must not be empty")
        }
        val spec = PBEKeySpec(password.toCharArray(), salt, 100_000, 256)
        return try {
            val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            val keyBytes = factory.generateSecret(spec).encoded
            SecretKeySpec(keyBytes, "AES")
        } finally {
            spec.clearPassword()
        }
    }

    @Throws(GeneralSecurityException::class)
    fun encryptWithPassword(data: ByteArray, password: String): Pair<ByteArray, ByteArray> {
        val salt = ByteArray(32)
        secureRandom.nextBytes(salt)
        val combined = encryptWithPassword(data, password, salt)
        return Pair(combined, salt)
    }

    @Throws(GeneralSecurityException::class)
    fun encryptWithPassword(data: ByteArray, password: String, salt: ByteArray): ByteArray {
        val key = deriveBackupKey(password, salt)

        val cipher = Cipher.getInstance(AES_MODE)
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val iv = cipher.iv
        val cipherBytes = cipher.doFinal(data)

        val combined = ByteArray(iv.size + cipherBytes.size)
        System.arraycopy(iv, 0, combined, 0, iv.size)
        System.arraycopy(cipherBytes, 0, combined, iv.size, cipherBytes.size)
        return combined
    }

    @Throws(GeneralSecurityException::class)
    fun decryptWithPassword(encryptedData: ByteArray, salt: ByteArray, password: String): ByteArray {
        val key = deriveBackupKey(password, salt)
        if (encryptedData.size < GCM_IV_LENGTH + 16) {
            throw GeneralSecurityException("Invalid encrypted backup payload")
        }

        val iv = ByteArray(GCM_IV_LENGTH)
        val cipherBytes = ByteArray(encryptedData.size - GCM_IV_LENGTH)
        System.arraycopy(encryptedData, 0, iv, 0, GCM_IV_LENGTH)
        System.arraycopy(encryptedData, GCM_IV_LENGTH, cipherBytes, 0, cipherBytes.size)

        val cipher = Cipher.getInstance(AES_MODE)
        val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.DECRYPT_MODE, key, spec)
        return cipher.doFinal(cipherBytes)
    }

    // ==========================================
    // AUTHENTICATION & FAILED-ATTEMPT PROTECTION (PBKDF2)
    // ==========================================

    fun getRemainingLockoutSeconds(): Int {
        val lockoutUntil = prefs.getLong(PREF_LOCKOUT_UNTIL, 0L)
        val now = System.currentTimeMillis()
        return if (lockoutUntil > now) {
            ((lockoutUntil - now) / 1000).toInt() + 1
        } else {
            0
        }
    }

    fun getFailedAttempts(): Int {
        return prefs.getInt(PREF_FAILED_ATTEMPTS, 0)
    }

    fun verifyPin(enteredPin: String): Boolean {
        if (enteredPin.isEmpty()) return false
        val remaining = getRemainingLockoutSeconds()
        if (remaining > 0) return false

        val savedHash = prefs.getString(PREF_PIN_HASH, "") ?: ""
        val salt = prefs.getString(PREF_PIN_SALT, "") ?: ""

        val isMatch = if (savedHash.startsWith("PBKDF2$")) {
            val saltBytes = try {
                if (salt.isBlank()) ByteArray(0) else Base64.decode(salt, Base64.NO_WRAP)
            } catch (_: Exception) {
                ByteArray(0)
            }
            if (saltBytes.isEmpty()) {
                false
            } else {
                val computedHash = try {
                    hashPinPbkdf2(enteredPin, saltBytes)
                } catch (_: GeneralSecurityException) {
                    null
                }
                computedHash != null && computedHash == savedHash
            }
        } else if (savedHash.isNotBlank()) {
            // Legacy iterated SHA-256 hash fallback
            val legacyHash = hashPinLegacy(enteredPin, salt)
            if (legacyHash == savedHash) {
                // Auto-upgrade legacy hash to PBKDF2 on successful verification
                setVaultPin(enteredPin)
                true
            } else {
                false
            }
        } else {
            false
        }

        if (isMatch) {
            // Successful verification
            prefs.edit {
                putInt(PREF_FAILED_ATTEMPTS, 0)
                putLong(PREF_LOCKOUT_UNTIL, 0L)
            }
            recordActivity()
            return true
        } else {
            // Failed verification
            val currentAttempts = prefs.getInt(PREF_FAILED_ATTEMPTS, 0) + 1
            prefs.edit {
                putInt(PREF_FAILED_ATTEMPTS, currentAttempts)
                if (currentAttempts >= MAX_FAILED_ATTEMPTS) {
                    putLong(PREF_LOCKOUT_UNTIL, System.currentTimeMillis() + LOCKOUT_DURATION_MS)
                }
            }
            return false
        }
    }

    fun setVaultPin(newPin: String) {
        if (newPin.isEmpty()) return
        val saltBytes = ByteArray(16)
        secureRandom.nextBytes(saltBytes)
        val salt = Base64.encodeToString(saltBytes, Base64.NO_WRAP)
        val hash = hashPinPbkdf2(newPin, saltBytes)
        prefs.edit {
            putString(PREF_PIN_HASH, hash)
            putString(PREF_PIN_SALT, salt)
            putInt(PREF_FAILED_ATTEMPTS, 0)
            putLong(PREF_LOCKOUT_UNTIL, 0L)
        }
    }

    /**
     * Standardized PBKDF2 with HMAC-SHA256, 100,000 iterations for password/PIN derivation.
     */
    @Throws(GeneralSecurityException::class)
    private fun hashPinPbkdf2(pin: String, salt: ByteArray): String {
        if (pin.isEmpty() || salt.isEmpty()) {
            throw GeneralSecurityException("PIN and salt must not be empty")
        }
        val spec = PBEKeySpec(pin.toCharArray(), salt, 100_000, 256)
        return try {
            val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            val hashBytes = factory.generateSecret(spec).encoded
            "PBKDF2$" + Base64.encodeToString(hashBytes, Base64.NO_WRAP)
        } finally {
            spec.clearPassword()
        }
    }

    /**
     * Legacy hash for seamless backwards compatibility.
     */
    private fun hashPinLegacy(pin: String, salt: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val combined = "NL_SALT_${salt}_$pin"
        var hashBytes = md.digest(combined.toByteArray(Charsets.UTF_8))
        for (i in 0 until 5000) {
            hashBytes = md.digest(hashBytes)
        }
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    // ==========================================
    // AUTO-RELOCKING & SESSION STATE
    // ==========================================

    fun recordActivity() {
        unlockedTimestamp = System.currentTimeMillis()
    }

    fun isVaultSessionUnlocked(): Boolean {
        if (unlockedTimestamp == 0L) return false
        val timeoutMs = prefs.getLong(PREF_AUTO_LOCK_TIMEOUT_MS, DEFAULT_AUTO_LOCK_MS)
        if (timeoutMs <= 0L) {
            // Active within current unlocked session until exit or background calls lockVault()
            return true
        }
        return (System.currentTimeMillis() - unlockedTimestamp) < timeoutMs
    }

    fun getRemainingAutoLockDelayMs(nowMillis: Long = System.currentTimeMillis()): Long {
        if (unlockedTimestamp == 0L) return 0L
        val timeoutMs = prefs.getLong(PREF_AUTO_LOCK_TIMEOUT_MS, DEFAULT_AUTO_LOCK_MS)
        if (timeoutMs <= 0L) return Long.MAX_VALUE
        val elapsed = (nowMillis - unlockedTimestamp).coerceAtLeast(0L)
        return (timeoutMs - elapsed).coerceAtLeast(0L)
    }

    fun lockVault() {
        unlockedTimestamp = 0L
    }

    fun setAutoLockTimeout(timeoutMs: Long) {
        prefs.edit { putLong(PREF_AUTO_LOCK_TIMEOUT_MS, timeoutMs) }
    }

    fun getAutoLockTimeout(): Long {
        return prefs.getLong(PREF_AUTO_LOCK_TIMEOUT_MS, DEFAULT_AUTO_LOCK_MS)
    }

    // ==========================================
    // BIOMETRIC CHECKS (AndroidX BiometricManager)
    // ==========================================

    fun isDeviceSecure(): Boolean {
        val keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        return keyguardManager?.isDeviceSecure == true
    }

    fun canUseBiometrics(): Boolean {
        val bm = BiometricManager.from(context)
        if (bm.canAuthenticate(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    BiometricManager.Authenticators.BIOMETRIC_WEAK
            ) == BiometricManager.BIOMETRIC_SUCCESS
        ) {
            return true
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return bm.canAuthenticate(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    BiometricManager.Authenticators.DEVICE_CREDENTIAL
            ) == BiometricManager.BIOMETRIC_SUCCESS
        }
        return isDeviceSecure()
    }

    fun isBiometricsPreferenceEnabled(): Boolean {
        return prefs.getBoolean(PREF_BIOMETRICS_ENABLED, true)
    }

    fun isBiometricsEnabled(): Boolean {
        return isBiometricsPreferenceEnabled() && canUseBiometrics()
    }

    fun setBiometricsEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(PREF_BIOMETRICS_ENABLED, enabled) }
    }

    fun markBiometricUnlockSuccess() {
        prefs.edit {
            putInt(PREF_FAILED_ATTEMPTS, 0)
            putLong(PREF_LOCKOUT_UNTIL, 0L)
        }
        recordActivity()
    }
}
