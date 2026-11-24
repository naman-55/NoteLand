package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.core.graphics.toColorInt
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.AttachmentEntity
import com.example.ui.components.DocumentAttachmentCard
import com.example.ui.components.DocumentMetadataDialog
import com.example.ui.components.EmptyStateView
import com.example.ui.components.HierarchicalFolderPickerSheet
import com.example.ui.components.RenameDocumentDialog
import com.example.ui.theme.editorialBackdropBrush
import com.example.ui.theme.editorialCardBorderColor
import com.example.ui.theme.editorialCardContainerColor
import com.example.ui.theme.editorialHeaderSubColor
import com.example.ui.theme.editorialHeaderTextColor
import com.example.ui.theme.isAppInDarkTheme
import com.example.ui.viewmodel.NoteViewModel
import com.example.util.FolderHierarchyUtils
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class PdfLibraryFilter(val label: String) {
    ALL("All"),
    FAVORITES("Favourites"),
    UNFILED("Unfiled"),
    LARGEST("Largest"),
    NAME_ASC("A–Z")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfToolsScreen(
    viewModel: NoteViewModel,
    onNavigateBack: () -> Unit,
    onOpenPdf: (attachmentId: String, filePath: String, fileName: String) -> Unit
) {
    val context = LocalContext.current
    val allAttachments by viewModel.attachments.collectAsStateWithLifecycle()
    val folders by viewModel.folders.collectAsStateWithLifecycle()
    val allNotes by viewModel.allActiveNotes.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var libraryFilter by remember { mutableStateOf(PdfLibraryFilter.ALL) }
    var pdfToMove by remember { mutableStateOf<AttachmentEntity?>(null) }
    var pdfToRename by remember { mutableStateOf<AttachmentEntity?>(null) }
    var pdfMetadataTarget by remember { mutableStateOf<AttachmentEntity?>(null) }
    var pendingExportPdf by remember { mutableStateOf<AttachmentEntity?>(null) }

    val exportPdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { targetUri: Uri? ->
        val att = pendingExportPdf
        pendingExportPdf = null
        if (targetUri != null && att != null) {
            viewModel.exportAttachmentToUri(att, targetUri) { res ->
                res.onSuccess {
                    Toast.makeText(context, "Saved copy of \"${att.fileName}\"", Toast.LENGTH_SHORT).show()
                }.onFailure { err ->
                    Toast.makeText(context, "Export failed: ${err.localizedMessage ?: "Error"}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val pdfAttachments = remember(allAttachments, searchQuery, libraryFilter) {
        val base = allAttachments.filter {
            !it.id.startsWith("demo-") && it.id != "demo_pdf" &&
                !it.isLocked && !it.isHidden && !it.isArchived && !it.isDeleted &&
                (it.mimeType.contains("pdf", ignoreCase = true) || it.fileExtension.equals("pdf", ignoreCase = true)) &&
                (searchQuery.isBlank() || it.fileName.contains(searchQuery, ignoreCase = true))
        }
        val filtered = when (libraryFilter) {
            PdfLibraryFilter.FAVORITES -> base.filter { it.isFavorite }
            PdfLibraryFilter.UNFILED -> base.filter { it.folderId.isNullOrBlank() }
            else -> base
        }
        when (libraryFilter) {
            PdfLibraryFilter.LARGEST -> filtered.sortedByDescending { it.sizeBytes }
            PdfLibraryFilter.NAME_ASC -> filtered.sortedBy { it.fileName.lowercase(Locale.getDefault()) }
            else -> filtered.sortedByDescending { it.updatedAtUtc }
        }
    }

    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            val fileName = "Document_${System.currentTimeMillis()}.pdf"
            viewModel.importMediaFile(
                uri = it,
                fileName = fileName,
                isPdf = true,
                onDeduplicated = { wasDedup ->
                    if (wasDedup) {
                        Toast.makeText(context, "Opened existing matching PDF (no duplicate stored)", Toast.LENGTH_SHORT).show()
                    }
                },
                onError = { err ->
                    Toast.makeText(context, err.message ?: "Invalid or corrupted PDF file", Toast.LENGTH_SHORT).show()
                }
            ) { created ->
                val fullPath = viewModel.attachmentStorageManager.getFileFromRelativePath(created.relativePath).absolutePath
                onOpenPdf(created.id, fullPath, created.fileName)
            }
        }
    }

    BackHandler {
        onNavigateBack()
    }

    val isDark = isAppInDarkTheme
    val headerTextColor = editorialHeaderTextColor(isDark)
    val headerSubColor = editorialHeaderSubColor(isDark)
    val searchPillBg = if (isDark) Color(0xFF2B221E).copy(alpha = 0.76f) else Color.White.copy(alpha = 0.80f)
    val searchPillBorder = if (isDark) Color.White.copy(alpha = 0.16f) else Color(0xFF1F1C19).copy(alpha = 0.12f)

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
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFEF4444).copy(alpha = 0.16f),
                                modifier = Modifier.size(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.PictureAsPdf,
                                        contentDescription = null,
                                        tint = Color(0xFFEF4444),
                                        modifier = Modifier.size(21.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(
                                    "PDF Reader",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = headerTextColor
                                )
                                Text(
                                    "Highlight, pen, text & sticky wall",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = headerSubColor
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = headerTextColor)
                        }
                    },
                    actions = {
                        IconButton(onClick = { pdfPickerLauncher.launch(arrayOf("application/pdf")) }) {
                            Icon(Icons.Default.FileDownload, contentDescription = "Import PDF", tint = headerTextColor)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = Color.Transparent
                    )
                )
            },
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    onClick = { pdfPickerLauncher.launch(arrayOf("application/pdf")) },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("Import PDF", fontWeight = FontWeight.SemiBold) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .navigationBarsPadding()
                        .testTag("import_pdf_fab")
                )
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Modern Search Pill
                item {
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = searchPillBg,
                        border = BorderStroke(0.8.dp, searchPillBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                    ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = "Search PDFs",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(19.dp)
                        )
                        Box(modifier = Modifier.weight(1f)) {
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = "Search PDF library...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            BasicTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                singleLine = true,
                                textStyle = MaterialTheme.typography.bodyMedium.copy(
                                    color = MaterialTheme.colorScheme.onSurface
                                ),
                                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("pdf_search_input")
                            )
                        }
                        if (searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { searchQuery = "" },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    Icons.Default.Clear,
                                    contentDescription = "Clear search",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Recents Carousel with Tactile Miniature Page Previews
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Recents",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        "Tap to read & annotate",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(Modifier.height(12.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    RecentPdfCard(
                        title = "Quick Start Guide.pdf",
                        pagesText = "3 Pages • Tips & Tricks",
                        badgeLabel = "GUIDE",
                        accentColor = Color(0xFF3B82F6),
                        onClick = {
                            val demoFile = File(context.filesDir, "attachments/Quick_Start_Guide.pdf")
                            if (!demoFile.exists()) {
                                demoFile.parentFile?.mkdirs()
                                createSamplePdf(demoFile, "Quick Start Guide")
                            }
                            onOpenPdf("demo-quick-start", demoFile.absolutePath, "Quick Start Guide.pdf")
                        }
                    )

                    RecentPdfCard(
                        title = "What's New(1).pdf",
                        pagesText = "5 Pages • Release Notes",
                        badgeLabel = "NOTES",
                        accentColor = Color(0xFF10B981),
                        onClick = {
                            val demoFile = File(context.filesDir, "attachments/Whats_New.pdf")
                            if (!demoFile.exists()) {
                                demoFile.parentFile?.mkdirs()
                                createSamplePdf(demoFile, "What's New in NoteLand")
                            }
                            onOpenPdf("demo-whats-new", demoFile.absolutePath, "What's New(1).pdf")
                        }
                    )

                    pdfAttachments.take(4).forEach { att ->
                        val localFile = File(context.filesDir, att.relativePath)
                        RecentPdfCard(
                            title = att.fileName,
                            pagesText = "${(att.sizeBytes / 1024).coerceAtLeast(1)} KB • PDF",
                            badgeLabel = "PDF",
                            accentColor = Color(0xFF6366F1),
                            onClick = {
                                onOpenPdf(att.id, localFile.absolutePath, att.fileName)
                            }
                        )
                    }
                }
            }

            // Document Library Section + Filter Chips
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "PDF Documents (${pdfAttachments.size})",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PdfLibraryFilter.values().forEach { filter ->
                            FilterChip(
                                selected = libraryFilter == filter,
                                onClick = { libraryFilter = filter },
                                label = { Text(filter.label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                        }
                    }
                }
            }

            if (pdfAttachments.isEmpty()) {
                item {
                    EmptyStateView(
                        icon = Icons.Default.PictureAsPdf,
                        title = if (searchQuery.isNotBlank() || libraryFilter != PdfLibraryFilter.ALL) "No Matching PDFs" else "No Imported PDFs Yet",
                        description = "Import a PDF or open one from Recents to highlight, draw with pen, place text, or pin multi-color sticky notes.",
                        actionText = "Import PDF",
                        onAction = { pdfPickerLauncher.launch(arrayOf("application/pdf")) }
                    )
                }
            } else {
                items(pdfAttachments, key = { it.id }) { pdf ->
                    val file = viewModel.attachmentStorageManager.getFileFromRelativePath(pdf.relativePath)
                    val folderLabel = FolderHierarchyUtils.getFolderPathString(folders, pdf.folderId)
                    DocumentAttachmentCard(
                        attachment = pdf,
                        folderName = folderLabel,
                        onOpen = { onOpenPdf(pdf.id, file.absolutePath, pdf.fileName) },
                        onMoveToFolder = { pdfToMove = pdf },
                        onRename = { pdfToRename = pdf },
                        onToggleFavorite = { viewModel.toggleAttachmentFavorite(pdf) },
                        onToggleArchive = { viewModel.toggleAttachmentArchive(pdf) },
                        onToggleLock = {
                            viewModel.toggleAttachmentLock(pdf) { res ->
                                res.onSuccess {
                                    Toast.makeText(context, "Moved \"${pdf.fileName}\" to Locked Folder", Toast.LENGTH_SHORT).show()
                                }.onFailure { err ->
                                    Toast.makeText(context, err.message ?: "Unable to lock PDF", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        onShowMetadata = { pdfMetadataTarget = pdf },
                        onExport = {
                            pendingExportPdf = pdf
                            exportPdfLauncher.launch(pdf.fileName)
                        },
                        onSoftDelete = { viewModel.softDeleteAttachment(pdf) }
                    )
                }
            }
        }
        }
    }

    pdfToMove?.let { movingPdf ->
        HierarchicalFolderPickerSheet(
            title = "Organize PDF in Folder",
            subtitle = movingPdf.fileName,
            folders = folders,
            notes = allNotes,
            selectedFolderId = movingPdf.folderId,
            rootOptionLabel = "No Folder (Unfiled)",
            allowCreateFolder = true,
            onCreateQuickFolder = { newName, parentId ->
                viewModel.createFolder(newName, "#D85C37", "folder", parentId) { created ->
                    viewModel.moveAttachmentToFolder(movingPdf.id, created.id)
                }
            },
            onSelectFolder = { picked ->
                viewModel.moveAttachmentToFolder(movingPdf.id, picked?.id) { res ->
                    res.onSuccess {
                        val dest = picked?.name ?: "Unfiled"
                        Toast.makeText(context, "Moved \"${movingPdf.fileName}\" to $dest", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onDismiss = { pdfToMove = null }
        )
    }

    pdfToRename?.let { targetPdf ->
        RenameDocumentDialog(
            initialName = targetPdf.fileName,
            onConfirm = { newName ->
                viewModel.renameAttachment(targetPdf.id, newName)
                pdfToRename = null
            },
            onDismiss = { pdfToRename = null }
        )
    }

    pdfMetadataTarget?.let { targetPdf ->
        DocumentMetadataDialog(
            attachment = targetPdf,
            folderPath = FolderHierarchyUtils.getFolderPathString(folders, targetPdf.folderId),
            onDismiss = { pdfMetadataTarget = null }
        )
    }
}

@Composable
fun RecentPdfCard(
    title: String,
    pagesText: String,
    accentColor: Color,
    badgeLabel: String = "PDF",
    onClick: () -> Unit
) {
    val isDark = isAppInDarkTheme
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = editorialCardContainerColor(isDark)),
        border = BorderStroke(0.8.dp, editorialCardBorderColor(isDark)),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isDark) 0.dp else 2.dp),
        modifier = Modifier
            .width(164.dp)
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Tactile miniature paper preview
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(116.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                accentColor.copy(alpha = if (isDark) 0.22f else 0.14f),
                                if (isDark) Color(0xFF1F1A17) else MaterialTheme.colorScheme.surfaceContainerLow
                            )
                        )
                    )
                    .padding(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isDark) Color(0xFF27211D) else MaterialTheme.colorScheme.surface,
                    shadowElevation = if (isDark) 0.dp else 2.dp,
                    border = BorderStroke(0.6.dp, editorialCardBorderColor(isDark)),
                    modifier = Modifier.fillMaxSize()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(42.dp)
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(accentColor)
                            )
                            Surface(
                                shape = RoundedCornerShape(5.dp),
                                color = accentColor.copy(alpha = 0.16f)
                            ) {
                                Text(
                                    badgeLabel,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = accentColor,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                )
                            }
                        }
                        Spacer(Modifier.height(2.dp))
                        listOf(0.9f, 0.75f, 0.85f, 0.55f).forEach { fraction ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(fraction)
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(10.dp))
            Text(
                title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                pagesText,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun PdfListItemCard(
    attachment: AttachmentEntity,
    onClick: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val isDark = isAppInDarkTheme

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = editorialCardContainerColor(isDark)),
        border = BorderStroke(0.8.dp, editorialCardBorderColor(isDark)),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isDark) 0.dp else 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFEF4444).copy(alpha = 0.12f),
                modifier = Modifier.size(46.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(24.dp))
                }
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    attachment.fileName,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val sizeKb = (attachment.sizeBytes / 1024).coerceAtLeast(1)
                val dateStr = SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(attachment.createdAtUtc))
                Text(
                    "$sizeKb KB • $dateStr",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Options")
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Open in Reader") },
                        leadingIcon = { Icon(Icons.Default.PictureAsPdf, contentDescription = null) },
                        onClick = {
                            showMenu = false
                            onClick()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Share PDF") },
                        leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) },
                        onClick = {
                            showMenu = false
                            onShare()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                        leadingIcon = { Icon(Icons.Default.Close, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                        onClick = {
                            showMenu = false
                            onDelete()
                        }
                    )
                }
            }
        }
    }
}

