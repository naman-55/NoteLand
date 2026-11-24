package com.example.security

import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/**
 * Wraps AndroidX BiometricPrompt & BiometricManager for Secure Vault authentication.
 */
object BiometricAuthHelper {

    sealed class Availability {
        data object BiometricReady : Availability()
        data object DeviceCredentialReady : Availability()
        data class Unavailable(val reason: String) : Availability()
    }

    tailrec fun Context.findFragmentActivity(): FragmentActivity? = when (this) {
        is FragmentActivity -> this
        is ContextWrapper -> baseContext.findFragmentActivity()
        else -> null
    }

    fun checkAvailability(context: Context): Availability {
        val biometricManager = BiometricManager.from(context)
        val biometricStatus = biometricManager.canAuthenticate(BIOMETRIC_STRONG or BIOMETRIC_WEAK)
        if (biometricStatus == BiometricManager.BIOMETRIC_SUCCESS) {
            return Availability.BiometricReady
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val credStatus = biometricManager.canAuthenticate(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)
            if (credStatus == BiometricManager.BIOMETRIC_SUCCESS) {
                return Availability.DeviceCredentialReady
            }
        }

        val reason = when (biometricStatus) {
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED ->
                "No fingerprint or face biometric enrolled in device Settings."
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE ->
                "This device does not have biometric hardware."
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE ->
                "Biometric sensor is currently unavailable."
            BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED ->
                "Biometric security update required on this device."
            else -> "Biometric authentication is unavailable. Use your Vault PIN."
        }
        return Availability.Unavailable(reason)
    }

    fun authenticate(
        context: Context,
        title: String = "Unlock Locked Folder",
        subtitle: String = "Authenticate with fingerprint, face, or device credential",
        negativeButtonText: String = "Use Vault PIN",
        onSuccess: () -> Unit,
        onFallbackToPin: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val activity = context.findFragmentActivity()
        if (activity == null) {
            onError("Unable to open biometric prompt from current window.")
            return
        }

        val availability = checkAvailability(activity)
        if (availability is Availability.Unavailable) {
            onError(availability.reason)
            return
        }

        val executor = ContextCompat.getMainExecutor(activity)
        val biometricPrompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    when (errorCode) {
                        BiometricPrompt.ERROR_NEGATIVE_BUTTON,
                        BiometricPrompt.ERROR_USER_CANCELED,
                        BiometricPrompt.ERROR_CANCELED -> onFallbackToPin()
                        else -> onError(errString.toString())
                    }
                }
            }
        )

        val promptBuilder = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setConfirmationRequired(false)

        when (availability) {
            is Availability.BiometricReady -> {
                promptBuilder
                    .setAllowedAuthenticators(BIOMETRIC_STRONG or BIOMETRIC_WEAK)
                    .setNegativeButtonText(negativeButtonText)
            }
            is Availability.DeviceCredentialReady -> {
                // DEVICE_CREDENTIAL cannot be combined with setNegativeButtonText
                promptBuilder.setAllowedAuthenticators(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)
            }
            is Availability.Unavailable -> Unit
        }

        biometricPrompt.authenticate(promptBuilder.build())
    }
}
