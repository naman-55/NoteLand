package com.example.ui.components

import androidx.core.graphics.toColorInt
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOff
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SubdirectoryArrowRight
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
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
import com.example.data.model.FolderEntity
import com.example.data.model.NoteEntity
import com.example.ui.screens.getFolderVector
import com.example.util.FolderHierarchyUtils
import com.example.util.FolderTreeNode

private val HierarchyTerracotta: Color
    @Composable
    get() = if (com.example.ui.theme.isAppInDarkTheme) MaterialTheme.colorScheme.primary else Color(0xFFD85C37)

/**
 * Interactive horizontal breadcrumb strip showing: All Folders > Parent > Subfolder > Current.
 */
@Composable
fun FolderBreadcrumbBar(
    trail: List<FolderEntity>,
    isDark: Boolean,
    onSelectFolderId: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    if (trail.isEmpty()) return

    val chipBg = if (isDark) Color.White.copy(alpha = 0.09f) else Color.White.copy(alpha = 0.72f)
    val chipBorder = if (isDark) Color.White.copy(alpha = 0.2f) else Color(0xFF1F1C19).copy(alpha = 0.14f)
    val textColor = if (isDark) Color.White.copy(alpha = 0.88f) else Color(0xFF2B2622)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .testTag("folder_breadcrumb_bar"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = chipBg,
            border = BorderStroke(0.8.dp, chipBorder),
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .clip(RoundedCornerShape(12.dp))
                .clickable { onSelectFolderId(null) }
                .testTag("breadcrumb_root")
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Icon(
                    Icons.Default.Home,
                    contentDescription = "All Folders",
                    tint = HierarchyTerracotta,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = "Folders",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                    color = textColor,
                    maxLines = 1
                )
            }
        }

        trail.forEachIndexed { index, crumb ->
            val isCurrent = index == trail.lastIndex
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = textColor.copy(alpha = 0.55f),
                modifier = Modifier.size(16.dp)
            )

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isCurrent) HierarchyTerracotta.copy(alpha = 0.16f) else chipBg,
                border = BorderStroke(
                    0.9.dp,
                    if (isCurrent) HierarchyTerracotta.copy(alpha = 0.5f) else chipBorder
                ),
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(enabled = !isCurrent) { onSelectFolderId(crumb.id) }
                    .testTag("breadcrumb_${crumb.id}")
            ) {
                Text(
                    text = crumb.name,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium
                    ),
                    color = if (isCurrent) HierarchyTerracotta else textColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }
    }
}

