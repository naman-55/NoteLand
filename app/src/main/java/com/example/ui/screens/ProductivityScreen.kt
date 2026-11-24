package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material.icons.filled.ViewKanban
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FocusSessionType
import com.example.ui.components.ProductivityHeaderIllustration
import com.example.ui.components.StaggeredSectionEntrance
import com.example.ui.components.springPressScale
import com.example.ui.theme.AppSection
import com.example.ui.theme.DarkProductivityAccent
import com.example.ui.theme.DisplayFontFamily
import com.example.ui.theme.LightProductivityAccent
import com.example.ui.theme.ProvideSectionTheme
import com.example.ui.theme.ScreenBackdropTone
import com.example.ui.theme.editorialBackdropBrush
import com.example.ui.theme.editorialCardBorderColor
import com.example.ui.theme.editorialCardContainerColor
import com.example.ui.theme.editorialHeaderSubColor
import com.example.ui.theme.editorialHeaderTextColor
import com.example.ui.theme.isAppInDarkTheme
import com.example.ui.viewmodel.NoteViewModel

private data class ProductivityTabMeta(
    val index: Int,
    val label: String,
    val icon: ImageVector,
    val testTag: String
)

private val ProductivityTabs = listOf(
    ProductivityTabMeta(0, "Kanban", Icons.Default.ViewKanban, "productivity_tab_kanban"),
    ProductivityTabMeta(1, "Habits", Icons.Default.TrackChanges, "productivity_tab_habits"),
    ProductivityTabMeta(2, "Focus", Icons.Default.Timer, "productivity_tab_focus"),
    ProductivityTabMeta(3, "Analytics", Icons.Default.Analytics, "productivity_tab_reports")
)

