package com.example.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.graphics.toColorInt
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.AttachmentEntity
import com.example.data.model.FolderEntity
import com.example.data.model.NoteEntity
import com.example.ui.components.DocumentAttachmentCard
import com.example.ui.components.DocumentMetadataDialog
import com.example.ui.components.FolderBreadcrumbBar
import com.example.ui.components.FolderTreeItemRow
import com.example.ui.components.HierarchicalFolderPickerSheet
import com.example.ui.components.RenameDocumentDialog
import com.example.ui.components.StaggeredSectionEntrance
import com.example.ui.components.isPdfDocument
import com.example.ui.components.springPressScale
import androidx.compose.foundation.layout.widthIn
import com.example.ui.navigation.Screen
import com.example.ui.theme.AppSection
import com.example.ui.theme.DarkFoldersAccent
import com.example.ui.theme.DisplayFontFamily
import com.example.ui.theme.LightFoldersAccent
import com.example.ui.theme.NotelandAmber
import com.example.ui.theme.NotelandBlue
import com.example.ui.theme.NotelandEmerald
import com.example.ui.theme.NotelandMint
import com.example.ui.theme.NotelandPurple
import com.example.ui.theme.ProvideSectionTheme
import com.example.ui.theme.ScreenBackdropTone
import com.example.ui.theme.editorialBackdropBrush
import com.example.ui.theme.editorialCardBorderColor
import com.example.ui.theme.editorialCardContainerColor
import com.example.ui.theme.editorialHeaderSubColor
import com.example.ui.theme.editorialHeaderTextColor
import com.example.ui.theme.isAppInDarkTheme
import com.example.ui.viewmodel.NoteViewModel
import com.example.util.FolderHierarchyUtils
import com.example.util.SmartNoteUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val EditorialTerracotta: Color
    @Composable
    get() = if (isAppInDarkTheme) DarkFoldersAccent else LightFoldersAccent
private val EditorialWarmSand = Color(0xFFF59E0B)
private val EditorialSageOlive = Color(0xFF15865D)
private val EditorialMutedSlate = Color(0xFF3B82F6)
private val EditorialWarmClay = Color(0xFF8B5CF6)
private val EditorialPinSlate = Color(0xFF34B089)

private enum class FolderFilterTab(val label: String) {
    ALL("All"),
    WITH_NOTES("Active"),
    EMPTY("Empty")
}

