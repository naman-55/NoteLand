package com.example.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.AttachmentEntity
import com.example.ui.components.DocumentAttachmentCard
import com.example.ui.components.EmptyStateView
import com.example.ui.components.NoteCard
import com.example.ui.components.isPdfDocument
import com.example.ui.navigation.Screen
import com.example.ui.theme.editorialBackdropBrush
import com.example.ui.theme.editorialHeaderTextColor
import com.example.ui.theme.isAppInDarkTheme
import com.example.ui.viewmodel.NoteViewModel
import com.example.util.FolderHierarchyUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArchiveScreen(
    viewModel: NoteViewModel,
    onNavigateToEditor: (String, String) -> Unit,
    onNavigateToCanvas: (String) -> Unit = {},
    onNavigateToDestination: (String) -> Unit = {},
    onNavigateBack: () -> Unit
) {
    val archivedNotes by viewModel.archivedNotes.collectAsStateWithLifecycle()
    val archivedAttachments by viewModel.archivedAttachments.collectAsStateWithLifecycle()
    val folders by viewModel.folders.collectAsStateWithLifecycle()

    val visibleNotes = androidx.compose.runtime.remember(archivedNotes) {
        archivedNotes.filter { !it.isLocked && !it.isDeleted }
    }
    val visibleAttachments = androidx.compose.runtime.remember(archivedAttachments) {
        archivedAttachments.filter { !it.isLocked && !it.isDeleted }
    }

    val isDark = isAppInDarkTheme
    val headerTextColor = editorialHeaderTextColor(isDark)
    val context = LocalContext.current
    var pendingExportDoc by remember { mutableStateOf<AttachmentEntity?>(null) }
    val exportDocLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("*/*")
    ) { targetUri: Uri? ->
        val att = pendingExportDoc
        pendingExportDoc = null
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

    fun openAttachment(att: AttachmentEntity) {
        val file = viewModel.attachmentStorageManager.getFileFromRelativePath(att.relativePath)
        val route = if (att.isPdfDocument()) {
            Screen.PdfReader.createRoute(att.id, file.absolutePath, att.fileName)
        } else {
            Screen.ImageAnnotator.createRoute(att.id, file.absolutePath, att.fileName)
        }
        onNavigateToDestination(route)
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
                            Icon(Icons.Default.Archive, contentDescription = null, tint = headerTextColor, modifier = Modifier.size(22.dp))
                            Spacer(Modifier.size(8.dp))
                            Text(
                                "Archive",
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
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = Color.Transparent
                    )
                )
            }
        ) { innerPadding ->
        if (visibleNotes.isEmpty() && visibleAttachments.isEmpty()) {
            EmptyStateView(
                icon = Icons.Default.Archive,
                title = "Archive is empty",
                description = "Notes and files you archive will appear here safely stored away from your main workspace.",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (visibleAttachments.isNotEmpty()) {
                    item {
                        Text(
                            "Archived Documents (${visibleAttachments.size})",
                            style = MaterialTheme.typography.titleSmall,
                            color = headerTextColor
                        )
                    }
                    items(visibleAttachments, key = { "arch_att_${it.id}" }) { att ->
                        val folderName = FolderHierarchyUtils.getFolderPathString(folders, att.folderId)
                        DocumentAttachmentCard(
                            attachment = att,
                            folderName = folderName,
                            statusBadge = "Archived",
                            onOpen = { openAttachment(att) },
                            onToggleFavorite = { viewModel.toggleAttachmentFavorite(att) },
                            onToggleArchive = { viewModel.toggleAttachmentArchive(att) },
                            onExport = {
                                pendingExportDoc = att
                                exportDocLauncher.launch(att.fileName)
                            },
                            onSoftDelete = { viewModel.softDeleteAttachment(att) }
                        )
                    }
                }

                if (visibleNotes.isNotEmpty()) {
                    if (visibleAttachments.isNotEmpty()) {
                        item {
                            Text(
                                "Archived Notes (${visibleNotes.size})",
                                style = MaterialTheme.typography.titleSmall,
                                color = headerTextColor,
                                modifier = Modifier.padding(top = 6.dp)
                            )
                        }
                    }
                    items(visibleNotes, key = { it.id }) { note ->
                        NoteCard(
                            note = note,
                            onClick = {
                                if (note.noteType == "CANVAS") {
                                    onNavigateToCanvas(note.id)
                                } else {
                                    onNavigateToEditor(note.id, note.noteType)
                                }
                            },
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
