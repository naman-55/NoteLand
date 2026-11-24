package com.example.ui.screens

import android.Manifest
import android.app.Activity
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.NoteEntity
import com.example.data.model.ReminderEntity
import com.example.ui.components.EmptyStateView
import com.example.ui.theme.editorialBackdropBrush
import com.example.ui.theme.editorialCardBorderColor
import com.example.ui.theme.editorialCardContainerColor
import com.example.ui.theme.editorialHeaderTextColor
import com.example.ui.theme.isAppInDarkTheme
import com.example.ui.viewmodel.NoteViewModel
import com.example.util.SmartNoteUtils
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemindersScreen(
    viewModel: NoteViewModel,
    onNavigateToEditor: (String, String) -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val allReminders by viewModel.allReminders.collectAsStateWithLifecycle()
    val allNotes by viewModel.allActiveNotes.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Upcoming, 1: Completed, 2: All
    var showCreateDialog by remember { mutableStateOf(false) }
    var reschedulingReminder by remember { mutableStateOf<ReminderEntity?>(null) }

    // Android 13+ Notification permission check
    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasNotificationPermission = isGranted
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // Filter reminders (keep pending & overdue tasks visible until completed)
    val upcomingReminders = remember(allReminders) {
        allReminders.filter { !it.isCompleted }.sortedBy { it.scheduledTimeUtc }
    }
    val completedReminders = remember(allReminders) {
        allReminders.filter { it.isCompleted }.sortedByDescending { it.scheduledTimeUtc }
    }
    val displayReminders = when (selectedTab) {
        0 -> upcomingReminders
        1 -> completedReminders
        else -> allReminders.sortedBy { it.scheduledTimeUtc }
    }

    val completedCount = allReminders.count { it.isCompleted }
    val totalCount = allReminders.size
    val progress = if (totalCount > 0) completedCount.toFloat() / totalCount.toFloat() else 0f

    val isDark = isAppInDarkTheme
    val headerTextColor = editorialHeaderTextColor(isDark)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(editorialBackdropBrush(isDark))
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            "Reminders & Tasks",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = headerTextColor
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("reminders_back_button")) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = headerTextColor)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = Color.Transparent
                    )
                )
            },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("create_reminder_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "New Reminder")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            // Permission Banner if missing
            if (!hasNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFD97706).copy(alpha = 0.14f)),
                    border = BorderStroke(0.8.dp, Color(0xFFD97706).copy(alpha = 0.45f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.NotificationsOff, contentDescription = null, tint = Color(0xFFD97706))
                            Text(
                                "Allow notifications to receive timely alerts when reminders trigger.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        TextButton(onClick = { permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }) {
                            Text("Enable", color = Color(0xFFD97706), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Overview Progress Card
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)),
                border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "My Tasks & Deadlines",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                "$completedCount/$totalCount completed",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Alarm,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                        strokeCap = StrokeCap.Round
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Tabs: Upcoming, Completed, All
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                listOf(
                    "Upcoming (${upcomingReminders.size})",
                    "Completed (${completedReminders.size})",
                    "All ($totalCount)"
                ).forEachIndexed { index, label ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                label,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Reminders list
            if (displayReminders.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Default.Alarm,
                    title = if (selectedTab == 1) "No Completed Reminders" else "No Upcoming Reminders",
                    description = if (selectedTab == 1) "Check off reminders as you complete your notes and tasks." else "Tap + to schedule an offline reminder with local notifications.",
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 80.dp)
                ) {
                    items(displayReminders, key = { it.id }) { reminder ->
                        val associatedNote = allNotes.firstOrNull { it.id == reminder.noteId }
                        ReminderItemCard(
                            reminder = reminder,
                            note = associatedNote,
                            onToggleComplete = { viewModel.toggleReminderCompleted(reminder) },
                            onSnooze = { millis -> viewModel.snoozeReminder(reminder, millis) },
                            onReschedule = { reschedulingReminder = reminder },
                            onDelete = { viewModel.deleteReminder(reminder.id) },
                            onClick = {
                                if (associatedNote != null) {
                                    if (associatedNote.isLocked && !viewModel.isVaultUnlocked.value) {
                                        Toast.makeText(context, "Locked note. Unlock Vault to open.", Toast.LENGTH_SHORT).show()
                                    } else {
                                        onNavigateToEditor(associatedNote.id, associatedNote.noteType)
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
        }
    }

    // Create Reminder Dialog
    if (showCreateDialog) {
        CreateReminderDialog(
            notes = allNotes.filter { !it.isLocked },
            onDismiss = { showCreateDialog = false },
            onConfirm = { noteId, title, scheduledUtc, repeat ->
                viewModel.scheduleNoteReminder(
                    noteId = noteId,
                    noteTitle = title,
                    timeUtc = scheduledUtc,
                    repeatInterval = repeat
                )
                showCreateDialog = false
            }
        )
    }

    // Reschedule Reminder Dialog
    reschedulingReminder?.let { rem ->
        RescheduleReminderDialog(
            currentUtc = rem.scheduledTimeUtc,
            onDismiss = { reschedulingReminder = null },
            onConfirm = { newUtc ->
                viewModel.rescheduleReminder(rem, newUtc)
                reschedulingReminder = null
            }
        )
    }
}

@Composable
fun ReminderItemCard(
    reminder: ReminderEntity,
    note: NoteEntity?,
    onToggleComplete: () -> Unit,
    onSnooze: (Long) -> Unit,
    onReschedule: () -> Unit,
    onDelete: () -> Unit,
    onClick: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val isDark = isAppInDarkTheme
    val isPastDue = reminder.scheduledTimeUtc < System.currentTimeMillis() && !reminder.isCompleted

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (reminder.isCompleted) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
            } else if (isPastDue) {
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f)
            } else {
                editorialCardContainerColor(isDark)
            }
        ),
        border = BorderStroke(
            0.8.dp,
            if (isPastDue) MaterialTheme.colorScheme.error.copy(alpha = 0.45f)
            else editorialCardBorderColor(isDark)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = reminder.isCompleted,
                onCheckedChange = { onToggleComplete() },
                colors = CheckboxDefaults.colors(
                    checkedColor = MaterialTheme.colorScheme.primary,
                    uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = when {
                        note?.isLocked == true -> "Locked Note Reminder"
                        else -> note?.title?.ifBlank { "Untitled Note" } ?: "Reminder"
                    },
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        textDecoration = if (reminder.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                    ),
                    color = if (reminder.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Default.Event,
                            contentDescription = null,
                            tint = if (isPastDue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            SmartNoteUtils.formatTimestamp(reminder.scheduledTimeUtc),
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isPastDue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (!reminder.repeatInterval.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Icon(Icons.Default.Repeat, contentDescription = null, modifier = Modifier.size(10.dp))
                                Text(
                                    reminder.repeatInterval,
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp)
                                )
                            }
                        }
                    }
                }
            }

            // Quick actions / overflow
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Options")
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    if (!reminder.isCompleted) {
                        DropdownMenuItem(
                            text = { Text("Snooze +15m") },
                            leadingIcon = { Icon(Icons.Default.Snooze, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                onSnooze(15 * 60 * 1000L)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Snooze +1 hour") },
                            leadingIcon = { Icon(Icons.Default.Snooze, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                onSnooze(60 * 60 * 1000L)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Reschedule...") },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                onReschedule()
                            }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text(if (reminder.isCompleted) "Mark Incomplete" else "Mark Complete") },
                        leadingIcon = { Icon(Icons.Default.Check, contentDescription = null) },
                        onClick = {
                            showMenu = false
                            onToggleComplete()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete Reminder", color = MaterialTheme.colorScheme.error) },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                        onClick = {
                            showMenu = false
                            onDelete()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun CreateReminderDialog(
    notes: List<NoteEntity>,
    onDismiss: () -> Unit,
    onConfirm: (noteId: String, title: String, scheduledUtc: Long, repeat: String?) -> Unit
) {
    val context = LocalContext.current
    var selectedNote by remember { mutableStateOf<NoteEntity?>(notes.firstOrNull()) }
    var customTitle by remember { mutableStateOf("") }
    var selectedRepeat by remember { mutableStateOf<String?>(null) } // null, "DAILY", "WEEKLY", "MONTHLY"

    val calendar = remember { Calendar.getInstance().apply { add(Calendar.HOUR_OF_DAY, 1) } }
    var chosenUtc by remember { mutableLongStateOf(calendar.timeInMillis) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Reminder") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // If there are existing notes, select which note to link or custom
                if (notes.isNotEmpty()) {
                    Text("Select Note:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth().height(120.dp)
                    ) {
                        LazyColumn(modifier = Modifier.padding(6.dp)) {
                            items(notes) { note ->
                                val isSelected = selectedNote?.id == note.id
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                                        .clickable { selectedNote = note }
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        note.title.ifBlank { "Untitled Note" },
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                } else {
                    OutlinedTextField(
                        value = customTitle,
                        onValueChange = { customTitle = it },
                        label = { Text("Reminder Title") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Date & Time picker buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            val hostActivity = context.findReminderActivity()
                            if (hostActivity != null && !hostActivity.isFinishing && !hostActivity.isDestroyed) {
                                try {
                                    val c = Calendar.getInstance().apply { timeInMillis = chosenUtc }
                                    DatePickerDialog(
                                        hostActivity,
                                        { _, y, m, d ->
                                            val newCal = Calendar.getInstance().apply {
                                                timeInMillis = chosenUtc
                                                set(Calendar.YEAR, y)
                                                set(Calendar.MONTH, m)
                                                set(Calendar.DAY_OF_MONTH, d)
                                            }
                                            chosenUtc = newCal.timeInMillis
                                        },
                                        c.get(Calendar.YEAR),
                                        c.get(Calendar.MONTH),
                                        c.get(Calendar.DAY_OF_MONTH)
                                    ).show()
                                } catch (_: Exception) {
                                }
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Event, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(SmartNoteUtils.formatShortDate(chosenUtc))
                    }

                    Button(
                        onClick = {
                            val hostActivity = context.findReminderActivity()
                            if (hostActivity != null && !hostActivity.isFinishing && !hostActivity.isDestroyed) {
                                try {
                                    val c = Calendar.getInstance().apply { timeInMillis = chosenUtc }
                                    TimePickerDialog(
                                        hostActivity,
                                        { _, hour, minute ->
                                            val newCal = Calendar.getInstance().apply {
                                                timeInMillis = chosenUtc
                                                set(Calendar.HOUR_OF_DAY, hour)
                                                set(Calendar.MINUTE, minute)
                                                set(Calendar.SECOND, 0)
                                            }
                                            chosenUtc = newCal.timeInMillis
                                        },
                                        c.get(Calendar.HOUR_OF_DAY),
                                        c.get(Calendar.MINUTE),
                                        false
                                    ).show()
                                } catch (_: Exception) {
                                }
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Alarm, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        val cal = Calendar.getInstance().apply { timeInMillis = chosenUtc }
                        val timeStr = String.format(java.util.Locale.getDefault(), "%02d:%02d", cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE))
                        Text(timeStr)
                    }
                }

                // Repeat option
                Text("Repeat Interval:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(null to "None", "DAILY" to "Daily", "WEEKLY" to "Weekly", "MONTHLY" to "Monthly").forEach { (value, label) ->
                        val isSelected = selectedRepeat == value
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.clickable { selectedRepeat = value }
                        ) {
                            Text(
                                label,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val noteId = selectedNote?.id ?: "quick_${System.currentTimeMillis()}"
                    val title = selectedNote?.title?.ifBlank { "Note Reminder" } ?: customTitle.ifBlank { "Quick Reminder" }
                    onConfirm(noteId, title, chosenUtc, selectedRepeat)
                }
            ) {
                Text("Schedule")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun RescheduleReminderDialog(
    currentUtc: Long,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit
) {
    val context = LocalContext.current
    var chosenUtc by remember { mutableLongStateOf(currentUtc.coerceAtLeast(System.currentTimeMillis() + 60000)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Reschedule Reminder") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Select new date and time for alert:", style = MaterialTheme.typography.bodyMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            val hostActivity = context.findReminderActivity()
                            if (hostActivity != null && !hostActivity.isFinishing && !hostActivity.isDestroyed) {
                                try {
                                    val c = Calendar.getInstance().apply { timeInMillis = chosenUtc }
                                    DatePickerDialog(
                                        hostActivity,
                                        { _, y, m, d ->
                                            val newCal = Calendar.getInstance().apply {
                                                timeInMillis = chosenUtc
                                                set(Calendar.YEAR, y)
                                                set(Calendar.MONTH, m)
                                                set(Calendar.DAY_OF_MONTH, d)
                                            }
                                            chosenUtc = newCal.timeInMillis
                                        },
                                        c.get(Calendar.YEAR),
                                        c.get(Calendar.MONTH),
                                        c.get(Calendar.DAY_OF_MONTH)
                                    ).show()
                                } catch (_: Exception) {
                                }
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(SmartNoteUtils.formatShortDate(chosenUtc))
                    }

                    Button(
                        onClick = {
                            val hostActivity = context.findReminderActivity()
                            if (hostActivity != null && !hostActivity.isFinishing && !hostActivity.isDestroyed) {
                                try {
                                    val c = Calendar.getInstance().apply { timeInMillis = chosenUtc }
                                    TimePickerDialog(
                                        hostActivity,
                                        { _, hour, minute ->
                                            val newCal = Calendar.getInstance().apply {
                                                timeInMillis = chosenUtc
                                                set(Calendar.HOUR_OF_DAY, hour)
                                                set(Calendar.MINUTE, minute)
                                                set(Calendar.SECOND, 0)
                                            }
                                            chosenUtc = newCal.timeInMillis
                                        },
                                        c.get(Calendar.HOUR_OF_DAY),
                                        c.get(Calendar.MINUTE),
                                        false
                                    ).show()
                                } catch (_: Exception) {
                                }
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        val cal = Calendar.getInstance().apply { timeInMillis = chosenUtc }
                        Text(String.format(java.util.Locale.getDefault(), "%02d:%02d", cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE)))
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(chosenUtc) }) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

private tailrec fun Context.findReminderActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findReminderActivity()
    else -> null
}
