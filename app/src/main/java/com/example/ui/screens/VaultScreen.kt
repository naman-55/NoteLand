package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.example.data.model.AttachmentEntity
import com.example.security.BiometricAuthHelper
import com.example.security.SecurityManager
import com.example.ui.components.DocumentAttachmentCard
import com.example.ui.components.DocumentMetadataDialog
import com.example.ui.components.EmptyStateView
import com.example.ui.components.NoteCard
import com.example.ui.components.PinDialog
import com.example.ui.components.RenameDocumentDialog
import com.example.ui.components.isPdfDocument
import com.example.ui.navigation.Screen
import com.example.ui.theme.editorialBackdropBrush
import com.example.ui.theme.editorialHeaderSubColor
import com.example.ui.theme.editorialHeaderTextColor
import com.example.ui.theme.isAppInDarkTheme
import com.example.ui.viewmodel.NoteViewModel
import com.example.util.FolderHierarchyUtils
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultScreen(
    viewModel: NoteViewModel,
    onNavigateToEditor: (String, String) -> Unit,
    onNavigateToDestination: (String) -> Unit = {},
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val isVaultUnlocked by viewModel.isVaultUnlocked.collectAsStateWithLifecycle()
    val hiddenNotes by viewModel.hiddenNotes.collectAsStateWithLifecycle()
    val lockedNotes by viewModel.lockedNotes.collectAsStateWithLifecycle()
    val lockedAttachments by viewModel.lockedAttachments.collectAsStateWithLifecycle()
    val folders by viewModel.folders.collectAsStateWithLifecycle()
    val lockoutSeconds by viewModel.vaultLockoutSeconds.collectAsStateWithLifecycle()
    val failedAttempts by viewModel.failedUnlockAttempts.collectAsStateWithLifecycle()
    val biometricsEnabled by viewModel.biometricsEnabled.collectAsStateWithLifecycle()
    val autoLockTimeoutMs by viewModel.autoLockTimeoutMs.collectAsStateWithLifecycle()

    var attachmentToRename by remember { mutableStateOf<AttachmentEntity?>(null) }
    var attachmentMetadataTarget by remember { mutableStateOf<AttachmentEntity?>(null) }

    val biometricAvailability = remember(context) {
        BiometricAuthHelper.checkAvailability(context)
    }
    val canPromptBiometrics = biometricsEnabled &&
        biometricAvailability !is BiometricAuthHelper.Availability.Unavailable

    // Unified single Locked list combining encrypted & legacy hidden notes without two tabs
    val unifiedLockedNotes = remember(lockedNotes, hiddenNotes) {
        (lockedNotes + hiddenNotes)
            .distinctBy { it.id }
            .sortedByDescending { it.updatedAtUtc }
    }
    val totalLockedCount = unifiedLockedNotes.size + lockedAttachments.size

    var showPinDialog by remember { mutableStateOf(!isVaultUnlocked && lockoutSeconds == 0) }
    var showTimeoutMenu by remember { mutableStateOf(false) }
    var hasAttemptedAutoBiometric by remember(isVaultUnlocked) { mutableStateOf(false) }

    val triggerBiometricPrompt = {
        BiometricAuthHelper.authenticate(
            context = context,
            title = "Unlock Locked Folder",
            subtitle = "Use your fingerprint, face, or device lock to view locked notes",
            negativeButtonText = "Use Vault PIN",
            onSuccess = {
                viewModel.unlockVaultWithBiometrics()
                showPinDialog = false
            },
            onFallbackToPin = {
                showPinDialog = true
            },
            onError = { err ->
                Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                showPinDialog = true
            }
        )
    }

    // Enforce user's auto-lock setting on background / resume
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                viewModel.handleVaultExitOrBackground()
            } else if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.checkAutoLockExpiration()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Ensure passcode prompt & BiometricPrompt engage whenever locked
    LaunchedEffect(isVaultUnlocked, lockoutSeconds, canPromptBiometrics) {
        if (!isVaultUnlocked && lockoutSeconds == 0) {
            showPinDialog = true
            if (canPromptBiometrics && !hasAttemptedAutoBiometric) {
                hasAttemptedAutoBiometric = true
                triggerBiometricPrompt()
            }
        }
    }

    // Periodic countdown timer for lockout (only while foregrounded)
    LaunchedEffect(lockoutSeconds, lifecycleOwner) {
        if (lockoutSeconds > 0) {
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                delay(1000)
                viewModel.refreshLockoutStatus()
            }
        }
    }

    val exitRespectingAutoLock = {
        viewModel.handleVaultExitOrBackground()
        onNavigateBack()
    }

    BackHandler {
        exitRespectingAutoLock()
    }

    val isPasscodeOverlayVisible = showPinDialog && lockoutSeconds == 0 && !isVaultUnlocked
    val isDark = isAppInDarkTheme
    val headerTextColor = editorialHeaderTextColor(isDark)
    val headerSubColor = editorialHeaderSubColor(isDark)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(editorialBackdropBrush(isDark))
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            modifier = Modifier
                .fillMaxSize()
                .then(if (isPasscodeOverlayVisible) Modifier.blur(28.dp) else Modifier),
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = if (isVaultUnlocked) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        if (isVaultUnlocked) Icons.Default.LockOpen else Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = if (isVaultUnlocked) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(
                                    "Locked Folder",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = headerTextColor
                                )
                                Text(
                                    if (isVaultUnlocked) {
                                        val lockSummary = if (autoLockTimeoutMs <= 0L) {
                                            "Locks immediately"
                                        } else {
                                            "Auto-locks ${SecurityManager.formatAutoLockLabel(autoLockTimeoutMs).lowercase()}"
                                        }
                                        "$totalLockedCount locked · $lockSummary"
                                    } else {
                                        "PIN & Biometric Protected"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = headerSubColor
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = exitRespectingAutoLock,
                            modifier = Modifier.testTag("vault_back_button")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = headerTextColor)
                        }
                    },
                    actions = {
                        if (isVaultUnlocked) {
                            Box {
                                IconButton(
                                    onClick = { showTimeoutMenu = true },
                                    modifier = Modifier.testTag("vault_autolock_menu_button")
                                ) {
                                    Icon(Icons.Default.Timer, contentDescription = "Auto-Lock Timeout", tint = headerTextColor)
                                }
                                DropdownMenu(
                                    expanded = showTimeoutMenu,
                                    onDismissRequest = { showTimeoutMenu = false }
                                ) {
                                    SecurityManager.AUTO_LOCK_OPTIONS_MS.forEach { optionMs ->
                                        val selected = autoLockTimeoutMs == optionMs
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = SecurityManager.formatAutoLockLabel(optionMs),
                                                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                                                )
                                            },
                                            trailingIcon = if (selected) {
                                                {
                                                    Icon(
                                                        Icons.Default.Check,
                                                        contentDescription = "Selected",
                                                        tint = MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                            } else {
                                                null
                                            },
                                            onClick = {
                                                viewModel.setAutoLockTimeout(optionMs)
                                                showTimeoutMenu = false
                                            }
                                        )
                                    }
                                }
                            }
                            IconButton(
                                onClick = { viewModel.lockVault() },
                                modifier = Modifier.testTag("lock_vault_now_button")
                            ) {
                                Icon(Icons.Default.Lock, contentDescription = "Lock Folder Now", tint = headerTextColor)
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = Color.Transparent
                    )
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (!isVaultUnlocked) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = if (lockoutSeconds > 0) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer,
                                modifier = Modifier.size(80.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        if (lockoutSeconds > 0) Icons.Default.Warning else Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = if (lockoutSeconds > 0) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.size(40.dp)
                                    )
                                }
                            }

                            Text(
                                if (lockoutSeconds > 0) "Locked Out" else "Folder is Locked",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                            )

                            if (lockoutSeconds > 0) {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(14.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            "Too many failed PIN attempts.",
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                            color = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            "Throttled for security. Retry in $lockoutSeconds seconds.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                    }
                                }
                            } else {
                                Text(
                                    "Locked notes are encrypted at rest with AES-256-GCM and stay hidden from search and lists.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )

                                if (failedAttempts > 0) {
                                    Text(
                                        "Failed unlock attempts: $failedAttempts / 5",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Button(
                                        onClick = { showPinDialog = true },
                                        modifier = Modifier.testTag("unlock_vault_button")
                                    ) {
                                        Icon(Icons.Default.Key, contentDescription = null)
                                        Spacer(Modifier.size(8.dp))
                                        Text("Enter PIN")
                                    }

                                    if (canPromptBiometrics) {
                                        OutlinedButton(
                                            onClick = triggerBiometricPrompt,
                                            modifier = Modifier.testTag("biometric_unlock_button")
                                        ) {
                                            Icon(Icons.Default.Fingerprint, contentDescription = null)
                                            Spacer(Modifier.size(8.dp))
                                            Text("Biometrics")
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Single unified Locked Folder list
                    if (totalLockedCount == 0) {
                        EmptyStateView(
                            icon = Icons.Default.LockOpen,
                            title = "No Locked Items",
                            description = "Lock any note or document from its menu to encrypt it inside your Locked Folder.",
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            item {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)),
                                    border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.Info,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(Modifier.width(10.dp))
                                        val lockMsg = if (autoLockTimeoutMs <= 0L) {
                                            "Viewing encrypted notes & documents. Folder locks immediately when you navigate away or background the app."
                                        } else {
                                            "Viewing encrypted notes & documents. Auto-lock is set to ${SecurityManager.formatAutoLockLabel(autoLockTimeoutMs).lowercase()}."
                                        }
                                        Text(
                                            lockMsg,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    }
                                }
                            }

                            if (lockedAttachments.isNotEmpty()) {
                                item {
                                    Text(
                                        "Locked Documents (${lockedAttachments.size})",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = headerTextColor
                                    )
                                }
                                items(lockedAttachments, key = { "vault_att_${it.id}" }) { att ->
                                    val vaultFile = viewModel.attachmentStorageManager.getFileFromRelativePath(att.relativePath)
                                    DocumentAttachmentCard(
                                        attachment = att,
                                        onOpen = {
                                            val route = if (att.isPdfDocument()) {
                                                Screen.PdfReader.createRoute(att.id, vaultFile.absolutePath, att.fileName)
                                            } else {
                                                Screen.ImageAnnotator.createRoute(att.id, vaultFile.absolutePath, att.fileName)
                                            }
                                            onNavigateToDestination(route)
                                        },
                                        onRename = { attachmentToRename = att },
                                        onToggleLock = {
                                            viewModel.toggleAttachmentLock(att) { res ->
                                                res.onSuccess {
                                                    Toast.makeText(context, "Moved \"${att.fileName}\" out of Locked Folder", Toast.LENGTH_SHORT).show()
                                                }.onFailure { err ->
                                                    Toast.makeText(context, err.message ?: "Unable to unlock document", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        },
                                        onShowMetadata = { attachmentMetadataTarget = att },
                                        onSoftDelete = { viewModel.softDeleteAttachment(att) }
                                    )
                                }
                            }

                            if (unifiedLockedNotes.isNotEmpty()) {
                                if (lockedAttachments.isNotEmpty()) {
                                    item {
                                        Text(
                                            "Locked Notes (${unifiedLockedNotes.size})",
                                            style = MaterialTheme.typography.titleSmall,
                                            color = headerTextColor,
                                            modifier = Modifier.padding(top = 6.dp)
                                        )
                                    }
                                }
                                items(unifiedLockedNotes, key = { it.id }) { note ->
                                    val displayNote = if (note.isLocked) {
                                        val decryptedContent = viewModel.securityManager.decrypt(note.content)
                                        val decryptedTitle = viewModel.securityManager.decrypt(note.title)
                                        val decryptedChecklist = if (note.checklistJson.isNotBlank()) {
                                            viewModel.securityManager.decrypt(note.checklistJson)
                                        } else {
                                            ""
                                        }
                                        note.copy(
                                            title = decryptedTitle,
                                            content = decryptedContent,
                                            checklistJson = decryptedChecklist,
                                            isLocked = false
                                        )
                                    } else {
                                        note
                                    }

                                    NoteCard(
                                        note = displayNote,
                                        onClick = { onNavigateToEditor(note.id, note.noteType) },
                                        onTogglePin = { viewModel.togglePin(note) },
                                        onToggleFavorite = { viewModel.toggleFavorite(note) },
                                        onToggleArchive = { viewModel.toggleArchive(note) },
                                        onDelete = { viewModel.softDeleteNote(note) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Full-screen blurred passcode overlay
        if (isPasscodeOverlayVisible) {
            val isSetup = !viewModel.isVaultInitialized()
            PinDialog(
                title = if (isSetup) "Setup Vault PIN" else "Unlock Locked Folder",
                message = if (isSetup) "Create a secure 4-digit PIN for your locked items" else "Enter your 4-digit PIN to access your locked items",
                showDefaultPinHint = false,
                onConfirm = { pin ->
                    if (isSetup) {
                        viewModel.updateVaultPin("", pin)
                        val ok = viewModel.unlockVault(pin)
                        if (ok) showPinDialog = false
                        ok
                    } else {
                        val ok = viewModel.unlockVault(pin)
                        if (ok) showPinDialog = false
                        ok
                    }
                },
                onDismiss = {
                    showPinDialog = false
                    exitRespectingAutoLock()
                },
                onBiometricClick = if (canPromptBiometrics) triggerBiometricPrompt else null
            )
        }
    }

    attachmentToRename?.let { targetAtt ->
        RenameDocumentDialog(
            initialName = targetAtt.fileName,
            onConfirm = { newName ->
                viewModel.renameAttachment(targetAtt.id, newName)
                attachmentToRename = null
            },
            onDismiss = { attachmentToRename = null }
        )
    }

    attachmentMetadataTarget?.let { targetAtt ->
        DocumentMetadataDialog(
            attachment = targetAtt,
            folderPath = FolderHierarchyUtils.getFolderPathString(folders, targetAtt.folderId),
            onDismiss = { attachmentMetadataTarget = null }
        )
    }
}