private enum class InsideFolderFilter(val label: String) {
    ALL("All"),
    DOCUMENTS("Files"),
    PINNED("Pinned"),
    SIMPLE_NOTES("Simple Notes"),
    TO_DO("To - Do")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoldersScreen(
    viewModel: NoteViewModel,
    onNavigateToEditor: (String, String) -> Unit,
    onNavigateToDestination: (String) -> Unit,
    onCreateNoteInFolder: (String) -> Unit = { onNavigateToEditor("0", "STANDARD") }
) {
    val folders by viewModel.folders.collectAsStateWithLifecycle()
    val allNotes by viewModel.allActiveNotes.collectAsStateWithLifecycle()
    val favorites by viewModel.favoriteNotes.collectAsStateWithLifecycle()
    val favoriteAttachments by viewModel.favoriteAttachments.collectAsStateWithLifecycle()
    val archivedNotes by viewModel.archivedNotes.collectAsStateWithLifecycle()
    val archivedAttachments by viewModel.archivedAttachments.collectAsStateWithLifecycle()
    val deletedNotes by viewModel.deletedNotes.collectAsStateWithLifecycle()
    val deletedAttachments by viewModel.deletedAttachments.collectAsStateWithLifecycle()
    val lockedNotes by viewModel.lockedNotes.collectAsStateWithLifecycle()
    val hiddenNotes by viewModel.hiddenNotes.collectAsStateWithLifecycle()
    val lockedAttachments by viewModel.lockedAttachments.collectAsStateWithLifecycle()
    val attachments by viewModel.attachments.collectAsStateWithLifecycle()
    val activeDocuments = remember(attachments) {
        attachments.filter {
            !it.id.startsWith("demo-") && it.id != "demo_pdf" &&
                !it.isLocked && !it.isHidden && !it.isArchived && !it.isDeleted
        }
    }
    val pdfCount = remember(activeDocuments) {
        activeDocuments.count { it.isPdfDocument() }
    }

    val context = LocalContext.current

    var showCreateDialog by remember { mutableStateOf(false) }
    var folderToEdit by remember { mutableStateOf<FolderEntity?>(null) }
    var folderNameInput by remember { mutableStateOf("") }
    var selectedColorHex by remember { mutableStateOf("#15865D") }
    var selectedIconName by remember { mutableStateOf("work") }
    var createParentFolderId by remember { mutableStateOf<String?>(null) }
    var showParentPickerSheet by remember { mutableStateOf(false) }

    var selectedFolderId by rememberSaveable { mutableStateOf<String?>(null) }
    var isTreeViewMode by rememberSaveable { mutableStateOf(false) }
    var includeSubfolderNotes by rememberSaveable { mutableStateOf(false) }
    val expandedTreeNodes = remember { mutableStateMapOf<String, Boolean>() }

    var folderSortOption by remember { mutableStateOf(FolderSortOption.DATE_MODIFIED) }
    var folderRootFilter by remember { mutableStateOf(FolderFilterTab.ALL) }
    var insideFilter by remember { mutableStateOf(InsideFolderFilter.ALL) }
    var folderToDelete by remember { mutableStateOf<FolderEntity?>(null) }
    var folderToMove by remember { mutableStateOf<FolderEntity?>(null) }
    var noteToMove by remember { mutableStateOf<NoteEntity?>(null) }
    var attachmentToMove by remember { mutableStateOf<AttachmentEntity?>(null) }
    var attachmentToRename by remember { mutableStateOf<AttachmentEntity?>(null) }
    var attachmentMetadataTarget by remember { mutableStateOf<AttachmentEntity?>(null) }
    var pendingImportTargetFolderId by remember { mutableStateOf<String?>(null) }
    var pendingExportDoc by remember { mutableStateOf<AttachmentEntity?>(null) }
    var rootSearchQuery by rememberSaveable { mutableStateOf("") }
    var showRootSortDropdown by remember { mutableStateOf(false) }
    var showAllUnfiledDocs by rememberSaveable { mutableStateOf(false) }

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

    val importDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { pickedUri ->
            val mime = (context.contentResolver.getType(pickedUri) ?: "").lowercase(Locale.US)
            val isPdf = mime.contains("pdf") || pickedUri.toString().lowercase(Locale.US).endsWith(".pdf")
            val fallbackName = if (isPdf) "Document_${System.currentTimeMillis()}.pdf" else "File_${System.currentTimeMillis()}"
            viewModel.importMediaFile(
                uri = pickedUri,
                fileName = fallbackName,
                isPdf = isPdf,
                folderId = pendingImportTargetFolderId,
                onDeduplicated = { wasDedup ->
                    if (wasDedup) {
                        Toast.makeText(context, "Linked to existing file (no duplicate storage)", Toast.LENGTH_SHORT).show()
                    }
                },
                onError = { err ->
                    Toast.makeText(context, err.message ?: "Could not import document", Toast.LENGTH_SHORT).show()
                },
                onSaved = { created ->
                    Toast.makeText(context, "Imported \"${created.fileName}\"", Toast.LENGTH_SHORT).show()
                }
            )
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

    val activeFolder = remember(folders, selectedFolderId) {
        selectedFolderId?.let { id -> folders.firstOrNull { it.id == id } }
    }

    val isDark = isAppInDarkTheme

    val presetColors = listOf(
        "#15865D" to "Emerald",
        "#34B089" to "Mint",
        "#3B82F6" to "Blue",
        "#8B5CF6" to "Purple",
        "#F59E0B" to "Amber",
        "#EC4899" to "Rose",
        "#D85C37" to "Coral"
    )

    val presetIcons = listOf(
        "work" to Icons.Default.Work,
        "person" to Icons.Default.Person,
        "school" to Icons.Default.School,
        "lightbulb" to Icons.Default.Lightbulb,
        "flight" to Icons.Default.Flight,
        "other" to Icons.Default.MoreHoriz
    )

    fun openCreateFolderDialog(parentId: String?) {
        folderToEdit = null
        folderNameInput = ""
        selectedColorHex = "#15865D"
        selectedIconName = "work"
        createParentFolderId = parentId
        showCreateDialog = true
    }

    fun openEditFolderDialog(folder: FolderEntity) {
        folderToEdit = folder
        folderNameInput = folder.name
        selectedColorHex = folder.colorHex
        selectedIconName = folder.iconName
        createParentFolderId = folder.parentId
        showCreateDialog = true
    }

    val pendingWidgetFolderId by viewModel.pendingWidgetFolderId.collectAsStateWithLifecycle()
    val pendingWidgetCreateFolder by viewModel.pendingWidgetCreateFolder.collectAsStateWithLifecycle()
    androidx.compose.runtime.LaunchedEffect(pendingWidgetFolderId, pendingWidgetCreateFolder) {
        val targetFolderId = pendingWidgetFolderId
        val shouldCreate = pendingWidgetCreateFolder
        if (!targetFolderId.isNullOrBlank()) {
            selectedFolderId = targetFolderId
            insideFilter = InsideFolderFilter.ALL
            viewModel.consumePendingFolderRequest()
        } else if (shouldCreate) {
            selectedFolderId = null
            openCreateFolderDialog(parentId = null)
            viewModel.consumePendingFolderRequest()
        }
    }

    // Back button steps up one level in hierarchy, or exits detail view back to root
    BackHandler(enabled = activeFolder != null) {
        selectedFolderId = activeFolder?.parentId
        insideFilter = InsideFolderFilter.ALL
    }

    val backdropBrush = editorialBackdropBrush(isDark, ScreenBackdropTone.FOLDERS_SKY)
    val headerTextColor = editorialHeaderTextColor(isDark)
    val headerSubColor = editorialHeaderSubColor(isDark)

    ProvideSectionTheme(section = AppSection.FOLDERS, isDark = isDark) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(backdropBrush)
    ) {
        val isCompactWidth = maxWidth < 600.dp
        AnimatedContent(
            targetState = activeFolder,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "folder_detail_transition"
        ) { currentFolder ->
            if (currentFolder == null) {
                val rootFolders = remember(folders) {
                    FolderHierarchyUtils.getSubfolders(folders, parentId = null)
                }
                val displayedRootFolders = remember(
                    rootFolders,
                    folders,
                    allNotes,
                    activeDocuments,
                    folderRootFilter,
                    rootSearchQuery,
                    folderSortOption
                ) {
                    val tabFiltered = when (folderRootFilter) {
                        FolderFilterTab.ALL -> rootFolders
                        FolderFilterTab.WITH_NOTES -> rootFolders.filter { f ->
                            FolderHierarchyUtils.isFolderOrSubtreeActive(folders, allNotes, activeDocuments, f.id)
                        }
                        FolderFilterTab.EMPTY -> rootFolders.filter { f ->
                            !FolderHierarchyUtils.isFolderOrSubtreeActive(folders, allNotes, activeDocuments, f.id)
                        }
                    }
                    val q = rootSearchQuery.trim()
                    val queryFiltered = if (q.isEmpty()) {
                        tabFiltered
                    } else {
                        tabFiltered.filter { f ->
                            val subtreeIds = FolderHierarchyUtils.getSubtreeFolderIds(folders, f.id)
                            f.name.contains(q, ignoreCase = true) ||
                                folders.any { sub -> sub.id in subtreeIds && sub.name.contains(q, ignoreCase = true) } ||
                                allNotes.any { n -> !n.isLocked && n.folderId in subtreeIds && (n.title.contains(q, ignoreCase = true) || n.content.contains(q, ignoreCase = true)) } ||
                                activeDocuments.any { d -> d.folderId in subtreeIds && d.fileName.contains(q, ignoreCase = true) }
                        }
                    }
                    fun latestActivity(folder: FolderEntity): Long {
                        val subtreeIds = FolderHierarchyUtils.getSubtreeFolderIds(folders, folder.id)
                        val noteMax = allNotes.filter { it.folderId != null && it.folderId in subtreeIds }.maxOfOrNull { it.updatedAtUtc } ?: 0L
                        val docMax = activeDocuments.filter { it.folderId != null && it.folderId in subtreeIds }.maxOfOrNull { it.updatedAtUtc } ?: 0L
                        return maxOf(folder.updatedAtUtc, noteMax, docMax)
                    }
                    when (folderSortOption) {
                        FolderSortOption.DATE_MODIFIED -> queryFiltered.sortedByDescending { latestActivity(it) }
                        FolderSortOption.DATE_CREATED -> queryFiltered.sortedByDescending { it.createdAtUtc }
                        FolderSortOption.ALPHABETICAL_ASC -> queryFiltered.sortedBy { it.name.lowercase(Locale.getDefault()) }
                        FolderSortOption.ALPHABETICAL_DESC -> queryFiltered.sortedByDescending { it.name.lowercase(Locale.getDefault()) }
                    }
                }

                // Visible nodes when Tree View mode is active
                val visibleTreeNodes = remember(folders, allNotes, activeDocuments, folderRootFilter, rootSearchQuery, expandedTreeNodes.toMap()) {
                    val byId = folders.associateBy { it.id }
                    val allNodes = FolderHierarchyUtils.flattenFolderTree(folders, allNotes, attachments = activeDocuments)
                    val filteredIds = when (folderRootFilter) {
                        FolderFilterTab.ALL -> null
                        FolderFilterTab.WITH_NOTES -> allNodes.filter { it.totalNoteCount > 0 || it.totalDocumentCount > 0 }.map { it.folder.id }.toSet()
                        FolderFilterTab.EMPTY -> allNodes.filter { it.totalNoteCount == 0 && it.totalDocumentCount == 0 }.map { it.folder.id }.toSet()
                    }
                    val q = rootSearchQuery.trim()
                    allNodes.filter { node ->
                        val passesFilter = filteredIds == null || node.folder.id in filteredIds
                        if (!passesFilter) return@filter false
                        if (q.isNotEmpty()) {
                            return@filter node.folder.name.contains(q, ignoreCase = true) ||
                                node.fullPath.contains(q, ignoreCase = true)
                        }
                        if (folderRootFilter != FolderFilterTab.ALL) return@filter true
                        // Check every ancestor is expanded (default expanded for top level)
                        val trail = FolderHierarchyUtils.getBreadcrumbTrail(folders, node.folder.id)
                        val ancestors = trail.dropLast(1)
                        ancestors.all { anc ->
                            val resolved = FolderHierarchyUtils.resolveEffectiveParentId(anc, byId)
                            expandedTreeNodes[anc.id] ?: (resolved == null)
                        }
                    }
                }

                val unfiledDocs = remember(activeDocuments, rootSearchQuery, folderSortOption) {
                    val q = rootSearchQuery.trim()
                    val base = activeDocuments.filter {
                        it.folderId == null && it.noteId == null &&
                            (q.isEmpty() || it.fileName.contains(q, ignoreCase = true))
                    }
                    when (folderSortOption) {
                        FolderSortOption.DATE_MODIFIED -> base.sortedByDescending { it.updatedAtUtc }
                        FolderSortOption.DATE_CREATED -> base.sortedByDescending { it.createdAtUtc }
                        FolderSortOption.ALPHABETICAL_ASC -> base.sortedBy { it.fileName.lowercase(Locale.getDefault()) }
                        FolderSortOption.ALPHABETICAL_DESC -> base.sortedByDescending { it.fileName.lowercase(Locale.getDefault()) }
                    }
                }

                val navBottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 840.dp)
                        .align(Alignment.TopCenter)
                        .statusBarsPadding()
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(top = 24.dp, bottom = 148.dp + navBottomPadding)
                ) {
                    // Top-Right Circular Action Controls Row + Left-Aligned Editorial "Folders" Header
                    item {
                        StaggeredSectionEntrance(index = 0) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 2.dp, bottom = 2.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    GlassSquareIconButton(
                                        icon = if (isTreeViewMode) Icons.Default.GridView else Icons.Default.AccountTree,
                                        contentDescription = if (isTreeViewMode) "Switch to Folder Cards Grid" else "Switch to Folder Hierarchy Tree",
                                        isDark = isDark,
                                        onClick = { isTreeViewMode = !isTreeViewMode },
                                        modifier = Modifier.testTag("folders_tree_toggle_button")
                                    )
                                    GlassSquareIconButton(
                                        icon = Icons.Default.Search,
                                        contentDescription = "Search",
                                        isDark = isDark,
                                        onClick = { onNavigateToDestination(Screen.Search.route) },
                                        modifier = Modifier.testTag("folders_search_button")
                                    )
                                    GlassSquareIconButton(
                                        icon = Icons.Default.Add,
                                        contentDescription = "Add Folder",
                                        isDark = isDark,
                                        accentHighlight = true,
                                        onClick = { openCreateFolderDialog(parentId = null) },
                                        modifier = Modifier.testTag("add_folder_button")
                                    )
                                }
                            }

                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = "Folders",
                                    style = MaterialTheme.typography.displayLarge.copy(
                                        fontFamily = DisplayFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 32.sp,
                                        lineHeight = 36.sp,
                                        letterSpacing = (-0.5).sp
                                    ),
                                    color = headerTextColor
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                val nestedCount = (folders.size - rootFolders.size).coerceAtLeast(0)
                                val filesLabel = if (activeDocuments.isNotEmpty()) " · ${activeDocuments.size} files" else ""
                                Text(
                                    text = if (nestedCount > 0) {
                                        "${rootFolders.size} main folders · $nestedCount nested · ${allNotes.size} notes$filesLabel"
                                    } else {
                                        "${folders.size} collections · ${allNotes.size} notes$filesLabel"
                                    },
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp),
                                    color = headerSubColor
                                )
                            }
                        }
                        }
                    }

                    // Crisp Pill Search Bar
                    item {
                        StaggeredSectionEntrance(index = 1) {
                        OutlinedTextField(
                            value = rootSearchQuery,
                            onValueChange = { rootSearchQuery = it },
                            placeholder = { Text("Filter folders and unfiled files...", fontSize = 13.5.sp) },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Search,
                                    contentDescription = null,
                                    tint = headerSubColor,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            trailingIcon = {
                                if (rootSearchQuery.isNotEmpty()) {
                                    IconButton(onClick = { rootSearchQuery = "" }) {
                                        Icon(
                                            Icons.Default.Clear,
                                            contentDescription = "Clear folder filter",
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            },
                            singleLine = true,
                            shape = CircleShape,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = editorialCardContainerColor(isDark),
                                unfocusedContainerColor = editorialCardContainerColor(isDark),
                                focusedBorderColor = (if (isDark) DarkFoldersAccent else LightFoldersAccent).copy(alpha = 0.85f),
                                unfocusedBorderColor = editorialCardBorderColor(isDark)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .then(
                                    if (!isDark) {
                                        Modifier.shadow(
                                            elevation = 4.dp,
                                            shape = CircleShape,
                                            ambientColor = LightFoldersAccent.copy(alpha = 0.08f),
                                            spotColor = Color.Black.copy(alpha = 0.05f)
                                        )
                                    } else Modifier
                                )
                                .testTag("folders_inline_search_input")
                        )
                        }
                    }

                    // Horizontal Filter & Sort Pills
                    item {
                        StaggeredSectionEntrance(index = 2) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FolderFilterTab.values().forEach { tab ->
                                EditorialFilterPill(
                                    label = tab.label,
                                    selected = folderRootFilter == tab,
                                    isDark = isDark,
                                    onClick = { folderRootFilter = tab }
                                )
                            }

                            Box {
                                Surface(
                                    shape = CircleShape,
                                    color = editorialCardContainerColor(isDark),
                                    border = BorderStroke(
                                        0.9.dp,
                                        editorialCardBorderColor(isDark)
                                    ),
                                    modifier = Modifier
                                        .heightIn(min = 40.dp)
                                        .clip(CircleShape)
                                        .clickable { showRootSortDropdown = true }
                                        .testTag("folders_root_sort_button")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 13.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                                    ) {
                                        Icon(
                                            Icons.AutoMirrored.Filled.Sort,
                                            contentDescription = "Sort Folders",
                                            modifier = Modifier.size(15.dp),
                                            tint = headerTextColor
                                        )
                                        Text(
                                            text = folderSortOption.label,
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                                            color = headerTextColor,
                                            modifier = Modifier.testTag("folders_root_sort_label")
                                        )
                                    }
                                }
                                DropdownMenu(
                                    expanded = showRootSortDropdown,
                                    onDismissRequest = { showRootSortDropdown = false }
                                ) {
                                    FolderSortOption.values().forEach { option ->
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    option.label,
                                                    fontWeight = if (folderSortOption == option) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (folderSortOption == option) EditorialTerracotta else MaterialTheme.colorScheme.onSurface
                                                )
                                            },
                                            onClick = {
                                                folderSortOption = option
                                                showRootSortDropdown = false
                                            },
                                            modifier = Modifier.testTag("folders_root_sort_${option.name.lowercase()}")
                                        )
                                    }
                                }
                            }
                        }
                        }
                    }

                    val isEmptyList = if (isTreeViewMode) visibleTreeNodes.isEmpty() else displayedRootFolders.isEmpty()
                    if (isEmptyList) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(22.dp),
                                color = editorialCardContainerColor(isDark),
                                border = BorderStroke(
                                    0.85.dp,
                                    editorialCardBorderColor(isDark)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(28.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "No folders match this view",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Tap + above to organize your notes into folders and subfolders.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (isDark) Color.White.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    } else if (isTreeViewMode) {
                        // Interactive Expandable Hierarchy Tree View
                        items(visibleTreeNodes, key = { "tree_${it.folder.id}" }) { node ->
                            val defaultExpanded = node.depth == 0
                            val isExpanded = expandedTreeNodes[node.folder.id] ?: defaultExpanded
                            FolderTreeItemRow(
                                node = node,
                                isExpanded = isExpanded,
                                isDark = isDark,
                                onToggleExpand = {
                                    expandedTreeNodes[node.folder.id] = !isExpanded
                                },
                                onOpenFolder = {
                                    insideFilter = InsideFolderFilter.ALL
                                    selectedFolderId = node.folder.id
                                },
                                onAddSubfolder = { openCreateFolderDialog(parentId = node.folder.id) },
                                onEditFolder = { openEditFolderDialog(node.folder) },
                                onMoveFolder = { folderToMove = node.folder },
                                onDeleteFolder = { folderToDelete = node.folder }
                            )
                        }
                    } else {
                        // 2-Column Editorial Root Folder Cards (with Subfolder & Recursive Note Counts)
                        itemsIndexed(displayedRootFolders.chunked(2), key = { _, pair -> pair.joinToString { it.id } }) { rowIdx, rowFolders ->
                            StaggeredSectionEntrance(index = 3 + rowIdx) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                rowFolders.forEachIndexed { idx, folder ->
                                    val subtreeIds = FolderHierarchyUtils.getSubtreeFolderIds(folders, folder.id)
                                    val subtreeNotes = allNotes.filter { it.folderId != null && it.folderId in subtreeIds }
                                    val subtreeDocs = activeDocuments.filter { it.folderId != null && it.folderId in subtreeIds }
                                    val directSubfolders = FolderHierarchyUtils.getSubfolders(folders, folder.id)
                                    val globalIdx = displayedRootFolders.indexOf(folder).coerceAtLeast(idx)
                                    val latestTs = maxOf(
                                        folder.updatedAtUtc,
                                        subtreeNotes.maxOfOrNull { it.updatedAtUtc } ?: 0L,
                                        subtreeDocs.maxOfOrNull { it.updatedAtUtc } ?: 0L
                                    )
                                    FolderPastelCard(
                                        folder = folder,
                                        noteCount = subtreeNotes.size,
                                        fileCount = subtreeDocs.size,
                                        subfolderCount = directSubfolders.size,
                                        latestUpdatedUtc = latestTs,
                                        cardToneIndex = globalIdx,
                                        onClick = {
                                            insideFilter = InsideFolderFilter.ALL
                                            selectedFolderId = folder.id
                                        },
                                        onAddSubfolder = { openCreateFolderDialog(parentId = folder.id) },
                                        onEdit = { openEditFolderDialog(folder) },
                                        onMove = { folderToMove = folder },
                                        onDelete = { folderToDelete = folder },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                if (rowFolders.size == 1) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                            }
                        }
                    }

                    // Loose unfiled documents (files not yet assigned to a folder or note)
                    if (unfiledDocs.isNotEmpty()) {
                        val visibleUnfiled = if (showAllUnfiledDocs) unfiledDocs else unfiledDocs.take(5)
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 10.dp, bottom = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Unfiled Documents (${unfiledDocs.size})",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 16.sp
                                    ),
                                    color = headerTextColor
                                )
                                if (unfiledDocs.size > 5) {
                                    TextButton(onClick = { showAllUnfiledDocs = !showAllUnfiledDocs }) {
                                        Text(
                                            if (showAllUnfiledDocs) "Show fewer" else "Show all (${unfiledDocs.size})",
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                                        )
                                    }
                                }
                            }
                        }
                        items(visibleUnfiled, key = { "unfiled_doc_${it.id}" }) { doc ->
                            DocumentAttachmentCard(
                                attachment = doc,
                                onOpen = { openAttachment(doc) },
                                onMoveToFolder = { attachmentToMove = doc },
                                onRename = { attachmentToRename = doc },
                                onToggleFavorite = { viewModel.toggleAttachmentFavorite(doc) },
                                onToggleArchive = { viewModel.toggleAttachmentArchive(doc) },
                                onToggleLock = {
                                    viewModel.toggleAttachmentLock(doc) { res ->
                                        res.onSuccess {
                                            Toast.makeText(context, "Moved \"${doc.fileName}\" to Locked Folder", Toast.LENGTH_SHORT).show()
                                        }.onFailure { err ->
                                            Toast.makeText(context, err.message ?: "Unable to lock document", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                onShowMetadata = { attachmentMetadataTarget = doc },
                                onExport = {
                                    pendingExportDoc = doc
                                    exportDocLauncher.launch(doc.fileName)
                                },
                                onSoftDelete = { viewModel.softDeleteAttachment(doc) }
                            )
                        }
                    }

                    // Quick Access Grouped Card
                    item {
                        StaggeredSectionEntrance(index = 4) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.padding(top = 6.dp)
                        ) {
                            Text(
                                "Quick Access",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontFamily = DisplayFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp
                                ),
                                color = headerTextColor
                            )

                            Card(
                                shape = RoundedCornerShape(22.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = editorialCardContainerColor(isDark)
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                                border = BorderStroke(1.2.dp, editorialCardBorderColor(isDark)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .then(
                                        if (!isDark) {
                                            Modifier.shadow(
                                                elevation = 4.dp,
                                                shape = RoundedCornerShape(22.dp),
                                                ambientColor = Color(0xFF15865D).copy(alpha = 0.08f),
                                                spotColor = Color.Black.copy(alpha = 0.06f)
                                            )
                                        } else Modifier
                                    )
                            ) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    QuickAccessItem(
                                        title = "Favourites",
                                        count = favorites.count { !it.isLocked && !it.isHidden } +
                                            favoriteAttachments.count { !it.isLocked && !it.isHidden },
                                        icon = Icons.Default.Favorite,
                                        iconColor = Color(0xFFE11D48),
                                        iconBgColor = Color(0xFFE11D48).copy(alpha = if (isDark) 0.22f else 0.12f),
                                        onClick = { onNavigateToDestination(Screen.Favorites.route) },
                                        modifier = Modifier.testTag("quick_access_favourites")
                                    )
                                    HorizontalDivider(
                                        modifier = Modifier.padding(start = 64.dp, end = 16.dp),
                                        thickness = 0.65.dp,
                                        color = editorialCardBorderColor(isDark)
                                    )
                                    QuickAccessItem(
                                        title = "PDF Reader",
                                        count = pdfCount,
                                        icon = Icons.Default.PictureAsPdf,
                                        iconColor = Color(0xFFDC2626),
                                        iconBgColor = Color(0xFFDC2626).copy(alpha = if (isDark) 0.22f else 0.12f),
                                        onClick = { onNavigateToDestination(Screen.PdfTools.route) },
                                        modifier = Modifier.testTag("quick_access_pdf_reader")
                                    )
                                    HorizontalDivider(
                                        modifier = Modifier.padding(start = 64.dp, end = 16.dp),
                                        thickness = 0.65.dp,
                                        color = editorialCardBorderColor(isDark)
                                    )
                                    QuickAccessItem(
                                        title = "Archive",
                                        count = archivedNotes.size + archivedAttachments.size,
                                        icon = Icons.Default.Archive,
                                        iconColor = NotelandEmerald,
                                        iconBgColor = NotelandEmerald.copy(alpha = if (isDark) 0.22f else 0.13f),
                                        onClick = { onNavigateToDestination(Screen.Archive.route) },
                                        modifier = Modifier.testTag("quick_access_archive")
                                    )
                                    HorizontalDivider(
                                        modifier = Modifier.padding(start = 64.dp, end = 16.dp),
                                        thickness = 0.65.dp,
                                        color = editorialCardBorderColor(isDark)
                                    )
                                    QuickAccessItem(
                                        title = "Locked Folder",
                                        count = lockedNotes.size + hiddenNotes.size + lockedAttachments.size,
                                        icon = Icons.Default.Lock,
                                        iconColor = NotelandAmber,
                                        iconBgColor = NotelandAmber.copy(alpha = if (isDark) 0.22f else 0.14f),
                                        onClick = { onNavigateToDestination(Screen.Vault.route) },
                                        modifier = Modifier.testTag("quick_access_locked_folder")
                                    )
                                    HorizontalDivider(
                                        modifier = Modifier.padding(start = 64.dp, end = 16.dp),
                                        thickness = 0.65.dp,
                                        color = editorialCardBorderColor(isDark)
                                    )
                                    QuickAccessItem(
                                        title = "Recycle Bin",
                                        count = deletedNotes.size + deletedAttachments.size,
                                        icon = Icons.Default.Delete,
                                        iconColor = Color(0xFFEF4444),
                                        iconBgColor = Color(0xFFEF4444).copy(alpha = if (isDark) 0.22f else 0.12f),
                                        onClick = { onNavigateToDestination(Screen.RecycleBin.route) },
                                        modifier = Modifier.testTag("quick_access_recycle_bin")
                                    )
                                }
                            }
                        }
                        }
                    }
                }
            } else {
                // INSIDE-FOLDER HIERARCHICAL VIEW (Breadcrumbs + Subfolders + Notes + Documents)
                val breadcrumbTrail = remember(folders, currentFolder.id) {
                    FolderHierarchyUtils.getBreadcrumbTrail(folders, currentFolder.id)
                }
                val childSubfolders = remember(folders, currentFolder.id) {
                    FolderHierarchyUtils.getSubfolders(folders, currentFolder.id)
                }
                val subtreeIds = remember(folders, currentFolder.id) {
                    FolderHierarchyUtils.getSubtreeFolderIds(folders, currentFolder.id)
                }
                val directNotes = remember(allNotes, currentFolder.id) {
                    allNotes.filter { it.folderId == currentFolder.id }
                }
                val totalSubtreeNotesCount = remember(allNotes, subtreeIds) {
                    allNotes.count { it.folderId != null && it.folderId in subtreeIds }
                }
                val visibleNotes = remember(allNotes, directNotes, includeSubfolderNotes, subtreeIds) {
                    if (includeSubfolderNotes) {
                        allNotes.filter { it.folderId != null && it.folderId in subtreeIds }
                    } else {
                        directNotes
                    }
                }
                val folderDocs = remember(activeDocuments, currentFolder.id, includeSubfolderNotes, subtreeIds) {
                    if (includeSubfolderNotes) {
                        activeDocuments.filter { it.folderId != null && it.folderId in subtreeIds }
                    } else {
                        activeDocuments.filter { it.folderId == currentFolder.id }
                    }
                }

                InsideFolderEditorialView(
                    folder = currentFolder,
                    allFolders = folders,
                    breadcrumbTrail = breadcrumbTrail,
                    subfolders = childSubfolders,
                    allNotes = allNotes,
                    allNotesInFolder = visibleNotes,
                    allActiveDocuments = activeDocuments,
                    documentsInFolder = folderDocs,
                    directNoteCount = directNotes.size,
                    totalSubtreeNoteCount = totalSubtreeNotesCount,
                    includeSubfolderNotes = includeSubfolderNotes,
                    onToggleIncludeSubfolderNotes = { includeSubfolderNotes = !includeSubfolderNotes },
                    insideFilter = insideFilter,
                    onFilterChange = { insideFilter = it },
                    sortOption = folderSortOption,
                    onSortChange = { folderSortOption = it },
                    isDark = isDark,
                    onBack = {
                        selectedFolderId = currentFolder.parentId
                        insideFilter = InsideFolderFilter.ALL
                    },
                    onSelectBreadcrumbFolder = { targetId ->
                        selectedFolderId = targetId
                        insideFilter = InsideFolderFilter.ALL
                    },
                    onOpenSubfolder = { sub ->
                        selectedFolderId = sub.id
                        insideFilter = InsideFolderFilter.ALL
                    },
                    onAddSubfolder = { openCreateFolderDialog(parentId = currentFolder.id) },
                    onAddSubfolderIn = { targetSub -> openCreateFolderDialog(parentId = targetSub.id) },
                    onEditSubfolder = { targetSub -> openEditFolderDialog(targetSub) },
                    onMoveSubfolder = { targetSub -> folderToMove = targetSub },
                    onDeleteSubfolder = { targetSub -> folderToDelete = targetSub },
                    onSearch = { onNavigateToDestination(Screen.Search.route) },
                    onOpenPdfReader = { onNavigateToDestination(Screen.PdfTools.route) },
                    onImportDocument = {
                        pendingImportTargetFolderId = currentFolder.id
                        importDocumentLauncher.launch(arrayOf("application/pdf", "image/*"))
                    },
                    onCreateNote = { onCreateNoteInFolder(currentFolder.id) },
                    onEditFolder = { openEditFolderDialog(currentFolder) },
                    onMoveCurrentFolder = { folderToMove = currentFolder },
                    onDeleteCurrentFolder = { folderToDelete = currentFolder },
                    onOpenNote = { note ->
                        if (note.noteType == "CANVAS") {
                            onNavigateToDestination("canvas?noteId=${note.id}")
                        } else {
                            onNavigateToEditor(note.id, note.noteType)
                        }
                    },
                    onMoveNote = { note -> noteToMove = note },
                    onTogglePin = { viewModel.togglePin(it) },
                    onToggleFavorite = { viewModel.toggleFavorite(it) },
                    onToggleArchive = { viewModel.toggleArchive(it) },
                    onSoftDelete = { viewModel.softDeleteNote(it) },
                    onOpenDocument = { openAttachment(it) },
                    onMoveDocument = { attachmentToMove = it },
                    onRenameDocument = { attachmentToRename = it },
                    onToggleFavoriteDocument = { viewModel.toggleAttachmentFavorite(it) },
                    onToggleArchiveDocument = { viewModel.toggleAttachmentArchive(it) },
                    onToggleLockDocument = { doc ->
                        viewModel.toggleAttachmentLock(doc) { res ->
                            res.onSuccess {
                                Toast.makeText(context, "Moved \"${doc.fileName}\" to Locked Folder", Toast.LENGTH_SHORT).show()
                            }.onFailure { err ->
                                Toast.makeText(context, err.message ?: "Unable to lock document", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    onShowDocumentMetadata = { attachmentMetadataTarget = it },
                    onExportDocument = { doc ->
                        pendingExportDoc = doc
                        exportDocLauncher.launch(doc.fileName)
                    },
                    onSoftDeleteDocument = { viewModel.softDeleteAttachment(it) }
                )
            }
        }
    }
    }

    // Create / Edit Folder Dialog (with Parent Folder selector for nested hierarchy)
    if (showCreateDialog) {
        val editing = folderToEdit
        val parentLabel = if (createParentFolderId == null) {
            "Root (Top-level)"
        } else {
            FolderHierarchyUtils.getFolderPathString(folders, createParentFolderId).ifBlank { "Root (Top-level)" }
        }

        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text(if (editing == null) "New Folder" else "Edit Folder") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = folderNameInput,
                        onValueChange = { folderNameInput = it },
                        label = { Text("Folder Name") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("folder_name_field")
                    )

                    Text("Parent Location", style = MaterialTheme.typography.labelMedium)
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        border = BorderStroke(0.9.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { showParentPickerSheet = true }
                            .testTag("folder_parent_selector")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    Icons.Default.AccountTree,
                                    contentDescription = null,
                                    tint = EditorialTerracotta,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = parentLabel,
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Text(
                                text = "Change",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = EditorialTerracotta
                            )
                        }
                    }

                    Text("Pick Color", style = MaterialTheme.typography.labelMedium)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        presetColors.forEach { (hex, _) ->
                            val color = try { Color(hex.toColorInt()) } catch (_: Exception) { EditorialTerracotta }
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .clickable { selectedColorHex = hex }
                                    .then(
                                        if (selectedColorHex.equals(hex, ignoreCase = true))
                                            Modifier.border(2.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                        else Modifier
                                    )
                            )
                        }
                    }

                    Text("Pick Icon", style = MaterialTheme.typography.labelMedium)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        presetIcons.forEach { (iconKey, vector) ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (selectedIconName == iconKey) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clickable { selectedIconName = iconKey }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(vector, contentDescription = iconKey, modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (folderNameInput.isNotBlank()) {
                            if (editing == null) {
                                viewModel.createFolder(
                                    name = folderNameInput.trim(),
                                    colorHex = selectedColorHex,
                                    icon = selectedIconName,
                                    parentId = createParentFolderId
                                )
                                createParentFolderId?.let { expandedTreeNodes[it] = true }
                            } else {
                                viewModel.updateFolder(
                                    editing.copy(
                                        parentId = createParentFolderId,
                                        name = folderNameInput.trim(),
                                        colorHex = selectedColorHex,
                                        iconName = selectedIconName,
                                        updatedAtUtc = System.currentTimeMillis()
                                    )
                                ) { res ->
                                    res.onFailure { err ->
                                        Toast.makeText(
                                            context,
                                            err.message ?: "Could not update folder",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            }
                            showCreateDialog = false
                        }
                    },
                    modifier = Modifier.testTag("save_folder_confirm")
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Parent picker for Create/Edit Folder dialog
    if (showParentPickerSheet) {
        HierarchicalFolderPickerSheet(
            title = "Select Parent Folder",
            subtitle = "Choose where this folder lives in your knowledge tree",
            folders = folders,
            notes = allNotes,
            selectedFolderId = createParentFolderId,
            rootOptionLabel = "Root (Top-level folder)",
            excludeSubtreeOfId = folderToEdit?.id,
            onSelectFolder = { picked ->
                createParentFolderId = picked?.id
            },
            onDismiss = { showParentPickerSheet = false }
        )
    }

    // Move Folder to another parent folder
    folderToMove?.let { movingFolder ->
        HierarchicalFolderPickerSheet(
            title = "Move \"${movingFolder.name}\"",
            subtitle = "Select a new parent folder or move to Top-Level",
            folders = folders,
            notes = allNotes,
            selectedFolderId = movingFolder.parentId,
            rootOptionLabel = "Root (Top-level folder)",
            excludeSubtreeOfId = movingFolder.id,
            onSelectFolder = { picked ->
                viewModel.moveFolder(movingFolder.id, picked?.id) { res ->
                    res.onSuccess {
                        val dest = picked?.name ?: "Root"
                        Toast.makeText(context, "Moved \"${movingFolder.name}\" to $dest", Toast.LENGTH_SHORT).show()
                    }.onFailure { err ->
                        Toast.makeText(context, err.message ?: "Unable to move folder", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onDismiss = { folderToMove = null }
        )
    }

    // Move Note to any folder/subfolder
    noteToMove?.let { movingNote ->
        HierarchicalFolderPickerSheet(
            title = "Move Note to Folder",
            subtitle = movingNote.title.ifBlank { "Untitled Note" },
            folders = folders,
            notes = allNotes,
            selectedFolderId = movingNote.folderId,
            rootOptionLabel = "No Folder (Uncategorized)",
            allowCreateFolder = true,
            onCreateQuickFolder = { newName, parentId ->
                viewModel.createFolder(newName, "#D85C37", "folder", parentId) { created ->
                    viewModel.moveNoteToFolder(movingNote.id, created)
                }
            },
            onSelectFolder = { picked ->
                viewModel.moveNoteToFolder(movingNote.id, picked) { res ->
                    res.onSuccess {
                        val dest = picked?.name ?: "No Folder"
                        Toast.makeText(context, "Moved note to $dest", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onDismiss = { noteToMove = null }
        )
    }

    // Move Document/File to any folder/subfolder
    attachmentToMove?.let { movingDoc ->
        HierarchicalFolderPickerSheet(
            title = "Move File to Folder",
            subtitle = movingDoc.fileName,
            folders = folders,
            notes = allNotes,
            attachments = activeDocuments,
            selectedFolderId = movingDoc.folderId,
            rootOptionLabel = "No Folder (Unfiled)",
            allowCreateFolder = true,
            onCreateQuickFolder = { newName, parentId ->
                viewModel.createFolder(newName, "#D85C37", "folder", parentId) { created ->
                    viewModel.moveAttachmentToFolder(movingDoc.id, created.id)
                }
            },
            onSelectFolder = { picked ->
                viewModel.moveAttachmentToFolder(movingDoc.id, picked?.id) { res ->
                    res.onSuccess {
                        val dest = picked?.name ?: "Unfiled"
                        Toast.makeText(context, "Moved \"${movingDoc.fileName}\" to $dest", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onDismiss = { attachmentToMove = null }
        )
    }

    attachmentToRename?.let { targetDoc ->
        RenameDocumentDialog(
            initialName = targetDoc.fileName,
            onConfirm = { newName ->
                viewModel.renameAttachment(targetDoc.id, newName)
                attachmentToRename = null
            },
            onDismiss = { attachmentToRename = null }
        )
    }

    attachmentMetadataTarget?.let { targetDoc ->
        DocumentMetadataDialog(
            attachment = targetDoc,
            folderPath = FolderHierarchyUtils.getFolderPathString(folders, targetDoc.folderId),
            onDismiss = { attachmentMetadataTarget = null }
        )
    }

    folderToDelete?.let { target ->
        val noteCount = allNotes.count { it.folderId == target.id }
        val docCount = activeDocuments.count { it.folderId == target.id }
        val subfolderCount = FolderHierarchyUtils.getSubfolders(folders, target.id).size
        AlertDialog(
            onDismissRequest = { folderToDelete = null },
            title = { Text("Delete folder \"${target.name}\"?") },
            text = {
                val message = buildString {
                    if (noteCount > 0 || docCount > 0) {
                        append("This folder contains $noteCount note(s) and $docCount file(s) (kept safely in your library). ")
                    }
                    if (subfolderCount > 0) {
                        append("$subfolderCount subfolder(s) inside it will move up one level cleanly. ")
                    }
                    if (noteCount == 0 && docCount == 0 && subfolderCount == 0) {
                        append("Are you sure you want to delete this folder?")
                    }
                }
                Text(message.trim())
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteFolder(target.id)
                        if (selectedFolderId == target.id) {
                            selectedFolderId = target.parentId
                        }
                        folderToDelete = null
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { folderToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun InsideFolderEditorialView(
    folder: FolderEntity,
    allFolders: List<FolderEntity>,
    breadcrumbTrail: List<FolderEntity>,
    subfolders: List<FolderEntity>,
    allNotes: List<NoteEntity>,
    allNotesInFolder: List<NoteEntity>,
    allActiveDocuments: List<AttachmentEntity>,
    documentsInFolder: List<AttachmentEntity>,
    directNoteCount: Int,
    totalSubtreeNoteCount: Int,
    includeSubfolderNotes: Boolean,
    onToggleIncludeSubfolderNotes: () -> Unit,
    insideFilter: InsideFolderFilter,
    onFilterChange: (InsideFolderFilter) -> Unit,
    sortOption: FolderSortOption,
    onSortChange: (FolderSortOption) -> Unit,
    isDark: Boolean,
    onBack: () -> Unit,
    onSelectBreadcrumbFolder: (String?) -> Unit,
    onOpenSubfolder: (FolderEntity) -> Unit,
    onAddSubfolder: () -> Unit,
    onAddSubfolderIn: (FolderEntity) -> Unit,
    onEditSubfolder: (FolderEntity) -> Unit,
    onMoveSubfolder: (FolderEntity) -> Unit,
    onDeleteSubfolder: (FolderEntity) -> Unit,
    onSearch: () -> Unit,
    onOpenPdfReader: () -> Unit,
    onImportDocument: () -> Unit,
    onCreateNote: () -> Unit,
    onEditFolder: () -> Unit,
    onMoveCurrentFolder: () -> Unit,
    onDeleteCurrentFolder: () -> Unit,
    onOpenNote: (NoteEntity) -> Unit,
    onMoveNote: (NoteEntity) -> Unit,
    onTogglePin: (NoteEntity) -> Unit,
    onToggleFavorite: (NoteEntity) -> Unit,
    onToggleArchive: (NoteEntity) -> Unit,
    onSoftDelete: (NoteEntity) -> Unit,
    onOpenDocument: (AttachmentEntity) -> Unit,
    onMoveDocument: (AttachmentEntity) -> Unit,
    onRenameDocument: (AttachmentEntity) -> Unit,
    onToggleFavoriteDocument: (AttachmentEntity) -> Unit,
    onToggleArchiveDocument: (AttachmentEntity) -> Unit,
    onToggleLockDocument: (AttachmentEntity) -> Unit,
    onShowDocumentMetadata: (AttachmentEntity) -> Unit,
    onExportDocument: (AttachmentEntity) -> Unit,
    onSoftDeleteDocument: (AttachmentEntity) -> Unit
) {
    var showOverflowMenu by remember { mutableStateOf(false) }
    var showSortDropdown by remember { mutableStateOf(false) }
    var insideSearchQuery by rememberSaveable(folder.id) { mutableStateOf("") }

    val filteredAndSorted = remember(allNotesInFolder, insideFilter, sortOption, insideSearchQuery) {
        val filtered = when (insideFilter) {
            InsideFolderFilter.ALL -> allNotesInFolder
            InsideFolderFilter.DOCUMENTS -> emptyList()
            InsideFolderFilter.PINNED -> allNotesInFolder.filter { it.isPinned }
            InsideFolderFilter.SIMPLE_NOTES -> allNotesInFolder.filter {
                SmartNoteUtils.parseChecklistJson(it.checklistJson).isEmpty() && it.noteType == "STANDARD"
            }
            InsideFolderFilter.TO_DO -> allNotesInFolder.filter {
                SmartNoteUtils.parseChecklistJson(it.checklistJson).isNotEmpty() || it.checklistJson.isNotBlank()
            }
        }
        val q = insideSearchQuery.trim()
        val queryFiltered = if (q.isEmpty()) {
            filtered
        } else {
            filtered.filter { !it.isLocked && (it.title.contains(q, ignoreCase = true) || it.content.contains(q, ignoreCase = true)) }
        }
        when (sortOption) {
            FolderSortOption.DATE_MODIFIED -> queryFiltered.sortedWith(compareByDescending<NoteEntity> { it.isPinned }.thenByDescending { it.updatedAtUtc })
            FolderSortOption.DATE_CREATED -> queryFiltered.sortedWith(compareByDescending<NoteEntity> { it.isPinned }.thenByDescending { it.createdAtUtc })
            FolderSortOption.ALPHABETICAL_ASC -> queryFiltered.sortedBy { it.title.lowercase() }
            FolderSortOption.ALPHABETICAL_DESC -> queryFiltered.sortedByDescending { it.title.lowercase() }
        }
    }

    val visibleDocuments = remember(documentsInFolder, insideFilter, sortOption, insideSearchQuery) {
        if (insideFilter != InsideFolderFilter.ALL && insideFilter != InsideFolderFilter.DOCUMENTS) {
            emptyList()
        } else {
            val q = insideSearchQuery.trim()
            val queryFiltered = if (q.isEmpty()) documentsInFolder else documentsInFolder.filter { it.fileName.contains(q, ignoreCase = true) }
            when (sortOption) {
                FolderSortOption.DATE_MODIFIED -> queryFiltered.sortedByDescending { it.updatedAtUtc }
                FolderSortOption.DATE_CREATED -> queryFiltered.sortedByDescending { it.createdAtUtc }
                FolderSortOption.ALPHABETICAL_ASC -> queryFiltered.sortedBy { it.fileName.lowercase() }
                FolderSortOption.ALPHABETICAL_DESC -> queryFiltered.sortedByDescending { it.fileName.lowercase() }
            }
        }
    }

    val headerTextColor = editorialHeaderTextColor(isDark)
    val headerSubColor = editorialHeaderSubColor(isDark)
    val navBottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    val featuredNote = filteredAndSorted.firstOrNull { it.isPinned } ?: filteredAndSorted.firstOrNull()
    val gridNotes = if (featuredNote != null) filteredAndSorted.filterNot { it.id == featuredNote.id } else emptyList()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 24.dp, bottom = 148.dp + navBottomPadding)
    ) {
        // Top bar: Back button on left, Add Subfolder + Add Note + Search + Overflow on right
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                GlassSquareIconButton(
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    isDark = isDark,
                    onClick = onBack,
                    modifier = Modifier.testTag("inside_folder_back_button")
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    GlassSquareIconButton(
                        icon = Icons.Default.CreateNewFolder,
                        contentDescription = "New subfolder inside ${folder.name}",
                        isDark = isDark,
                        onClick = onAddSubfolder,
                        modifier = Modifier.testTag("inside_folder_add_subfolder_button")
                    )

                    GlassSquareIconButton(
                        icon = Icons.Default.Add,
                        contentDescription = "New note in ${folder.name}",
                        isDark = isDark,
                        accentHighlight = true,
                        onClick = onCreateNote,
                        modifier = Modifier.testTag("inside_folder_add_note_button")
                    )

                    GlassSquareIconButton(
                        icon = Icons.Default.Search,
                        contentDescription = "Search notes",
                        isDark = isDark,
                        onClick = onSearch
                    )

                    Box {
                        GlassSquareIconButton(
                            icon = Icons.Default.MoreVert,
                            contentDescription = "Folder options",
                            isDark = isDark,
                            onClick = { showOverflowMenu = true }
                        )
                        DropdownMenu(
                            expanded = showOverflowMenu,
                            onDismissRequest = { showOverflowMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("New Note in ${folder.name}") },
                                leadingIcon = { Icon(Icons.Default.Add, contentDescription = null, tint = EditorialTerracotta) },
                                onClick = {
                                    showOverflowMenu = false
                                    onCreateNote()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("New Subfolder") },
                                leadingIcon = { Icon(Icons.Default.CreateNewFolder, contentDescription = null, tint = EditorialTerracotta) },
                                onClick = {
                                    showOverflowMenu = false
                                    onAddSubfolder()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Move Folder...") },
                                leadingIcon = { Icon(Icons.Default.AccountTree, contentDescription = null) },
                                onClick = {
                                    showOverflowMenu = false
                                    onMoveCurrentFolder()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Edit Folder") },
                                onClick = {
                                    showOverflowMenu = false
                                    onEditFolder()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Import PDF / File") },
                                leadingIcon = { Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = EditorialTerracotta) },
                                onClick = {
                                    showOverflowMenu = false
                                    onImportDocument()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("PDF Reader") },
                                leadingIcon = { Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = Color(0xFFDC2626)) },
                                onClick = {
                                    showOverflowMenu = false
                                    onOpenPdfReader()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Sort Notes") },
                                leadingIcon = { Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = null) },
                                onClick = {
                                    showOverflowMenu = false
                                    showSortDropdown = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Delete Folder", color = MaterialTheme.colorScheme.error) },
                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                                onClick = {
                                    showOverflowMenu = false
                                    onDeleteCurrentFolder()
                                }
                            )
                        }
                    }
                }
            }
        }

        // Interactive Hierarchical Breadcrumb Strip
        item {
            FolderBreadcrumbBar(
                trail = breadcrumbTrail,
                isDark = isDark,
                onSelectFolderId = onSelectBreadcrumbFolder
            )
        }

        // Centered Large Editorial Folder Name Title
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = folder.name,
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontFamily = DisplayFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 36.sp,
                        lineHeight = 42.sp,
                        letterSpacing = (-0.5).sp
                    ),
                    color = headerTextColor,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(6.dp))
                val subtitle = buildString {
                    if (subfolders.isNotEmpty()) {
                        append(if (subfolders.size == 1) "1 subfolder" else "${subfolders.size} subfolders")
                        append(" · ")
                    }
                    append(if (directNoteCount == 1) "1 direct note" else "$directNoteCount direct notes")
                    if (documentsInFolder.isNotEmpty()) {
                        append(if (documentsInFolder.size == 1) " · 1 file" else " · ${documentsInFolder.size} files")
                    }
                    if (totalSubtreeNoteCount > directNoteCount) {
                        append(" · $totalSubtreeNoteCount in branch")
                    }
                }
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                    color = headerSubColor
                )
            }
        }

        // Subfolders Section inside current folder
        if (subfolders.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Subfolders (${subfolders.size})",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp
                        ),
                        color = headerTextColor
                    )
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = EditorialTerracotta.copy(alpha = 0.14f),
                        border = BorderStroke(0.8.dp, EditorialTerracotta.copy(alpha = 0.45f)),
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable(onClick = onAddSubfolder)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = EditorialTerracotta, modifier = Modifier.size(14.dp))
                            Text(
                                text = "Subfolder",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = EditorialTerracotta
                            )
                        }
                    }
                }
            }

            items(subfolders.chunked(2), key = { row -> "sub_${row.joinToString { it.id }}" }) { rowSubs ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    rowSubs.forEachIndexed { idx, sub ->
                        val subtreeIds = FolderHierarchyUtils.getSubtreeFolderIds(allFolders, sub.id)
                        val subNotes = allNotes.filter { it.folderId != null && it.folderId in subtreeIds }
                        val subDocs = allActiveDocuments.filter { it.folderId != null && it.folderId in subtreeIds }
                        val nestedChildren = FolderHierarchyUtils.getSubfolders(allFolders, sub.id)
                        val globalIdx = subfolders.indexOf(sub).coerceAtLeast(idx)
                        val latestTs = maxOf(
                            sub.updatedAtUtc,
                            subNotes.maxOfOrNull { it.updatedAtUtc } ?: 0L,
                            subDocs.maxOfOrNull { it.updatedAtUtc } ?: 0L
                        )
                        FolderPastelCard(
                            folder = sub,
                            noteCount = subNotes.size,
                            fileCount = subDocs.size,
                            subfolderCount = nestedChildren.size,
                            latestUpdatedUtc = latestTs,
                            cardToneIndex = globalIdx + 1,
                            onClick = { onOpenSubfolder(sub) },
                            onAddSubfolder = { onAddSubfolderIn(sub) },
                            onEdit = { onEditSubfolder(sub) },
                            onMove = { onMoveSubfolder(sub) },
                            onDelete = { onDeleteSubfolder(sub) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (rowSubs.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        // Inline Filter Search inside current folder
        item {
            OutlinedTextField(
                value = insideSearchQuery,
                onValueChange = { insideSearchQuery = it },
                placeholder = { Text("Filter notes & files in ${folder.name}...", fontSize = 13.5.sp) },
                leadingIcon = {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        tint = headerSubColor,
                        modifier = Modifier.size(18.dp)
                    )
                },
                trailingIcon = {
                    if (insideSearchQuery.isNotEmpty()) {
                        IconButton(onClick = { insideSearchQuery = "" }) {
                            Icon(
                                Icons.Default.Clear,
                                contentDescription = "Clear folder filter",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                singleLine = true,
                shape = CircleShape,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = editorialCardContainerColor(isDark),
                    unfocusedContainerColor = editorialCardContainerColor(isDark),
                    focusedBorderColor = (if (isDark) DarkFoldersAccent else LightFoldersAccent).copy(alpha = 0.85f),
                    unfocusedBorderColor = editorialCardBorderColor(isDark)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("inside_folder_search_input")
            )
        }

        // Filter Pills Strip
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                InsideFolderFilter.values().forEach { filter ->
                    EditorialFilterPill(
                        label = filter.label,
                        selected = insideFilter == filter,
                        isDark = isDark,
                        onClick = { onFilterChange(filter) }
                    )
                }

                if (subfolders.isNotEmpty()) {
                    EditorialFilterPill(
                        label = "Include Subfolders",
                        selected = includeSubfolderNotes,
                        isDark = isDark,
                        onClick = onToggleIncludeSubfolderNotes
                    )
                }

                // Compact Sort Dropdown Pill (preserving existing sort testTags)
                Box {
                    Surface(
                        shape = CircleShape,
                        color = editorialCardContainerColor(isDark),
                        border = BorderStroke(
                            0.9.dp,
                            editorialCardBorderColor(isDark)
                        ),
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable { showSortDropdown = true }
                            .testTag("folder_sort_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.Sort,
                                contentDescription = "Sort Notes",
                                modifier = Modifier.size(15.dp),
                                tint = headerTextColor
                            )
                            Text(
                                text = sortOption.label,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                                color = headerTextColor,
                                modifier = Modifier.testTag("folder_sort_label")
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = showSortDropdown,
                        onDismissRequest = { showSortDropdown = false }
                    ) {
                        FolderSortOption.values().forEach { option ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        option.label,
                                        fontWeight = if (sortOption == option) FontWeight.Bold else FontWeight.Normal,
                                        color = if (sortOption == option) EditorialTerracotta else MaterialTheme.colorScheme.onSurface
                                    )
                                },
                                onClick = {
                                    onSortChange(option)
                                    showSortDropdown = false
                                },
                                modifier = Modifier.testTag("folder_sort_${option.name.lowercase()}")
                            )
                        }
                    }
                }
            }
        }

        if (filteredAndSorted.isEmpty() && visibleDocuments.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = editorialCardContainerColor(isDark),
                    border = BorderStroke(
                        0.85.dp,
                        editorialCardBorderColor(isDark)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = when {
                                insideSearchQuery.isNotBlank() -> "No matching notes or files in ${folder.name}"
                                insideFilter == InsideFolderFilter.DOCUMENTS -> "No files in ${folder.name} yet"
                                else -> "No notes or files in ${folder.name} yet"
                            },
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = when {
                                insideSearchQuery.isNotBlank() -> "Try another search keyword or clear the folder filter."
                                subfolders.isNotEmpty() && !includeSubfolderNotes ->
                                    "Create a note here, drill into a subfolder above, or enable \"Include Subfolders\"."
                                else -> "Create your first note in this folder or organize topics with a subfolder."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isDark) Color.White.copy(alpha = 0.72f) else MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        if (insideSearchQuery.isNotBlank()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            TextButton(onClick = { insideSearchQuery = "" }) {
                                Text("Clear Filter", fontWeight = FontWeight.SemiBold)
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = EditorialTerracotta,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable(onClick = onCreateNote)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                    Text(
                                        "New Note",
                                        color = Color.White,
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = EditorialTerracotta.copy(alpha = 0.14f),
                                border = BorderStroke(0.9.dp, EditorialTerracotta.copy(alpha = 0.45f)),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable(onClick = onAddSubfolder)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                                ) {
                                    Icon(Icons.Default.CreateNewFolder, contentDescription = null, tint = EditorialTerracotta, modifier = Modifier.size(18.dp))
                                    Text(
                                        "New Subfolder",
                                        color = EditorialTerracotta,
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Featured Elevated White Card
            featuredNote?.let { topNote ->
                item(key = "featured_${topNote.id}") {
                    FeaturedWhiteNoteCard(
                        note = topNote,
                        onClick = { onOpenNote(topNote) },
                        onMoveToFolder = { onMoveNote(topNote) },
                        onTogglePin = { onTogglePin(topNote) },
                        onToggleFavorite = { onToggleFavorite(topNote) },
                        onToggleArchive = { onToggleArchive(topNote) },
                        onSoftDelete = { onSoftDelete(topNote) }
                    )
                }
            }

            // 2-Column Earthy Matte Note Grid
            items(gridNotes.chunked(2), key = { row -> row.joinToString { it.id } }) { rowNotes ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    rowNotes.forEachIndexed { idx, note ->
                        val globalIdx = gridNotes.indexOf(note).coerceAtLeast(idx)
                        EarthyGridNoteCard(
                            note = note,
                            toneIndex = globalIdx,
                            onClick = { onOpenNote(note) },
                            onMoveToFolder = { onMoveNote(note) },
                            onTogglePin = { onTogglePin(note) },
                            onToggleFavorite = { onToggleFavorite(note) },
                            onToggleArchive = { onToggleArchive(note) },
                            onSoftDelete = { onSoftDelete(note) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (rowNotes.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }

            // Documents & PDFs inside this folder
            if (visibleDocuments.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Documents & Files (${visibleDocuments.size})",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp
                            ),
                            color = headerTextColor
                        )
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = EditorialTerracotta.copy(alpha = 0.14f),
                            border = BorderStroke(0.8.dp, EditorialTerracotta.copy(alpha = 0.45f)),
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable(onClick = onImportDocument)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = EditorialTerracotta, modifier = Modifier.size(14.dp))
                                Text(
                                    text = "Import File",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = EditorialTerracotta
                                )
                            }
                        }
                    }
                }
                items(visibleDocuments, key = { "folder_doc_${it.id}" }) { doc ->
                    val folderLabel = if (includeSubfolderNotes && doc.folderId != folder.id) {
                        FolderHierarchyUtils.getFolderPathString(allFolders, doc.folderId)
                    } else ""
                    DocumentAttachmentCard(
                        attachment = doc,
                        folderName = folderLabel,
                        onOpen = { onOpenDocument(doc) },
                        onMoveToFolder = { onMoveDocument(doc) },
                        onRename = { onRenameDocument(doc) },
                        onToggleFavorite = { onToggleFavoriteDocument(doc) },
                        onToggleArchive = { onToggleArchiveDocument(doc) },
                        onToggleLock = { onToggleLockDocument(doc) },
                        onShowMetadata = { onShowDocumentMetadata(doc) },
                        onExport = { onExportDocument(doc) },
                        onSoftDelete = { onSoftDeleteDocument(doc) }
                    )
                }
            }
        }
    }
}

@Composable
private fun FeaturedWhiteNoteCard(
    note: NoteEntity,
    onClick: () -> Unit,
    onMoveToFolder: () -> Unit,
    onTogglePin: () -> Unit,
    onToggleFavorite: () -> Unit,
    onToggleArchive: () -> Unit,
    onSoftDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val shortDate = remember(note.updatedAtUtc) {
        SimpleDateFormat("d MMM", Locale.getDefault()).format(Date(note.updatedAtUtc))
    }
    val checklistItems = remember(note.checklistJson) {
        SmartNoteUtils.parseChecklistJson(note.checklistJson)
    }

    val isDark = isAppInDarkTheme
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = editorialCardContainerColor(isDark)),
        border = BorderStroke(1.2.dp, editorialCardBorderColor(isDark)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (!isDark) {
                    Modifier.shadow(
                        elevation = 6.dp,
                        shape = RoundedCornerShape(22.dp),
                        ambientColor = Color.Black.copy(alpha = 0.08f),
                        spotColor = Color.Black.copy(alpha = 0.09f)
                    )
                } else Modifier
            )
            .testTag("note_card_${note.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp)
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
                    if (note.isPinned) {
                        Icon(
                            imageVector = Icons.Default.PushPin,
                            contentDescription = "Pinned",
                            tint = EditorialPinSlate,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Text(
                        text = shortDate,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        color = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFF6B655E)
                    )
                }

                NoteOverflowMenuBox(
                    expanded = showMenu,
                    onOpen = { showMenu = true },
                    onDismiss = { showMenu = false },
                    isPinned = note.isPinned,
                    isFavorite = note.isFavorite,
                    isArchived = note.isArchived,
                    iconTint = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFF7D7770),
                    onMoveToFolder = onMoveToFolder,
                    onTogglePin = onTogglePin,
                    onToggleFavorite = onToggleFavorite,
                    onToggleArchive = onToggleArchive,
                    onSoftDelete = onSoftDelete
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(EditorialTerracotta),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (note.isLocked) Icons.Default.Lock else Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(12.dp)
                    )
                }

                Text(
                    text = if (note.isLocked) "Locked Note" else note.title.ifBlank { "Untitled" },
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = if (isDark) MaterialTheme.colorScheme.onSurface else Color(0xFF191816),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            val previewBody = when {
                note.isLocked -> "Encrypted in Secure Vault. Tap to unlock with PIN."
                note.content.isNotBlank() -> note.content.replace("#", "").replace("*", "").trim()
                checklistItems.isNotEmpty() -> {
                    val done = checklistItems.count { it.isDone }
                    "$done of ${checklistItems.size} tasks completed"
                }
                else -> "Tap to open and start writing..."
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = previewBody,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                ),
                color = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFF5A554E),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun EarthyGridNoteCard(
    note: NoteEntity,
    toneIndex: Int,
    onClick: () -> Unit,
    onMoveToFolder: () -> Unit,
    onTogglePin: () -> Unit,
    onToggleFavorite: () -> Unit,
    onToggleArchive: () -> Unit,
    onSoftDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }
    val shortDate = remember(note.updatedAtUtc) {
        SimpleDateFormat("d MMM", Locale.getDefault()).format(Date(note.updatedAtUtc))
    }

    val earthyTones = listOf(
        EditorialWarmSand,
        EditorialSageOlive,
        EditorialMutedSlate,
        EditorialWarmClay
    )
    val cardColor = earthyTones[toneIndex % earthyTones.size]
    val checklistItems = remember(note.checklistJson) {
        SmartNoteUtils.parseChecklistJson(note.checklistJson)
    }

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier
            .fillMaxWidth()
            .height(148.dp)
            .clip(RoundedCornerShape(22.dp))
            .clickable(onClick = onClick)
            .testTag("note_card_${note.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = shortDate,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    color = Color.White.copy(alpha = 0.88f)
                )

                NoteOverflowMenuBox(
                    expanded = showMenu,
                    onOpen = { showMenu = true },
                    onDismiss = { showMenu = false },
                    isPinned = note.isPinned,
                    isFavorite = note.isFavorite,
                    isArchived = note.isArchived,
                    iconTint = Color.White.copy(alpha = 0.88f),
                    onMoveToFolder = onMoveToFolder,
                    onTogglePin = onTogglePin,
                    onToggleFavorite = onToggleFavorite,
                    onToggleArchive = onToggleArchive,
                    onSoftDelete = onSoftDelete
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = if (note.isLocked) "Locked Note" else note.title.ifBlank { "Untitled" },
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 16.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        lineHeight = 20.sp
                    ),
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                val snippet = when {
                    note.isLocked -> "Encrypted note"
                    note.content.isNotBlank() -> note.content.replace("#", "").replace("*", "").trim()
                    checklistItems.isNotEmpty() -> "${checklistItems.count { it.isDone }}/${checklistItems.size} tasks"
                    else -> "Empty note"
                }

                Text(
                    text = snippet,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    ),
                    color = Color.White.copy(alpha = 0.84f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun NoteOverflowMenuBox(
    expanded: Boolean,
    onOpen: () -> Unit,
    onDismiss: () -> Unit,
    isPinned: Boolean,
    isFavorite: Boolean,
    isArchived: Boolean,
    iconTint: Color,
    onMoveToFolder: () -> Unit,
    onTogglePin: () -> Unit,
    onToggleFavorite: () -> Unit,
    onToggleArchive: () -> Unit,
    onSoftDelete: () -> Unit
) {
    Box {
        IconButton(
            onClick = onOpen,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                Icons.Default.MoreVert,
                contentDescription = "Note options",
                tint = iconTint,
                modifier = Modifier.size(18.dp)
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = onDismiss
        ) {
            DropdownMenuItem(
                text = { Text("Move to Folder...") },
                leadingIcon = { Icon(Icons.AutoMirrored.Filled.DriveFileMove, contentDescription = null) },
                onClick = {
                    onDismiss()
                    onMoveToFolder()
                }
            )
            DropdownMenuItem(
                text = { Text(if (isPinned) "Unpin" else "Pin") },
                onClick = {
                    onDismiss()
                    onTogglePin()
                }
            )
            DropdownMenuItem(
                text = { Text(if (isFavorite) "Remove from Favorites" else "Add to Favorites") },
                onClick = {
                    onDismiss()
                    onToggleFavorite()
                }
            )
            DropdownMenuItem(
                text = { Text(if (isArchived) "Unarchive" else "Archive") },
                onClick = {
                    onDismiss()
                    onToggleArchive()
                }
            )
            DropdownMenuItem(
                text = { Text("Move to Trash", color = MaterialTheme.colorScheme.error) },
                onClick = {
                    onDismiss()
                    onSoftDelete()
                }
            )
        }
    }
}

@Composable
private fun GlassSquareIconButton(
    icon: ImageVector,
    contentDescription: String,
    isDark: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accentHighlight: Boolean = false
) {
    val activeAccent = if (isDark) DarkFoldersAccent else LightFoldersAccent
    val bgColor = when {
        accentHighlight -> activeAccent
        else -> editorialCardContainerColor(isDark)
    }
    val borderColor = when {
        accentHighlight -> activeAccent
        else -> editorialCardBorderColor(isDark)
    }
    val iconTint = when {
        accentHighlight && isDark -> Color(0xFF082247)
        accentHighlight -> Color.White
        else -> editorialHeaderTextColor(isDark)
    }

    val interactionSource = remember { MutableInteractionSource() }
    Surface(
        onClick = onClick,
        interactionSource = interactionSource,
        shape = CircleShape,
        color = bgColor,
        border = BorderStroke(1.15.dp, borderColor),
        modifier = modifier
            .size(48.dp)
            .then(
                if (!isDark) {
                    Modifier.shadow(
                        elevation = if (accentHighlight) 4.dp else 3.dp,
                        shape = CircleShape,
                        ambientColor = (if (accentHighlight) activeAccent else Color.Black).copy(alpha = 0.10f),
                        spotColor = Color.Black.copy(alpha = 0.06f)
                    )
                } else Modifier
            )
            .springPressScale(interactionSource, pressedScale = 0.91f)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = iconTint,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun EditorialFilterPill(
    label: String,
    selected: Boolean,
    isDark: Boolean,
    onClick: () -> Unit
) {
    val activeAccent = if (isDark) DarkFoldersAccent else LightFoldersAccent
    val containerColor = when {
        selected -> activeAccent
        else -> editorialCardContainerColor(isDark)
    }
    val borderColor = when {
        selected -> activeAccent
        else -> editorialCardBorderColor(isDark)
    }
    val textColor = when {
        selected && isDark -> Color(0xFF082247)
        selected -> Color.White
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    val interactionSource = remember { MutableInteractionSource() }
    Surface(
        onClick = onClick,
        interactionSource = interactionSource,
        shape = CircleShape,
        color = containerColor,
        border = BorderStroke(1.15.dp, borderColor),
        modifier = Modifier
            .then(
                if (!isDark) {
                    Modifier.shadow(
                        elevation = if (selected) 3.dp else 2.dp,
                        shape = CircleShape,
                        ambientColor = (if (selected) activeAccent else Color.Black).copy(alpha = if (selected) 0.16f else 0.05f),
                        spotColor = Color.Black.copy(alpha = 0.05f)
                    )
                } else Modifier
            )
            .springPressScale(interactionSource, pressedScale = 0.94f)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge.copy(
                fontSize = 13.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
            ),
            color = textColor,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp)
        )
    }
}

@Composable
fun FolderPastelCard(
    folder: FolderEntity,
    noteCount: Int,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    fileCount: Int = 0,
    subfolderCount: Int = 0,
    onAddSubfolder: (() -> Unit)? = null,
    onMove: (() -> Unit)? = null,
    latestUpdatedUtc: Long = folder.updatedAtUtc,
    cardToneIndex: Int = 0
) {
    var showMenu by remember { mutableStateOf(false) }
    val isDark = isAppInDarkTheme

    // Luminous pastel surfaces matching the design system (Amber, Mint, Sky Blue, Lavender)
    val lightPastels = listOf(
        Color(0xFFFFF6E0),
        Color(0xFFECFCF3),
        Color(0xFFEFF6FF),
        Color(0xFFF5F3FF)
    )
    val darkPastels = listOf(
        Color(0xFF2A2213),
        Color(0xFF1A2818),
        Color(0xFF13233A),
        Color(0xFF221B35)
    )
    val vibrantAccents = listOf(
        NotelandAmber,
        NotelandEmerald,
        NotelandBlue,
        NotelandPurple
    )

    val toneIdx = cardToneIndex % lightPastels.size
    val cardBg = if (isDark) darkPastels[toneIdx] else lightPastels[toneIdx]
    val fallbackAccent = vibrantAccents[toneIdx]
    val folderAccent = try {
        val parsed = Color(folder.colorHex.toColorInt())
        if (folder.colorHex.equals("#D85C37", ignoreCase = true)) fallbackAccent else parsed
    } catch (_: Exception) {
        fallbackAccent
    }
    val titleColor = if (isDark) Color(0xFFF1F5F3) else Color(0xFF0F241C)
    val subColor = if (isDark) Color(0xFFA8B3AF) else Color(0xFF526A60)
    val cardBorder = if (isDark) folderAccent.copy(alpha = 0.50f) else folderAccent.copy(alpha = 0.42f)
    val cardInteraction = remember { MutableInteractionSource() }

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = BorderStroke(1.25.dp, cardBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier
            .fillMaxWidth()
            .height(142.dp)
            .then(
                if (!isDark) {
                    Modifier.shadow(
                        elevation = 5.dp,
                        shape = RoundedCornerShape(22.dp),
                        ambientColor = folderAccent.copy(alpha = 0.14f),
                        spotColor = Color.Black.copy(alpha = 0.06f)
                    )
                } else Modifier
            )
            .springPressScale(cardInteraction)
            .clip(RoundedCornerShape(22.dp))
            .clickable(
                interactionSource = cardInteraction,
                indication = LocalIndication.current
            ) { onClick() }
            .testTag("folder_card_${folder.name}")
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Subtle bottom-right decorative curve matching the pastel card mockup
            Canvas(modifier = Modifier.fillMaxSize()) {
                val wave = Path().apply {
                    moveTo(size.width * 0.45f, size.height)
                    quadraticTo(
                        size.width * 0.72f,
                        size.height * 0.62f,
                        size.width,
                        size.height * 0.74f
                    )
                    lineTo(size.width, size.height)
                    close()
                }
                drawPath(
                    path = wave,
                    color = folderAccent.copy(alpha = if (isDark) 0.14f else 0.10f)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(15.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = folderAccent,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                getFolderVector(folder.iconName),
                                contentDescription = folder.name,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Box {
                        IconButton(
                            onClick = { showMenu = true },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.MoreVert,
                                contentDescription = "Options",
                                tint = subColor,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            if (onAddSubfolder != null) {
                                DropdownMenuItem(
                                    text = { Text("New Subfolder") },
                                    leadingIcon = { Icon(Icons.Default.CreateNewFolder, contentDescription = null, tint = EditorialTerracotta) },
                                    onClick = {
                                        showMenu = false
                                        onAddSubfolder()
                                    }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("Edit") },
                                onClick = {
                                    showMenu = false
                                    onEdit()
                                }
                            )
                            if (onMove != null) {
                                DropdownMenuItem(
                                    text = { Text("Move Folder...") },
                                    leadingIcon = { Icon(Icons.Default.AccountTree, contentDescription = null) },
                                    onClick = {
                                        showMenu = false
                                        onMove()
                                    }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                                onClick = {
                                    showMenu = false
                                    onDelete()
                                }
                            )
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        folder.name,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = DisplayFontFamily,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = titleColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    val countSummary = buildString {
                        if (subfolderCount > 0) {
                            append(if (subfolderCount == 1) "1 sub" else "$subfolderCount subs")
                            append(" · ")
                        }
                        append(if (noteCount == 1) "1 note" else "$noteCount notes")
                        if (fileCount > 0) {
                            append(if (fileCount == 1) " · 1 file" else " · $fileCount files")
                        }
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(folderAccent)
                        )
                        Text(
                            countSummary,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp),
                            color = subColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun QuickAccessItem(
    title: String,
    count: Int,
    icon: ImageVector,
    iconColor: Color,
    iconBgColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isAppInDarkTheme
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(11.dp),
                color = iconBgColor,
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(18.dp))
                }
            }
            Text(
                title,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                color = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                "$count",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = if (isDark) Color.White.copy(alpha = 0.72f) else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = (if (isDark) Color.White else MaterialTheme.colorScheme.onSurfaceVariant).copy(alpha = 0.5f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

fun getFolderVector(iconName: String): ImageVector {
    return when (iconName.lowercase()) {
        "work" -> Icons.Default.Work
        "person" -> Icons.Default.Person
        "school" -> Icons.Default.School
        "lightbulb" -> Icons.Default.Lightbulb
        "flight" -> Icons.Default.Flight
        else -> Icons.Default.Folder
    }
}

enum class FolderSortOption(val label: String) {
    DATE_MODIFIED("Date Modified"),
    DATE_CREATED("Date Created"),
    ALPHABETICAL_ASC("Title (A-Z)"),
    ALPHABETICAL_DESC("Title (Z-A)")
}
