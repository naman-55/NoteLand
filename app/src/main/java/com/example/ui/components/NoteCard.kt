package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.NoteEntity
import com.example.data.model.NoteType
import com.example.ui.theme.editorialCardBorderColor
import com.example.ui.theme.editorialCardContainerColor
import com.example.ui.theme.isAppInDarkTheme
import com.example.ui.theme.resolveNoteBackgroundBrush
import com.example.util.SmartNoteUtils
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun NoteCard(
    note: NoteEntity,
    onClick: () -> Unit,
    onTogglePin: () -> Unit = {},
    onToggleFavorite: () -> Unit = {},
    onToggleArchive: () -> Unit = {},
    onDelete: () -> Unit = {},
    onSoftDelete: () -> Unit = onDelete,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }
    val isDark = isAppInDarkTheme

    val textureKey = remember(note.canvasDataJson) {
        SmartNoteUtils.parseNoteTexture(note.canvasDataJson)
    }
    val bgImagePath = remember(note.canvasDataJson) {
        SmartNoteUtils.parseNoteBackgroundImage(note.canvasDataJson)
    }
    val hasHandwritingInk = remember(note.canvasDataJson) {
        SmartNoteUtils.hasHandwritingStrokes(note.canvasDataJson)
    }
    val voiceClipsCount = remember(note.canvasDataJson) {
        SmartNoteUtils.parseVoiceRecordings(note.canvasDataJson).size
    }

    val checklistItems = remember(note.checklistJson) {
        SmartNoteUtils.parseChecklistJson(note.checklistJson)
    }

    val isDiary = note.tagsCsv.contains("Diary", ignoreCase = true) ||
        note.title.startsWith("Daily", ignoreCase = true) ||
        note.title.startsWith("Diary", ignoreCase = true)
    val isChecklist = checklistItems.isNotEmpty() || note.checklistJson.isNotBlank()
    val (typeIcon, typeLabel, typeTint) = when {
        note.isLocked -> Triple(Icons.Default.Lock, "Vault", Color(0xFFD97706))
        isDiary -> Triple(Icons.Default.CalendarToday, "Diary", Color(0xFFEA580C))
        note.noteType == NoteType.CANVAS.name -> Triple(Icons.Default.Brush, "Board", Color(0xFF2563EB))
        note.noteType == NoteType.STICKY.name -> Triple(Icons.Default.Layers, "Sticky Wall", Color(0xFF7C3AED))
        isChecklist -> Triple(Icons.Default.CheckBox, "Tasks", Color(0xFF059669))
        else -> Triple(Icons.Default.Description, "Note", Color(0xFF0EA5E9))
    }

    val fallbackBase = editorialCardContainerColor(isDark)
    val fallbackTinted = androidx.compose.ui.graphics.lerp(fallbackBase, typeTint, if (isDark) 0.12f else 0.08f)

    val cardBrush = resolveNoteBackgroundBrush(
        colorKey = note.colorHex,
        isDark = isDark,
        fallbackColor = fallbackTinted
    )

    val cardShape = RoundedCornerShape(18.dp)
    val cardBorderColor = if (note.isPinned) {
        MaterialTheme.colorScheme.primary.copy(alpha = if (isDark) 0.85f else 0.72f)
    } else {
        androidx.compose.ui.graphics.lerp(editorialCardBorderColor(isDark), typeTint, if (isDark) 0.3f else 0.4f)
    }
    val cardBorderWidth = if (note.isPinned) 1.6.dp else 1.25.dp

    Box(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (!isDark) {
                    Modifier.shadow(
                        elevation = 2.5.dp,
                        shape = cardShape,
                        ambientColor = Color.Black.copy(alpha = 0.06f),
                        spotColor = Color.Black.copy(alpha = 0.05f)
                    )
                } else Modifier
            )
            .clip(cardShape)
            .background(cardBrush, cardShape)
            .border(cardBorderWidth, cardBorderColor, cardShape)
            .testTag("note_card_${note.id}")
            .combinedClickable(
                onClick = onClick,
                onLongClick = { showMenu = true }
            )
    ) {
            if (bgImagePath.isNotBlank()) {
                AsyncImage(
                    model = if (bgImagePath.startsWith("/")) File(bgImagePath) else bgImagePath,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize()
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            if (isDark) Color(0xFF040708).copy(alpha = 0.72f)
                            else Color.White.copy(alpha = 0.74f)
                        )
                )
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .drawNoteTexture(textureKey, isDark = isDark, subtle = true)
                    .padding(14.dp)
            ) {
            // Top Utility Row: Type badge + Compact Action Controls (leaves full width below for title)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = typeTint.copy(alpha = if (isDark) 0.18f else 0.10f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                    ) {
                        Icon(
                            typeIcon,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = typeTint
                        )
                        Text(
                            text = typeLabel,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 10.sp
                            ),
                            color = typeTint
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier
                            .size(30.dp)
                            .testTag("fav_button_${note.id}")
                    ) {
                        Icon(
                            if (note.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = if (note.isFavorite) "Remove from favourites" else "Add to favourites",
                            tint = if (note.isFavorite) Color(0xFFEF4444) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    IconButton(
                        onClick = onTogglePin,
                        modifier = Modifier
                            .size(30.dp)
                            .testTag("pin_button_${note.id}")
                    ) {
                        Icon(
                            if (note.isPinned) Icons.Default.PushPin else Icons.Outlined.PushPin,
                            contentDescription = if (note.isPinned) "Unpin" else "Pin",
                            tint = if (note.isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Box {
                        IconButton(
                            onClick = { showMenu = true },
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(
                                Icons.Default.MoreVert,
                                contentDescription = "More Options",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(if (note.isFavorite) "Remove from Favourites" else "Add to Favourites") },
                                onClick = {
                                    showMenu = false
                                    onToggleFavorite()
                                },
                                leadingIcon = {
                                    Icon(
                                        if (note.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                        contentDescription = null,
                                        tint = Color(0xFFEF4444)
                                    )
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(if (note.isArchived) "Unarchive" else "Archive") },
                                onClick = {
                                    showMenu = false
                                    onToggleArchive()
                                },
                                leadingIcon = { Icon(Icons.Default.Archive, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Move to Trash", color = MaterialTheme.colorScheme.error) },
                                onClick = {
                                    showMenu = false
                                    onSoftDelete()
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Title (Full Card Width)
            Text(
                text = if (note.isLocked) "Locked Note" else note.title.ifBlank { "Untitled" },
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.5.sp,
                    lineHeight = 20.sp
                ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            // Content Preview
            if (note.isLocked) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Encrypted in secure vault. Tap to unlock with PIN.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            } else if (note.content.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                val cleanPreview = note.content
                    .replace("<u>", "")
                    .replace("</u>", "")
                    .replace("~~", "")
                    .replace("==", "")
                    .replace("#", "")
                    .replace("*", "")
                    .trim()
                    .take(140)
                Text(
                    text = cleanPreview,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Checklist preview
            if (checklistItems.isNotEmpty() && !note.isLocked) {
                Spacer(Modifier.height(8.dp))
                val (done, total) = SmartNoteUtils.calculateChecklistProgress(checklistItems)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(13.dp),
                        tint = Color(0xFF10B981)
                    )
                    Text(
                        "$done/$total tasks",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { if (total > 0) done.toFloat() / total.toFloat() else 0f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.5.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = Color(0xFF10B981),
                    trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                    strokeCap = StrokeCap.Round
                )
            }

            // Tags
            val parsedTags = remember(note.tagsCsv) {
                note.tagsCsv.split(",").map { it.trim() }.filter { it.isNotBlank() }
            }
            if (parsedTags.isNotEmpty() && !note.isLocked) {
                Spacer(Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    parsedTags.take(2).forEach { tag ->
                        Surface(
                            color = if (isDark) Color.White.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceContainerLow,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(
                                0.5.dp,
                                if (isDark) Color.White.copy(alpha = 0.14f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )
                        ) {
                            Text(
                                "#$tag",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    if (parsedTags.size > 2) {
                        Text(
                            "+${parsedTags.size - 2}",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // Footer: Concise date & optional reminder indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val formattedDate = remember(note.updatedAtUtc) {
                    SimpleDateFormat("MMM d · HH:mm", Locale.getDefault()).format(Date(note.updatedAtUtc))
                }
                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                    maxLines = 1
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (voiceClipsCount > 0 && !note.isLocked) {
                        val voiceTint = if (isDark) MaterialTheme.colorScheme.primary else Color(0xFFD85C37)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                Icons.Default.GraphicEq,
                                contentDescription = "Has voice recordings",
                                modifier = Modifier.size(12.dp),
                                tint = voiceTint
                            )
                            Text(
                                text = if (voiceClipsCount == 1) "Voice" else "Voice ($voiceClipsCount)",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = voiceTint,
                                maxLines = 1
                            )
                        }
                    }

                    if (hasHandwritingInk && !note.isLocked) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                Icons.Default.Draw,
                                contentDescription = "Has handwritten annotations",
                                modifier = Modifier.size(12.dp),
                                tint = typeTint
                            )
                            Text(
                                "Ink",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = typeTint,
                                maxLines = 1
                            )
                        }
                    }

                    if (note.reminderTimestamp != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                Icons.Default.Notifications,
                                contentDescription = "Reminder",
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                SmartNoteUtils.formatShortDate(note.reminderTimestamp),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
        Box(
            modifier = Modifier
                .matchParentSize()
                .border(
                    width = cardBorderWidth,
                    color = cardBorderColor,
                    shape = cardShape
                )
        )
    }
}

/**
 * True 2-column masonry layout that places each card immediately below the
 * shortest column without leaving empty vertical gaps while waiting for the
 * neighboring column to finish.
 */
@Composable
fun StaggeredTwoColumnNotesLayout(
    horizontalSpacing: Dp = 12.dp,
    verticalSpacing: Dp = 12.dp,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Layout(
        content = content,
        modifier = modifier
    ) { measurables, constraints ->
        if (measurables.isEmpty()) {
            return@Layout layout(constraints.maxWidth, 0) {}
        }
        val hSpacingPx = horizontalSpacing.roundToPx()
        val vSpacingPx = verticalSpacing.roundToPx()
        val columnWidth = ((constraints.maxWidth - hSpacingPx) / 2).coerceAtLeast(0)
        val itemConstraints = constraints.copy(
            minWidth = columnWidth,
            maxWidth = columnWidth,
            minHeight = 0,
            maxHeight = Constraints.Infinity
        )

        val columnHeights = IntArray(2) { 0 }
        val placements = ArrayList<Triple<Placeable, Int, Int>>(measurables.size)

        measurables.forEach { measurable ->
            val placeable = measurable.measure(itemConstraints)
            val targetCol = if (columnHeights[0] <= columnHeights[1]) 0 else 1
            val x = targetCol * (columnWidth + hSpacingPx)
            val y = columnHeights[targetCol]
            placements.add(Triple(placeable, x, y))
            columnHeights[targetCol] = y + placeable.height + vSpacingPx
        }

        val totalHeight = (columnHeights.maxOrNull()?.minus(vSpacingPx) ?: 0).coerceAtLeast(0)
        layout(constraints.maxWidth, totalHeight) {
            placements.forEach { (placeable, x, y) ->
                placeable.placeRelative(x = x, y = y)
            }
        }
    }
}