@Composable
fun ProductivityScreen(
    viewModel: NoteViewModel,
    initialTab: Int = 0,
    isRootTab: Boolean = false,
    onNavigateToCalendar: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val isDark = isAppInDarkTheme
    var selectedTab by remember(initialTab) { mutableIntStateOf(initialTab.coerceIn(0, ProductivityTabs.lastIndex)) }
    var triggerNewTask by remember { mutableStateOf(false) }
    var triggerNewHabit by remember { mutableStateOf(false) }

    val headerTextColor = editorialHeaderTextColor(isDark)
    val headerSubColor = editorialHeaderSubColor(isDark)
    val prodAccent = if (isDark) DarkProductivityAccent else LightProductivityAccent
    val navBottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    ProvideSectionTheme(section = AppSection.PRODUCTIVITY, isDark = isDark) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(editorialBackdropBrush(isDark, ScreenBackdropTone.PRODUCTIVITY_MINT))
            .testTag("productivity_screen_root")
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0, 0, 0, 0)
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .statusBarsPadding()
            ) {
                // Hero Header with Hanging Lamp / Clock / Plant Vector Artwork
                StaggeredSectionEntrance(index = 0) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 6.dp)
                ) {
                    ProductivityHeaderIllustration(
                        isDark = isDark,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(end = 6.dp, top = 24.dp)
                    )

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.End
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = editorialCardContainerColor(isDark),
                                    border = BorderStroke(1.2.dp, editorialCardBorderColor(isDark)),
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .clickable { /* TODO */ }
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

                                Surface(
                                    shape = CircleShape,
                                    color = editorialCardContainerColor(isDark),
                                    border = BorderStroke(1.2.dp, editorialCardBorderColor(isDark)),
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .clickable { /* TODO */ }
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Default.Notifications,
                                            contentDescription = "Notifications",
                                            tint = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        // Red dot badge
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(top = 10.dp, end = 12.dp)
                                                .size(8.dp)
                                                .background(Color.Red, CircleShape)
                                        )
                                    }
                                }
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "Productivity",
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontFamily = DisplayFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 29.sp
                                ),
                                color = headerTextColor
                            )
                            Text(
                                text = "Plan · Organize · Focus · Achieve",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp),
                                color = headerSubColor
                            )
                        }
                    }
                }
                }

                // Inspirational Quote Banner
                StaggeredSectionEntrance(index = 1) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = editorialCardContainerColor(isDark),
                    border = BorderStroke(1.2.dp, editorialCardBorderColor(isDark)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
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
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FormatQuote,
                            contentDescription = null,
                            tint = prodAccent,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = "Small steps every day lead to big results.",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.5.sp,
                                lineHeight = 18.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        val chartPulseTransition = rememberInfiniteTransition(label = "quote_mini_chart")
                        val barScale by chartPulseTransition.animateFloat(
                            initialValue = 0.85f,
                            targetValue = 1.08f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1900, easing = FastOutSlowInEasing),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "barScale"
                        )
                        val chartTint = prodAccent
                        val chartInteraction = remember { MutableInteractionSource() }
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isDark) Color(0xFF193221) else prodAccent.copy(alpha = 0.14f),
                            modifier = Modifier
                                .size(36.dp)
                                .springPressScale(chartInteraction, pressedScale = 0.92f)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable(
                                    interactionSource = chartInteraction,
                                    indication = LocalIndication.current
                                ) { selectedTab = 3 }
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(7.dp)) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    val w = size.width
                                    val h = size.height
                                    val barW = w * 0.22f
                                    val heights = listOf(0.45f * barScale, 0.72f, 0.95f * (2f - barScale))
                                    val xs = listOf(w * 0.08f, w * 0.39f, w * 0.70f)
                                    heights.forEachIndexed { idx, frac ->
                                        val bh = (h * frac.coerceIn(0.30f, 1.0f))
                                        drawRoundRect(
                                            color = chartTint,
                                            topLeft = Offset(xs[idx], h - bh),
                                            size = Size(barW, bh),
                                            cornerRadius = CornerRadius(barW / 2f, barW / 2f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                }

                // Card-Tile Navigation across all 6 Productivity tools
                StaggeredSectionEntrance(index = 2) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ProductivityTabs.forEach { tab ->
                        val active = selectedTab == tab.index
                        val tabInteraction = remember { MutableInteractionSource() }
                        val containerColor by animateColorAsState(
                            targetValue = when {
                                active && isDark -> Color(0xFF287547)
                                active -> LightProductivityAccent
                                else -> editorialCardContainerColor(isDark)
                            },
                            animationSpec = tween(220),
                            label = "prodTabBg_${tab.index}"
                        )
                        val contentColor by animateColorAsState(
                            targetValue = when {
                                active -> Color.White
                                else -> MaterialTheme.colorScheme.onSurface
                            },
                            animationSpec = tween(220),
                            label = "prodTabFg_${tab.index}"
                        )
                        val iconTint by animateColorAsState(
                            targetValue = when {
                                active -> Color.White
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            animationSpec = tween(220),
                            label = "prodTabIcon_${tab.index}"
                        )
                        val tabScale by animateFloatAsState(
                            targetValue = if (active) 1.03f else 1f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMedium
                            ),
                            label = "prodTabScale_${tab.index}"
                        )
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = containerColor,
                            border = when {
                                active && isDark -> BorderStroke(1.dp, DarkProductivityAccent.copy(alpha = 0.65f))
                                active -> null
                                else -> BorderStroke(1.2.dp, editorialCardBorderColor(isDark))
                            },
                            modifier = Modifier
                                .widthIn(min = 76.dp)
                                .graphicsLayer {
                                    scaleX = tabScale
                                    scaleY = tabScale
                                }
                                .then(
                                    if (!isDark) {
                                        Modifier.shadow(
                                            elevation = if (active) 5.dp else 3.dp,
                                            shape = RoundedCornerShape(16.dp),
                                            ambientColor = Color(0xFF15865D).copy(alpha = if (active) 0.20f else 0.08f),
                                            spotColor = Color.Black.copy(alpha = 0.06f)
                                        )
                                    } else Modifier
                                )
                                .springPressScale(tabInteraction, pressedScale = 0.94f)
                                .clip(RoundedCornerShape(16.dp))
                                .clickable(
                                    interactionSource = tabInteraction,
                                    indication = LocalIndication.current
                                ) { selectedTab = tab.index }
                                .testTag(tab.testTag)
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                            ) {
                                Icon(
                                    imageVector = tab.icon,
                                    contentDescription = tab.label,
                                    tint = iconTint,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = tab.label,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = contentColor
                                )
                            }
                        }
                    }
                }
                }

                Spacer(Modifier.height(4.dp))

                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    AnimatedContent(
                        targetState = selectedTab,
                        transitionSpec = {
                            val forward = targetState >= initialState
                            val dir = if (forward) 1 else -1
                            (slideInHorizontally(
                                animationSpec = tween(220, easing = FastOutSlowInEasing),
                                initialOffsetX = { fullWidth -> (fullWidth * 0.08f).toInt() * dir }
                            ) + fadeIn(animationSpec = tween(200))) togetherWith
                                (slideOutHorizontally(
                                    animationSpec = tween(170, easing = FastOutSlowInEasing),
                                    targetOffsetX = { fullWidth -> -(fullWidth * 0.05f).toInt() * dir }
                                ) + fadeOut(animationSpec = tween(150)))
                        },
                        label = "productivity_tab_content"
                    ) { activeTab ->
                        when (activeTab) {
                            0 -> KanbanBoardTab(
                                viewModel = viewModel,
                                onStartFocusForTask = { task ->
                                    val sec = (task.estimatedMinutes.coerceIn(5, 90)) * 60
                                    viewModel.setPomodoroLinks(task.id, task.linkedNoteId)
                                    viewModel.startPomodoro(
                                        type = FocusSessionType.WORK,
                                        durationSec = sec,
                                        linkedTaskId = task.id,
                                        linkedNoteId = task.linkedNoteId
                                    )
                                    selectedTab = 2
                                },
                                onOpenTaskEditorRequested = triggerNewTask,
                                onClearExternalTaskRequest = { triggerNewTask = false }
                            )
                            1 -> HabitsTab(
                                viewModel = viewModel,
                                onOpenHabitDialogRequested = triggerNewHabit,
                                onClearExternalHabitRequest = { triggerNewHabit = false }
                            )
                            2 -> PomodoroFocusTab(viewModel = viewModel)
                            3 -> ProgressReportsTab(viewModel = viewModel)
                            4 -> PlannerTab(
                                viewModel = viewModel,
                                onNavigateToCalendar = onNavigateToCalendar,
                                onRequestCreateTask = {
                                    selectedTab = 0
                                    triggerNewTask = true
                                }
                            )
                            else -> GoalsTab(viewModel = viewModel)
                        }
                    }
                }
            }
        }
    }
    }
}