fun writeFallbackMinimalPdf(file: File, docTitle: String) {
    val pdfContent = "%PDF-1.4\n" +
        "1 0 obj << /Type /Catalog /Pages 2 0 R >> endobj\n" +
        "2 0 obj << /Type /Pages /Kids [3 0 R] /Count 1 >> endobj\n" +
        "3 0 obj << /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Resources << /Font << /F1 4 0 R >> >> /Contents 5 0 R >> endobj\n" +
        "4 0 obj << /Type /Font /Subtype /Type1 /BaseFont /Helvetica >> endobj\n" +
        "5 0 obj << /Length 50 >> stream\n" +
        "BT /F1 14 Tf 50 750 Td ($docTitle) Tj ET\n" +
        "endstream\n" +
        "endobj\n" +
        "xref\n" +
        "0 6\n" +
        "0000000000 65535 f \n" +
        "0000000009 00000 n \n" +
        "0000000058 00000 n \n" +
        "0000000115 00000 n \n" +
        "0000000244 00000 n \n" +
        "0000000325 00000 n \n" +
        "trailer << /Size 6 /Root 1 0 R >>\n" +
        "startxref\n" +
        "427\n" +
        "%%EOF\n"
    file.writeText(pdfContent, Charsets.UTF_8)
}

fun createPdfFromNote(context: Context, title: String, content: String): File {
    val sanitized = "${title.ifBlank { "Note" }.replace(" ", "_")}_${System.currentTimeMillis()}.pdf"
    val file = File(context.filesDir, "attachments/$sanitized")
    file.parentFile?.mkdirs()

    try {
        val document = PdfDocument()
        try {
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
            val page = document.startPage(pageInfo)
            val canvas = page.canvas

            val paint = Paint().apply {
                color = android.graphics.Color.BLACK
                textSize = 24f
                isAntiAlias = true
                typeface = Typeface.DEFAULT_BOLD
            }
            canvas.drawText(title.ifBlank { "NoteLand Document" }, 50f, 70f, paint)

            paint.textSize = 12f
            paint.color = android.graphics.Color.GRAY
            canvas.drawText("Generated on ${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())}", 50f, 95f, paint)

            paint.textSize = 14f
            paint.color = android.graphics.Color.DKGRAY
            paint.typeface = Typeface.DEFAULT
            var yPos = 140f
            val lines = content.split("\n")
            for (line in lines) {
                if (yPos > 800f) break
                canvas.drawText(line.take(80), 50f, yPos, paint)
                yPos += 22f
            }

            document.finishPage(page)
            FileOutputStream(file).use { out -> document.writeTo(out) }
        } finally {
            runCatching { document.close() }
        }
    } catch (_: Throwable) {
        writeFallbackMinimalPdf(file, title.ifBlank { "NoteLand Document" })
    }
    return file
}

