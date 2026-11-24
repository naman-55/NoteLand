package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.NoteType
import com.example.ui.components.EmptyStateView
import com.example.ui.components.NoteCard
import com.example.ui.components.StaggeredTwoColumnNotesLayout
import com.example.ui.theme.AppSection
import com.example.ui.theme.DarkHomeAccent
import com.example.ui.theme.DisplayFontFamily
import com.example.ui.theme.LightHomeAccent
import com.example.ui.theme.ProvideSectionTheme
import com.example.ui.theme.ScreenBackdropTone
import com.example.ui.theme.editorialBackdropBrush
import com.example.ui.theme.editorialHeaderSubColor
import com.example.ui.theme.editorialHeaderTextColor
import com.example.ui.theme.isAppInDarkTheme
import com.example.ui.viewmodel.NoteViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PinnedNotesScreen(
    viewModel: NoteViewModel,
    onNavigateToEditor: (String, String) -> Unit,
    onNavigateToCanvas: (String) -> Unit,
    onNavigateBack: () -> Unit
) {
    val allNotes by viewModel.allActiveNotes.collectAsStateWithLifecycle()
    val isGridView by viewModel.isGridView.collectAsStateWithLifecycle()
    val isDark = isAppInDarkTheme
    val headerTextColor = editorialHeaderTextColor(isDark)
    val headerSubColor = editorialHeaderSubColor(isDark)
    val homeAccent = if (isDark) DarkHomeAccent else LightHomeAccent
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val pinnedNotes = remember(allNotes) {
        allNotes
            .filter { it.isPinned && !it.isLocked && !it.isHidden && !it.isArchived && !it.isDeleted && it.noteType != NoteType.PDF_DOC.name }
            .sortedByDescending { it.updatedAtUtc }
    }

    ProvideSectionTheme(section = AppSection.HOME, isDark = isDark) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(editorialBackdropBrush(isDark, ScreenBackdropTone.DEFAULT))
                .testTag("pinned_notes_screen_root")
        ) {
            Scaffold(
                containerColor = Color.Transparent,
                snackbarHost = {
                    SnackbarHost(
                        hostState = snackbarHostState,
                        modifier = Modifier.navigationBarsPadding()
                    )
                },
                topBar = {
                    TopAppBar(
                        navigationIcon = {
                            IconButton(
                                onClick = onNavigateBack,
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("pinned_notes_back_btn")
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = headerTextColor
                                )
                            }
                        },
                        title = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(11.dp),
                                    color = homeAccent.copy(alpha = if (isDark) 0.22f else 0.14f),
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Default.PushPin,
                                            contentDescription = null,
                                            tint = homeAccent,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "Pinned Notes",
                                    style = MaterialTheme.typography.headlineSmall.copy(
                                        fontFamily = DisplayFontFamily,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = headerTextColor
                                )
                            }
                        },
                        actions = {
                            IconButton(
                                onClick = { viewModel.toggleGridView() },
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("pinned_notes_layout_toggle")
                            ) {
                                Icon(
                                    imageVector = if (isGridView) Icons.AutoMirrored.Filled.ViewList else Icons.Default.GridView,
                                    contentDescription = if (isGridView) "Switch to List View" else "Switch to Grid View",
                                    tint = headerSubColor
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
                if (pinnedNotes.isEmpty()) {
                    EmptyStateView(
                        icon = Icons.Default.PushPin,
                        title = "No Pinned Notes Yet",
                        description = "Pin any important note to keep it right here for instant access.",
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .testTag("pinned_notes_empty_state")
                    )
                } else {
                    val navBottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        LazyColumn(
                            modifier = Modifier
                                .widthIn(max = 840.dp)
                                .fillMaxSize()
                                .testTag("pinned_notes_list"),
                            contentPadding = PaddingValues(
                                start = 20.dp,
                                end = 20.dp,
                                top = 8.dp,
                                bottom = 32.dp + navBottomPadding
                            ),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            item {
                                Text(
                                    text = "${pinnedNotes.size} pinned ${if (pinnedNotes.size == 1) "note" else "notes"}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = headerSubColor
                                )
                            }

                            if (isGridView) {
                                item(key = "pinned_notes_masonry_grid") {
                                    StaggeredTwoColumnNotesLayout(
                                        horizontalSpacing = 12.dp,
                                        verticalSpacing = 12.dp,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        pinnedNotes.forEach { note ->
                                            key(note.id) {
                                                NoteCard(
                                                    note = note,
                                                    onClick = {
                                                        if (note.noteType == NoteType.CANVAS.name) {
                                                            onNavigateToCanvas(note.id)
                                                        } else {
                                                            onNavigateToEditor(note.id, note.noteType)
                                                        }
                                                    },
                                                    onTogglePin = { viewModel.togglePin(note) },
                                                    onToggleFavorite = { viewModel.toggleFavorite(note) },
                                                    onToggleArchive = {
                                                        viewModel.toggleArchive(note)
                                                        scope.launch {
                                                            val res = snackbarHostState.showSnackbar(
                                                                message = "Note archived",
                                                                actionLabel = "Undo",
                                                                duration = SnackbarDuration.Short
                                                            )
                                                            if (res == SnackbarResult.ActionPerformed) {
                                                                viewModel.toggleArchive(note.copy(isArchived = true))
                                                            }
                                                        }
                                                    },
                                                    onDelete = {
                                                        viewModel.softDeleteNote(note)
                                                        scope.launch {
                                                            val res = snackbarHostState.showSnackbar(
                                                                message = "Note moved to trash",
                                                                actionLabel = "Undo",
                                                                duration = SnackbarDuration.Short
                                                            )
                                                            if (res == SnackbarResult.ActionPerformed) {
                                                                viewModel.restoreNote(note)
                                                            }
                                                        }
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            } else {
                                items(pinnedNotes, key = { it.id }) { note ->
                                    NoteCard(
                                        note = note,
                                        onClick = {
                                            if (note.noteType == NoteType.CANVAS.name) {
                                                onNavigateToCanvas(note.id)
                                            } else {
                                                onNavigateToEditor(note.id, note.noteType)
                                            }
                                        },
                                        onTogglePin = { viewModel.togglePin(note) },
                                        onToggleFavorite = { viewModel.toggleFavorite(note) },
                                        onToggleArchive = {
                                            viewModel.toggleArchive(note)
                                            scope.launch {
                                                val res = snackbarHostState.showSnackbar(
                                                    message = "Note archived",
                                                    actionLabel = "Undo",
                                                    duration = SnackbarDuration.Short
                                                )
                                                if (res == SnackbarResult.ActionPerformed) {
                                                    viewModel.toggleArchive(note.copy(isArchived = true))
                                                }
                                            }
                                        },
                                        onDelete = {
                                            viewModel.softDeleteNote(note)
                                            scope.launch {
                                                val res = snackbarHostState.showSnackbar(
                                                    message = "Note moved to trash",
                                                    actionLabel = "Undo",
                                                    duration = SnackbarDuration.Short
                                                )
                                                if (res == SnackbarResult.ActionPerformed) {
                                                    viewModel.restoreNote(note)
                                                }
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth()
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
