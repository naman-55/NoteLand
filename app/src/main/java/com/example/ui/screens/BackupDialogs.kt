package com.example.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.example.security.SecurityManager
import com.example.ui.components.ChangePinOverlay
import com.example.ui.viewmodel.NoteViewModel
import com.example.util.BackupPreview
import com.example.util.RestoreStrategy
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ChangePinBlurOverlay(
    onDismiss: () -> Unit,
    onUpdatePin: (String, String) -> Boolean,
    onVerifyCurrentPin: ((String) -> Boolean)? = null
) {
    ChangePinOverlay(
        onDismiss = onDismiss,
        onUpdatePin = onUpdatePin,
        onVerifyCurrentPin = onVerifyCurrentPin
    )
}

@Composable
fun AutoLockSelectionDialog(
    currentTimeoutMs: Long,
    onSelect: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Auto-Lock Vault") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    "Choose when the Locked Folder automatically re-locks after leaving or backgrounding:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                SecurityManager.AUTO_LOCK_OPTIONS_MS.forEach { optionMs ->
                    val selected = currentTimeoutMs == optionMs
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f) else Color.Transparent,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                onSelect(optionMs)
                                onDismiss()
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = if (selected) Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked,
                                contentDescription = null,
                                tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = SecurityManager.formatAutoLockLabel(optionMs),
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
        }
    )
}

