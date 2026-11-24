package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.StickyNote2
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.Event
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DisplayFontFamily
import com.example.ui.theme.editorialCardBorderColor
import com.example.ui.theme.editorialCardContainerColor
import com.example.ui.theme.isAppInDarkTheme

enum class QuickCreateAction(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val accentColor: Color,
    val testTag: String
) {
    NOTE(
        title = "Note",
        subtitle = "Rich text & checklists",
        icon = Icons.Default.Description,
        accentColor = Color(0xFF4F46E5),
        testTag = "quick_create_note"
    ),
    TASK(
        title = "Task",
        subtitle = "Priority, due & reminder",
        icon = Icons.Default.CheckCircleOutline,
        accentColor = Color(0xFFD97706),
        testTag = "quick_create_task"
    ),
    EVENT(
        title = "Event",
        subtitle = "Schedule on calendar",
        icon = Icons.Default.Event,
        accentColor = Color(0xFF7C3AED),
        testTag = "quick_create_event"
    ),
    MIND_MAP(
        title = "Mind Map",
        subtitle = "Visual branch diagram",
        icon = Icons.Default.AccountTree,
        accentColor = Color(0xFF059669),
        testTag = "quick_create_mindmap"
    ),
    CANVAS(
        title = "Canvas",
        subtitle = "Freehand sketching",
        icon = Icons.Default.Draw,
        accentColor = Color(0xFF2563EB),
        testTag = "quick_create_canvas"
    ),
    STICKY_WALL(
        title = "Sticky Item",
        subtitle = "Pin note on wall",
        icon = Icons.AutoMirrored.Filled.StickyNote2,
        accentColor = Color(0xFFDB2777),
        testTag = "quick_create_sticky"
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickCreateSheet(
    onDismiss: () -> Unit,
    onSelectAction: (QuickCreateAction) -> Unit
) {
    val isDark = isAppInDarkTheme
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = if (isDark) MaterialTheme.colorScheme.surfaceContainer else Color(0xFFFAF9F7),
        modifier = Modifier.testTag("quick_create_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Quick Create",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontFamily = DisplayFontFamily,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        text = "Capture a note, task, event, or studio project",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            QuickCreateAction.entries.chunked(2).forEach { pair ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    pair.forEach { action ->
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = editorialCardContainerColor(isDark),
                            border = BorderStroke(0.85.dp, editorialCardBorderColor(isDark)),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 74.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .clickable {
                                    onDismiss()
                                    onSelectAction(action)
                                }
                                .testTag(action.testTag)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = action.accentColor.copy(alpha = if (isDark) 0.22f else 0.13f),
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = action.icon,
                                            contentDescription = action.title,
                                            tint = action.accentColor,
                                            modifier = Modifier.size(19.dp)
                                        )
                                    }
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = action.title,
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 14.sp
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = action.subtitle,
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
        }
    }
}