fun createPdfFromImageUri(context: Context, imageUri: Uri, title: String): File? {
    var bitmap: android.graphics.Bitmap? = null
    return try {
        val boundsOpts = android.graphics.BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        context.contentResolver.openInputStream(imageUri)?.use { stream ->
            android.graphics.BitmapFactory.decodeStream(stream, null, boundsOpts)
        }
        var sampleSize = 1
        val maxDim = 2048
        while (boundsOpts.outWidth / sampleSize > maxDim || boundsOpts.outHeight / sampleSize > maxDim) {
            sampleSize *= 2
        }
        val decodeOpts = android.graphics.BitmapFactory.Options().apply {
            inSampleSize = sampleSize
        }
        bitmap = context.contentResolver.openInputStream(imageUri)?.use { stream ->
            android.graphics.BitmapFactory.decodeStream(stream, null, decodeOpts)
        } ?: return null

        val sanitized = "${title.replace(" ", "_")}.pdf"
        val file = File(context.filesDir, "attachments/$sanitized")
        file.parentFile?.mkdirs()

        val document = PdfDocument()
        try {
            val pageInfo = PdfDocument.PageInfo.Builder(bitmap.width.coerceAtLeast(1), bitmap.height.coerceAtLeast(1), 1).create()
            val page = document.startPage(pageInfo)
            page.canvas.drawBitmap(bitmap, 0f, 0f, null)
            document.finishPage(page)

            FileOutputStream(file).use { out -> document.writeTo(out) }
        } finally {
            document.close()
        }
        file
    } catch (_: Throwable) {
        null
    } finally {
        bitmap?.recycle()
    }
}