@Composable
fun EditProfileDialog(
    initialName: String,
    initialEmail: String,
    onSave: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var nameInput by remember { mutableStateOf(initialName) }
    var emailInput by remember { mutableStateOf(initialEmail) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Profile") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    label = { Text("Name") },
                    placeholder = { Text("Enter your name") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("profile_name_input")
                )
                OutlinedTextField(
                    value = emailInput,
                    onValueChange = { emailInput = it },
                    label = { Text("Email") },
                    placeholder = { Text("Enter your email") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("profile_email_input")
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(nameInput, emailInput) },
                modifier = Modifier.testTag("save_profile_button")
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun ZipBackupExportDialog(
    viewModel: NoteViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var zipExportPassword by remember { mutableStateOf("") }
    var isZipExportEncrypted by remember { mutableStateOf(false) }
    var isExporting by remember { mutableStateOf(false) }

    val validatePassword: () -> String? = {
        val pwd = if (isZipExportEncrypted) zipExportPassword.trim() else null
        if (isZipExportEncrypted && (pwd == null || pwd.length < 4)) {
            Toast.makeText(context, "Password must be at least 4 characters", Toast.LENGTH_SHORT).show()
            null
        } else {
            pwd ?: ""
        }
    }

    val saveToFilesLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { destinationUri ->
        if (destinationUri == null) return@rememberLauncherForActivityResult
        val pwd = if (isZipExportEncrypted) zipExportPassword.trim() else null
        isExporting = true
        viewModel.exportBackupZipToUri(destinationUri, pwd) { res ->
            isExporting = false
            res.onSuccess { bytes ->
                Toast.makeText(context, "Backup exported to Files (${bytes / 1024} KB)", Toast.LENGTH_LONG).show()
                onDismiss()
            }.onFailure { err ->
                Toast.makeText(context, "Export failed: ${err.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    AlertDialog(
        onDismissRequest = { if (!isExporting) onDismiss() },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Archive, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text("Create Backup Archive")
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Exports all notes, folders, tags, stickies, canvases, attachments, and reminders into a standalone .zip archive with SHA-256 integrity checksums.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Encrypt with password", style = MaterialTheme.typography.bodyMedium)
                    Switch(
                        checked = isZipExportEncrypted,
                        enabled = !isExporting,
                        onCheckedChange = { isZipExportEncrypted = it }
                    )
                }

                if (isZipExportEncrypted) {
                    OutlinedTextField(
                        value = zipExportPassword,
                        onValueChange = { zipExportPassword = it },
                        label = { Text("Backup Password") },
                        placeholder = { Text("Minimum 4 characters") },
                        singleLine = true,
                        enabled = !isExporting,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        "Uses PBKDF2 (100k iterations) and AES-256-GCM. Save your password safely — backups cannot be decrypted without it.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Portable cross-device export options (SAF & FileProvider share)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedButton(
                        onClick = {
                            if (validatePassword() == null) return@OutlinedButton
                            val stamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
                            saveToFilesLauncher.launch("NoteLand_Backup_$stamp.zip")
                        },
                        enabled = !isExporting,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("backup_save_to_files_button")
                    ) {
                        Icon(Icons.Default.SaveAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Save to Files")
                    }

                    OutlinedButton(
                        onClick = {
                            val validated = validatePassword() ?: return@OutlinedButton
                            val pwd = validated.takeIf { it.isNotEmpty() }
                            isExporting = true
                            viewModel.exportBackupZip(
                                password = pwd,
                                onError = { err ->
                                    isExporting = false
                                    Toast.makeText(context, "Export failed: ${err.message}", Toast.LENGTH_LONG).show()
                                }
                            ) { file ->
                                isExporting = false
                                runCatching {
                                    val shareUri = FileProvider.getUriForFile(
                                        context,
                                        "${context.packageName}.fileprovider",
                                        file
                                    )
                                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "application/zip"
                                        putExtra(Intent.EXTRA_STREAM, shareUri)
                                        putExtra(Intent.EXTRA_SUBJECT, "NoteLand Backup Archive")
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(sendIntent, "Share Backup Archive"))
                                }.onFailure { err ->
                                    Toast.makeText(context, "Unable to share backup: ${err.message}", Toast.LENGTH_LONG).show()
                                }
                                onDismiss()
                            }
                        },
                        enabled = !isExporting,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("backup_share_zip_button")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Share ZIP")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = !isExporting,
                onClick = {
                    val validated = validatePassword() ?: return@Button
                    val pwd = validated.takeIf { it.isNotEmpty() }
                    isExporting = true
                    viewModel.exportBackupZip(
                        password = pwd,
                        onError = { err ->
                            isExporting = false
                            Toast.makeText(context, "Export failed: ${err.message}", Toast.LENGTH_LONG).show()
                        }
                    ) { file ->
                        isExporting = false
                        val sizeKb = file.length() / 1024
                        Toast.makeText(context, "Backup saved: ${file.name} (${sizeKb} KB)", Toast.LENGTH_LONG).show()
                        onDismiss()
                    }
                }
            ) {
                Text(if (isExporting) "Exporting…" else "Export ZIP")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isExporting) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun ZipBackupRestoreDialog(
    viewModel: NoteViewModel,
    availableBackups: List<File>,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedBackupFile by remember { mutableStateOf<File?>(null) }
    var zipRestorePassword by remember { mutableStateOf("") }
    var restorePreview by remember { mutableStateOf<BackupPreview?>(null) }
    var restoreStrategy by remember { mutableStateOf(RestoreStrategy.MERGE) }
    var showRestoreConfirmDialog by remember { mutableStateOf(false) }

    val importZipLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        viewModel.importBackupZipFromUri(uri) { importRes ->
            importRes.onSuccess { importedFile ->
                selectedBackupFile = importedFile
                viewModel.previewBackupZip(importedFile, null) { res ->
                    restorePreview = res.getOrNull()
                    if (res.isFailure) {
                        Toast.makeText(
                            context,
                            "Invalid backup archive: ${res.exceptionOrNull()?.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }.onFailure { err ->
                Toast.makeText(context, "Import failed: ${err.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Upload, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text("Restore Archive")
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                val selectedFile = selectedBackupFile
                if (selectedFile == null) {
                    Text(
                        "Import a .zip archive from Files or choose a local snapshot below:",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    OutlinedButton(
                        onClick = {
                            importZipLauncher.launch(
                                arrayOf(
                                    "application/zip",
                                    "application/x-zip-compressed",
                                    "application/octet-stream"
                                )
                            )
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("backup_import_from_files_button")
                    ) {
                        Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Import .zip from Device")
                    }

                    if (availableBackups.isEmpty()) {
                        Text(
                            "No local backup archives found in app storage yet. Import a .zip file or create a backup first.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(availableBackups) { file ->
                                Card(
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedBackupFile = file
                                            viewModel.previewBackupZip(file, null) { res ->
                                                restorePreview = res.getOrNull()
                                            }
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(file.name, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                                            Text("${file.length() / 1024} KB", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Text(
                        "File: ${selectedFile.name}",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )

                    if (restorePreview?.isEncrypted == true && restorePreview?.errorMessage != null) {
                        Text(
                            "Encrypted Backup: Enter password to decrypt and preview.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                        OutlinedTextField(
                            value = zipRestorePassword,
                            onValueChange = { zipRestorePassword = it },
                            label = { Text("Backup Password") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Button(
                            onClick = {
                                viewModel.previewBackupZip(selectedFile, zipRestorePassword.trim()) { res ->
                                    restorePreview = res.getOrNull()
                                    if (res.isFailure) {
                                        Toast.makeText(context, "Invalid password: ${res.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Verify & Preview")
                        }
                    } else if (restorePreview?.isValid == true) {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Integrity: Checksums verified (SHA-256)", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.primary)
                                Text("Schema: Version ${restorePreview?.schemaVersion} (${restorePreview?.appVersion})", style = MaterialTheme.typography.bodySmall)
                                Text("Contents: ${restorePreview?.notesCount} notes, ${restorePreview?.foldersCount} folders, ${restorePreview?.attachmentsCount} attachments", style = MaterialTheme.typography.bodySmall)
                            }
                        }

                        Text("Restore Strategy:", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { restoreStrategy = RestoreStrategy.MERGE },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                if (restoreStrategy == RestoreStrategy.MERGE) Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text("Merge with existing data", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                                Text("Keeps current notes, adds missing records", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { restoreStrategy = RestoreStrategy.REPLACE },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                if (restoreStrategy == RestoreStrategy.REPLACE) Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text("Replace all existing data", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.error)
                                Text("Wipes local SQLite data and restores exact snapshot", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    TextButton(
                        onClick = {
                            selectedBackupFile = null
                            restorePreview = null
                            zipRestorePassword = ""
                        }
                    ) {
                        Text("Choose Different File")
                    }
                }
            }
        },
        confirmButton = {
            val activeFile = selectedBackupFile
            if (activeFile != null && restorePreview?.isValid == true && restorePreview?.errorMessage == null) {
                Button(
                    onClick = {
                        if (restoreStrategy == RestoreStrategy.REPLACE) {
                            showRestoreConfirmDialog = true
                        } else {
                            viewModel.restoreBackupZip(
                                file = activeFile,
                                strategy = RestoreStrategy.MERGE,
                                password = if (restorePreview?.isEncrypted == true) zipRestorePassword.trim() else null
                            ) { res ->
                                res.onSuccess { result ->
                                    Toast.makeText(context, "Restored ${result.restoredNotesCount} notes and ${result.restoredAttachmentsCount} attachments!", Toast.LENGTH_LONG).show()
                                    onDismiss()
                                }.onFailure { err ->
                                    Toast.makeText(context, "Restore error: ${err.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    }
                ) {
                    Text("Restore Now")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )

    if (showRestoreConfirmDialog) {
        selectedBackupFile?.let { fileToReplace ->
        AlertDialog(
            onDismissRequest = { showRestoreConfirmDialog = false },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Replace All Data?") },
            text = {
                Text("This will permanently overwrite your current notes, canvases, and attachments with the contents of ${fileToReplace.name}. This operation cannot be undone.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showRestoreConfirmDialog = false
                        viewModel.restoreBackupZip(
                            file = fileToReplace,
                            strategy = RestoreStrategy.REPLACE,
                            password = if (restorePreview?.isEncrypted == true) zipRestorePassword.trim() else null
                        ) { res ->
                            res.onSuccess { result ->
                                Toast.makeText(context, "Replaced database with ${result.restoredNotesCount} notes and ${result.restoredAttachmentsCount} attachments!", Toast.LENGTH_LONG).show()
                                onDismiss()
                            }.onFailure { err ->
                                Toast.makeText(context, "Restore error: ${err.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Yes, Replace All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
        }
    }
}
