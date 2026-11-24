package com.example.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.RestoreFromTrash
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.AttachmentEntity
import com.example.ui.components.DocumentAttachmentCard
import com.example.ui.components.EmptyStateView
import com.example.ui.theme.editorialBackdropBrush
import com.example.ui.theme.editorialCardBorderColor
import com.example.ui.theme.editorialCardContainerColor
import com.example.ui.theme.editorialHeaderTextColor
import com.example.ui.theme.isAppInDarkTheme
import com.example.ui.viewmodel.NoteViewModel
import com.example.util.SmartNoteUtils

private val TRASH_RETENTION_OPTIONS = listOf(
    7 to "7 days",
    14 to "14 days",
    30 to "30 days (Default)",
    90 to "90 days",
    -1 to "Never auto-delete"
)

private fun formatRetentionBadge(deletedAtUtc: Long?, retentionDays: Int): String? {
    if (retentionDays <= 0 || deletedAtUtc == null) return null
    val expiresAt = deletedAtUtc + retentionDays * 86_400_000L
    val remainingMs = (expiresAt - System.currentTimeMillis()).coerceAtLeast(0L)
    val remainingDays = ((remainingMs + 86_399_999L) / 86_400_000L).toInt().coerceAtLeast(0)
    return if (remainingDays <= 1) "Purges soon" else "$remainingDays d left"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecycleBinScreen(
    viewModel: NoteViewModel,
    onNavigateBack: () -> Unit
) {
    val deletedNotes by viewModel.deletedNotes.collectAsStateWithLifecycle()
    val deletedAttachments by viewModel.deletedAttachments.collectAsStateWithLifecycle()
    val retentionDays by viewModel.trashRetentionDays.collectAsStateWithLifecycle()
    val isVaultUnlocked by viewModel.isVaultUnlocked.collectAsStateWithLifecycle()

    var showEmptyConfirmDialog by remember { mutableStateOf(false) }
    var showRetentionMenu by remember { mutableStateOf(false) }
    var attachmentToPurge by remember { mutableStateOf<AttachmentEntity?>(null) }

    val totalTrashCount = deletedNotes.size + deletedAttachments.size
    val isDark = isAppInDarkTheme
    val headerTextColor = editorialHeaderTextColor(isDark)
    val retentionLabel = remember(retentionDays) {
        when (retentionDays) {
            7 -> "7 days"
            14 -> "14 days"
            30 -> "30 days"
            90 -> "90 days"
            else -> "Never"
        }
    }

    BackHandler {
        onNavigateBack()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(editorialBackdropBrush(isDark))
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(22.dp))
                            Spacer(Modifier.size(8.dp))
                            Text(
                                "Recycle Bin",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = headerTextColor
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = headerTextColor)
                        }
                    },
                    actions = {
                        Box {
                            IconButton(
                                onClick = { showRetentionMenu = true },
                                modifier = Modifier.testTag("recycle_retention_menu_button")
                            ) {
                                Icon(Icons.Default.Schedule, contentDescription = "Retention Policy", tint = headerTextColor)
                            }
                            DropdownMenu(
                                expanded = showRetentionMenu,
                                onDismissRequest = { showRetentionMenu = false }
                            ) {
                                TRASH_RETENTION_OPTIONS.forEach { (days, label) ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = label,
                                                fontWeight = if (retentionDays == days) FontWeight.Bold else FontWeight.Normal,
                                                color = if (retentionDays == days) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                            )
                                        },
                                        onClick = {
                                            viewModel.setTrashRetentionDays(days)
                                            showRetentionMenu = false
                                        },
                                        modifier = Modifier.testTag("retention_option_$days")
                                    )
                                }
                            }
                        }

                        if (totalTrashCount > 0) {
                            TextButton(
                                onClick = { showEmptyConfirmDialog = true },
                                modifier = Modifier.testTag("empty_recycle_bin_button")
                            ) {
                                Text("Empty Bin", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
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
            // Retention Policy Banner
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                border = BorderStroke(0.8.dp, editorialCardBorderColor(isDark)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            Icons.Default.Schedule,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = if (retentionDays > 0) {
                                "Auto-purges items after $retentionLabel"
                            } else {
                                "Auto-purge disabled (kept until emptied)"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    TextButton(
                        onClick = { showRetentionMenu = true },
                        modifier = Modifier.testTag("change_retention_policy_button")
                    ) {
                        Text("Change", fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            if (totalTrashCount == 0) {
                EmptyStateView(
                    icon = Icons.Default.Delete,
                    title = "Recycle Bin is empty",
                    description = "Deleted notes and documents can be safely restored or permanently purged from here.",
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (deletedAttachments.isNotEmpty()) {
                        item {
                            Text(
                                "Deleted Documents (${deletedAttachments.size})",
                                style = MaterialTheme.typography.titleSmall,
                                color = headerTextColor
                            )
                        }
                        items(deletedAttachments, key = { "trash_att_${it.id}" }) { att ->
                            val isMaskedLocked = att.isLocked && !isVaultUnlocked
                            val displayAtt = if (isMaskedLocked) {
                                att.copy(
                                    fileName = "Locked Document",
                                    pageCount = 0,
                                    sizeBytes = 0L
                                )
                            } else att
                            DocumentAttachmentCard(
                                attachment = displayAtt,
                                statusBadge = formatRetentionBadge(att.deletedAtUtc, retentionDays),
                                onOpen = { /* Stay read-only in Trash until restored */ },
                                onRestore = { viewModel.restoreAttachment(att) },
                                onDeleteForever = { attachmentToPurge = att }
                            )
                        }
                    }

                    if (deletedNotes.isNotEmpty()) {
                        item {
                            Text(
                                "Deleted Notes (${deletedNotes.size})",
                                style = MaterialTheme.typography.titleSmall,
                                color = headerTextColor,
                                modifier = Modifier.padding(top = if (deletedAttachments.isNotEmpty()) 6.dp else 0.dp)
                            )
                        }
                        items(deletedNotes, key = { it.id }) { note ->
                            val countdownBadge = formatRetentionBadge(note.deletedAtUtc, retentionDays)
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = editorialCardContainerColor(isDark)),
                                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                                border = BorderStroke(0.8.dp, editorialCardBorderColor(isDark)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (note.isLocked) "Locked Note" else note.title.ifBlank { "Untitled Note" },
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                        )
                                        Spacer(Modifier.height(4.dp))
                                        val deletedTime = note.deletedAtUtc
                                        val deletedLine = buildString {
                                            if (deletedTime != null) {
                                                append("Deleted: ${SmartNoteUtils.formatTimestamp(deletedTime)}")
                                            } else {
                                                append("Deleted")
                                            }
                                            if (!countdownBadge.isNullOrBlank()) {
                                                append(" · $countdownBadge")
                                            }
                                        }
                                        Text(
                                            text = deletedLine,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        IconButton(
                                            onClick = { viewModel.restoreNote(note) }
                                        ) {
                                            Icon(
                                                Icons.Default.RestoreFromTrash,
                                                contentDescription = "Restore",
                                                tint = Color(0xFF10B981)
                                            )
                                        }

                                        IconButton(
                                            onClick = { viewModel.deletePermanently(note.id) }
                                        ) {
                                            Icon(
                                                Icons.Default.DeleteForever,
                                                contentDescription = "Delete Forever",
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        }
    }

    attachmentToPurge?.let { targetAtt ->
        val safeTitle = if (targetAtt.isLocked && !isVaultUnlocked) "Locked Document" else targetAtt.fileName
        AlertDialog(
            onDismissRequest = { attachmentToPurge = null },
            title = { Text("Delete Document Forever?") },
            text = { Text("\"$safeTitle\" will be permanently removed from your device.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteAttachmentPermanently(targetAtt)
                        attachmentToPurge = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete Forever")
                }
            },
            dismissButton = {
                TextButton(onClick = { attachmentToPurge = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showEmptyConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showEmptyConfirmDialog = false },
            title = { Text("Empty Recycle Bin?") },
            text = {
                Text("All $totalTrashCount deleted item(s) will be permanently removed. This cannot be undone.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.emptyRecycleBin()
                        showEmptyConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Empty All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEmptyConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
