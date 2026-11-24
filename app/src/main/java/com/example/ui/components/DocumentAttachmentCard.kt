package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.RestoreFromTrash
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AttachmentEntity
import com.example.ui.theme.editorialCardBorderColor
import com.example.ui.theme.editorialCardContainerColor
import com.example.ui.theme.isAppInDarkTheme
import com.example.util.SmartNoteUtils

fun AttachmentEntity.isPdfDocument(): Boolean {
    return fileExtension.equals("pdf", ignoreCase = true) ||
        mimeType.contains("pdf", ignoreCase = true)
}

fun AttachmentEntity.isImageDocument(): Boolean {
    return !isPdfDocument() && (
        mimeType.contains("image", ignoreCase = true) ||
            fileExtension.lowercase() in listOf("jpg", "jpeg", "png", "webp", "gif", "heic", "bmp")
        )
}

@Composable
fun DocumentAttachmentCard(
    attachment: AttachmentEntity,
    folderName: String = "",
    statusBadge: String? = null,
    onOpen: () -> Unit,
    onMoveToFolder: (() -> Unit)? = null,
    onRename: (() -> Unit)? = null,
    onToggleFavorite: (() -> Unit)? = null,
    onToggleArchive: (() -> Unit)? = null,
    onToggleLock: (() -> Unit)? = null,
    onSoftDelete: (() -> Unit)? = null,
    onRestore: (() -> Unit)? = null,
    onDeleteForever: (() -> Unit)? = null,
    onShowMetadata: (() -> Unit)? = null,
    onExport: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isDark = isAppInDarkTheme
    var showMenu by remember { mutableStateOf(false) }

    val isPdf = attachment.isPdfDocument()
    val isImage = attachment.isImageDocument()
    val accentColor = when {
        isPdf -> Color(0xFFDC2626)
        isImage -> Color(0xFF2563EB)
        else -> Color(0xFF0D9488)
    }
    val typeIcon = when {
        isPdf -> Icons.Default.PictureAsPdf
        isImage -> Icons.Default.Image
        else -> Icons.Default.Description
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = editorialCardContainerColor(isDark)),
        border = BorderStroke(0.8.dp, editorialCardBorderColor(isDark)),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isDark) 0.dp else 1.dp),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onOpen)
            .testTag("attachment_card_${attachment.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = accentColor.copy(alpha = 0.13f),
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = typeIcon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = attachment.fileName,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(2.dp))

                val metaLine = buildString {
                    append(attachment.fileExtension.uppercase().ifBlank { "FILE" })
                    if (isPdf && attachment.pageCount > 0) {
                        append(" · ${attachment.pageCount}p")
                    }
                    if (attachment.sizeBytes > 0L) {
                        append(" · ${SmartNoteUtils.formatFileSize(attachment.sizeBytes)}")
                    }
                    append(" · ${SmartNoteUtils.formatTimestamp(attachment.updatedAtUtc)}")
                }
                Text(
                    text = metaLine,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (folderName.isNotBlank() || !statusBadge.isNullOrBlank() || attachment.isLocked) {
                    Spacer(Modifier.height(5.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (folderName.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Folder,
                                        contentDescription = null,
                                        modifier = Modifier.size(11.dp),
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                    Text(
                                        text = folderName,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                        if (attachment.isLocked) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFD97706).copy(alpha = 0.16f)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Lock,
                                        contentDescription = null,
                                        modifier = Modifier.size(10.dp),
                                        tint = Color(0xFFD97706)
                                    )
                                    Text(
                                        text = "Encrypted",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFFD97706)
                                    )
                                }
                            }
                        }
                        if (!statusBadge.isNullOrBlank()) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.75f)
                            ) {
                                Text(
                                    text = statusBadge,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }

            if (onToggleFavorite != null && !attachment.isDeleted) {
                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = if (attachment.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = if (attachment.isFavorite) "Remove from Favourites" else "Add to Favourites",
                        tint = if (attachment.isFavorite) Color(0xFFD85C37) else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            if (onRestore != null && attachment.isDeleted) {
                IconButton(onClick = onRestore) {
                    Icon(
                        Icons.Default.RestoreFromTrash,
                        contentDescription = "Restore File",
                        tint = Color(0xFF10B981)
                    )
                }
            }

            if (onDeleteForever != null && attachment.isDeleted) {
                IconButton(onClick = onDeleteForever) {
                    Icon(
                        Icons.Default.DeleteForever,
                        contentDescription = "Delete Forever",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            val hasMenuItems = !attachment.isDeleted && (
                onMoveToFolder != null ||
                    onRename != null ||
                    onToggleArchive != null ||
                    onToggleLock != null ||
                    onSoftDelete != null ||
                    onShowMetadata != null ||
                    onExport != null
                )

            if (hasMenuItems) {
                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("attachment_menu_${attachment.id}")
                    ) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = "File options",
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Open") },
                            leadingIcon = { Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                onOpen()
                            }
                        )
                        if (onRename != null) {
                            DropdownMenuItem(
                                text = { Text("Rename") },
                                leadingIcon = { Icon(Icons.Default.DriveFileRenameOutline, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    onRename()
                                }
                            )
                        }
                        if (onMoveToFolder != null) {
                            DropdownMenuItem(
                                text = { Text("Move to Folder") },
                                leadingIcon = { Icon(Icons.AutoMirrored.Filled.DriveFileMove, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    onMoveToFolder()
                                }
                            )
                        }
                        if (onToggleArchive != null) {
                            DropdownMenuItem(
                                text = { Text(if (attachment.isArchived) "Unarchive" else "Archive") },
                                leadingIcon = {
                                    Icon(
                                        if (attachment.isArchived) Icons.Default.Unarchive else Icons.Default.Archive,
                                        contentDescription = null
                                    )
                                },
                                onClick = {
                                    showMenu = false
                                    onToggleArchive()
                                }
                            )
                        }
                        if (onToggleLock != null) {
                            DropdownMenuItem(
                                text = { Text(if (attachment.isLocked) "Unlock & Move Out" else "Lock in Vault") },
                                leadingIcon = {
                                    Icon(
                                        if (attachment.isLocked) Icons.Default.LockOpen else Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = Color(0xFFD97706)
                                    )
                                },
                                onClick = {
                                    showMenu = false
                                    onToggleLock()
                                }
                            )
                        }
                        if (onShowMetadata != null) {
                            DropdownMenuItem(
                                text = { Text("File Details") },
                                leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    onShowMetadata()
                                }
                            )
                        }
                        if (onExport != null) {
                            DropdownMenuItem(
                                text = { Text("Save Copy") },
                                leadingIcon = { Icon(Icons.Default.FileDownload, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    onExport()
                                }
                            )
                        }
                        if (onSoftDelete != null) {
                            DropdownMenuItem(
                                text = { Text("Move to Recycle Bin", color = MaterialTheme.colorScheme.error) },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                },
                                onClick = {
                                    showMenu = false
                                    onSoftDelete()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RenameDocumentDialog(
    initialName: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var nameInput by remember(initialName) { mutableStateOf(initialName) }
    val ext = initialName.substringAfterLast('.', "")
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename Document") },
        text = {
            OutlinedTextField(
                value = nameInput,
                onValueChange = { nameInput = it },
                label = { Text("File name") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("rename_attachment_input")
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val trimmed = nameInput.trim()
                    if (trimmed.isNotEmpty()) {
                        val finalName = if (ext.isNotBlank() && !trimmed.contains('.')) {
                            "$trimmed.$ext"
                        } else trimmed
                        onConfirm(finalName)
                    }
                },
                enabled = nameInput.trim().isNotEmpty(),
                modifier = Modifier.testTag("rename_attachment_confirm")
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
fun DocumentMetadataDialog(
    attachment: AttachmentEntity,
    folderPath: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Document Metadata") },
        text = {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MetadataInfoRow("Name", attachment.fileName)
                    MetadataInfoRow("Format", "${attachment.fileExtension.uppercase()} (${attachment.mimeType})")
                    MetadataInfoRow("Size", SmartNoteUtils.formatFileSize(attachment.sizeBytes))
                    if (attachment.isPdfDocument() && attachment.pageCount > 0) {
                        MetadataInfoRow("Pages", "${attachment.pageCount}")
                    }
                    MetadataInfoRow("Folder", folderPath.ifBlank { "Root Library" })
                    MetadataInfoRow(
                        "Protection",
                        if (attachment.isLocked) "AES-256-GCM Encrypted Vault" else "App-Private Storage"
                    )
                    if (attachment.contentHash.isNotBlank()) {
                        MetadataInfoRow("SHA-256", "${attachment.contentHash.take(16)}…")
                    }
                    MetadataInfoRow("Added", SmartNoteUtils.formatTimestamp(attachment.createdAtUtc))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun MetadataInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
