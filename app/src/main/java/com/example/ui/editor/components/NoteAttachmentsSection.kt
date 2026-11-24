package com.example.ui.editor.components

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.core.graphics.createBitmap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.AttachmentEntity
import com.example.util.SmartNoteUtils
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun NoteAttachmentsSection(
    attachments: List<AttachmentEntity>,
    resolveFile: (AttachmentEntity) -> File,
    isReadOnly: Boolean,
    accentColor: Color,
    isDark: Boolean,
    onOpenAttachment: (AttachmentEntity) -> Unit,
    onDownloadAttachment: (AttachmentEntity) -> Unit,
    onRemoveAttachment: (AttachmentEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    if (attachments.isEmpty()) return

    var textPreviewAttachment by remember { mutableStateOf<Pair<AttachmentEntity, String>?>(null) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .testTag("note_attachments_section"),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.AttachFile,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = "Attachments (${attachments.size})",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        attachments.forEach { attachment ->
            val file = remember(attachment.relativePath) { resolveFile(attachment) }
            val ext = attachment.fileExtension.lowercase()
            val isImage = attachment.mimeType.startsWith("image/") || ext in setOf("png", "jpg", "jpeg", "webp", "gif")
            val isPdf = attachment.mimeType.contains("pdf", ignoreCase = true) || ext == "pdf"
            val isTextPreviewable = ext in setOf("txt", "md", "csv", "json", "xml", "log") ||
                attachment.mimeType.startsWith("text/")

            val typeLabel = when {
                isPdf -> "PDF Document"
                isImage -> "Image · ${ext.uppercase().ifBlank { "IMG" }}"
                ext.isNotBlank() -> "${ext.uppercase()} Document"
                else -> attachment.mimeType.substringAfterLast('/').uppercase().ifBlank { "File" }
            }

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = if (isDark) MaterialTheme.colorScheme.surfaceContainerLow else Color.White.copy(alpha = 0.92f),
                border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable {
                        val isVaultLockedFile = attachment.isLocked || attachment.relativePath.contains("vault_files/")
                        if (isTextPreviewable && !isVaultLockedFile && file.exists() && file.length() <= 256_000L) {
                            val snippet = runCatching { file.readText().take(8_000) }.getOrDefault("")
                            textPreviewAttachment = attachment to snippet
                        } else {
                            onOpenAttachment(attachment)
                        }
                    }
                    .testTag("attachment_card_${attachment.id}")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    AttachmentThumbnail(
                        file = file,
                        isImage = isImage,
                        isPdf = isPdf,
                        ext = ext,
                        accentColor = accentColor,
                        isDark = isDark
                    )

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = attachment.fileName,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "$typeLabel · ${SmartNoteUtils.formatFileSize(attachment.sizeBytes)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    IconButton(
                        onClick = { onDownloadAttachment(attachment) },
                        modifier = Modifier
                            .size(40.dp)
                            .testTag("download_attachment_${attachment.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "Download ${attachment.fileName}",
                            tint = accentColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    if (!isReadOnly) {
                        IconButton(
                            onClick = { onRemoveAttachment(attachment) },
                            modifier = Modifier
                                .size(40.dp)
                                .testTag("remove_attachment_${attachment.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Remove ${attachment.fileName}",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    textPreviewAttachment?.let { (att, textBody) ->
        AlertDialog(
            onDismissRequest = { textPreviewAttachment = null },
            title = {
                Text(
                    text = att.fileName,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            text = {
                Text(
                    text = textBody.ifBlank { "(Empty document)" },
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 18.sp
                    ),
                    maxLines = 22,
                    overflow = TextOverflow.Ellipsis
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val target = att
                    textPreviewAttachment = null
                    onDownloadAttachment(target)
                }) {
                    Text("Download")
                }
            },
            dismissButton = {
                TextButton(onClick = { textPreviewAttachment = null }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
private fun AttachmentThumbnail(
    file: File,
    isImage: Boolean,
    isPdf: Boolean,
    ext: String,
    accentColor: Color,
    isDark: Boolean
) {
    var pdfBitmap by remember(file.absolutePath, isPdf) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(file.absolutePath, isPdf) {
        if (isPdf && file.exists()) {
            pdfBitmap = withContext(Dispatchers.IO) {
                renderPdfFirstPageThumbnail(file)
            }
        }
    }

    val badgeBg = when {
        isPdf -> if (isDark) Color(0xFF3A1D1F) else Color(0xFFFEE2E2)
        isImage -> accentColor.copy(alpha = 0.14f)
        else -> if (isDark) Color(0xFF17283B) else Color(0xFFDBEAFE)
    }
    val badgeTint = when {
        isPdf -> if (isDark) Color(0xFFF87171) else Color(0xFFDC2626)
        isImage -> accentColor
        else -> if (isDark) Color(0xFF60A5FA) else Color(0xFF2563EB)
    }

    Box(
        modifier = Modifier
            .size(46.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(badgeBg),
        contentAlignment = Alignment.Center
    ) {
        when {
            isImage && file.exists() -> {
                AsyncImage(
                    model = file,
                    contentDescription = "Image attachment preview",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            isPdf -> {
                val bmp = pdfBitmap
                if (bmp != null) {
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = "PDF preview",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.PictureAsPdf,
                        contentDescription = "PDF file",
                        tint = badgeTint,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            isImage -> {
                Icon(
                    imageVector = Icons.Default.Image,
                    contentDescription = "Image file",
                    tint = badgeTint,
                    modifier = Modifier.size(24.dp)
                )
            }
            else -> {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = "Document file",
                        tint = badgeTint,
                        modifier = Modifier.size(20.dp)
                    )
                    if (ext.isNotBlank()) {
                        Text(
                            text = ext.take(4).uppercase(),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = badgeTint
                        )
                    }
                }
            }
        }
    }
}

private fun renderPdfFirstPageThumbnail(file: File): Bitmap? {
    return try {
        ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
            PdfRenderer(pfd).use { renderer ->
                if (renderer.pageCount <= 0) return null
                renderer.openPage(0).use { page ->
                    val targetW = 128
                    val targetH = ((page.height.toFloat() / page.width.coerceAtLeast(1)) * targetW)
                        .toInt()
                        .coerceIn(96, 180)
                    val bmp = createBitmap(targetW, targetH)
                    bmp.eraseColor(android.graphics.Color.WHITE)
                    page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    bmp
                }
            }
        }
    } catch (_: Throwable) {
        null
    }
}
