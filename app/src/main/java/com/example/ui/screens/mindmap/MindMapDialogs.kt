package com.example.ui.screens.mindmap

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import com.example.data.model.CanvasEntity
import com.example.ui.screens.CanvasItem
import com.example.ui.theme.isAppInDarkTheme

private val DialogEmerald = Color(0xFF16A34A)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MindMapNodeEditDialog(
    node: MindMapNode,
    hasChildren: Boolean = false,
    onDismiss: () -> Unit,
    onSave: (title: String, iconKey: String, isLocked: Boolean) -> Unit,
    onAddChild: () -> Unit,
    onAddSibling: () -> Unit = {},
    onAddParent: () -> Unit = {},
    onToggleCollapse: () -> Unit = {},
    onDetachParent: () -> Unit = {},
    onDelete: () -> Unit
) {
    val isDark = isAppInDarkTheme
    val selectedBg = if (isDark) Color(0xFF134E3A) else Color(0xFFDCFCE7)
    val selectedBorder = if (isDark) Color(0xFF34D399) else DialogEmerald
    var title by remember(node.id) { mutableStateOf(node.title) }
    var iconKey by remember(node.id) { mutableStateOf(node.iconKey) }
    var locked by remember(node.id) { mutableStateOf(node.isLocked) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (node.level == 0) "Edit Main Topic" else "Edit Node",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Node Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("node_edit_title_input")
                )

                Text("Icon", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (iconKey.isBlank()) selectedBg else MaterialTheme.colorScheme.surfaceContainerLow,
                        border = BorderStroke(1.dp, if (iconKey.isBlank()) selectedBorder else MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.clickable { iconKey = "" }
                    ) {
                        Text(
                            text = "None",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                    MindMapNodeIcons.forEach { (key, glyph) ->
                        val picked = iconKey == key
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (picked) selectedBg else MaterialTheme.colorScheme.surfaceContainerLow,
                            border = BorderStroke(1.dp, if (picked) selectedBorder else MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.clickable { iconKey = key }
                        ) {
                            Text(
                                text = glyph,
                                fontSize = 16.sp,
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Lock position", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    Switch(checked = locked, onCheckedChange = { locked = it })
                }

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            onSave(title.trim().ifBlank { node.title }, iconKey, locked)
                            onAddChild()
                        }
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Add Child", fontSize = 12.sp)
                    }
                    if (node.parentId != null) {
                        OutlinedButton(
                            onClick = {
                                onSave(title.trim().ifBlank { node.title }, iconKey, locked)
                                onAddSibling()
                            }
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Add Sibling", fontSize = 12.sp)
                        }
                    }
                    OutlinedButton(
                        onClick = {
                            onSave(title.trim().ifBlank { node.title }, iconKey, locked)
                            onAddParent()
                        }
                    ) {
                        Icon(Icons.Default.AccountTree, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Add Parent", fontSize = 12.sp)
                    }
                    if (hasChildren) {
                        OutlinedButton(
                            onClick = {
                                onSave(title.trim().ifBlank { node.title }, iconKey, locked)
                                onToggleCollapse()
                            }
                        ) {
                            Text(if (node.isCollapsed) "Expand" else "Collapse", fontSize = 12.sp)
                        }
                    }
                    if (node.parentId != null) {
                        OutlinedButton(onClick = onDetachParent) {
                            Text("Unlink", fontSize = 12.sp)
                        }
                    }
                    OutlinedButton(
                        onClick = onDelete,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete node", modifier = Modifier.size(16.dp))
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(title.trim().ifBlank { node.title }, iconKey, locked) },
                colors = ButtonDefaults.buttonColors(containerColor = DialogEmerald),
                modifier = Modifier.testTag("node_edit_save_btn")
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MindMapIconPickerDialog(
    currentIconKey: String,
    onDismiss: () -> Unit,
    onPickIcon: (String) -> Unit
) {
    val isDark = isAppInDarkTheme
    val selectedBg = if (isDark) Color(0xFF134E3A) else Color(0xFFDCFCE7)
    val selectedBorder = if (isDark) Color(0xFF34D399) else DialogEmerald

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose Node Icon", fontWeight = FontWeight.Bold) },
        text = {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (currentIconKey.isBlank()) selectedBg else MaterialTheme.colorScheme.surfaceContainerLow,
                    border = BorderStroke(1.dp, if (currentIconKey.isBlank()) selectedBorder else MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.clickable { onPickIcon("") }
                ) {
                    Text(
                        "No Icon",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                    )
                }
                MindMapNodeIcons.forEach { (key, glyph) ->
                    val picked = currentIconKey == key
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (picked) selectedBg else MaterialTheme.colorScheme.surfaceContainerLow,
                        border = BorderStroke(1.2.dp, if (picked) selectedBorder else MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.clickable { onPickIcon(key) }
                    ) {
                        Text(glyph, fontSize = 22.sp, modifier = Modifier.padding(10.dp))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MindMapBoardSwitcherSheet(
    currentCanvasId: String,
    canvases: List<CanvasEntity>,
    onDismiss: () -> Unit,
    onSelectCanvas: (CanvasEntity) -> Unit,
    onCreateNewBoard: (String) -> Unit,
    onDeleteCanvas: (CanvasEntity) -> Unit
) {
    val isDark = isAppInDarkTheme
    val selectedBg = if (isDark) Color(0xFF134E3A) else Color(0xFFDCFCE7)
    val selectedBorder = if (isDark) Color(0xFF34D399) else DialogEmerald

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Mind Maps & Boards", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Button(
                    onClick = {
                        onCreateNewBoard("Untitled Mind Map")
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DialogEmerald),
                    modifier = Modifier.testTag("board_switcher_new_btn")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("New Map")
                }
            }

            HorizontalDivider()

            LazyColumn(
                modifier = Modifier.heightIn(max = 320.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(canvases, key = { it.id }) { board ->
                    val isCurrent = board.id == currentCanvasId
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isCurrent) selectedBg else MaterialTheme.colorScheme.surfaceContainerLow,
                        border = BorderStroke(1.dp, if (isCurrent) selectedBorder else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSelectCanvas(board)
                                onDismiss()
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(
                                    Icons.Default.AccountTree,
                                    contentDescription = null,
                                    tint = if (isCurrent) selectedBorder else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    text = board.title.ifBlank { "Untitled Mind Map" },
                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            if (canvases.size > 1 && !isCurrent) {
                                IconButton(onClick = { onDeleteCanvas(board) }, modifier = Modifier.size(36.dp)) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete board", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MindMapLayersSheet(
    nodes: List<MindMapNode>,
    canvasItems: List<CanvasItem>,
    selectedNodeId: String?,
    selectedItemId: String?,
    onDismiss: () -> Unit,
    onSelectNode: (MindMapNode) -> Unit,
    onToggleNodeLock: (MindMapNode) -> Unit,
    onDeleteNode: (MindMapNode) -> Unit,
    onSelectCanvasItem: (CanvasItem) -> Unit,
    onDeleteCanvasItem: (CanvasItem) -> Unit
) {
    val isDark = isAppInDarkTheme
    val selectedBg = if (isDark) Color(0xFF134E3A) else Color(0xFFDCFCE7)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .padding(bottom = 24.dp)
                .testTag("mindmap_layers_sheet"),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Layers (${nodes.size} nodes · ${canvasItems.size} strokes/items)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            HorizontalDivider()
            LazyColumn(
                modifier = Modifier.heightIn(max = 360.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(nodes, key = { it.id }) { node ->
                    val isSelected = node.id == selectedNodeId
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) selectedBg else MaterialTheme.colorScheme.surfaceContainerLow,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSelectNode(node)
                                onDismiss()
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(Color(node.colorArgb))
                            )
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${resolveMindMapIconGlyph(node.iconKey)} ${node.title}".trim(),
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = when (node.level) {
                                        0 -> "Root Topic"
                                        1 -> "Primary Topic"
                                        else -> "Subtopic · L${node.level}"
                                    },
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = { onToggleNodeLock(node) }, modifier = Modifier.size(36.dp)) {
                                Icon(
                                    imageVector = if (node.isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                                    contentDescription = if (node.isLocked) "Unlock node" else "Lock node",
                                    tint = if (node.isLocked) DialogEmerald else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            IconButton(onClick = { onDeleteNode(node) }, modifier = Modifier.size(36.dp)) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete node", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
                items(canvasItems, key = { it.id }) { item ->
                    val isSelected = item.id == selectedItemId
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) selectedBg else MaterialTheme.colorScheme.surfaceContainerLow,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSelectCanvasItem(item)
                                onDismiss()
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(Color(item.colorArgb))
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = if (item.text.isNotBlank()) "${item.type}: ${item.text}" else item.type,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            IconButton(onClick = { onDeleteCanvasItem(item) }, modifier = Modifier.size(36.dp)) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete element", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MindMapFindDialog(
    nodes: List<MindMapNode>,
    onDismiss: () -> Unit,
    onSelectMatch: (MindMapNode) -> Unit
) {
    var query by remember { mutableStateOf("") }
    val matches = remember(query, nodes) {
        if (query.isBlank()) nodes
        else nodes.filter { it.title.contains(query, ignoreCase = true) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Find Topic or Node", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    placeholder = { Text("Search node title...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("mindmap_find_input")
                )
                LazyColumn(
                    modifier = Modifier.heightIn(max = 220.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(matches, key = { it.id }) { node ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSelectMatch(node)
                                    onDismiss()
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(Color(node.colorArgb))
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(node.title, fontWeight = FontWeight.Medium, fontSize = 14.sp)
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

@Composable
fun MindMapSettingsDialog(
    styleConfig: MindMapStyleConfig,
    onDismiss: () -> Unit,
    onUpdateStyleConfig: (MindMapStyleConfig) -> Unit,
    onResetStarterMap: () -> Unit,
    onClearFreehand: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Mind Map Settings", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Show Subtle Dot Grid", fontSize = 14.sp)
                    Switch(
                        checked = styleConfig.showGrid,
                        onCheckedChange = { onUpdateStyleConfig(styleConfig.copy(showGrid = it)) }
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Ambient Mint Waves", fontSize = 14.sp)
                    Switch(
                        checked = styleConfig.showMintWaves,
                        onCheckedChange = { onUpdateStyleConfig(styleConfig.copy(showMintWaves = it)) }
                    )
                }
                HorizontalDivider()
                OutlinedButton(
                    onClick = {
                        onResetStarterMap()
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Reset to Starter Mind Map")
                }
                OutlinedButton(
                    onClick = {
                        onClearFreehand()
                        onDismiss()
                    },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Clear Freehand Ink & Shapes")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = DialogEmerald)
            ) {
                Text("Done")
            }
        }
    )
}

@Composable
fun MindMapExportDialog(
    title: String,
    onDismiss: () -> Unit,
    onSaveNow: () -> Unit,
    onExport: (shareImmediately: Boolean, format: String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Export \"$title\"", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Save your Mind Map locally or export a crisp high-resolution image or PDF to share.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(
                    onClick = {
                        onExport(true, "png")
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DialogEmerald),
                    modifier = Modifier.fillMaxWidth().testTag("export_share_png_btn")
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Export & Share PNG")
                }
                Button(
                    onClick = {
                        onExport(true, "jpg")
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DialogEmerald),
                    modifier = Modifier.fillMaxWidth().testTag("export_share_jpg_btn")
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Export & Share JPG")
                }
                Button(
                    onClick = {
                        onExport(true, "pdf")
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DialogEmerald),
                    modifier = Modifier.fillMaxWidth().testTag("export_share_pdf_btn")
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Export & Share PDF")
                }
                OutlinedButton(
                    onClick = {
                        onExport(false, "png")
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth().testTag("export_save_png_btn")
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Save PNG to Cache")
                }
                OutlinedButton(
                    onClick = {
                        onSaveNow()
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Save Mind Map Board")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
fun MindMapTextOrStickyDialog(
    isSticky: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isSticky) "Add Sticky Note" else "Add Canvas Text", fontWeight = FontWeight.Bold) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                placeholder = { Text(if (isSticky) "Write a quick note..." else "Enter text...") },
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    if (text.isNotBlank()) onConfirm(text.trim())
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = DialogEmerald)
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
