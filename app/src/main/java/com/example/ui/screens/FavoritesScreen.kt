package com.example.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.AttachmentEntity
import com.example.ui.components.DocumentAttachmentCard
import com.example.ui.components.EmptyStateView
import com.example.ui.components.NoteCard
import com.example.ui.components.isPdfDocument
import com.example.ui.navigation.Screen
import com.example.ui.theme.editorialBackdropBrush
import com.example.ui.theme.editorialHeaderSubColor
import com.example.ui.theme.editorialHeaderTextColor
import com.example.ui.theme.isAppInDarkTheme
import com.example.ui.viewmodel.NoteViewModel
import com.example.util.FolderHierarchyUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    viewModel: NoteViewModel,
    onNavigateToEditor: (String, String) -> Unit,
    onNavigateToCanvas: (String) -> Unit = {},
    onNavigateToDestination: (String) -> Unit = {}
) {
    val favoriteNotes by viewModel.favoriteNotes.collectAsStateWithLifecycle()
    val favoriteAttachments by viewModel.favoriteAttachments.collectAsStateWithLifecycle()
    val folders by viewModel.folders.collectAsStateWithLifecycle()

    val visibleNotes = androidx.compose.runtime.remember(favoriteNotes) {
        favoriteNotes.filter { !it.isLocked && !it.isHidden && !it.isArchived && !it.isDeleted }
    }
    val visibleAttachments = androidx.compose.runtime.remember(favoriteAttachments) {
        favoriteAttachments.filter { !it.isLocked && !it.isHidden && !it.isArchived && !it.isDeleted }
    }

    val isDark = isAppInDarkTheme
    val headerTextColor = editorialHeaderTextColor(isDark)
    val headerSubColor = editorialHeaderSubColor(isDark)
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(11.dp),
                                color = Color(0xFFD97706).copy(alpha = 0.16f),
                                modifier = Modifier.size(34.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.Favorite,
                                        contentDescription = null,
                                        tint = Color(0xFFD97706),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Text(
                                "Favourites",
                                style = MaterialTheme.typography.headlineMedium,
                                color = headerTextColor
                            )
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
                icon = Icons.Default.Star,
                title = "No Favourites Yet",
                description = "Tap the heart icon on any note, PDF, or file to pin it here for instant access.",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )
        } else {
            val navBottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 104.dp + navBottomPadding)
            ) {
                item {
                    Text(
                        "${visibleNotes.size} notes · ${visibleAttachments.size} documents",
                        style = MaterialTheme.typography.bodySmall,
                        color = headerSubColor,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }

                if (visibleAttachments.isNotEmpty()) {
                    item {
                        Text(
                            "Starred Documents (${visibleAttachments.size})",
                            style = MaterialTheme.typography.titleSmall,
                            color = headerTextColor,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    items(visibleAttachments, key = { "fav_att_${it.id}" }) { att ->
                        val folderName = FolderHierarchyUtils.getFolderPathString(folders, att.folderId)
                        DocumentAttachmentCard(
                            attachment = att,
                            folderName = folderName,
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
                                "Starred Notes (${visibleNotes.size})",
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
                            onSoftDelete = { viewModel.softDeleteNote(note) }
                        )
                    }
                }
            }
        }
        }
    }
}
