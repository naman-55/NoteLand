package com.example.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items as lazyItems
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ManageSearch
import androidx.compose.material.icons.automirrored.filled.StickyNote2
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.NoteEntity
import com.example.data.model.NoteType
import com.example.data.model.StickyNoteEntity
import com.example.data.model.TaskPriority
import com.example.ui.components.BotanicalLeafCornerAccent
import com.example.ui.components.EmptyStateView
import com.example.ui.components.HeroCardLeafWatermark
import com.example.ui.components.NoteCard
import com.example.ui.components.QuickCreateAction
import com.example.ui.components.QuickCreateSheet
import com.example.ui.components.StaggeredTwoColumnNotesLayout
import com.example.ui.components.TaskEditorDialog
import com.example.ui.components.springPressScale
import com.example.ui.navigation.Screen
import com.example.ui.theme.AppSection
import com.example.ui.theme.DarkHomeAccent
import com.example.ui.theme.DisplayFontFamily
import com.example.ui.theme.LightHomeAccent
import com.example.ui.theme.NotelandAmber
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
import com.example.util.FolderHierarchyUtils
import com.example.util.SmartNoteUtils
import com.example.util.rememberLifecycleCurrentHour
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Modern, clean, and elegant Home Screen for NoteLand matching the reference UI:
 * - Left Calendar icon button with expanded pill-shaped search bar
 * - Time-based greeting header with "New note" action button
 * - Hero Calendar & Tasks summary cards and 6 rounded feature cards
 * - "Recent notes" section with "View all" and high-aesthetic note cards
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: NoteViewModel,
    onNavigateToEditor: (String, String) -> Unit,
    onNavigateToCanvas: (String) -> Unit,
    onNavigateToScreen: (String) -> Unit
) {
    val filteredNotes by viewModel.filteredNotes.collectAsStateWithLifecycle()
    val allNotes by viewModel.allActiveNotes.collectAsStateWithLifecycle()
    val allReminders by viewModel.allReminders.collectAsStateWithLifecycle()
    val activeTasks by viewModel.activeTasks.collectAsStateWithLifecycle()
    val kanbanColumns by viewModel.kanbanColumns.collectAsStateWithLifecycle()
    val taskDependencies by viewModel.taskDependencies.collectAsStateWithLifecycle()
    val folders by viewModel.folders.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedTag by viewModel.selectedTag.collectAsStateWithLifecycle()
    val selectedFolderId by viewModel.selectedFolderId.collectAsStateWithLifecycle()
    val isGridView by viewModel.isGridView.collectAsStateWithLifecycle()

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val isDark = isAppInDarkTheme

    val currentHour = rememberLifecycleCurrentHour()

    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var newFolderName by remember { mutableStateOf("") }
    var showQuickCreateSheet by remember { mutableStateOf(false) }
    var showQuickTaskDialog by remember { mutableStateOf(false) }
    var showQuickEventDialog by remember { mutableStateOf(false) }
    var showQuickStickyDialog by remember { mutableStateOf(false) }

    val displayNotes = remember(filteredNotes) {
        filteredNotes.filter { it.noteType != NoteType.PDF_DOC.name }
    }
    val pinnedNotes = remember(allNotes) {
        allNotes.filter { it.isPinned && !it.isLocked && !it.isHidden && it.noteType != NoteType.PDF_DOC.name }
            .sortedByDescending { it.updatedAtUtc }
    }
    val todayTasksPreview = remember(activeTasks) {
        val endOfToday = SmartNoteUtils.startOfDayUtc(System.currentTimeMillis()) + 86_400_000L
        activeTasks
            .filter { !it.isCompleted }
            .sortedWith(
                compareBy<com.example.data.model.TaskEntity> {
                    val ts = it.scheduledStartUtc ?: it.dueAtUtc
                    if (ts != null && ts < endOfToday) 0 else 1
                }.thenBy { it.dueAtUtc ?: Long.MAX_VALUE }
            )
            .take(4)
    }
    val upcomingRemindersPreview = remember(allReminders, allNotes) {
        val now = System.currentTimeMillis()
        allReminders
            .filter { !it.isCompleted && it.scheduledTimeUtc >= now - 86_400_000L }
            .sortedBy { it.scheduledTimeUtc }
            .take(3)
    }

    val backdropBrush = editorialBackdropBrush(isDark, ScreenBackdropTone.DEFAULT)
    val headerTextColor = editorialHeaderTextColor(isDark)
    val headerSubColor = editorialHeaderSubColor(isDark)
    val homeAccent = if (isDark) DarkHomeAccent else LightHomeAccent
    val topBarCardColor = editorialCardContainerColor(isDark)
    val topBarCardBorder = editorialCardBorderColor(isDark)

    ProvideSectionTheme(section = AppSection.HOME, isDark = isDark) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(backdropBrush)
            .statusBarsPadding()
            .testTag("home_screen_root")
    ) {
            BotanicalLeafCornerAccent(
                isDark = isDark,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(width = 168.dp, height = 200.dp)
                    .padding(top = 6.dp)
            )
            val isWideScreen = maxWidth >= 600.dp
            val navBottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
            LazyColumn(
                contentPadding = PaddingValues(
                    start = 20.dp,
                    end = 20.dp,
                    top = 24.dp,
                    bottom = 118.dp + navBottomPadding
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .widthIn(max = 840.dp)
                    .fillMaxSize()
                    .align(Alignment.TopCenter)
                    .testTag("home_staggered_grid")
            ) {
                // 1. Top Bar: Full-width Material You Pill Search Bar + Integrated Search & Calendar Controls
                item {
                    Surface(
                        shape = CircleShape,
                        color = topBarCardColor,
                        border = BorderStroke(1.2.dp, topBarCardBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 2.dp, bottom = 4.dp)
                            .heightIn(min = 52.dp)
                            .then(
                                if (!isDark) {
                                    Modifier.shadow(
                                        elevation = 4.dp,
                                        shape = CircleShape,
                                        ambientColor = homeAccent.copy(alpha = 0.08f),
                                        spotColor = Color.Black.copy(alpha = 0.05f)
                                    )
                                } else Modifier
                            )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, end = 6.dp, top = 4.dp, bottom = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = "Search",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(19.dp)
                            )
                            Box(modifier = Modifier.weight(1f)) {
                                if (searchQuery.isEmpty()) {
                                    Text(
                                        text = "Search notes, sticky wall, boards...",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                BasicTextField(
                                    value = searchQuery,
                                    onValueChange = { viewModel.setSearchQuery(it) },
                                    singleLine = true,
                                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                                        color = MaterialTheme.colorScheme.onSurface
                                    ),
                                    cursorBrush = SolidColor(homeAccent),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("search_text_input")
                                )
                            }
                            if (searchQuery.isNotEmpty()) {
                                IconButton(
                                    onClick = { viewModel.setSearchQuery("") },
                                    modifier = Modifier.minimumInteractiveComponentSize()
                                ) {
                                    Icon(
                                        Icons.Default.Clear,
                                        contentDescription = "Clear search",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(17.dp)
                                    )
                                }
                            }
                            IconButton(
                                onClick = { onNavigateToScreen(Screen.Calendar.route) },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("home_calendar_button")
                            ) {
                                Icon(
                                    Icons.Default.CalendarToday,
                                    contentDescription = "My Calendar",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            IconButton(
                                onClick = {
                                    viewModel.setSearchFilterType("ALL")
                                    onNavigateToScreen(Screen.Search.route)
                                },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("home_global_search_button")
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ManageSearch,
                                    contentDescription = "Open Global Search",
                                    tint = homeAccent,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                // 2. Time-Based Greeting Header + Live Date + "New note" primary action
                item {
                    val greetingText = SmartNoteUtils.getTimeBasedGreeting(currentHour)
                    val (greetingIcon, greetingTint) = when (currentHour) {
                        in 5..11 -> Icons.Default.WbSunny to Color(0xFFF59E0B)
                        in 12..16 -> Icons.Default.WbSunny to Color(0xFFEA580C)
                        in 17..20 -> Icons.Default.WbTwilight to Color(0xFF8B5CF6)
                        else -> Icons.Default.Bedtime to Color(0xFF6366F1)
                    }
                    val todayDateStr = remember(currentHour) {
                        "Today · ${SmartNoteUtils.formatFullDateHeader(System.currentTimeMillis())}"
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp, bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 14.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = greetingText,
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        fontFamily = DisplayFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 27.sp,
                                        lineHeight = 33.sp
                                    ),
                                    color = headerTextColor,
                                    modifier = Modifier.testTag("home_greeting_text")
                                )
                                Icon(
                                    imageVector = greetingIcon,
                                    contentDescription = null,
                                    tint = greetingTint,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(Modifier.height(5.dp))
                            Text(
                                text = todayDateStr,
                                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.5.sp),
                                color = headerSubColor
                            )
                        }

                        val newNoteInteraction = remember { MutableInteractionSource() }
                        val newNoteContainer = if (isDark) DarkHomeAccent else LightHomeAccent
                        val newNoteContent = if (isDark) Color(0xFF072110) else Color.White
                        Surface(
                            onClick = { onNavigateToEditor("0", NoteType.STANDARD.name) },
                            interactionSource = newNoteInteraction,
                            shape = CircleShape,
                            color = newNoteContainer,
                            shadowElevation = 2.dp,
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .springPressScale(newNoteInteraction, pressedScale = 0.95f)
                                .testTag("home_new_note_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = null,
                                    tint = newNoteContent,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "New Note",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                                    color = newNoteContent
                                )
                            }
                        }
                    }
                }

                // 2a. Hero Calendar & Tasks Summary Cards
                item {
                    val todayStart = System.currentTimeMillis()
                    val linkedReminderIds = remember(activeTasks) {
                        activeTasks.mapNotNull { it.linkedReminderId }.toSet()
                    }
                    val standaloneReminders = remember(allReminders, linkedReminderIds) {
                        allReminders.filter { it.id !in linkedReminderIds }
                    }
                    val todayEventsCount = remember(standaloneReminders, activeTasks, allNotes) {
                        activeTasks.count { task ->
                            val ts = task.scheduledStartUtc ?: task.dueAtUtc
                            !task.isCompleted && ts != null && SmartNoteUtils.isSameDay(ts, todayStart)
                        } +
                            standaloneReminders.count { !it.isCompleted && SmartNoteUtils.isSameDay(it.scheduledTimeUtc, todayStart) } +
                            allNotes.count { it.reminderTimestamp != null && SmartNoteUtils.isSameDay(it.reminderTimestamp, todayStart) && allReminders.none { r -> r.noteId == it.id } }
                    }
                    val (tasksDone, tasksTotal) = remember(allNotes, standaloneReminders, activeTasks) {
                        var done = activeTasks.count { it.isCompleted } + standaloneReminders.count { it.isCompleted }
                        var total = activeTasks.size + standaloneReminders.size
                        allNotes.filter { !it.isLocked && it.checklistJson.isNotBlank() }.forEach { note ->
                            val list = SmartNoteUtils.parseChecklistJson(note.checklistJson)
                            done += list.count { it.isDone }
                            total += list.size
                        }
                        done to total
                    }

                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(bottom = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Emerald "My Calendar" Hero Card
                            val calendarHeroGradient = if (isDark) {
                                Brush.linearGradient(colors = listOf(Color(0xFF217A4A), Color(0xFF104628)))
                            } else {
                                Brush.linearGradient(colors = listOf(Color(0xFF28B97F), Color(0xFF0E7952)))
                            }
                            val tasksHeroGradient = if (isDark) {
                                Brush.linearGradient(colors = listOf(Color(0xFF9C6D21), Color(0xFF563A10)))
                            } else {
                                Brush.linearGradient(colors = listOf(Color(0xFFF7C34E), Color(0xFFDF8712)))
                            }
                            val calHeroInteraction = remember { MutableInteractionSource() }
                            val tasksHeroInteraction = remember { MutableInteractionSource() }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(122.dp)
                                    .then(
                                        if (!isDark) {
                                            Modifier.shadow(
                                                elevation = 6.dp,
                                                shape = RoundedCornerShape(22.dp),
                                                ambientColor = Color(0xFF15865D).copy(alpha = 0.22f),
                                                spotColor = Color(0xFF15865D).copy(alpha = 0.18f)
                                            )
                                        } else Modifier
                                    )
                                    .springPressScale(calHeroInteraction)
                                    .clip(RoundedCornerShape(22.dp))
                                    .background(calendarHeroGradient)
                                    .then(
                                        if (isDark) {
                                            Modifier.border(
                                                width = 1.dp,
                                                color = Color(0xFF6BBF80).copy(alpha = 0.34f),
                                                shape = RoundedCornerShape(22.dp)
                                            )
                                        } else Modifier
                                    )
                                    .clickable(
                                        interactionSource = calHeroInteraction,
                                        indication = androidx.compose.foundation.LocalIndication.current
                                    ) { onNavigateToScreen(Screen.Calendar.route) }
                                    .testTag("hero_calendar_card")
                            ) {
                                HeroCardLeafWatermark(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .size(width = 86.dp, height = 96.dp)
                                )
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(16.dp),
                                    verticalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(11.dp))
                                            .background(Color.White.copy(alpha = if (isDark) 0.18f else 0.24f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.CalendarMonth,
                                            contentDescription = "My Calendar",
                                            tint = Color.White,
                                            modifier = Modifier.size(19.dp)
                                        )
                                    }
                                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text(
                                            text = if (todayEventsCount > 0) "$todayEventsCount scheduled today" else "Schedules & Events",
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.5.sp),
                                            color = Color.White.copy(alpha = 0.90f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "My Calendar",
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 17.5.sp
                                            ),
                                            color = Color.White
                                        )
                                    }
                                }
                            }

                            // Amber Gold "Tasks" Hero Card
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(122.dp)
                                    .then(
                                        if (!isDark) {
                                            Modifier.shadow(
                                                elevation = 6.dp,
                                                shape = RoundedCornerShape(22.dp),
                                                ambientColor = Color(0xFFF59E0B).copy(alpha = 0.22f),
                                                spotColor = Color(0xFFF59E0B).copy(alpha = 0.18f)
                                            )
                                        } else Modifier
                                    )
                                    .springPressScale(tasksHeroInteraction)
                                    .clip(RoundedCornerShape(22.dp))
                                    .background(tasksHeroGradient)
                                    .then(
                                        if (isDark) {
                                            Modifier.border(
                                                width = 1.dp,
                                                color = Color(0xFFF5B749).copy(alpha = 0.32f),
                                                shape = RoundedCornerShape(22.dp)
                                            )
                                        } else Modifier
                                    )
                                    .clickable(
                                        interactionSource = tasksHeroInteraction,
                                        indication = androidx.compose.foundation.LocalIndication.current
                                    ) { onNavigateToScreen(Screen.Tasks.route) }
                                    .testTag("hero_tasks_card")
                            ) {
                                HeroCardLeafWatermark(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .size(width = 86.dp, height = 96.dp)
                                )
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(16.dp),
                                    verticalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(11.dp))
                                            .background(Color.White.copy(alpha = if (isDark) 0.18f else 0.24f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.CheckBox,
                                            contentDescription = "Tasks",
                                            tint = Color.White,
                                            modifier = Modifier.size(19.dp)
                                        )
                                    }
                                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text(
                                            text = if (tasksTotal > 0) "$tasksDone/$tasksTotal completed" else "Manage & Track",
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.5.sp),
                                            color = Color.White.copy(alpha = 0.90f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "Tasks & Track",
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 17.sp
                                            ),
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }

                        // Compact Task Completion Progress Bar Card
                        if (tasksTotal > 0) {
                            val rawRatio = (tasksDone.toFloat() / tasksTotal.toFloat()).coerceIn(0f, 1f)
                            val animatedRatio by animateFloatAsState(
                                targetValue = rawRatio,
                                animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
                                label = "home_tasks_progress"
                            )
                            val pct = (rawRatio * 100).toInt()
                            val progressInteraction = remember { MutableInteractionSource() }
                            Surface(
                                onClick = { onNavigateToScreen(Screen.Tasks.route) },
                                interactionSource = progressInteraction,
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
                                    .springPressScale(progressInteraction, pressedScale = 0.985f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Text(
                                            text = "Today's Tasks Progress",
                                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "$tasksDone of $tasksTotal tasks done",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.weight(0.95f)
                                    ) {
                                        LinearProgressIndicator(
                                            progress = { animatedRatio },
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(8.dp)
                                                .clip(RoundedCornerShape(4.dp)),
                                            color = homeAccent,
                                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                            strokeCap = StrokeCap.Round
                                        )
                                        Text(
                                            text = "$pct%",
                                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 3. Workspace Feature Grid (2 columns x 2 rows)
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 2.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Workspace",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = DisplayFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            ),
                            color = headerTextColor,
                            modifier = Modifier.padding(bottom = 2.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            FeatureCard(
                                title = "Diary",
                                subtitle = "Daily journal",
                                icon = Icons.Default.CalendarMonth,
                                iconTint = NotelandAmber,
                                onClick = {
                                    openOrCreateDailyJournal(displayNotes, viewModel, onNavigateToEditor)
                                },
                                modifier = Modifier.weight(1f)
                            )

                            FeatureCard(
                                title = "Board",
                                subtitle = "Freeform canvas",
                                icon = Icons.Default.AutoStories,
                                iconTint = NotelandBlue,
                                onClick = {
                                    onNavigateToScreen(Screen.Canvas.route)
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            FeatureCard(
                                title = "Sticky Wall",
                                subtitle = "Knowledge wall",
                                icon = Icons.Default.Layers,
                                iconTint = NotelandPurple,
                                onClick = {
                                    onNavigateToScreen(Screen.StickyBoard.route)
                                },
                                modifier = Modifier.weight(1f)
                            )

                            FeatureCard(
                                title = "Pinned Notes",
                                subtitle = if (pinnedNotes.isNotEmpty()) "${pinnedNotes.size} pinned" else "Quick pinned access",
                                icon = Icons.Default.PushPin,
                                iconTint = homeAccent,
                                onClick = {
                                    onNavigateToScreen(Screen.PinnedNotes.route)
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // 3b. Today's Tasks & Upcoming Reminders Agenda
                if (todayTasksPreview.isNotEmpty() || upcomingRemindersPreview.isNotEmpty()) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = editorialCardContainerColor(isDark),
                            border = BorderStroke(1.2.dp, editorialCardBorderColor(isDark)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("home_today_agenda_section")
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Today's Tasks & Upcoming Deadlines",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    TextButton(
                                        onClick = { onNavigateToScreen(Screen.Tasks.route) },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text("Tasks", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold))
                                    }
                                }

                                todayTasksPreview.forEach { task ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable { onNavigateToScreen(Screen.Tasks.route) }
                                            .padding(end = 6.dp)
                                    ) {
                                        IconButton(
                                            onClick = { viewModel.toggleTaskCompleted(task.id) },
                                            modifier = Modifier
                                                .size(48.dp)
                                                .testTag("home_task_toggle_${task.id}")
                                        ) {
                                            Icon(
                                                imageVector = if (task.isCompleted) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                                                contentDescription = "Complete ${task.title}",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = task.title.ifBlank { "Task" },
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    fontWeight = FontWeight.SemiBold,
                                                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                                                ),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            val dueLabel = (task.scheduledStartUtc ?: task.dueAtUtc)?.let {
                                                SmartNoteUtils.formatShortDate(it)
                                            } ?: task.category
                                            Text(
                                                text = "${TaskPriority.fromName(task.priority).label} · $dueLabel",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }

                                upcomingRemindersPreview.forEach { rem ->
                                    val linkedNote = allNotes.firstOrNull { it.id == rem.noteId }
                                    val titleText = when {
                                        linkedNote?.isLocked == true -> "Locked Note Reminder"
                                        linkedNote != null -> linkedNote.title.ifBlank { "Reminder" }
                                        else -> "Scheduled Reminder"
                                    }
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable { onNavigateToScreen(Screen.Calendar.route) }
                                            .padding(horizontal = 12.dp, vertical = 8.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Alarm,
                                            contentDescription = "Reminder",
                                            tint = Color(0xFFE11D48),
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = titleText,
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                            modifier = Modifier.weight(1f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = SmartNoteUtils.formatShortDate(rem.scheduledTimeUtc),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 4. Section Header: "Recent notes"
                item {
                    val hasActiveFilter = searchQuery.isNotEmpty() || selectedTag != null || selectedFolderId != null
                    val activeFolderPath = remember(folders, selectedFolderId) {
                        if (selectedFolderId != null) {
                            FolderHierarchyUtils.getFolderPathString(folders, selectedFolderId)
                        } else ""
                    }
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 2.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Recent notes",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontFamily = DisplayFontFamily,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = headerTextColor
                            )

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                IconButton(
                                    onClick = { viewModel.toggleGridView() },
                                    modifier = Modifier.size(48.dp).testTag("grid_list_toggle_button")
                                ) {
                                    Icon(
                                        imageVector = if (isGridView) Icons.AutoMirrored.Filled.ViewList else Icons.Default.GridView,
                                        contentDescription = if (isGridView) "Switch to List View" else "Switch to Grid View",
                                        tint = headerSubColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                TextButton(
                                    onClick = {
                                        if (hasActiveFilter) {
                                            viewModel.setSearchQuery("")
                                            viewModel.setSelectedTag(null)
                                            viewModel.setSelectedFolder(null)
                                        } else {
                                            viewModel.setSearchFilterType("ALL")
                                            onNavigateToScreen(Screen.Search.route)
                                        }
                                    },
                                    modifier = Modifier
                                        .heightIn(min = 48.dp)
                                        .testTag("view_all_button")
                                ) {
                                    Text(
                                        text = if (hasActiveFilter) "Clear Filter" else "View all",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 14.sp
                                        ),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }

                        if (activeFolderPath.isNotBlank() || selectedTag != null) {
                            @OptIn(ExperimentalLayoutApi::class)
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (activeFolderPath.isNotBlank()) {
                                    val folderChipAccent = MaterialTheme.colorScheme.primary
                                    Surface(
                                        shape = CircleShape,
                                        color = folderChipAccent.copy(alpha = 0.14f),
                                        border = BorderStroke(0.9.dp, folderChipAccent.copy(alpha = 0.55f)),
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .clickable { viewModel.setSelectedFolder(null) }
                                            .testTag("home_active_folder_chip")
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Folder,
                                                contentDescription = null,
                                                tint = folderChipAccent,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Text(
                                                text = activeFolderPath,
                                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                                color = folderChipAccent,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Icon(
                                                Icons.Default.Close,
                                                contentDescription = "Clear folder filter",
                                                tint = folderChipAccent,
                                                modifier = Modifier.size(13.dp)
                                            )
                                        }
                                    }
                                }

                                if (selectedTag != null) {
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .clickable { viewModel.setSelectedTag(null) }
                                            .testTag("home_active_tag_chip")
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                        ) {
                                            Text(
                                                text = "#${selectedTag}",
                                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                                color = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                            Icon(
                                                Icons.Default.Close,
                                                contentDescription = "Clear tag filter",
                                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                                modifier = Modifier.size(13.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 5. Recent Notes Cards (True Staggered Masonry Grid or Full-Width List Layout)
                if (displayNotes.isEmpty()) {
                    item {
                        EmptyStateView(
                            icon = Icons.AutoMirrored.Filled.StickyNote2,
                            title = "No Notes Found",
                            description = "Tap 'New note' above or select Diary, Board, Sticky Wall, or Tasks to start capturing your thoughts.",
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp)
                        )
                    }
                } else if (isGridView) {
                    item(key = "home_notes_masonry_grid") {
                        StaggeredTwoColumnNotesLayout(
                            horizontalSpacing = 12.dp,
                            verticalSpacing = 12.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            displayNotes.forEach { note ->
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
                                            val willArchive = !note.isArchived
                                            viewModel.toggleArchive(note)
                                            if (willArchive) {
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
                    lazyItems(
                        items = displayNotes,
                        key = { it.id }
                    ) { note ->
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
                                val willArchive = !note.isArchived
                                viewModel.toggleArchive(note)
                                if (willArchive) {
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
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                        )
                    }
                }
            }

            // Snackbar Host with Undo action
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 84.dp)
            )
        }

    // Create Folder Dialog
    if (showCreateFolderDialog) {
        AlertDialog(
            onDismissRequest = { showCreateFolderDialog = false },
            title = { Text("Create New Folder") },
            text = {
                OutlinedTextField(
                    value = newFolderName,
                    onValueChange = { newFolderName = it },
                    label = { Text("Folder Name") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newFolderName.isNotBlank()) {
                            viewModel.createFolder(newFolderName.trim(), "#3B82F6")
                            newFolderName = ""
                            showCreateFolderDialog = false
                        }
                    }
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateFolderDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showQuickCreateSheet) {
        com.example.ui.components.QuickCreateSheet(
            onDismiss = { showQuickCreateSheet = false },
            onSelectAction = { action ->
                showQuickCreateSheet = false
                when (action) {
                    QuickCreateAction.NOTE -> onNavigateToEditor("0", NoteType.STANDARD.name)
                    QuickCreateAction.TASK -> showQuickTaskDialog = true
                    QuickCreateAction.EVENT -> showQuickEventDialog = true
                    QuickCreateAction.MIND_MAP -> {
                        viewModel.createMindMapProject("Untitled Mind Map") { newId ->
                            onNavigateToCanvas(newId)
                        }
                    }
                    QuickCreateAction.CANVAS -> {
                        viewModel.createFreeformCanvasProject("Untitled") { newId ->
                            onNavigateToCanvas(newId)
                        }
                    }
                    QuickCreateAction.STICKY_WALL -> showQuickStickyDialog = true
                }
            }
        )
    }

    if (showQuickTaskDialog) {
        TaskEditorDialog(
            initialTask = null,
            initialColumnId = kanbanColumns.firstOrNull { !it.isDoneColumn }?.id ?: "col_todo",
            columns = kanbanColumns.sortedBy { it.position },
            allTasks = activeTasks,
            allDependencies = taskDependencies,
            eligibleNotes = allNotes.filter { !it.isLocked && !it.isHidden },
            onDismiss = { showQuickTaskDialog = false },
            onSave = { task, selectedDeps ->
                viewModel.saveTaskWithReminder(task, selectedDeps, false, null)
                showQuickTaskDialog = false
            },
            onSaveWithReminder = { task, selectedDeps, reminderEnabled, reminderTimeUtc ->
                viewModel.saveTaskWithReminder(task, selectedDeps, reminderEnabled, reminderTimeUtc)
                showQuickTaskDialog = false
            }
        )
    }

    if (showQuickEventDialog) {
        CalendarEventDialog(
            defaultDateUtc = SmartNoteUtils.startOfDayUtc(System.currentTimeMillis()),
            onDismiss = { showQuickEventDialog = false },
            onConfirm = { title, desc, startUtc, endUtc, cat, colorHex, repeat, subtasks ->
                viewModel.createCalendarEvent(
                    title = title,
                    description = desc,
                    startUtc = startUtc,
                    endUtc = endUtc,
                    category = cat,
                    colorHex = colorHex,
                    repeatInterval = repeat,
                    checklistItems = subtasks
                ) {
                    showQuickEventDialog = false
                }
            }
        )
    }

        quickStickyCreateDialogContent(
            showQuickStickyDialog = showQuickStickyDialog,
            onDismiss = { showQuickStickyDialog = false },
            viewModel = viewModel,
            onNavigateToSticky = { onNavigateToScreen(Screen.StickyBoard.route) }
        )
    }
}

@Composable
private fun quickStickyCreateDialogContent(
    showQuickStickyDialog: Boolean,
    onDismiss: () -> Unit,
    viewModel: NoteViewModel,
    onNavigateToSticky: () -> Unit
) {
    if (showQuickStickyDialog) {
        QuickStickyCreateDialog(
            onDismiss = onDismiss,
            onCreate = { title, body, colorHex ->
                val now = System.currentTimeMillis()
                viewModel.saveStickyNote(
                    StickyNoteEntity(
                        id = UUID.randomUUID().toString(),
                        title = title,
                        content = body,
                        colorHex = colorHex,
                        createdAtUtc = now,
                        updatedAtUtc = now
                    )
                )
                onDismiss()
                onNavigateToSticky()
            }
        )
    }
}

/**
 * Feature Card component representing one of the 6 quick-access tool modules.
 */
@Composable
fun FeatureCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color,
    badgeBg: Color = Color.Unspecified,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isAppInDarkTheme
    val resolvedBadgeBg = if (badgeBg != Color.Unspecified) {
        badgeBg
    } else {
        iconTint.copy(alpha = if (isDark) 0.20f else 0.13f)
    }
    val interactionSource = remember { MutableInteractionSource() }
    Surface(
        onClick = onClick,
        interactionSource = interactionSource,
        shape = RoundedCornerShape(18.dp),
        color = editorialCardContainerColor(isDark),
        border = BorderStroke(1.25.dp, editorialCardBorderColor(isDark)),
        shadowElevation = 0.dp,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 68.dp)
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
            .springPressScale(interactionSource)
            .testTag("feature_card_${title.lowercase()}")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = resolvedBadgeBg,
                border = if (isDark) BorderStroke(0.6.dp, iconTint.copy(alpha = 0.28f)) else null,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = iconTint,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.5.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
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
        }
    }
}

/**
 * Helper to open an existing diary entry for today or create a new one.
 */
internal fun openOrCreateDailyJournal(
    notes: List<NoteEntity>,
    viewModel: NoteViewModel,
    onNavigateToEditor: (String, String) -> Unit
) {
    viewModel.openOrCreateDailyDiary(onNavigateToEditor)
}
