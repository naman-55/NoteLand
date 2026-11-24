package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.StickyNote2
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.RemoveRedEye
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.toColorInt
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.CanvasEntity
import com.example.data.model.NoteType
import com.example.data.model.StickyNoteEntity
import com.example.ui.components.EmptyStateView
import com.example.ui.components.StaggeredSectionEntrance
import com.example.ui.components.StudioHeaderIllustration
import com.example.ui.components.springPressScale
import com.example.ui.navigation.Screen
import com.example.ui.theme.AppSection
import com.example.ui.theme.DarkFoldersAccent
import com.example.ui.theme.DarkHomeAccent
import com.example.ui.theme.DarkStudioAccent
import com.example.ui.theme.DisplayFontFamily
import com.example.ui.theme.LightFoldersAccent
import com.example.ui.theme.LightHomeAccent
import com.example.ui.theme.LightStudioAccent
import com.example.ui.theme.NoteColorPresets
import com.example.ui.theme.NotelandBlue
import com.example.ui.theme.NotelandEmerald
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
import com.example.util.SmartNoteUtils
import java.util.UUID

@Composable
fun StudioScreen(
    viewModel: NoteViewModel,
    onNavigateToCanvas: (String) -> Unit,
    onNavigateToDestination: (String) -> Unit
) {
    val isDark = isAppInDarkTheme
    val canvases by viewModel.canvases.collectAsStateWithLifecycle()
    val stickyNotes by viewModel.stickyNotes.collectAsStateWithLifecycle()
    val stickyBoards by viewModel.stickyBoards.collectAsStateWithLifecycle()
    val currentStickyBoard by viewModel.currentStickyBoard.collectAsStateWithLifecycle()
    val allActiveNotes by viewModel.allActiveNotes.collectAsStateWithLifecycle()

    var showConvertNoteDialog by remember { mutableStateOf(false) }
    val navBottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    val headerTextColor = editorialHeaderTextColor(isDark)
    val headerSubColor = editorialHeaderSubColor(isDark)

    val studioAccent = if (isDark) DarkStudioAccent else LightStudioAccent
    var canvasToDelete by remember { mutableStateOf<CanvasEntity?>(null) }

    var showQuickStickyDialog by remember { mutableStateOf(false) }

    ProvideSectionTheme(section = AppSection.STUDIO, isDark = isDark) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(editorialBackdropBrush(isDark, ScreenBackdropTone.STUDIO_LAVENDER))
            .statusBarsPadding()
            .testTag("studio_screen_root")
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 760.dp)
                .align(Alignment.TopCenter),
            contentPadding = PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = 24.dp,
                bottom = 118.dp + navBottomPadding
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header with desk tablet & plant vector illustration
            item {
                StaggeredSectionEntrance(index = 0) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = editorialCardContainerColor(isDark),
                            border = BorderStroke(1.2.dp, editorialCardBorderColor(isDark)),
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Search,
                                    contentDescription = "Search",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Studio",
                                style = MaterialTheme.typography.headlineLarge.copy(
                                    fontFamily = DisplayFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 31.sp
                                ),
                                color = headerTextColor
                            )
                            Text(
                                text = "Canvas, Mind Maps & Sticky Wall",
                                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                                color = headerSubColor
                            )
                        }
                        StudioHeaderIllustration(isDark = isDark)
                    }
                }
                }
            }

            // Primary 4 Studio Workspaces
            item {
                StaggeredSectionEntrance(index = 1) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StudioToolCard(
                            title = "Canvas",
                            subtitle = "Freedom to draw & create",
                            icon = Icons.Default.Palette,
                            accent = Color(0xFF10B981),
                            countLabel = "${canvases.size} boards",
                            onClick = {
                                viewModel.createFreeformCanvasProject("Untitled") { newId ->
                                    onNavigateToCanvas(newId)
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("studio_card_canvas")
                        )

                        StudioToolCard(
                            title = "Mind Map",
                            subtitle = "Visual ideas & connections",
                            icon = Icons.Default.Share,
                            accent = Color(0xFF10B981),
                            countLabel = "${canvases.size} maps",
                            onClick = {
                                viewModel.createMindMapProject("Untitled Mind Map") { newId ->
                                    onNavigateToCanvas(newId)
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("studio_card_mindmap")
                        )
                    }

                    StudioToolCard(
                        title = "Sticky Wall",
                        subtitle = "Capture & organize notes",
                        icon = Icons.Default.Layers,
                        accent = Color(0xFF8B5CF6),
                        countLabel = "${stickyBoards.size} boards",
                        onClick = { onNavigateToDestination(Screen.StickyBoard.route) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("studio_card_sticky_wall")
                    )
                }
                }
            }

            // Sticky Wall Preview Strip + Quick Add
            item {
                StaggeredSectionEntrance(index = 2) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$currentStickyBoard (${stickyNotes.size})",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = DisplayFontFamily,
                            fontWeight = FontWeight.Bold
                        ),
                        color = headerTextColor
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = { showQuickStickyDialog = true },
                            modifier = Modifier.heightIn(min = 48.dp).testTag("studio_add_sticky_btn")
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = null,
                                tint = studioAccent,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(3.dp))
                            Text(
                                "New Sticky",
                                color = studioAccent,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Surface(
                            onClick = { onNavigateToDestination(Screen.StickyBoard.route) },
                            shape = CircleShape,
                            color = editorialCardContainerColor(isDark),
                            border = BorderStroke(1.15.dp, editorialCardBorderColor(isDark))
                        ) {
                            Text(
                                text = "Open Wall",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = headerTextColor,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                            )
                        }
                    }
                }
                }
            }

            if (stickyNotes.isEmpty()) {
                item {
                    Surface(
                        onClick = { onNavigateToDestination(Screen.StickyBoard.route) },
                        shape = RoundedCornerShape(18.dp),
                        color = editorialCardContainerColor(isDark),
                        border = BorderStroke(1.2.dp, editorialCardBorderColor(isDark)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.StickyNote2,
                                contentDescription = null,
                                tint = studioAccent,
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                text = "No sticky notes yet. Tap New Sticky or open Sticky Wall.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            } else {
                item {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(stickyNotes.take(10), key = { it.id }) { sticky ->
                            val bg = remember(sticky.colorHex, isDark) {
                                resolveStudioStickyColor(sticky.colorHex, isDark)
                            }
                            val stickyInteraction = remember { MutableInteractionSource() }
                            Surface(
                                onClick = { onNavigateToDestination(Screen.StickyBoard.route) },
                                interactionSource = stickyInteraction,
                                shape = RoundedCornerShape(18.dp),
                                color = bg,
                                border = BorderStroke(1.2.dp, editorialCardBorderColor(isDark)),
                                modifier = Modifier
                                    .width(164.dp)
                                    .heightIn(min = 112.dp)
                                    .then(
                                        if (!isDark) {
                                            Modifier.shadow(
                                                elevation = 4.dp,
                                                shape = RoundedCornerShape(18.dp),
                                                ambientColor = Color.Black.copy(alpha = 0.08f),
                                                spotColor = Color.Black.copy(alpha = 0.06f)
                                            )
                                        } else Modifier
                                    )
                                    .springPressScale(stickyInteraction)
                            ) {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    Canvas(modifier = Modifier.matchParentSize()) {
                                        val cornerWave = Path().apply {
                                            moveTo(size.width * 0.48f, size.height)
                                            quadraticTo(
                                                size.width * 0.72f,
                                                size.height * 0.68f,
                                                size.width,
                                                size.height * 0.76f
                                            )
                                            lineTo(size.width, size.height)
                                            close()
                                        }
                                        drawPath(
                                            path = cornerWave,
                                            color = (if (isDark) Color.White else Color(0xFF15865D)).copy(alpha = if (isDark) 0.08f else 0.10f)
                                        )
                                    }
                                    Column(
                                        modifier = Modifier.padding(13.dp),
                                        verticalArrangement = Arrangement.spacedBy(5.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.Top
                                        ) {
                                            Text(
                                                text = sticky.title.ifBlank { "Sticky Note" },
                                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Icon(
                                                imageVector = Icons.Default.PushPin,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                                                modifier = Modifier
                                                    .size(14.dp)
                                                    .rotate(28f)
                                            )
                                        }
                                        Text(
                                            text = sticky.content.ifBlank { "Tap to edit on Sticky Wall" },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 3,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Canvases & Mind Maps Section
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Boards & Mind Maps (${canvases.size})",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = DisplayFontFamily,
                            fontWeight = FontWeight.Bold
                        ),
                        color = headerTextColor
                    )
                    TextButton(
                        onClick = { onNavigateToCanvas("0") },
                        modifier = Modifier.heightIn(min = 48.dp).testTag("studio_quick_mindmap_btn")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = studioAccent, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Blank Board", color = studioAccent, fontWeight = FontWeight.SemiBold)
                    }
                    IconButton(
                        onClick = { showConvertNoteDialog = true },
                        modifier = Modifier.testTag("studio_convert_note_btn")
                    ) {
                        Icon(Icons.Default.AccountTree, contentDescription = "Convert Note to Mind Map", tint = studioAccent)
                    }
                }
            }

            if (canvases.isEmpty()) {
                item {
                    EmptyStateView(
                        icon = Icons.Default.AccountTree,
                        title = "No Studio Boards Yet",
                        description = "Create a freeform Canvas or structured Mind Map above to start sketching.",
                        actionText = "New Mind Map",
                        onAction = {
                            viewModel.createMindMapProject("Untitled Mind Map") { newId ->
                                onNavigateToCanvas(newId)
                            }
                        }
                    )
                }
            } else {
                items(canvases.sortedByDescending { it.updatedAtUtc }, key = { it.id }) { canvas ->
                    val boardInteraction = remember { MutableInteractionSource() }
                    Surface(
                        onClick = { onNavigateToCanvas(canvas.id) },
                        interactionSource = boardInteraction,
                        shape = RoundedCornerShape(18.dp),
                        color = editorialCardContainerColor(isDark),
                        border = BorderStroke(1.2.dp, editorialCardBorderColor(isDark)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(
                                if (!isDark) {
                                    Modifier.shadow(
                                        elevation = 4.dp,
                                        shape = RoundedCornerShape(18.dp),
                                        ambientColor = Color(0xFF15865D).copy(alpha = 0.08f),
                                        spotColor = Color.Black.copy(alpha = 0.06f)
                                    )
                                } else Modifier
                            )
                            .springPressScale(boardInteraction, pressedScale = 0.985f)
                            .testTag("studio_canvas_item_${canvas.id}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF059669).copy(alpha = if (isDark) 0.22f else 0.12f),
                                modifier = Modifier.size(42.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = if (canvas.title.contains("Canvas", ignoreCase = true) || canvas.title.equals("Untitled", ignoreCase = true)) Icons.Default.Draw else Icons.Default.AccountTree,
                                        contentDescription = null,
                                        tint = Color(0xFF059669),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = canvas.title.ifBlank { "Untitled" },
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Updated ${SmartNoteUtils.formatRelativeTime(canvas.updatedAtUtc)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            var expandMenu by remember { mutableStateOf(false) }
                            Box {
                                IconButton(
                                    onClick = { expandMenu = true },
                                    modifier = Modifier.minimumInteractiveComponentSize()
                                ) {
                                    Icon(
                                        Icons.Default.MoreVert,
                                        contentDescription = "Options",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                androidx.compose.material3.DropdownMenu(
                                    expanded = expandMenu,
                                    onDismissRequest = { expandMenu = false }
                                ) {
                                    androidx.compose.material3.DropdownMenuItem(
                                        text = { Text("Duplicate") },
                                        onClick = {
                                            expandMenu = false
                                            viewModel.duplicateCanvas(canvas.id)
                                        },
                                        leadingIcon = {
                                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                                        }
                                    )
                                    androidx.compose.material3.DropdownMenuItem(
                                        text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                                        onClick = {
                                            expandMenu = false
                                            canvasToDelete = canvas
                                        },
                                        leadingIcon = {
                                            Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                        }
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

    if (showQuickStickyDialog) {
        QuickStickyCreateDialog(
            onDismiss = { showQuickStickyDialog = false },
            onCreate = { title, body, colorHex ->
                val now = System.currentTimeMillis()
                viewModel.saveStickyNote(
                    StickyNoteEntity(
                        id = UUID.randomUUID().toString(),
                        boardName = currentStickyBoard,
                        title = title,
                        content = body,
                        colorHex = colorHex,
                        createdAtUtc = now,
                        updatedAtUtc = now
                    )
                )
                showQuickStickyDialog = false
            }
        )
    }

    canvasToDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { canvasToDelete = null },
            title = { Text("Delete Board?") },
            text = { Text("Delete \"${target.title.ifBlank { "Untitled" }}\"? This cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteCanvas(target.id)
                        canvasToDelete = null
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { canvasToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showConvertNoteDialog) {
        AlertDialog(
            onDismissRequest = { showConvertNoteDialog = false },
            title = { Text("Convert Note to Mind Map") },
            text = {
                if (allActiveNotes.isEmpty()) {
                    Text("You have no notes to convert.")
                } else {
                    androidx.compose.foundation.lazy.LazyColumn(
                        modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp)
                    ) {
                        items(allActiveNotes.size) { index ->
                            val note = allActiveNotes[index]
                            androidx.compose.material3.ListItem(
                                headlineContent = { Text(note.title.ifBlank { "Untitled Note" }) },
                                modifier = Modifier.clickable {
                                    viewModel.convertNoteToMindMap(note.id) { newId ->
                                        onNavigateToCanvas(newId)
                                    }
                                    showConvertNoteDialog = false
                                }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showConvertNoteDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
private fun StudioToolCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accent: Color,
    countLabel: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isAppInDarkTheme
    val interactionSource = remember { MutableInteractionSource() }
    Surface(
        onClick = onClick,
        interactionSource = interactionSource,
        shape = RoundedCornerShape(20.dp),
        color = editorialCardContainerColor(isDark),
        border = BorderStroke(1.25.dp, editorialCardBorderColor(isDark)),
        modifier = modifier
            .heightIn(min = 112.dp)
            .then(
                if (!isDark) {
                    Modifier.shadow(
                        elevation = 4.dp,
                        shape = RoundedCornerShape(20.dp),
                        ambientColor = accent.copy(alpha = 0.10f),
                        spotColor = Color.Black.copy(alpha = 0.06f)
                    )
                } else Modifier
            )
            .springPressScale(interactionSource)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = accent.copy(alpha = if (isDark) 0.22f else 0.13f),
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = title,
                            tint = accent,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Options",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(Modifier.height(10.dp))

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            Spacer(Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = accent.copy(alpha = if (isDark) 0.24f else 0.14f)
                ) {
                    Text(
                        text = countLabel,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = accent,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun QuickStickyCreateDialog(
    onDismiss: () -> Unit,
    onCreate: (title: String, body: String, colorHex: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    val stickyColors = remember {
        NoteColorPresets.drop(1).take(6)
    }
    var selectedColorHex by remember { mutableStateOf(stickyColors.firstOrNull()?.hexCode ?: "#FEF3C7") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Sticky Wall Item") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("quick_sticky_title_input")
                )
                OutlinedTextField(
                    value = body,
                    onValueChange = { body = it },
                    label = { Text("Note") },
                    maxLines = 4,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("quick_sticky_body_input")
                )
                val studioColors = com.example.ui.theme.currentSectionColors
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    stickyColors.forEach { preset ->
                        val active = selectedColorHex.equals(preset.hexCode, ignoreCase = true)
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .clickable { selectedColorHex = preset.hexCode }
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = preset.lightBackground,
                                border = BorderStroke(
                                    width = if (active) 2.2.dp else 0.8.dp,
                                    color = if (active) studioColors.accent else MaterialTheme.colorScheme.outline
                                ),
                                modifier = Modifier.size(28.dp)
                            ) {}
                        }
                    }
                }
            }
        },
        confirmButton = {
            val studioColors = com.example.ui.theme.currentSectionColors
            Button(
                onClick = {
                    onCreate(
                        title.trim().ifBlank { "Sticky Note" },
                        body.trim(),
                        selectedColorHex
                    )
                },
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = studioColors.accent,
                    contentColor = studioColors.onAccent
                ),
                modifier = Modifier.testTag("quick_sticky_save_btn")
            ) {
                Text("Pin Sticky")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun resolveStudioStickyColor(hex: String, isDark: Boolean): Color {
    val preset = NoteColorPresets.firstOrNull { it.hexCode.equals(hex, ignoreCase = true) }
    if (preset != null && preset.hexCode.isNotEmpty()) {
        return preset.resolve(isDark)
    }
    return try {
        val raw = Color(hex.toColorInt())
        if (isDark) raw.copy(alpha = 0.25f) else raw
    } catch (_: Exception) {
        if (isDark) Color(0xFF2B2214) else Color(0xFFFEF3C7)
    }
}