fun sharePdfFile(context: Context, file: File) {
    if (!file.exists()) {
        Toast.makeText(context, "File does not exist", Toast.LENGTH_SHORT).show()
        return
    }
    try {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share PDF Document"))
    } catch (e: Exception) {
        Toast.makeText(context, "Unable to share PDF: ${e.localizedMessage ?: "Unknown error"}", Toast.LENGTH_SHORT).show()
    }
}

fun createSamplePdf(file: File, docTitle: String, pageCount: Int = 3) {
    try {
        val document = PdfDocument()
        try {
            for (pageIndex in 1..pageCount) {
                val pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageIndex).create()
                val page = document.startPage(pageInfo)
                val canvas = page.canvas

                val paint = Paint().apply {
                    color = android.graphics.Color.DKGRAY
                    textSize = 22f
                    isAntiAlias = true
                    typeface = Typeface.DEFAULT_BOLD
                }
                canvas.drawText("NoteLand PDF Reader & Editor", 50f, 60f, paint)

                paint.textSize = 14f
                paint.typeface = Typeface.DEFAULT
                paint.color = android.graphics.Color.GRAY
                canvas.drawText("$docTitle • Page $pageIndex of $pageCount", 50f, 85f, paint)

                paint.strokeWidth = 1f
                paint.color = android.graphics.Color.LTGRAY
                canvas.drawLine(50f, 100f, 545f, 100f, paint)

                paint.textSize = 13f
                paint.color = android.graphics.Color.BLACK

                var y = 140f
                fun drawP(text: String, isHeading: Boolean = false) {
                    if (isHeading) {
                        paint.textSize = 16f
                        paint.typeface = Typeface.DEFAULT_BOLD
                        paint.color = android.graphics.Color.BLACK
                        y += 10f
                    } else {
                        paint.textSize = 12.5f
                        paint.typeface = Typeface.DEFAULT
                        paint.color = android.graphics.Color.DKGRAY
                    }
                    val words = text.split(" ")
                    var currentLine = ""
                    for (w in words) {
                        if (paint.measureText("$currentLine $w") < 480f) {
                            currentLine = if (currentLine.isEmpty()) w else "$currentLine $w"
                        } else {
                            canvas.drawText(currentLine, 50f, y, paint)
                            y += 18f
                            currentLine = w
                        }
                    }
                    if (currentLine.isNotEmpty()) {
                        canvas.drawText(currentLine, 50f, y, paint)
                        y += 24f
                    }
                }

                drawP("From papers to PDFs, we all love to highlight the most valuable content. Most of our users prefer to mark up PDF documents with highlights. We rounded up some special tips provided by our users to help you to make good use of the PDF highlighter.")
                drawP("Growing your business often means you have to spend lots of time on the move. Traveling to client sites for meetings and presentations is important, but productivity often takes a back seat.")
                drawP("Transition from Paper to Digital Files", isHeading = true)
                drawP("While being emailed an agreement, you could print out the file, sign it, and then scan it back in and email it. However, there's always a better, faster way.")
                drawP("Open a PDF and tap any markup tool in the bottom dock — Highlight, Pen, Text, or Multi-Color Sticky Wall — to annotate directly on the page.")

                if (pageIndex == 1) {
                    val boxPaint = Paint().apply {
                        color = "#E0E7FF".toColorInt()
                        style = Paint.Style.FILL
                    }
                    canvas.drawRoundRect(50f, y + 20f, 545f, y + 200f, 16f, 16f, boxPaint)

                    val artPaint = Paint().apply {
                        color = "#3B82F6".toColorInt()
                        style = Paint.Style.FILL
                    }
                    val path = android.graphics.Path().apply {
                        moveTo(180f, y + 200f)
                        lineTo(350f, y + 90f)
                        lineTo(520f, y + 200f)
                        close()
                    }
                    canvas.drawPath(path, artPaint)

                    artPaint.color = android.graphics.Color.WHITE
                    canvas.drawCircle(130f, y + 90f, 30f, artPaint)
                }

                document.finishPage(page)
            }

            FileOutputStream(file).use { out -> document.writeTo(out) }
        } finally {
            runCatching { document.close() }
        }
    } catch (_: Throwable) {
        writeFallbackMinimalPdf(file, docTitle)
    }
}
