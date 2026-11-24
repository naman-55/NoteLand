package com.example.ui.screens

import androidx.core.graphics.toColorInt
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Attachment
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.AttachmentEntity
import com.example.data.model.CanvasEntity
import com.example.data.model.FolderEntity
import com.example.data.model.NoteEntity
import com.example.data.model.StickyNoteEntity
import com.example.data.model.TaskEntity
import com.example.data.model.TaskPriority
import com.example.ui.components.EmptyStateView
import com.example.ui.components.NoteCard
import com.example.ui.navigation.Screen
import com.example.ui.theme.editorialBackdropBrush
import com.example.ui.theme.editorialCardBorderColor
import com.example.ui.theme.editorialCardContainerColor
import com.example.ui.theme.editorialHeaderTextColor
import com.example.ui.theme.isAppInDarkTheme
import com.example.ui.viewmodel.NoteViewModel
import com.example.util.FolderHierarchyUtils
import com.example.util.KnowledgeBrainEngine
import com.example.util.SmartNoteUtils
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SearchScreen(
    viewModel: NoteViewModel,
    onNavigateToEditor: (String, String) -> Unit,
    onNavigateToCanvas: (String) -> Unit = {},
    onNavigateToDestination: (String) -> Unit = {},
    onNavigateBack: () -> Unit
) {
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val searchFilterType by viewModel.searchFilterType.collectAsStateWithLifecycle()
    val searchSortOrder by viewModel.searchSortOrder.collectAsStateWithLifecycle()
    val searchPinnedOnly by viewModel.searchPinnedOnly.collectAsStateWithLifecycle()
    val searchHasChecklistOnly by viewModel.searchHasChecklistOnly.collectAsStateWithLifecycle()
    val searchFavoritesOnly by viewModel.searchFavoritesOnly.collectAsStateWithLifecycle()

    val universalNoteMatches by viewModel.universalNoteMatches.collectAsStateWithLifecycle()
    val universalTagMatches by viewModel.universalTagMatches.collectAsStateWithLifecycle()
    val noteLinks by viewModel.noteLinks.collectAsStateWithLifecycle()
    val allNotes by viewModel.allActiveNotes.collectAsStateWithLifecycle()
    val allFolders by viewModel.folders.collectAsStateWithLifecycle()
    val allAttachments by viewModel.attachments.collectAsStateWithLifecycle()
    val activeTasks by viewModel.activeTasks.collectAsStateWithLifecycle()
    val canvases by viewModel.canvases.collectAsStateWithLifecycle()
    val stickyNotes by viewModel.stickyNotes.collectAsStateWithLifecycle()
    val searchHistory by viewModel.searchHistory.collectAsStateWithLifecycle()

    var localQuery by remember { mutableStateOf(searchQuery) }
    var showSortMenu by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    val eligibleNotes = remember(allNotes) {
        allNotes.filter { KnowledgeBrainEngine.isNoteKnowledgeEligible(it) }
    }
    val eligibleNoteIds = remember(eligibleNotes) {
        eligibleNotes.map { it.id }.toSet()
    }
    val linkedNoteIds = remember(noteLinks, eligibleNoteIds) {
        noteLinks
            .filter { it.sourceNoteId in eligibleNoteIds }
            .flatMap { link -> listOfNotNull(link.sourceNoteId, link.targetNoteId?.takeIf { it in eligibleNoteIds }) }
            .toSet()
    }

    val displayNoteMatches = remember(universalNoteMatches, searchFilterType, localQuery, linkedNoteIds) {
        if (searchFilterType == "LINKS") {
            universalNoteMatches.filter { match ->
                if (localQuery.isBlank()) {
                    match.note.id in linkedNoteIds
                } else {
                    match.matchReasons.any {
                        it.startsWith("Links to", ignoreCase = true) ||
                            it.startsWith("Linked from", ignoreCase = true)
                    }
                }
            }
        } else {
            universalNoteMatches
        }
    }

    // 300ms Debounced search input
    LaunchedEffect(localQuery) {
        if (localQuery != searchQuery) {
            delay(300)
            viewModel.setSearchQuery(localQuery)
            if (localQuery.trim().length >= 2) {
                viewModel.addSearchQueryToHistory(localQuery)
            }
        }
    }

    // Matching folders & attachments (attachments belonging to locked/hidden/archived/deleted notes are excluded)
    val matchingFolders = remember(localQuery, allFolders, searchFilterType) {
        if (localQuery.isBlank()) {
            if (searchFilterType == "FOLDERS") allFolders else emptyList()
        } else {
            allFolders.filter { folder ->
                folder.name.contains(localQuery, ignoreCase = true) ||
                    FolderHierarchyUtils.getFolderPathString(allFolders, folder.id).contains(localQuery, ignoreCase = true)
            }
        }
    }

    val matchingAttachments = remember(localQuery, allAttachments, searchFilterType, eligibleNoteIds) {
        val visibleAttachments = allAttachments.filter { att ->
            !att.isLocked && !att.isHidden && !att.isArchived && !att.isDeleted &&
                (att.noteId == null || att.noteId in eligibleNoteIds)
        }
        if (localQuery.isBlank()) {
            if (searchFilterType == "ATTACHMENTS") visibleAttachments else emptyList()
        } else {
            visibleAttachments.filter { it.fileName.contains(localQuery, ignoreCase = true) }
        }
    }

    val matchingTasks = remember(localQuery, activeTasks, searchFilterType) {
        if (localQuery.isBlank()) {
            if (searchFilterType == "TASKS") activeTasks else emptyList()
        } else {
            activeTasks.filter { t ->
                t.title.contains(localQuery, ignoreCase = true) ||
                    t.description.contains(localQuery, ignoreCase = true) ||
                    t.category.contains(localQuery, ignoreCase = true)
            }
        }
    }

    val matchingCanvases = remember(localQuery, canvases, searchFilterType) {
        if (localQuery.isBlank()) {
            if (searchFilterType == "STUDIO") canvases else emptyList()
        } else {
            canvases.filter { it.title.contains(localQuery, ignoreCase = true) }
        }
    }

    val matchingStickies = remember(localQuery, stickyNotes, searchFilterType) {
        if (localQuery.isBlank()) {
            if (searchFilterType == "STUDIO") stickyNotes else emptyList()
        } else {
            stickyNotes.filter { s ->
                s.title.contains(localQuery, ignoreCase = true) ||
                    s.content.contains(localQuery, ignoreCase = true)
            }
        }
    }

    val sortLabel = when (searchSortOrder) {
        "CREATED_DESC" -> "Date Created"
        "TITLE_ASC" -> "Title A-Z"
        "TITLE_DESC" -> "Title Z-A"
        else -> "Recently Updated"
    }

    val isDark = isAppInDarkTheme
    val headerTextColor = editorialHeaderTextColor(isDark)
    val searchPillBg = if (isDark) com.example.ui.theme.EditorialDarkGlassCard.copy(alpha = 0.92f) else Color.White.copy(alpha = 0.80f)
    val searchPillBorder = if (isDark) Color.White.copy(alpha = 0.14f) else Color(0xFF1F1C19).copy(alpha = 0.12f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(editorialBackdropBrush(isDark))
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        // Top Search Row
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            IconButton(
                                onClick = {
                                    viewModel.setSearchQuery("")
                                    onNavigateBack()
                                },
                                modifier = Modifier.testTag("search_back_button")
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = headerTextColor)
                            }

                            Surface(
                                shape = RoundedCornerShape(24.dp),
                                color = searchPillBg,
                                border = BorderStroke(0.8.dp, searchPillBorder),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                            ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.Search,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(19.dp)
                                )
                                Box(modifier = Modifier.weight(1f)) {
                                    if (localQuery.isEmpty()) {
                                        Text(
                                            "Search notes, tasks, files, studio...",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    BasicTextField(
                                        value = localQuery,
                                        onValueChange = { localQuery = it },
                                        singleLine = true,
                                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                                            color = MaterialTheme.colorScheme.onSurface
                                        ),
                                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .focusRequester(focusRequester)
                                            .testTag("global_search_input")
                                    )
                                }
                                if (localQuery.isNotBlank()) {
                                    IconButton(
                                        onClick = {
                                            localQuery = ""
                                            viewModel.setSearchQuery("")
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Clear",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        // Sort Dropdown button
                        Box {
                            IconButton(
                                onClick = { showSortMenu = true },
                                modifier = Modifier.testTag("search_sort_button")
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Sort Options")
                            }
                            DropdownMenu(
                                expanded = showSortMenu,
                                onDismissRequest = { showSortMenu = false }
                            ) {
                                listOf(
                                    "UPDATED_DESC" to "Recently Updated",
                                    "CREATED_DESC" to "Date Created",
                                    "TITLE_ASC" to "Title A-Z",
                                    "TITLE_DESC" to "Title Z-A"
                                ).forEach { (orderKey, label) ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                label,
                                                fontWeight = if (searchSortOrder == orderKey) FontWeight.Bold else FontWeight.Normal,
                                                color = if (searchSortOrder == orderKey) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                            )
                                        },
                                        onClick = {
                                            viewModel.setSearchSortOrder(orderKey)
                                            showSortMenu = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Type & Attribute filter chips (wrapped cleanly so every scope is discoverable without hidden scrolling)
                    FlowRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        listOf(
                            "ALL" to "All",
                            "NOTES" to "Notes",
                            "TASKS" to "Tasks",
                            "FOLDERS" to "Folders",
                            "STUDIO" to "Studio",
                            "TAGS" to "Tags",
                            "LINKS" to "Links",
                            "ATTACHMENTS" to "Files"
                        ).forEach { (typeKey, label) ->
                            val isSelected = searchFilterType == typeKey
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.setSearchFilterType(typeKey) },
                                label = { Text(label) },
                                shape = RoundedCornerShape(14.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                modifier = Modifier.testTag("filter_chip_$typeKey")
                            )
                        }

                        FilterChip(
                            selected = searchPinnedOnly,
                            onClick = { viewModel.toggleSearchPinned() },
                            leadingIcon = {
                                Icon(Icons.Default.PushPin, contentDescription = null, modifier = Modifier.size(14.dp))
                            },
                            label = { Text("Pinned") },
                            shape = RoundedCornerShape(14.dp)
                        )

                        FilterChip(
                            selected = searchHasChecklistOnly,
                            onClick = { viewModel.toggleSearchChecklist() },
                            leadingIcon = {
                                Icon(Icons.Default.CheckBox, contentDescription = null, modifier = Modifier.size(14.dp))
                            },
                            label = { Text("Checklist") },
                            shape = RoundedCornerShape(14.dp)
                        )

                        FilterChip(
                            selected = searchFavoritesOnly,
                            onClick = { viewModel.toggleSearchFavorites() },
                            leadingIcon = {
                                Icon(Icons.Default.Favorite, contentDescription = null, modifier = Modifier.size(14.dp))
                            },
                            label = { Text("Favourites") },
                            shape = RoundedCornerShape(14.dp)
                        )
                    }
                }
                androidx.compose.material3.HorizontalDivider(
                    thickness = 0.8.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f)
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
        ) {
            val showFolders = searchFilterType == "ALL" || searchFilterType == "FOLDERS"
            val showTags = searchFilterType == "ALL" || searchFilterType == "TAGS"
            val showNotes = searchFilterType == "ALL" || searchFilterType == "NOTES" || searchFilterType == "LINKS"
            val showAttachments = searchFilterType == "ALL" || searchFilterType == "ATTACHMENTS"
            val showTasks = searchFilterType == "ALL" || searchFilterType == "TASKS"
            val showStudio = searchFilterType == "ALL" || searchFilterType == "STUDIO"

            val hasActiveFilters = localQuery.isNotBlank() ||
                searchPinnedOnly ||
                searchHasChecklistOnly ||
                searchFavoritesOnly ||
                searchFilterType != "ALL"

            val hasResults = (showNotes && displayNoteMatches.isNotEmpty()) ||
                (showTasks && matchingTasks.isNotEmpty()) ||
                (showStudio && (matchingCanvases.isNotEmpty() || matchingStickies.isNotEmpty())) ||
                (showFolders && matchingFolders.isNotEmpty()) ||
                (showTags && universalTagMatches.isNotEmpty() && (localQuery.isNotBlank() || searchFilterType == "TAGS")) ||
                (showAttachments && matchingAttachments.isNotEmpty())

            if (!hasResults) {
                if (!hasActiveFilters && searchHistory.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.History, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    "Recent Searches",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            TextButton(onClick = { viewModel.clearSearchHistory() }) {
                                Text("Clear History", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            searchHistory.forEach { historyQuery ->
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                    modifier = Modifier.clickable {
                                        localQuery = historyQuery
                                        viewModel.setSearchQuery(historyQuery)
                                    }
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(start = 12.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(historyQuery, style = MaterialTheme.typography.bodyMedium)
                                        IconButton(
                                            onClick = { viewModel.removeSearchHistoryItem(historyQuery) },
                                            modifier = Modifier.size(20.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Close,
                                                contentDescription = "Remove",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else if (!hasActiveFilters) {
                    EmptyStateView(
                        icon = Icons.Default.Search,
                        title = "Find Anything in NoteLand",
                        description = "Search across note titles, body paragraphs, markdown checklists, tags, attachment file names, and folders with instant offline results.",
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    EmptyStateView(
                        icon = Icons.Default.Search,
                        title = "No Matches Found",
                        description = if (localQuery.isBlank()) {
                            "No items match the currently selected filters. Try clearing a filter chip or entering a keyword."
                        } else {
                            "We couldn't find anything matching \"$localQuery\". Try checking the spelling, selecting \"All\" filters, or searching another keyword."
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(top = 12.dp, bottom = 48.dp)
                ) {
                    // Compact Recent Searches strip when browsing "All" with blank query
                    if (!hasActiveFilters && searchHistory.isNotEmpty()) {
                        item(key = "recent_searches_strip") {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.History,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            "Recent Searches",
                                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    TextButton(onClick = { viewModel.clearSearchHistory() }) {
                                        Text("Clear", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium)
                                    }
                                }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    searchHistory.forEach { historyQuery ->
                                        Surface(
                                            shape = RoundedCornerShape(16.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                            modifier = Modifier.clickable {
                                                localQuery = historyQuery
                                                viewModel.setSearchQuery(historyQuery)
                                            }
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(start = 10.dp, end = 4.dp, top = 5.dp, bottom = 5.dp),
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Text(historyQuery, style = MaterialTheme.typography.bodySmall)
                                                IconButton(
                                                    onClick = { viewModel.removeSearchHistoryItem(historyQuery) },
                                                    modifier = Modifier.size(18.dp)
                                                ) {
                                                    Icon(
                                                        Icons.Default.Close,
                                                        contentDescription = "Remove",
                                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        modifier = Modifier.size(12.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Folders Section
                    if (showFolders && matchingFolders.isNotEmpty()) {
                        item {
                            Text(
                                "Folders (${matchingFolders.size})",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                        items(matchingFolders, key = { "folder_${it.id}" }) { folder ->
                            FolderSearchResultCard(
                                folder = folder,
                                allFolders = allFolders,
                                allNotes = eligibleNotes
                            ) {
                                viewModel.setSearchQuery("")
                                viewModel.setSelectedFolder(folder.id)
                                onNavigateBack()
                            }
                        }
                    }

                    // Tags Section
                    if (showTags && universalTagMatches.isNotEmpty() && (localQuery.isNotBlank() || searchFilterType == "TAGS")) {
                        item {
                            Text(
                                "Tags (${universalTagMatches.size})",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                modifier = Modifier.padding(top = 6.dp, bottom = 4.dp)
                            )
                        }
                        item(key = "universal_tags_row") {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                universalTagMatches.forEach { tagMatch ->
                                    Surface(
                                        shape = RoundedCornerShape(14.dp),
                                        color = editorialCardContainerColor(isDark),
                                        border = BorderStroke(0.8.dp, editorialCardBorderColor(isDark)),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(14.dp))
                                            .clickable {
                                                val tagQuery = "#${tagMatch.name}"
                                                localQuery = tagQuery
                                                viewModel.setSearchQuery(tagQuery)
                                                viewModel.setSearchFilterType("ALL")
                                            }
                                            .testTag("search_tag_chip_${tagMatch.name}")
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Tag,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Text(
                                                text = "#${tagMatch.name} (${tagMatch.noteCount})",
                                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Tasks Section
                    if (showTasks && matchingTasks.isNotEmpty()) {
                        item {
                            Text(
                                "Tasks (${matchingTasks.size})",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                            )
                        }
                        items(matchingTasks, key = { "search_task_${it.id}" }) { task ->
                            TaskSearchResultCard(
                                task = task,
                                onToggleComplete = { viewModel.toggleTaskCompleted(task.id) },
                                onClick = { onNavigateToDestination(Screen.Tasks.route) }
                            )
                        }
                    }

                    // Studio Projects Section (Canvas / Mind Map / Sticky Wall)
                    if (showStudio && (matchingCanvases.isNotEmpty() || matchingStickies.isNotEmpty())) {
                        item {
                            Text(
                                "Studio (${matchingCanvases.size + matchingStickies.size})",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                            )
                        }
                        items(matchingCanvases, key = { "search_canvas_${it.id}" }) { canvas ->
                            StudioSearchResultCard(
                                title = canvas.title.ifBlank { "Untitled Board" },
                                subtitle = "Board · Updated ${SmartNoteUtils.formatRelativeTime(canvas.updatedAtUtc)}",
                                isSticky = false,
                                onClick = { onNavigateToCanvas(canvas.id) }
                            )
                        }
                        items(matchingStickies, key = { "search_sticky_${it.id}" }) { sticky ->
                            StudioSearchResultCard(
                                title = sticky.title.ifBlank { "Sticky Note" },
                                subtitle = sticky.content.replace("\n", " ").ifBlank { "Sticky Wall item" },
                                isSticky = true,
                                onClick = { onNavigateToDestination(Screen.StickyBoard.route) }
                            )
                        }
                    }

                    // Attachments Section
                    if (showAttachments && matchingAttachments.isNotEmpty()) {
                        item {
                            Text(
                                "Files (${matchingAttachments.size})",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                            )
                        }
                        items(matchingAttachments, key = { "att_${it.id}" }) { attachment ->
                            AttachmentSearchResultCard(attachment = attachment) {
                                val isPdf = attachment.mimeType.contains("pdf", ignoreCase = true) || attachment.fileName.endsWith(".pdf", ignoreCase = true)
                                val localFile = if (attachment.relativePath.startsWith("/")) {
                                    java.io.File(attachment.relativePath)
                                } else {
                                    viewModel.attachmentStorageManager.getFileFromRelativePath(attachment.relativePath)
                                }
                                if (isPdf) {
                                    onNavigateToDestination(com.example.ui.navigation.Screen.PdfReader.createRoute(attachment.id, localFile.absolutePath, attachment.fileName))
                                } else {
                                    onNavigateToDestination(com.example.ui.navigation.Screen.ImageAnnotator.createRoute(attachment.id, localFile.absolutePath, attachment.fileName))
                                }
                            }
                        }
                    }

                    // Notes & Linked Knowledge Section
                    if (showNotes && displayNoteMatches.isNotEmpty()) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    if (searchFilterType == "LINKS") "Linked Notes (${displayNoteMatches.size})" else "Notes (${displayNoteMatches.size})",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    "Sorted: $sortLabel",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        items(displayNoteMatches, key = { "note_${it.note.id}" }) { match ->
                            val note = match.note
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                if (match.matchReasons.isNotEmpty()) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        match.matchReasons.take(3).forEach { reason ->
                                            val isLinkReason = reason.startsWith("Link", ignoreCase = true)
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = if (isLinkReason) {
                                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.75f)
                                                } else {
                                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                                                }
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                ) {
                                                    if (isLinkReason) {
                                                        Icon(
                                                            Icons.Default.Link,
                                                            contentDescription = null,
                                                            tint = MaterialTheme.colorScheme.primary,
                                                            modifier = Modifier.size(12.dp)
                                                        )
                                                    }
                                                    Text(
                                                        text = reason,
                                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                                                        color = if (isLinkReason) {
                                                            MaterialTheme.colorScheme.onPrimaryContainer
                                                        } else {
                                                            MaterialTheme.colorScheme.onSurfaceVariant
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
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
    }
}

@Composable
fun AttachmentSearchResultCard(
    attachment: AttachmentEntity,
    onClick: () -> Unit
) {
    val isPdf = attachment.mimeType.contains("pdf", ignoreCase = true) || attachment.fileName.endsWith(".pdf", ignoreCase = true)

    val isDark = isAppInDarkTheme
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = editorialCardContainerColor(isDark)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(0.8.dp, editorialCardBorderColor(isDark)),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(11.dp),
                    color = if (isPdf) Color(0xFFEF4444).copy(alpha = 0.13f) else Color(0xFF3B82F6).copy(alpha = 0.13f),
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            if (isPdf) Icons.Default.PictureAsPdf else Icons.Default.Image,
                            contentDescription = null,
                            tint = if (isPdf) Color(0xFFDC2626) else Color(0xFF2563EB),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Column {
                    Text(
                        attachment.fileName,
                        style = MaterialTheme.typography.titleSmall.copy(fontSize = 15.sp),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "${SmartNoteUtils.formatFileSize(attachment.sizeBytes)} · ${SmartNoteUtils.formatRelativeTime(attachment.createdAtUtc)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
fun FolderSearchResultCard(
    folder: FolderEntity,
    allFolders: List<FolderEntity> = emptyList(),
    allNotes: List<NoteEntity> = emptyList(),
    onClick: () -> Unit
) {
    val baseColor = try {
        Color(folder.colorHex.toColorInt())
    } catch (_: Exception) {
        MaterialTheme.colorScheme.primary
    }
    val parentPath = remember(allFolders, folder.id) {
        FolderHierarchyUtils.getParentPathString(allFolders, folder.id)
    }
    val subCount = remember(allFolders, folder.id) {
        FolderHierarchyUtils.getSubfolders(allFolders, folder.id).size
    }
    val totalNoteCount = remember(allFolders, allNotes, folder.id) {
        FolderHierarchyUtils.countSubtreeNotes(allFolders, allNotes, folder.id)
    }
    val subtitleText = buildList {
        if (parentPath.isNotBlank()) add("Inside $parentPath") else add("Top-level folder")
        if (subCount > 0) add("$subCount sub")
        if (allNotes.isNotEmpty()) add("$totalNoteCount notes")
    }.joinToString(" · ")

    val isDark = isAppInDarkTheme
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = editorialCardContainerColor(isDark)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(0.8.dp, editorialCardBorderColor(isDark)),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    shape = RoundedCornerShape(11.dp),
                    color = baseColor.copy(alpha = 0.14f),
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Folder, contentDescription = null, tint = baseColor, modifier = Modifier.size(20.dp))
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        folder.name,
                        style = MaterialTheme.typography.titleSmall.copy(fontSize = 15.sp),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        subtitleText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun TaskSearchResultCard(
    task: TaskEntity,
    onToggleComplete: () -> Unit,
    onClick: () -> Unit
) {
    val isDark = isAppInDarkTheme
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = editorialCardContainerColor(isDark)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(0.8.dp, editorialCardBorderColor(isDark)),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .testTag("search_task_card_${task.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(onClick = onToggleComplete, modifier = Modifier.size(48.dp)) {
                Icon(
                    imageVector = if (task.isCompleted) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                    contentDescription = "Toggle ${task.title}",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title.ifBlank { "Task" },
                    style = MaterialTheme.typography.titleSmall.copy(fontSize = 15.sp),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val datePart = (task.scheduledStartUtc ?: task.dueAtUtc)?.let { SmartNoteUtils.formatShortDate(it) }
                Text(
                    text = listOfNotNull(TaskPriority.fromName(task.priority).label, task.category.ifBlank { null }, datePart).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.padding(end = 8.dp).size(16.dp)
            )
        }
    }
}

@Composable
private fun StudioSearchResultCard(
    title: String,
    subtitle: String,
    isSticky: Boolean,
    onClick: () -> Unit
) {
    val isDark = isAppInDarkTheme
    val tint = if (isSticky) Color(0xFFF59E0B) else Color(0xFF059669)
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = editorialCardContainerColor(isDark)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(0.8.dp, editorialCardBorderColor(isDark)),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    shape = RoundedCornerShape(11.dp),
                    color = tint.copy(alpha = 0.14f),
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            if (isSticky) Icons.Default.Layers else Icons.Default.AccountTree,
                            contentDescription = null,
                            tint = tint,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall.copy(fontSize = 15.sp),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
