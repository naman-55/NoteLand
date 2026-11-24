package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.util.LruCache
import androidx.core.graphics.createBitmap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.NoteAdd
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.StickyNote2
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BookmarkEntity
import com.example.data.model.FolderEntity
import com.example.data.model.NoteEntity
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PdfReaderSettingsSheet(
    fileName: String,
    totalPages: Int,
    allAnnotations: List<PdfPageAnnotation>,
    activeTheme: ReadingTheme,
    isTtsPlaying: Boolean,
    bookmarkCount: Int,
    saveStatus: PdfSaveStatus,
    isAutoSaveEnabled: Boolean,
    hasUnsavedChanges: Boolean,
    onManualSave: () -> Unit,
    onToggleAutoSave: (Boolean) -> Unit,
    onSelectTheme: (ReadingTheme) -> Unit,
    onToggleTts: () -> Unit,
    onOpenBookmarks: () -> Unit,
    onSharePdf: () -> Unit,
    onClearCurrentPage: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Reader Studio",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )

            // Save & Auto-save Controls
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Auto-save PDF Changes",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                when {
                                    saveStatus == PdfSaveStatus.SAVING -> "Saving changes to device..."
                                    hasUnsavedChanges -> "Unsaved edits pending"
                                    isAutoSaveEnabled -> "All annotations saved automatically"
                                    else -> "Manual save mode • All changes saved"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = if (hasUnsavedChanges) Color(0xFFD97706) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = isAutoSaveEnabled,
                            onCheckedChange = onToggleAutoSave,
                            modifier = Modifier.testTag("pdf_sheet_autosave_switch")
                        )
                    }

                    Button(
                        onClick = {
                            onManualSave()
                            onDismiss()
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("pdf_sheet_manual_save")
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(if (hasUnsavedChanges) "Save Changes Now" else "Save PDF Annotations")
                    }
                }
            }

            // Reading Theme Selector Cards
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Reading Surface",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ReadingTheme.values().forEach { theme ->
                        val selected = activeTheme == theme
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
                            border = BorderStroke(
                                width = if (selected) 1.5.dp else 0.8.dp,
                                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { onSelectTheme(theme) }
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(26.dp)
                                        .clip(CircleShape)
                                        .background(theme.swatchColor)
                                        .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (selected) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = null,
                                            tint = if (theme.isDark) Color.White else MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                                Text(
                                    theme.displayName,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                    ),
                                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            // Quick Actions Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (isTtsPlaying) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable {
                            onDismiss()
                            onToggleTts()
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            if (isTtsPlaying) Icons.Default.Pause else Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            if (isTtsPlaying) "Pause Audio" else "Read Aloud",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable {
                            onDismiss()
                            onOpenBookmarks()
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Bookmark,
                            contentDescription = null,
                            tint = Color(0xFFD97706),
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            "Bookmarks ($bookmarkCount)",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                        )
                    }
                }
            }

            // Document Overview Card
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(fileName.ifBlank { "PDF Document" }, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                    }
                    Text(
                        "$totalPages pages • ${allAnnotations.size} total annotations (${allAnnotations.count { it.type == "HIGHLIGHT" }} highlights, ${allAnnotations.count { it.type == "COMMENT" }} sticky notes)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalDivider(thickness = 0.7.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        onDismiss()
                        onSharePdf()
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Export & Share")
                }

                Button(
                    onClick = {
                        onDismiss()
                        onClearCurrentPage()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    ),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Clear Page")
                }
            }

            Spacer(Modifier.height(12.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PdfThumbnailsSheet(
    pdfFile: File,
    totalPages: Int,
    currentPageIndex: Int,
    allAnnotations: List<PdfPageAnnotation>,
    bookmarkedPages: Set<Int>,
    onSelectPage: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val renderSemaphore = remember(pdfFile.absolutePath) { Semaphore(2) }
    val thumbCache = remember(pdfFile.absolutePath) { LruCache<Int, Bitmap>(24) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp)
        ) {
            Text(
                "Pages ($totalPages)",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                "Tap any page thumbnail to jump directly",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(14.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 220.dp, max = 420.dp)
            ) {
                items((1..totalPages).toList(), key = { it }) { pageNumber ->
                    val pageIdx = pageNumber - 1
                    val isCurrent = pageIdx == currentPageIndex
                    val markCount = allAnnotations.count { it.pageNumber == pageNumber }
                    val isBookmarked = bookmarkedPages.contains(pageNumber)

                    var thumbBitmap by remember(pdfFile.absolutePath, pageIdx) {
                        mutableStateOf<Bitmap?>(thumbCache.get(pageIdx))
                    }
                    LaunchedEffect(pdfFile.absolutePath, pageIdx) {
                        val cached = thumbCache.get(pageIdx)
                        if (cached != null) {
                            thumbBitmap = cached
                        } else {
                            val rendered = renderSemaphore.withPermit {
                                withContext(Dispatchers.IO) {
                                    renderThumbnailBitmap(pdfFile, pageIdx)
                                }
                            }
                            if (rendered != null) {
                                thumbCache.put(pageIdx, rendered)
                                thumbBitmap = rendered
                            }
                        }
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                onSelectPage(pageIdx)
                                onDismiss()
                            }
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(0.72f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White)
                                .border(
                                    width = if (isCurrent) 2.5.dp else 0.8.dp,
                                    color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                    shape = RoundedCornerShape(12.dp)
                                )
                        ) {
                            val bmp = thumbBitmap
                            if (bmp != null) {
                                Image(
                                    bitmap = bmp.asImageBitmap(),
                                    contentDescription = "Page $pageNumber preview",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.PictureAsPdf,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.outline
                                    )
                                }
                            }

                            if (isBookmarked) {
                                Icon(
                                    Icons.Default.Bookmark,
                                    contentDescription = null,
                                    tint = Color(0xFFD97706),
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(4.dp)
                                        .size(16.dp)
                                )
                            }

                            if (markCount > 0) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(4.dp)
                                ) {
                                    Text(
                                        "$markCount",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }
                        Text(
                            "Page $pageNumber",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium
                            ),
                            color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
        }
    }
}

private fun renderThumbnailBitmap(pdfFile: File, pageIdx: Int): Bitmap? {
    var pfd: ParcelFileDescriptor? = null
    var renderer: PdfRenderer? = null
    var page: PdfRenderer.Page? = null
    return try {
        pfd = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY)
        renderer = PdfRenderer(pfd)
        val safeIdx = pageIdx.coerceIn(0, (renderer.pageCount - 1).coerceAtLeast(0))
        page = renderer.openPage(safeIdx)
        val targetWidth = 220
        val targetHeight = ((page.height.toFloat() / page.width.coerceAtLeast(1)) * targetWidth).toInt().coerceIn(160, 1200)
        val bmp = createBitmap(targetWidth, targetHeight)
        val canvas = android.graphics.Canvas(bmp)
        canvas.drawColor(android.graphics.Color.WHITE)
        page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
        bmp
    } catch (_: Throwable) {
        null
    } finally {
        try { page?.close() } catch (_: Throwable) {}
        try { renderer?.close() } catch (_: Throwable) {}
        try { pfd?.close() } catch (_: Throwable) {}
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PdfBookmarksSheet(
    fileName: String,
    pdfBookmarks: List<BookmarkEntity>,
    totalPages: Int,
    onSelectPage: (Int) -> Unit,
    onRenameBookmark: (BookmarkEntity, String) -> Unit = { _, _ -> },
    onDeleteBookmark: (BookmarkEntity) -> Unit = {},
    onDismiss: () -> Unit
) {
    var editingBookmark by remember { mutableStateOf<BookmarkEntity?>(null) }
    var editLabelInput by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Bookmarks • $fileName", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            if (pdfBookmarks.isEmpty()) {
                Text(
                    "No bookmarks yet. Tap the bookmark ribbon in the bottom dock to save important pages.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 360.dp)
                ) {
                    items(pdfBookmarks, key = { it.id }) { bookmark ->
                        val pNum = bookmark.targetId.substringAfter("#").toIntOrNull() ?: 1
                        val displayTitle = bookmark.label.ifBlank { "Page $pNum" }
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable {
                                    onSelectPage((pNum - 1).coerceIn(0, (totalPages - 1).coerceAtLeast(0)))
                                    onDismiss()
                                }
                                .testTag("pdf_bookmark_item_${bookmark.id}")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Bookmark, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(20.dp))
                                    Spacer(Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = displayTitle,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "Page $pNum",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = {
                                            editingBookmark = bookmark
                                            editLabelInput = bookmark.label
                                        },
                                        modifier = Modifier.size(34.dp)
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = "Rename Bookmark", modifier = Modifier.size(16.dp))
                                    }
                                    IconButton(
                                        onClick = { onDeleteBookmark(bookmark) },
                                        modifier = Modifier.size(34.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Remove Bookmark",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }

    editingBookmark?.let { target ->
        AlertDialog(
            onDismissRequest = { editingBookmark = null },
            title = { Text("Rename Bookmark") },
            text = {
                OutlinedTextField(
                    value = editLabelInput,
                    onValueChange = { editLabelInput = it },
                    label = { Text("Bookmark label") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val cleaned = editLabelInput.trim()
                        if (cleaned.isNotEmpty()) {
                            onRenameBookmark(target, cleaned)
                        }
                        editingBookmark = null
                    }
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { editingBookmark = null }) { Text("Cancel") }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PdfStickyWallSheet(
    allStickyNotes: List<PdfPageAnnotation>,
    currentPageIndex: Int,
    selectedStickyPalette: PdfStickyPalette,
    stickyAsCard: Boolean,
    onSelectStickyPalette: (PdfStickyPalette) -> Unit,
    onToggleStickyStyle: (Boolean) -> Unit,
    onPinNote: (String, Boolean) -> Unit,
    onPlaceDraftOnPdf: (String, Boolean) -> Unit,
    onPlaceExistingOnPdf: (PdfPageAnnotation) -> Unit,
    onJumpToStickyPage: (Int) -> Unit,
    onEditSticky: (PdfPageAnnotation) -> Unit,
    onDeleteSticky: (PdfPageAnnotation) -> Unit,
    onDismiss: () -> Unit
) {
    var wallScopeAllPages by remember { mutableStateOf(true) }
    var wallColorFilter by remember { mutableStateOf<String?>(null) }
    var quickWallNoteText by remember { mutableStateOf("") }

    val visibleStickies = allStickyNotes.filter { note ->
        val pageOk = wallScopeAllPages || note.pageNumber == (currentPageIndex + 1)
        val colorOk = wallColorFilter == null || findStickyPalette(note.colorArgb).name == wallColorFilter
        pageOk && colorOk
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column {
                Text(
                    "Multi-Color Sticky Wall",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    "${allStickyNotes.size} pinned note(s) • Place as attractive card right on your PDF",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Quick Inline Sticky Note Composer
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = selectedStickyPalette.fill),
                border = BorderStroke(1.dp, selectedStickyPalette.accent.copy(alpha = 0.7f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = quickWallNoteText,
                        onValueChange = { quickWallNoteText = it },
                        placeholder = {
                            Text(
                                "Write a sticky note for Page ${currentPageIndex + 1}...",
                                color = selectedStickyPalette.ink.copy(alpha = 0.65f)
                            )
                        },
                        minLines = 2,
                        maxLines = 3,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("pdf_sticky_wall_input")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            PdfStickyColors.forEach { palette ->
                                val isChosen = selectedStickyPalette.name == palette.name
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(palette.fill)
                                        .border(
                                            width = if (isChosen) 2.5.dp else 1.dp,
                                            color = if (isChosen) palette.accent else palette.ink.copy(alpha = 0.35f),
                                            shape = CircleShape
                                        )
                                        .clickable { onSelectStickyPalette(palette) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isChosen) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = palette.name,
                                            tint = palette.ink,
                                            modifier = Modifier.size(13.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            ShapeSelectButton("Card on PDF", stickyAsCard) { onToggleStickyStyle(true) }
                            ShapeSelectButton("Mini Pin", !stickyAsCard) { onToggleStickyStyle(false) }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val trimmed = quickWallNoteText.trim()
                                if (trimmed.isNotEmpty()) {
                                    onPlaceDraftOnPdf(trimmed, stickyAsCard)
                                    quickWallNoteText = ""
                                    onDismiss()
                                }
                            },
                            enabled = quickWallNoteText.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = selectedStickyPalette.accent,
                                contentColor = Color.White
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.PushPin, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Place on PDF", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                val trimmed = quickWallNoteText.trim()
                                if (trimmed.isNotEmpty()) {
                                    onPinNote(trimmed, stickyAsCard)
                                    quickWallNoteText = ""
                                }
                            },
                            enabled = quickWallNoteText.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = selectedStickyPalette.ink.copy(alpha = 0.14f),
                                contentColor = selectedStickyPalette.ink
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Quick Add", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Filter Strip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ShapeSelectButton("All (${allStickyNotes.size})", wallScopeAllPages) { wallScopeAllPages = true }
                ShapeSelectButton("Page ${currentPageIndex + 1}", !wallScopeAllPages) { wallScopeAllPages = false }
                Spacer(Modifier.width(4.dp))
                ShapeSelectButton("All Colors", wallColorFilter == null) { wallColorFilter = null }
                PdfStickyColors.forEach { palette ->
                    val activeFilter = wallColorFilter == palette.name
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(palette.fill)
                            .border(
                                width = if (activeFilter) 2.5.dp else 1.dp,
                                color = if (activeFilter) palette.accent else MaterialTheme.colorScheme.outlineVariant,
                                shape = CircleShape
                            )
                            .clickable { wallColorFilter = if (activeFilter) null else palette.name }
                    )
                }
            }

            if (visibleStickies.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No sticky notes match this view. Write one above and tap Place on PDF, or tap anywhere in Sticky mode.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 160.dp, max = 340.dp)
                ) {
                    items(visibleStickies, key = { it.id }) { sticky ->
                        val palette = findStickyPalette(sticky.colorArgb)
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = palette.fill),
                            border = BorderStroke(1.dp, palette.accent.copy(alpha = 0.7f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .clickable {
                                    onJumpToStickyPage(sticky.pageNumber)
                                    onDismiss()
                                }
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = palette.accent.copy(alpha = 0.18f)
                                    ) {
                                        Text(
                                            "P.${sticky.pageNumber}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = palette.ink,
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                        )
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                        IconButton(
                                            onClick = {
                                                onPlaceExistingOnPdf(sticky)
                                                onDismiss()
                                            },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(Icons.Default.OpenWith, contentDescription = "Place on PDF", tint = palette.ink, modifier = Modifier.size(15.dp))
                                        }
                                        IconButton(onClick = { onEditSticky(sticky) }, modifier = Modifier.size(28.dp)) {
                                            Icon(Icons.Default.Edit, contentDescription = "Edit sticky note", tint = palette.ink, modifier = Modifier.size(15.dp))
                                        }
                                        IconButton(onClick = { onDeleteSticky(sticky) }, modifier = Modifier.size(28.dp)) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete sticky note", tint = palette.ink, modifier = Modifier.size(15.dp))
                                        }
                                    }
                                }
                                Text(
                                    text = sticky.content,
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp,
                                    color = palette.ink,
                                    maxLines = 5
                                )
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
internal fun PdfStickyNoteEditDialog(
    comment: PdfPageAnnotation,
    linkedNoteTitle: String? = null,
    onLinkNote: (() -> Unit)? = null,
    onOpenLinkedNote: (() -> Unit)? = null,
    onSave: (String, Color, Boolean, Float, Float) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    var editContent by remember(comment.id) { mutableStateOf(comment.content) }
    var editPalette by remember(comment.id) { mutableStateOf(findStickyPalette(comment.colorArgb)) }
    var displayAsCard by remember(comment.id) { mutableStateOf(comment.isStickyCard()) }
    var cardWidth by remember(comment.id) {
        mutableFloatStateOf(
            if (comment.isStickyCard()) comment.stickyWidth() else STICKY_CARD_WIDTH
        )
    }
    var cardHeight by remember(comment.id) {
        mutableFloatStateOf(
            if (comment.isStickyCard()) comment.stickyHeight() else comment.estimateStickyCardHeightForWidth(STICKY_CARD_WIDTH)
        )
    }
    var pinSize by remember(comment.id) {
        mutableFloatStateOf(
            if (!comment.isStickyCard()) comment.stickyWidth() else STICKY_BADGE_DEFAULT_SIZE
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.AutoMirrored.Filled.StickyNote2, contentDescription = null, tint = editPalette.accent)
                Spacer(Modifier.width(8.dp))
                Text("Sticky Note • Page ${comment.pageNumber}")
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PdfStickyColors.forEach { palette ->
                        val isChosen = editPalette.name == palette.name
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(palette.fill)
                                .border(
                                    width = if (isChosen) 2.5.dp else 1.dp,
                                    color = if (isChosen) palette.accent else Color.Gray.copy(alpha = 0.4f),
                                    shape = CircleShape
                                )
                                .clickable { editPalette = palette },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isChosen) {
                                Icon(Icons.Default.Check, contentDescription = palette.name, tint = palette.ink, modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Style:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    ShapeSelectButton("Sticky Card", displayAsCard) { displayAsCard = true }
                    ShapeSelectButton("Mini Pin", !displayAsCard) { displayAsCard = false }
                }

                if (displayAsCard) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Size:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        ShapeSelectButton("S", cardWidth < 210f) {
                            cardWidth = 186f
                            cardHeight = 132f
                        }
                        ShapeSelectButton("M", cardWidth in 210f..310f) {
                            cardWidth = 256f
                            cardHeight = 176f
                        }
                        ShapeSelectButton("L", cardWidth > 310f) {
                            cardWidth = 360f
                            cardHeight = 240f
                        }
                        Spacer(Modifier.weight(1f))
                        Text(
                            "${cardWidth.toInt()}×${cardHeight.toInt()}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("Width", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Slider(
                            value = cardWidth,
                            onValueChange = { cardWidth = it },
                            valueRange = STICKY_CARD_MIN_WIDTH..STICKY_CARD_MAX_WIDTH
                        )
                        Text("Height", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Slider(
                            value = cardHeight,
                            onValueChange = { cardHeight = it },
                            valueRange = STICKY_CARD_MIN_HEIGHT..STICKY_CARD_MAX_HEIGHT
                        )
                    }
                } else {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Pin Size:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        ShapeSelectButton("S", pinSize < 46f) { pinSize = 40f }
                        ShapeSelectButton("M", pinSize in 46f..68f) { pinSize = 54f }
                        ShapeSelectButton("L", pinSize > 68f) { pinSize = 82f }
                    }
                    Slider(
                        value = pinSize,
                        onValueChange = { pinSize = it },
                        valueRange = STICKY_BADGE_MIN_SIZE..STICKY_BADGE_MAX_SIZE
                    )
                }

                OutlinedTextField(
                    value = editContent,
                    onValueChange = { editContent = it },
                    label = { Text("Sticky note text (drag handles on page to resize or rotate)") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )

                if (onLinkNote != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = onLinkNote,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = if (linkedNoteTitle != null) "Relink Note" else "Link to Note",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (linkedNoteTitle != null && onOpenLinkedNote != null) {
                            TextButton(onClick = onOpenLinkedNote) {
                                Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Open", maxLines = 1)
                            }
                        }
                    }
                    if (linkedNoteTitle != null) {
                        Text(
                            text = "Linked note: $linkedNoteTitle",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val trimmed = editContent.trim()
                    if (trimmed.isNotEmpty()) {
                        val finalW = if (displayAsCard) cardWidth else pinSize
                        val finalH = if (displayAsCard) cardHeight else pinSize
                        onSave(trimmed, editPalette.fill, displayAsCard, finalW, finalH)
                    } else {
                        onDismiss()
                    }
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDelete) {
                Text("Delete", color = MaterialTheme.colorScheme.error)
            }
        }
    )
}

@Composable
internal fun PdfStickyNoteCreateDialog(
    pageNumber: Int,
    selectedStickyPalette: PdfStickyPalette,
    initialAsCard: Boolean,
    onSelectPalette: (PdfStickyPalette) -> Unit,
    onCreate: (String, Boolean, Float, Float) -> Unit,
    onDismiss: () -> Unit
) {
    var content by remember { mutableStateOf("") }
    var displayAsCard by remember(initialAsCard) { mutableStateOf(initialAsCard) }
    var sizePreset by remember { mutableStateOf("M") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Place Sticky Note • Page $pageNumber") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PdfStickyColors.forEach { palette ->
                        val isChosen = selectedStickyPalette.name == palette.name
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(palette.fill)
                                .border(
                                    width = if (isChosen) 2.5.dp else 1.dp,
                                    color = if (isChosen) palette.accent else Color.Gray.copy(alpha = 0.4f),
                                    shape = CircleShape
                                )
                                .clickable { onSelectPalette(palette) },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isChosen) {
                                Icon(Icons.Default.Check, contentDescription = palette.name, tint = palette.ink, modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Style:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    ShapeSelectButton("Sticky Card", displayAsCard) { displayAsCard = true }
                    ShapeSelectButton("Mini Pin", !displayAsCard) { displayAsCard = false }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Size:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    listOf("S", "M", "L").forEach { preset ->
                        ShapeSelectButton(preset, sizePreset == preset) { sizePreset = preset }
                    }
                }
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Note content...") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (content.isNotBlank()) {
                        val (w, h) = if (displayAsCard) {
                            when (sizePreset) {
                                "S" -> 186f to 132f
                                "L" -> 360f to 240f
                                else -> 256f to 176f
                            }
                        } else {
                            when (sizePreset) {
                                "S" -> 40f to 40f
                                "L" -> 82f to 82f
                                else -> 54f to 54f
                            }
                        }
                        onCreate(content.trim(), displayAsCard, w, h)
                    } else {
                        onDismiss()
                    }
                }
            ) {
                Text("Place on PDF")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
internal fun PdfTextAnnotationCreateDialog(
    pageNumber: Int,
    textFontSize: Float,
    onSelectSize: (Float) -> Unit,
    onCreate: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var input by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Text on Page $pageNumber") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    label = { Text("Enter text to place on PDF...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Size:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    listOf("S" to 26f, "M" to 36f, "L" to 48f).forEach { (label, sz) ->
                        ShapeSelectButton(label, textFontSize == sz) { onSelectSize(sz) }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (input.isNotBlank()) onCreate(input.trim()) else onDismiss()
                }
            ) {
                Text("Place Text")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
internal fun PdfTextAnnotationEditDialog(
    textAnn: PdfPageAnnotation,
    linkedNoteTitle: String? = null,
    onLinkNote: (() -> Unit)? = null,
    onOpenLinkedNote: (() -> Unit)? = null,
    onSave: (String, Int, Float) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    var editValue by remember(textAnn.id) { mutableStateOf(textAnn.content) }
    var editColor by remember(textAnn.id) { mutableStateOf(Color(textAnn.colorArgb)) }
    var editSize by remember(textAnn.id) { mutableFloatStateOf(textAnn.strokeWidth.coerceAtLeast(24f)) }
    val txtPalette = listOf(
        Color(0xFF1E293B),
        Color(0xFFDC2626),
        Color(0xFF2563EB),
        Color(0xFF16A34A),
        Color(0xFF7C3AED),
        Color(0xFFF97316)
    )
    var showCustomColorDialog by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Text Annotation") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = editValue,
                    onValueChange = { editValue = it },
                    label = { Text("Text content") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    txtPalette.forEach { c ->
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .clip(CircleShape)
                                .background(c)
                                .border(
                                    width = if (editColor == c) 2.5.dp else 1.dp,
                                    color = if (editColor == c) MaterialTheme.colorScheme.primary else Color.LightGray,
                                    shape = CircleShape
                                )
                                .clickable { editColor = c }
                        )
                    }
                    PdfColorGradientButton(
                        activeColor = editColor,
                        isCustomSelected = txtPalette.none { it == editColor },
                        testTag = "text_edit_gradient_picker",
                        onClick = { showCustomColorDialog = true }
                    )
                }
                if (showCustomColorDialog) {
                    PdfColorPickerDialog(
                        title = "Text Color Grades & Gradients",
                        initialColor = editColor,
                        onSelectColor = {
                            editColor = it
                            showCustomColorDialog = false
                        },
                        onDismiss = { showCustomColorDialog = false }
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Size:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    listOf("S" to 26f, "M" to 36f, "L" to 48f).forEach { (label, sz) ->
                        ShapeSelectButton(label, editSize == sz) { editSize = sz }
                    }
                }
                if (onLinkNote != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = onLinkNote,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(if (linkedNoteTitle != null) "Relink Note" else "Link to Note")
                        }
                        if (linkedNoteTitle != null && onOpenLinkedNote != null) {
                            TextButton(onClick = onOpenLinkedNote) {
                                Text("Open")
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val trimmed = editValue.trim()
                    if (trimmed.isNotEmpty()) {
                        onSave(trimmed, editColor.toArgb(), editSize)
                    } else {
                        onDismiss()
                    }
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDelete) {
                Text("Delete", color = MaterialTheme.colorScheme.error)
            }
        }
    )
}

@Composable
internal fun PdfExportAndShareDialog(
    fileName: String,
    annotationCount: Int,
    onShareAnnotated: () -> Unit,
    onSaveAnnotatedCopy: () -> Unit,
    onShareOriginal: () -> Unit,
    onSaveOriginalCopy: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Export & Share PDF") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "$fileName • Original file stays untouched in NoteLand.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(
                    onClick = {
                        onDismiss()
                        onShareAnnotated()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pdf_export_share_annotated")
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Share Annotated PDF ($annotationCount marks)")
                }
                OutlinedButton(
                    onClick = {
                        onDismiss()
                        onSaveAnnotatedCopy()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pdf_export_save_annotated")
                ) {
                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Save Annotated Copy to Device")
                }
                HorizontalDivider(thickness = 0.6.dp)
                OutlinedButton(
                    onClick = {
                        onDismiss()
                        onShareOriginal()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pdf_export_share_original")
                ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Share Original Unmodified PDF")
                }
                OutlinedButton(
                    onClick = {
                        onDismiss()
                        onSaveOriginalCopy()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pdf_export_save_original")
                ) {
                    Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Save Original Copy to Device")
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
internal fun PdfCreateNoteFromPageDialog(
    fileName: String,
    pageNumber: Int,
    initialBodyText: String,
    folders: List<FolderEntity>,
    onCreateNote: (title: String, body: String, folderId: String?, openAfterCreate: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val defaultTitle = remember(fileName, pageNumber) {
        val base = fileName.substringBeforeLast('.').ifBlank { "PDF Extract" }
        "$base — Page $pageNumber"
    }
    var titleInput by remember(defaultTitle) { mutableStateOf(defaultTitle) }
    var bodyInput by remember(initialBodyText) { mutableStateOf(initialBodyText) }
    var selectedFolderId by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.AutoMirrored.Filled.NoteAdd, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text("Create Note from Page $pageNumber")
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = titleInput,
                    onValueChange = { titleInput = it },
                    label = { Text("Note Title") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pdf_extract_note_title")
                )
                OutlinedTextField(
                    value = bodyInput,
                    onValueChange = { bodyInput = it },
                    label = { Text("Extracted / Selected Text") },
                    placeholder = { Text("Type or edit excerpt from Page $pageNumber...") },
                    minLines = 4,
                    maxLines = 8,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pdf_extract_note_body")
                )
                if (folders.isNotEmpty()) {
                    Text(
                        "Destination Folder",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ShapeSelectButton("Unfiled", selectedFolderId == null) { selectedFolderId = null }
                        folders.forEach { f ->
                            ShapeSelectButton(f.name, selectedFolderId == f.id) { selectedFolderId = f.id }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                TextButton(
                    onClick = {
                        onCreateNote(titleInput, bodyInput, selectedFolderId, false)
                    },
                    modifier = Modifier.testTag("pdf_extract_save_stay")
                ) {
                    Text("Save Note")
                }
                Button(
                    onClick = {
                        onCreateNote(titleInput, bodyInput, selectedFolderId, true)
                    },
                    modifier = Modifier.testTag("pdf_extract_save_open")
                ) {
                    Text("Save & Open")
                }
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
internal fun PdfLinkNotePickerDialog(
    currentLinkedNoteId: String?,
    availableNotes: List<NoteEntity>,
    onSelectNote: (NoteEntity?) -> Unit,
    onCreateAndLinkNote: () -> Unit,
    onDismiss: () -> Unit
) {
    var search by remember { mutableStateOf("") }
    val filtered = remember(availableNotes, search) {
        val q = search.trim()
        availableNotes
            .filter { !it.isLocked && !it.isDeleted }
            .filter {
                q.isEmpty() ||
                    it.title.contains(q, ignoreCase = true) ||
                    it.content.contains(q, ignoreCase = true)
            }
            .sortedByDescending { it.updatedAtUtc }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Link, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text("Link Annotation to Note")
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    placeholder = { Text("Search notes...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedButton(
                    onClick = onCreateAndLinkNote,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.AutoMirrored.Filled.NoteAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Create New Note from Page & Link")
                }
                if (currentLinkedNoteId != null) {
                    TextButton(
                        onClick = { onSelectNote(null) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.LinkOff, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Remove Existing Link", color = MaterialTheme.colorScheme.error)
                    }
                }
                if (filtered.isEmpty()) {
                    Text(
                        "No unlocked notes match \"$search\".",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 260.dp)
                    ) {
                        items(filtered, key = { it.id }) { note ->
                            val isSelected = note.id == currentLinkedNoteId
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { onSelectNote(note) }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = note.title.ifBlank { "Untitled Note" },
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (note.content.isNotBlank()) {
                                            Text(
                                                text = note.content.replace('\n', ' ').trim(),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                    if (isSelected) {
                                        Icon(Icons.Default.Check, contentDescription = "Linked", tint = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}
