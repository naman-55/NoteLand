package com.example.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.model.AttachmentEntity
import com.example.data.model.NoteType
import com.example.ui.components.EmptyStateView
import com.example.ui.navigation.Screen
import com.example.ui.theme.editorialBackdropBrush
import com.example.ui.theme.editorialCardBorderColor
import com.example.ui.theme.editorialCardContainerColor
import com.example.ui.theme.editorialHeaderSubColor
import com.example.ui.theme.editorialHeaderTextColor
import com.example.ui.theme.isAppInDarkTheme
import com.example.ui.viewmodel.NoteViewModel
import com.example.util.SmartNoteUtils
import java.io.File

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MediaScreen(
    viewModel: NoteViewModel,
    onNavigateToDestination: (String) -> Unit,
    onNavigateBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val attachments by viewModel.attachments.collectAsStateWithLifecycle()
    val allNotes by viewModel.allActiveNotes.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableIntStateOf(0) } // 0: All, 1: Images, 2: PDFs
    var selectedAttachmentForDetails by remember { mutableStateOf<AttachmentEntity?>(null) }
    var attachmentToDelete by remember { mutableStateOf<AttachmentEntity?>(null) }

    // Combine attachments from db + unlocked media notes that have imageUri or are PDF
    val combinedMediaItems = remember(attachments, allNotes) {
        val lockedNoteIds = allNotes.filter { it.isLocked }.map { it.id }.toSet()
        val list = mutableListOf<AttachmentEntity>()
        // From attachments table (exclude any locked, hidden, archived, deleted, or linked to locked notes)
        list.addAll(
            attachments.filter {
                !it.isLocked && !it.isHidden && !it.isArchived && !it.isDeleted &&
                    (it.noteId == null || it.noteId !in lockedNoteIds)
            }
        )

        // From unlocked notes with images/PDF that might not be in attachments table yet
        for (note in allNotes) {
            if (note.isLocked) continue
            if (note.imageUri.isNotBlank() && list.none { it.noteId == note.id }) {
                val isPdf = note.noteType == NoteType.PDF_DOC.name
                val ext = if (isPdf) "pdf" else "jpg"
                val f = File(note.imageUri)
                list.add(
                    AttachmentEntity(
                        id = note.id,
                        noteId = note.id,
                        fileName = note.title.ifBlank { if (isPdf) "Document.pdf" else "Photo.jpg" },
                        fileExtension = ext,
                        relativePath = if (f.isAbsolute) f.absolutePath else "attachments/${f.name}",
                        mimeType = if (isPdf) "application/pdf" else "image/jpeg",
                        sizeBytes = if (f.exists()) f.length() else 102400L,
                        createdAtUtc = note.createdAtUtc
                    )
                )
            }
        }
        list.distinctBy { it.id }
    }

    val filteredMedia = remember(combinedMediaItems, selectedTab) {
        when (selectedTab) {
            1 -> combinedMediaItems.filter { it.isImageMedia() }
            2 -> combinedMediaItems.filter { it.isPdfMedia() }
            else -> combinedMediaItems
        }
    }

    // Photo picker launcher (stores untouched original in app-private storage)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let {
            val fileName = "Photo_${System.currentTimeMillis()}.jpg"
            viewModel.importMediaFile(
                uri = it,
                fileName = fileName,
                isPdf = false,
                onError = { err ->
                    android.widget.Toast.makeText(
                        context,
                        err.message ?: "Could not import selected photo",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            ) { created ->
                val fullPath = viewModel.attachmentStorageManager.getFileFromRelativePath(created.relativePath).absolutePath
                onNavigateToDestination(Screen.ImageAnnotator.createRoute(created.id, fullPath, created.fileName))
            }
        }
    }

    // Document picker launcher for local PDFs
    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            val fileName = "Document_${System.currentTimeMillis()}.pdf"
            viewModel.importMediaFile(
                uri = it,
                fileName = fileName,
                isPdf = true,
                onError = { err ->
                    android.widget.Toast.makeText(
                        context,
                        err.message ?: "Invalid or corrupted PDF file",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            ) { created ->
                val fullPath = viewModel.attachmentStorageManager.getFileFromRelativePath(created.relativePath).absolutePath
                onNavigateToDestination(Screen.PdfReader.createRoute(created.id, fullPath, created.fileName))
            }
        }
    }

    BackHandler {
        onNavigateBack()
    }

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
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Image, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                }
                            }
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(
                                    "Media Workspace",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = headerTextColor
                                )
                                Text(
                                    "${filteredMedia.size} items • Offline Storage",
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
                        IconButton(onClick = {
                            onNavigateToDestination(Screen.PdfTools.route)
                        }) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = "PDF Reader", tint = Color(0xFFDC2626))
                        }

                        IconButton(onClick = {
                            photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        }) {
                            Icon(Icons.Default.Image, contentDescription = "Import Photo", tint = MaterialTheme.colorScheme.primary)
                        }

                        IconButton(onClick = {
                            pdfPickerLauncher.launch(arrayOf("application/pdf"))
                        }) {
                            Icon(Icons.Default.Add, contentDescription = "Import PDF", tint = MaterialTheme.colorScheme.primary)
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
                // Tab Row: All Media, Photos, PDFs
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("All (${combinedMediaItems.size})", fontWeight = FontWeight.SemiBold) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Text(
                            "Photos (${combinedMediaItems.count { it.isImageMedia() }})",
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = {
                        Text(
                            "PDFs (${combinedMediaItems.count { it.isPdfMedia() }})",
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                )
            }

            if (filteredMedia.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        EmptyStateView(
                            icon = if (selectedTab == 2) Icons.Default.PictureAsPdf else Icons.Default.Image,
                            title = if (selectedTab == 2) "No PDF Documents Found" else "No Media Imported Yet",
                            message = "Import photos to annotate or PDF documents to read and highlight completely offline."
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(
                                onClick = {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                }
                            ) {
                                Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Import Photo")
                            }

                            OutlinedButton(
                                onClick = {
                                    pdfPickerLauncher.launch(arrayOf("application/pdf"))
                                }
                            ) {
                                Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Import PDF")
                            }
                        }
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredMedia, key = { it.id }) { item ->
                        val isPdf = item.isPdfMedia()
                        val localFile = if (item.relativePath.startsWith("/")) {
                            File(item.relativePath)
                        } else {
                            viewModel.attachmentStorageManager.getFileFromRelativePath(item.relativePath)
                        }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .combinedClickable(
                                    onClick = {
                                        if (isPdf) {
                                            onNavigateToDestination(
                                                Screen.PdfReader.createRoute(item.id, localFile.absolutePath, item.fileName)
                                            )
                                        } else {
                                            onNavigateToDestination(
                                                Screen.ImageAnnotator.createRoute(item.id, localFile.absolutePath, item.fileName)
                                            )
                                        }
                                    },
                                    onLongClick = {
                                        selectedAttachmentForDetails = item
                                    }
                                ),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = editorialCardContainerColor(isDark)),
                            border = BorderStroke(0.8.dp, editorialCardBorderColor(isDark)),
                            elevation = CardDefaults.cardElevation(defaultElevation = if (isDark) 0.dp else 2.dp)
                        ) {
                            Column {
                                // Thumbnail / Icon Container
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(130.dp)
                                        .background(
                                            if (isPdf) {
                                                if (isDark) Color(0xFF3A1D22) else Color(0xFFFFE4E6)
                                            } else {
                                                MaterialTheme.colorScheme.surfaceVariant
                                            }
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (!isPdf && localFile.exists()) {
                                        AsyncImage(
                                            model = localFile,
                                            contentDescription = item.fileName,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Icon(
                                                if (isPdf) Icons.Default.PictureAsPdf else Icons.Default.Image,
                                                contentDescription = null,
                                                tint = if (isPdf) {
                                                    if (isDark) Color(0xFFFCA5A5) else Color(0xFFDC2626)
                                                } else MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(46.dp)
                                            )
                                            Spacer(Modifier.height(4.dp))
                                            Text(
                                                if (isPdf) "PDF Document" else "Image",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isPdf) {
                                                    if (isDark) Color(0xFFFECDD3) else Color(0xFF991B1B)
                                                } else MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }

                                    // Format Pill Badge on top-right
                                    Surface(
                                        color = Color.Black.copy(alpha = 0.6f),
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(8.dp)
                                    ) {
                                        Text(
                                            item.fileExtension.uppercase(),
                                            color = Color.White,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        text = item.fileName,
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    Spacer(Modifier.height(2.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = SmartNoteUtils.formatFileSize(item.sizeBytes),
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )

                                        Row {
                                            IconButton(
                                                onClick = { selectedAttachmentForDetails = item },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(Icons.Default.Info, contentDescription = "Info", tint = Color.Gray, modifier = Modifier.size(15.dp))
                                            }

                                            IconButton(
                                                onClick = { attachmentToDelete = item },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(15.dp))
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
    }

    // Metadata & Integrity Modal Sheet
    selectedAttachmentForDetails?.let { item ->
        val isPdf = item.isPdfMedia()
        val file = if (item.relativePath.startsWith("/")) File(item.relativePath) else viewModel.attachmentStorageManager.getFileFromRelativePath(item.relativePath)

        ModalBottomSheet(
            onDismissRequest = { selectedAttachmentForDetails = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Media Metadata & Integrity",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )

                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("File Name", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(item.fileName, fontWeight = FontWeight.Bold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Format", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(item.fileExtension.uppercase(), fontWeight = FontWeight.Bold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("File Size", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(SmartNoteUtils.formatFileSize(item.sizeBytes), fontWeight = FontWeight.Bold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Storage Location", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("App-Private Local", fontWeight = FontWeight.Bold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Original File Integrity", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("Pristine / Untouched", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Imported Date", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(SmartNoteUtils.formatTimestamp(item.createdAtUtc), fontWeight = FontWeight.Medium)
                        }
                    }
                }

                var pendingMediaExport by remember { mutableStateOf<AttachmentEntity?>(null) }
                val mediaExportLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                    contract = androidx.activity.result.contract.ActivityResultContracts.CreateDocument("*/*")
                ) { targetUri ->
                    val target = pendingMediaExport
                    pendingMediaExport = null
                    if (targetUri != null && target != null) {
                        viewModel.exportAttachmentToUri(target, targetUri) { res ->
                            res.onSuccess {
                                Toast.makeText(context, "Saved copy of \"${target.fileName}\"", Toast.LENGTH_SHORT).show()
                            }.onFailure { err ->
                                Toast.makeText(context, "Export failed: ${err.localizedMessage ?: "Error"}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                ) {
                    androidx.compose.material3.OutlinedButton(
                        onClick = {
                            pendingMediaExport = item
                            mediaExportLauncher.launch(item.fileName)
                        }
                    ) {
                        Text("Save Copy")
                    }
                    Button(
                        onClick = {
                            val route = if (isPdf) {
                                Screen.PdfReader.createRoute(item.id, file.absolutePath, item.fileName)
                            } else {
                                Screen.ImageAnnotator.createRoute(item.id, file.absolutePath, item.fileName)
                            }
                            selectedAttachmentForDetails = null
                            onNavigateToDestination(route)
                        }
                    ) {
                        Icon(if (isPdf) Icons.Default.PictureAsPdf else Icons.Default.Draw, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(if (isPdf) "Open PDF Reader" else "Annotate Image")
                    }
                }

                Spacer(Modifier.height(10.dp))
            }
        }
    }

    // Delete Confirmation Dialog (moves to Recycle Bin for safe recovery)
    attachmentToDelete?.let { toDelete ->
        AlertDialog(
            onDismissRequest = { attachmentToDelete = null },
            title = { Text("Move File to Recycle Bin?") },
            text = { Text("Move '${toDelete.fileName}' to Recycle Bin? You can restore it anytime before retention purge.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.softDeleteAttachment(toDelete)
                        attachmentToDelete = null
                    }
                ) {
                    Text("Move to Bin", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { attachmentToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

private fun AttachmentEntity.isPdfMedia(): Boolean {
    return fileExtension.equals("pdf", ignoreCase = true) ||
        mimeType.contains("pdf", ignoreCase = true)
}

private fun AttachmentEntity.isImageMedia(): Boolean {
    return !isPdfMedia() && (
        mimeType.contains("image", ignoreCase = true) ||
            fileExtension.lowercase() in listOf("jpg", "jpeg", "png", "webp", "gif", "heic", "bmp")
        )
}

