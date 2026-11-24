package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.ViewColumn
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.KanbanColumnEntity
import com.example.data.model.NoteEntity
import com.example.data.model.TaskDependencyEntity
import com.example.data.model.TaskEntity
import com.example.data.model.TaskPriority
import com.example.data.model.TaskRecurrenceRule
import com.example.ui.components.EmptyStateView
import com.example.ui.components.KanbanColumnManagerDialog
import com.example.ui.components.KanbanProgressEmptyIllustration
import com.example.ui.components.KanbanTodoEmptyIllustration
import com.example.ui.components.TaskEditorDialog
import com.example.ui.components.parseHexColorSafe
import com.example.ui.components.springPressScale
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import com.example.ui.theme.DarkProductivityAccent
import com.example.ui.theme.LightProductivityAccent
import com.example.ui.theme.NotelandEmerald
import com.example.ui.theme.editorialCardBorderColor
import com.example.ui.theme.editorialCardContainerColor
import com.example.ui.theme.isAppInDarkTheme
import com.example.ui.viewmodel.NoteViewModel
import com.example.util.ProductivityEngine
import com.example.util.SmartNoteUtils

@Composable
fun KanbanBoardTab(
    viewModel: NoteViewModel,
    onStartFocusForTask: (TaskEntity) -> Unit,
    onOpenTaskEditorRequested: Boolean = false,
    onClearExternalTaskRequest: () -> Unit = {}
) {
    val context = LocalContext.current
    val columns by viewModel.kanbanColumns.collectAsStateWithLifecycle()
    val tasks by viewModel.activeTasks.collectAsStateWithLifecycle()
    val dependencies by viewModel.taskDependencies.collectAsStateWithLifecycle()
    val notes by viewModel.allActiveNotes.collectAsStateWithLifecycle()
    val isDark = isAppInDarkTheme

    val currentBoard by viewModel.currentKanbanBoard.collectAsStateWithLifecycle()
    val allBoards by viewModel.kanbanBoards.collectAsStateWithLifecycle()
    
    var showBoardSelector by remember { mutableStateOf(false) }
    var showCreateBoardDialog by remember { mutableStateOf(false) }
    var newBoardName by remember { mutableStateOf("") }

    var editingTask by remember { mutableStateOf<TaskEntity?>(null) }
    var showCreateTaskDialog by remember { mutableStateOf(false) }
    var targetNewTaskColumnId by remember { mutableStateOf("col_todo") }
    var showColumnManager by remember { mutableStateOf(false) }
    var selectedPriorityFilter by remember { mutableStateOf<TaskPriority?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var isSearchExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(onOpenTaskEditorRequested) {
        if (onOpenTaskEditorRequested && !showCreateTaskDialog) {
            editingTask = null
            targetNewTaskColumnId = columns.firstOrNull { !it.isDoneColumn }?.id ?: columns.firstOrNull()?.id ?: "col_todo"
            showCreateTaskDialog = true
            onClearExternalTaskRequest()
        }
    }

    val sortedColumns = remember(columns) { columns.sortedBy { it.position } }
    val tasksById = remember(tasks) { tasks.associateBy { it.id } }
    val notesById = remember(notes) { notes.associateBy { it.id } }

    // Drag & Drop state tracking in root coordinates
    val columnBounds = remember { mutableStateMapOf<String, Rect>() }
    var draggingTask by remember { mutableStateOf<TaskEntity?>(null) }
    var dragPointerRoot by remember { mutableStateOf(Offset.Zero) }
    var dragOriginOffset by remember { mutableStateOf(Offset.Zero) }
    val hoveredColumnId = remember(dragPointerRoot, draggingTask) {
        if (draggingTask == null) null
        else columnBounds.entries.firstOrNull { (_, rect) -> rect.contains(dragPointerRoot) }?.key
    }

    if (showCreateBoardDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showCreateBoardDialog = false },
            title = { androidx.compose.material3.Text("Create New Board") },
            text = {
                androidx.compose.material3.OutlinedTextField(
                    value = newBoardName,
                    onValueChange = { newBoardName = it },
                    label = { androidx.compose.material3.Text("Board Name") },
                    singleLine = true
                )
            },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    onClick = {
                        if (newBoardName.isNotBlank()) {
                            viewModel.addKanbanBoard(newBoardName.trim())
                            showCreateBoardDialog = false
                            newBoardName = ""
                        }
                    }
                ) {
                    androidx.compose.material3.Text("Create")
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showCreateBoardDialog = false }) {
                    androidx.compose.material3.Text("Cancel")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("kanban_board_tab")
    ) {
        // Controls & Priority Filter Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val boardBtnInteraction = remember { MutableInteractionSource() }
            Surface(
                shape = CircleShape,
                color = editorialCardContainerColor(isDark),
                border = BorderStroke(0.8.dp, editorialCardBorderColor(isDark)),
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .then(
                        if (!isDark) {
                            Modifier.shadow(
                                elevation = 3.dp,
                                shape = CircleShape,
                                ambientColor = Color(0xFF15865D).copy(alpha = 0.07f),
                                spotColor = Color.Black.copy(alpha = 0.05f)
                            )
                        } else Modifier
                    )
                    .springPressScale(boardBtnInteraction)
                    .clip(CircleShape)
                    .clickable(
                        interactionSource = boardBtnInteraction,
                        indication = LocalIndication.current
                    ) { showBoardSelector = true }
            ) {
                Box {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(horizontal = 13.dp, vertical = 10.dp)
                    ) {
                        Icon(
                            Icons.Default.ViewColumn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = currentBoard,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Icon(
                            Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    
                    DropdownMenu(
                        expanded = showBoardSelector,
                        onDismissRequest = { showBoardSelector = false }
                    ) {
                        allBoards.forEach { board ->
                            androidx.compose.material3.DropdownMenuItem(
                                text = { Text(board) },
                                onClick = {
                                    viewModel.setCurrentKanbanBoard(board)
                                    showBoardSelector = false
                                }
                            )
                        }
                        androidx.compose.material3.HorizontalDivider()
                        androidx.compose.material3.DropdownMenuItem(
                            text = { Text("Create New Board...", color = MaterialTheme.colorScheme.primary) },
                            onClick = {
                                showBoardSelector = false
                                showCreateBoardDialog = true
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            if (selectedPriorityFilter != null) {
                TextButton(onClick = { selectedPriorityFilter = null }) {
                    Text("Filter: ${selectedPriorityFilter?.label} ✕", fontSize = 12.sp)
                }
            } else {
                TaskPriority.entries.filter { it == TaskPriority.HIGH || it == TaskPriority.URGENT }.forEach { p ->
                    val accent = parseHexColorSafe(p.colorHex)
                    Surface(
                        shape = CircleShape,
                        color = accent.copy(alpha = if (isDark) 0.22f else 0.14f),
                        border = BorderStroke(0.7.dp, accent.copy(alpha = 0.30f)),
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable { selectedPriorityFilter = p }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = null,
                                tint = accent,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = p.label,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = accent
                            )
                        }
                    }
                }
            }

            Surface(
                shape = CircleShape,
                color = editorialCardContainerColor(isDark),
                border = BorderStroke(0.8.dp, editorialCardBorderColor(isDark)),
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .clickable { isSearchExpanded = !isSearchExpanded }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = "Search tasks",
                        tint = if (searchQuery.isNotBlank() || isSearchExpanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Surface(
                shape = CircleShape,
                color = editorialCardContainerColor(isDark),
                border = BorderStroke(0.8.dp, editorialCardBorderColor(isDark)),
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .clickable { showColumnManager = true }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = "Configure columns",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        if (isSearchExpanded) {
            androidx.compose.material3.OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Filter cards by title or description...") },
                singleLine = true,
                leadingIcon = {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Clear search",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }

        // Active Drag Indicator Banner
        draggingTask?.let { dragged ->
            val targetTitle = sortedColumns.firstOrNull { it.id == hoveredColumnId }?.title ?: "column"
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "Dragging '${dragged.title}' → Drop onto $targetTitle",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        }

        if (sortedColumns.isEmpty()) {
            EmptyStateView(
                icon = Icons.Default.ViewColumn,
                title = "No Kanban Columns",
                description = "Add your first Kanban column to organize tasks and deadlines.",
                actionText = "Manage Columns",
                onAction = { showColumnManager = true }
            )
        } else {
            val navBottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
            LazyRow(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 112.dp + navBottomPadding),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                itemsIndexed(sortedColumns, key = { _, col -> col.id }) { colIndex, column ->
                    val colTasks = remember(tasks, column.id, selectedPriorityFilter, searchQuery) {
                        tasks.filter { task ->
                            task.columnId == column.id &&
                                (selectedPriorityFilter == null || task.priority == selectedPriorityFilter?.name) &&
                                (searchQuery.isBlank() || task.title.contains(searchQuery, ignoreCase = true) || task.description.contains(searchQuery, ignoreCase = true))
                        }.sortedWith(compareBy<TaskEntity> { it.orderInColumn }.thenByDescending { it.updatedAtUtc })
                    }
                    val colAccent = parseHexColorSafe(column.colorHex)
                    val isDropTarget = hoveredColumnId == column.id && draggingTask?.columnId != column.id
                    val isOverWip = column.wipLimit > 0 && colTasks.size > column.wipLimit

                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = editorialCardContainerColor(isDark),
                        border = BorderStroke(
                            width = if (isDropTarget) 2.2.dp else 0.9.dp,
                            color = when {
                                isDropTarget -> colAccent
                                isOverWip -> MaterialTheme.colorScheme.error
                                else -> editorialCardBorderColor(isDark)
                            }
                        ),
                        modifier = Modifier
                            .width(292.dp)
                            .fillMaxHeight()
                            .then(
                                if (!isDark) {
                                    Modifier.shadow(
                                        elevation = 5.dp,
                                        shape = RoundedCornerShape(22.dp),
                                        ambientColor = Color(0xFF15865D).copy(alpha = 0.08f),
                                        spotColor = Color.Black.copy(alpha = 0.07f)
                                    )
                                } else Modifier
                            )
                            .onGloballyPositioned { coords ->
                                columnBounds[column.id] = coords.boundsInRoot()
                            }
                            .testTag("kanban_column_${column.id}")
                    ) {
                        Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                            // Column Header
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(colAccent)
                                    )
                                    Text(
                                        text = column.title,
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Surface(
                                        shape = CircleShape,
                                        color = if (isOverWip) MaterialTheme.colorScheme.errorContainer else colAccent.copy(alpha = 0.14f)
                                    ) {
                                        val countLabel = if (column.wipLimit > 0) {
                                            "${colTasks.size}/${column.wipLimit}"
                                        } else {
                                            "${colTasks.size}"
                                        }
                                        Text(
                                            text = countLabel,
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = if (isOverWip) MaterialTheme.colorScheme.error else colAccent,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = {
                                        editingTask = null
                                        targetNewTaskColumnId = column.id
                                        showCreateTaskDialog = true
                                    },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .testTag("kanban_add_task_${column.id}")
                                ) {
                                    Icon(
                                        Icons.Default.Add,
                                        contentDescription = "Add task to ${column.title}",
                                        tint = colAccent
                                    )
                                }
                            }

                            Spacer(Modifier.height(6.dp))

                            if (colTasks.isEmpty()) {
                                val isProgressCol = column.id.contains("progress", ignoreCase = true) ||
                                    column.title.contains("Progress", ignoreCase = true)
                                val pillAccent = when {
                                    isDark && isProgressCol -> Color(0xFFF5B749)
                                    isDark -> DarkProductivityAccent
                                    isProgressCol -> Color(0xFFD97706)
                                    else -> LightProductivityAccent
                                }
                                val pillContainer = when {
                                    isDark && isProgressCol -> Color(0xFF332713)
                                    isDark -> Color(0xFF1E3220)
                                    else -> pillAccent.copy(alpha = 0.16f)
                                }
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f)
                                        .padding(horizontal = 10.dp, vertical = 14.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    if (isProgressCol) {
                                        KanbanProgressEmptyIllustration(isDark = isDark)
                                    } else {
                                        KanbanTodoEmptyIllustration(isDark = isDark)
                                    }
                                    Spacer(Modifier.height(10.dp))
                                    Text(
                                        text = if (isProgressCol) {
                                            "No tasks in progress"
                                        } else {
                                            "Your ${column.title} list is empty"
                                        },
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        ),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = if (isProgressCol) {
                                            "Move a task here to track your progress."
                                        } else {
                                            "Add a task to get started and stay productive."
                                        },
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(Modifier.height(14.dp))
                                    val addBtnInteraction = remember { MutableInteractionSource() }
                                    Surface(
                                        shape = CircleShape,
                                        color = pillContainer,
                                        border = BorderStroke(0.85.dp, pillAccent.copy(alpha = if (isDark) 0.36f else 0.28f)),
                                        modifier = Modifier
                                            .springPressScale(addBtnInteraction, pressedScale = 0.94f)
                                            .clip(CircleShape)
                                            .clickable(
                                                interactionSource = addBtnInteraction,
                                                indication = LocalIndication.current
                                            ) {
                                                editingTask = null
                                                targetNewTaskColumnId = column.id
                                                showCreateTaskDialog = true
                                            }
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Add,
                                                contentDescription = null,
                                                tint = pillAccent,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text(
                                                text = "Add Task",
                                                style = MaterialTheme.typography.labelLarge.copy(
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontSize = 13.sp
                                                ),
                                                color = pillAccent
                                            )
                                        }
                                    }
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(10.dp),
                                    contentPadding = PaddingValues(bottom = 8.dp)
                                ) {
                                    items(colTasks, key = { it.id }) { task ->
                                        val taskIndex = colTasks.indexOfFirst { it.id == task.id }
                                        var cardRootTopLeft by remember { mutableStateOf(Offset.Zero) }
                                        KanbanTaskCard(
                                            task = task,
                                            columns = sortedColumns,
                                            currentColIndex = colIndex,
                                            canMoveUp = taskIndex > 0,
                                            canMoveDown = taskIndex < colTasks.lastIndex,
                                            blockingCount = ProductivityEngine.getBlockingTaskIds(
                                                task.id,
                                                dependencies,
                                                tasksById
                                            ).size,
                                            linkedNote = task.linkedNoteId?.let { notesById[it] },
                                            isDraggingThis = draggingTask?.id == task.id,
                                            modifier = Modifier
                                                .onGloballyPositioned { coords ->
                                                    cardRootTopLeft = coords.boundsInRoot().topLeft
                                                }
                                                .pointerInput(task.id, sortedColumns) {
                                                    detectDragGesturesAfterLongPress(
                                                        onDragStart = { startOffset ->
                                                            draggingTask = task
                                                            dragOriginOffset = cardRootTopLeft
                                                            dragPointerRoot = cardRootTopLeft + startOffset
                                                        },
                                                        onDrag = { change, dragAmount ->
                                                            change.consume()
                                                            dragPointerRoot += dragAmount
                                                        },
                                                        onDragEnd = {
                                                            val droppedCol = columnBounds.entries
                                                                .firstOrNull { (_, r) -> r.contains(dragPointerRoot) }
                                                                ?.key
                                                            if (droppedCol != null && droppedCol != task.columnId) {
                                                                viewModel.moveTaskToColumn(task.id, droppedCol)
                                                            }
                                                            draggingTask = null
                                                        },
                                                        onDragCancel = {
                                                            draggingTask = null
                                                        }
                                                    )
                                                },
                                            onToggleDone = { viewModel.toggleTaskCompleted(task.id) },
                                            onEdit = {
                                                editingTask = task
                                                showCreateTaskDialog = true
                                            },
                                            onMoveToColumn = { targetId ->
                                                viewModel.moveTaskToColumn(task.id, targetId)
                                            },
                                            onReorder = { delta ->
                                                viewModel.reorderTaskInColumn(task.id, delta)
                                            },
                                            onStartFocus = { onStartFocusForTask(task) }
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

    if (showCreateTaskDialog) {
        TaskEditorDialog(
            initialTask = editingTask,
            initialColumnId = targetNewTaskColumnId,
            columns = sortedColumns,
            allTasks = tasks,
            allDependencies = dependencies,
            eligibleNotes = notes,
            onDismiss = {
                showCreateTaskDialog = false
                editingTask = null
            },
            onSave = { task, selectedDeps ->
                viewModel.saveTaskWithReminder(task, selectedDeps, false, null)
                showCreateTaskDialog = false
                editingTask = null
            },
            onSaveWithReminder = { task, selectedDeps, reminderEnabled, reminderTimeUtc ->
                viewModel.saveTaskWithReminder(task, selectedDeps, reminderEnabled, reminderTimeUtc)
                showCreateTaskDialog = false
                editingTask = null
            },
            onDelete = { target ->
                viewModel.deleteTask(target.id)
                showCreateTaskDialog = false
                editingTask = null
            }
        )
    }

    if (showColumnManager) {
        KanbanColumnManagerDialog(
            columns = sortedColumns,
            onDismiss = { showColumnManager = false },
            onSaveColumn = { col -> viewModel.saveKanbanColumn(col.copy(boardId = currentBoard)) },
            onReorderColumn = { colId, delta -> viewModel.reorderKanbanColumn(colId, delta) },
            onDeleteColumn = { colId ->
                viewModel.deleteKanbanColumn(colId) { res ->
                    res.onFailure { err ->
                        Toast.makeText(context, err.message ?: "Cannot delete column", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }
}

@Composable
private fun KanbanTaskCard(
    task: TaskEntity,
    columns: List<KanbanColumnEntity>,
    currentColIndex: Int,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    blockingCount: Int,
    linkedNote: NoteEntity?,
    isDraggingThis: Boolean,
    modifier: Modifier = Modifier,
    onToggleDone: () -> Unit,
    onEdit: () -> Unit,
    onMoveToColumn: (String) -> Unit,
    onReorder: (Int) -> Unit,
    onStartFocus: () -> Unit
) {
    val priority = TaskPriority.fromName(task.priority)
    val priorityColor = parseHexColorSafe(priority.colorHex)
    val subtasks = remember(task.subtasksJson) { SmartNoteUtils.parseChecklistJson(task.subtasksJson) }
    val subtasksDone = subtasks.count { it.isDone }
    val recurrence = TaskRecurrenceRule.fromName(task.recurrenceRule)
    var showMenu by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDraggingThis) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
            } else {
                MaterialTheme.colorScheme.surfaceContainerLow
            }
        ),
        border = BorderStroke(0.8.dp, priorityColor.copy(alpha = 0.28f)),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isDraggingThis) 6.dp else 0.dp),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onEdit)
            .testTag("kanban_task_card_${task.id}")
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                IconButton(
                    onClick = onToggleDone,
                    modifier = Modifier.size(36.dp).testTag("kanban_toggle_done_${task.id}")
                ) {
                    Icon(
                        imageVector = if (task.isCompleted) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                        contentDescription = if (task.isCompleted) "Mark incomplete" else "Mark completed",
                        tint = priorityColor
                    )
                }

                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(36.dp).testTag("kanban_task_menu_${task.id}")
                    ) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Task actions", modifier = Modifier.size(18.dp))
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("Edit Details") },
                            onClick = {
                                showMenu = false
                                onEdit()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Start Focus Session") },
                            onClick = {
                                showMenu = false
                                onStartFocus()
                            }
                        )
                        columns.forEach { targetCol ->
                            if (targetCol.id != task.columnId) {
                                DropdownMenuItem(
                                    text = { Text("Move to ${targetCol.title}") },
                                    onClick = {
                                        showMenu = false
                                        onMoveToColumn(targetCol.id)
                                    }
                                )
                            }
                        }
                    }
                }
            }

            if (task.description.isNotBlank()) {
                Text(
                    text = task.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Metadata Badges
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(shape = CircleShape, color = priorityColor.copy(alpha = 0.14f)) {
                    Text(
                        text = priority.label,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = priorityColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                if (task.dueAtUtc != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(Icons.Default.Event, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.primary)
                        Text(
                            text = SmartNoteUtils.formatShortDate(task.dueAtUtc),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (recurrence != TaskRecurrenceRule.NONE) {
                    Icon(
                        Icons.Default.Repeat,
                        contentDescription = "Recurring ${recurrence.label}",
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(14.dp)
                    )
                }

                if (blockingCount > 0) {
                    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.errorContainer) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = "Blocked", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(11.dp))
                            Text("Blocked ($blockingCount)", fontSize = 10.sp, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            if (linkedNote != null) {
                val safeTitle = ProductivityEngine.safeTaskLinkedNoteTitle(linkedNote)
                Text(
                    text = "Note: $safeTitle",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (subtasks.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Subtasks $subtasksDone/${subtasks.size}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    LinearProgressIndicator(
                        progress = { subtasksDone.toFloat() / subtasks.size.toFloat() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = priorityColor,
                        strokeCap = StrokeCap.Round
                    )
                }
            }

            // Accessible Move & Focus Controls (>= 48dp hit target row / explicit buttons)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    if (currentColIndex > 0) {
                        val leftCol = columns[currentColIndex - 1]
                        IconButton(
                            onClick = { onMoveToColumn(leftCol.id) },
                            modifier = Modifier.size(40.dp).testTag("kanban_move_left_${task.id}")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Move to ${leftCol.title}", modifier = Modifier.size(16.dp))
                        }
                    }
                    if (currentColIndex < columns.lastIndex) {
                        val rightCol = columns[currentColIndex + 1]
                        IconButton(
                            onClick = { onMoveToColumn(rightCol.id) },
                            modifier = Modifier.size(40.dp).testTag("kanban_move_right_${task.id}")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Move to ${rightCol.title}", modifier = Modifier.size(16.dp))
                        }
                    }
                    if (canMoveUp) {
                        IconButton(
                            onClick = { onReorder(-1) },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(Icons.Default.ArrowUpward, contentDescription = "Move task up", modifier = Modifier.size(16.dp))
                        }
                    }
                    if (canMoveDown) {
                        IconButton(
                            onClick = { onReorder(1) },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(Icons.Default.ArrowDownward, contentDescription = "Move task down", modifier = Modifier.size(16.dp))
                        }
                    }
                }

                IconButton(
                    onClick = onStartFocus,
                    modifier = Modifier.size(40.dp).testTag("kanban_focus_${task.id}")
                ) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = "Start focus timer on ${task.title}",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