/**
 * Reusable bottom sheet showing the full hierarchical folder tree for selecting a note's folder,
 * moving a folder to a new parent, or quickly creating a nested folder.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HierarchicalFolderPickerSheet(
    title: String,
    subtitle: String? = null,
    folders: List<FolderEntity>,
    notes: List<NoteEntity> = emptyList(),
    attachments: List<AttachmentEntity> = emptyList(),
    selectedFolderId: String?,
    rootOptionLabel: String = "No Folder (Uncategorized)",
    excludeSubtreeOfId: String? = null,
    allowCreateFolder: Boolean = false,
    onCreateQuickFolder: ((name: String, parentId: String?) -> Unit)? = null,
    onSelectFolder: (FolderEntity?) -> Unit,
    onDismiss: () -> Unit
) {
    var searchText by remember { mutableStateOf("") }
    var showQuickCreateRow by remember { mutableStateOf(false) }
    var quickFolderName by remember { mutableStateOf("") }
    var quickParentId by remember { mutableStateOf<String?>(selectedFolderId) }

    val treeNodes = remember(folders, notes, attachments, excludeSubtreeOfId) {
        FolderHierarchyUtils.flattenFolderTree(
            folders = folders,
            notes = notes,
            excludeSubtreeOfId = excludeSubtreeOfId,
            attachments = attachments
        )
    }

    val filteredNodes = remember(treeNodes, searchText) {
        val q = searchText.trim()
        if (q.isEmpty()) treeNodes
        else treeNodes.filter {
            it.folder.name.contains(q, ignoreCase = true) ||
                it.fullPath.contains(q, ignoreCase = true)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("hierarchical_folder_picker_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (!subtitle.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (allowCreateFolder && onCreateQuickFolder != null) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = HierarchyTerracotta.copy(alpha = 0.14f),
                        border = BorderStroke(0.8.dp, HierarchyTerracotta.copy(alpha = 0.45f)),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                quickParentId = selectedFolderId
                                showQuickCreateRow = !showQuickCreateRow
                            }
                            .testTag("picker_quick_new_folder_btn")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                        ) {
                            Icon(
                                Icons.Default.CreateNewFolder,
                                contentDescription = "New Folder",
                                tint = HierarchyTerracotta,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "New",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = HierarchyTerracotta
                            )
                        }
                    }
                }
            }

            if (showQuickCreateRow && onCreateQuickFolder != null) {
                Spacer(modifier = Modifier.height(10.dp))
                val parentPath = if (quickParentId != null) {
                    FolderHierarchyUtils.getFolderPathString(folders, quickParentId)
                } else {
                    "Root"
                }
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Create inside: $parentPath",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = HierarchyTerracotta
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = quickFolderName,
                                onValueChange = { quickFolderName = it },
                                placeholder = { Text("Subfolder name") },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("picker_quick_folder_input")
                            )
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = HierarchyTerracotta,
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        val clean = quickFolderName.trim()
                                        if (clean.isNotEmpty()) {
                                            onCreateQuickFolder(clean, quickParentId)
                                            quickFolderName = ""
                                            showQuickCreateRow = false
                                        }
                                    }
                                    .testTag("picker_quick_folder_create_confirm")
                            ) {
                                Text(
                                    text = "Add",
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
                                )
                            }
                        }
                    }
                }
            }

            if (treeNodes.size > 5) {
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = searchText,
                    onValueChange = { searchText = it },
                    placeholder = { Text("Filter folders...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 380.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                item(key = "root_option") {
                    val isSelected = selectedFolderId == null
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isSelected) HierarchyTerracotta.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceContainerLow,
                        border = BorderStroke(
                            0.8.dp,
                            if (isSelected) HierarchyTerracotta.copy(alpha = 0.55f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable {
                                onSelectFolder(null)
                                onDismiss()
                            }
                            .testTag("picker_root_option")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    Icons.Default.FolderOff,
                                    contentDescription = null,
                                    tint = if (isSelected) HierarchyTerracotta else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = rootOptionLabel,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            if (isSelected) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = HierarchyTerracotta,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                items(filteredNodes, key = { it.folder.id }) { node ->
                    val folder = node.folder
                    val isSelected = selectedFolderId == folder.id
                    val fallbackAccent = HierarchyTerracotta
                    val accent = remember(folder.colorHex, fallbackAccent) {
                        try {
                            Color(folder.colorHex.toColorInt())
                        } catch (_: Exception) {
                            fallbackAccent
                        }
                    }
                    val indentDp = if (searchText.isBlank()) (node.depth.coerceAtMost(5) * 18).dp else 0.dp

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isSelected) HierarchyTerracotta.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface,
                        border = BorderStroke(
                            0.8.dp,
                            if (isSelected) HierarchyTerracotta.copy(alpha = 0.55f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = indentDp)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable {
                                onSelectFolder(folder)
                                onDismiss()
                            }
                            .testTag("picker_folder_${folder.id}")
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
                                if (node.depth > 0 && searchText.isBlank()) {
                                    Icon(
                                        Icons.Default.SubdirectoryArrowRight,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = accent.copy(alpha = 0.16f),
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            getFolderVector(folder.iconName),
                                            contentDescription = null,
                                            tint = accent,
                                            modifier = Modifier.size(17.dp)
                                        )
                                    }
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = folder.name,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold
                                        ),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    val metaParts = buildList {
                                        if (node.depth > 0 && searchText.isNotBlank()) add(node.fullPath)
                                        if (node.directSubfolderCount > 0) add("${node.directSubfolderCount} sub")
                                        add("${node.totalNoteCount} notes")
                                        if (node.totalDocumentCount > 0) add("${node.totalDocumentCount} files")
                                    }
                                    Text(
                                        text = metaParts.joinToString(" · "),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            if (isSelected) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = HierarchyTerracotta,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Interactive collapsible folder tree row used in Tree View mode on FoldersScreen.
 */
@Composable
fun FolderTreeItemRow(
    node: FolderTreeNode,
    isExpanded: Boolean,
    isDark: Boolean,
    onToggleExpand: () -> Unit,
    onOpenFolder: () -> Unit,
    onAddSubfolder: () -> Unit,
    onEditFolder: () -> Unit,
    onMoveFolder: () -> Unit,
    onDeleteFolder: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }
    val folder = node.folder
    val hasChildren = node.directSubfolderCount > 0
    val fallbackAccent = HierarchyTerracotta
    val accent = remember(folder.colorHex, fallbackAccent) {
        try {
            Color(folder.colorHex.toColorInt())
        } catch (_: Exception) {
            fallbackAccent
        }
    }
    val cardBg = if (isDark) Color.White.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.90f)
    val cardBorder = if (isDark) Color.White.copy(alpha = 0.14f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.68f)
    val textColor = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface
    val subColor = if (isDark) Color.White.copy(alpha = 0.72f) else MaterialTheme.colorScheme.onSurfaceVariant
    val indentDp = (node.depth.coerceAtMost(5) * 18).dp

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = cardBg,
        border = BorderStroke(0.8.dp, cardBorder),
        modifier = modifier
            .fillMaxWidth()
            .padding(start = indentDp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onOpenFolder)
            .testTag("folder_tree_row_${folder.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 6.dp, end = 6.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (hasChildren) {
                IconButton(
                    onClick = onToggleExpand,
                    modifier = Modifier
                        .size(38.dp)
                        .testTag("folder_tree_expand_${folder.id}")
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandMore else Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = if (isExpanded) "Collapse ${folder.name}" else "Expand ${folder.name}",
                        tint = subColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            } else {
                Box(
                    modifier = Modifier.size(38.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (node.depth > 0) {
                        Icon(
                            Icons.Default.SubdirectoryArrowRight,
                            contentDescription = null,
                            tint = subColor.copy(alpha = 0.55f),
                            modifier = Modifier.size(16.dp)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(accent.copy(alpha = 0.65f))
                        )
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = accent.copy(alpha = 0.16f),
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isExpanded && hasChildren) Icons.Default.FolderOpen else getFolderVector(folder.iconName),
                        contentDescription = folder.name,
                        tint = accent,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = folder.name,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    ),
                    color = textColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val summary = buildString {
                    if (node.directSubfolderCount > 0) {
                        append(if (node.directSubfolderCount == 1) "1 subfolder" else "${node.directSubfolderCount} subfolders")
                        append(" · ")
                    }
                    append(if (node.totalNoteCount == 1) "1 note" else "${node.totalNoteCount} notes")
                    if (node.totalDocumentCount > 0) {
                        append(if (node.totalDocumentCount == 1) " · 1 file" else " · ${node.totalDocumentCount} files")
                    }
                }
                Text(
                    text = summary,
                    style = MaterialTheme.typography.labelSmall,
                    color = subColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(
                onClick = onAddSubfolder,
                modifier = Modifier
                    .size(40.dp)
                    .testTag("folder_tree_add_sub_${folder.id}")
            ) {
                Icon(
                    Icons.Default.CreateNewFolder,
                    contentDescription = "Add subfolder inside ${folder.name}",
                    tint = HierarchyTerracotta,
                    modifier = Modifier.size(18.dp)
                )
            }

            Box {
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = "Folder actions",
                        tint = subColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("New Subfolder") },
                        leadingIcon = { Icon(Icons.Default.CreateNewFolder, contentDescription = null, tint = HierarchyTerracotta) },
                        onClick = {
                            showMenu = false
                            onAddSubfolder()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Edit Folder") },
                        onClick = {
                            showMenu = false
                            onEditFolder()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Move to Folder...") },
                        leadingIcon = { Icon(Icons.Default.AccountTree, contentDescription = null) },
                        onClick = {
                            showMenu = false
                            onMoveFolder()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                        onClick = {
                            showMenu = false
                            onDeleteFolder()
                        }
                    )
                }
            }
        }
    }
}
