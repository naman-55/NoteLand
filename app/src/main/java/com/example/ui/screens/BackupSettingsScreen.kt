package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.DashboardCustomize
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.security.BiometricAuthHelper
import com.example.security.SecurityManager
import com.example.ui.components.PinDialog
import com.example.ui.components.SettingsHeaderIllustration
import com.example.ui.components.StaggeredSectionEntrance

import com.example.ui.theme.AppSection
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.DarkSettingsAccent
import com.example.ui.theme.DisplayFontFamily
import com.example.ui.theme.LightSettingsAccent
import com.example.ui.theme.ProvideSectionTheme
import com.example.ui.theme.ScreenBackdropTone
import com.example.ui.theme.editorialBackdropBrush
import com.example.ui.theme.editorialHeaderSubColor
import com.example.ui.theme.editorialHeaderTextColor
import com.example.ui.theme.isAppInDarkTheme
import com.example.ui.viewmodel.NoteViewModel
import com.example.util.SmartNoteUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupSettingsScreen(
    viewModel: NoteViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToDestination: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val isDark = isAppInDarkTheme

    val currentTheme by viewModel.themeMode.collectAsStateWithLifecycle()
    val dynamicColor by viewModel.dynamicColorEnabled.collectAsStateWithLifecycle()
    val userName by viewModel.userProfileName.collectAsStateWithLifecycle()
    val userEmail by viewModel.userProfileEmail.collectAsStateWithLifecycle()
    val searchHistory by viewModel.searchHistory.collectAsStateWithLifecycle()
    val allNotes by viewModel.allActiveNotes.collectAsStateWithLifecycle()
    val attachments by viewModel.attachments.collectAsStateWithLifecycle()
    val deletedNotes by viewModel.deletedNotes.collectAsStateWithLifecycle()
    val deletedAttachments by viewModel.deletedAttachments.collectAsStateWithLifecycle()
    val trashRetentionDays by viewModel.trashRetentionDays.collectAsStateWithLifecycle()
    val isGridView by viewModel.isGridView.collectAsStateWithLifecycle()
    val editorFontSizeSp by viewModel.editorFontSizeSp.collectAsStateWithLifecycle()
    val notificationsEnabled by viewModel.notificationsEnabled.collectAsStateWithLifecycle()
    val pdfAutoSaveEnabled by viewModel.pdfAutoSaveEnabled.collectAsStateWithLifecycle()
    val biometricsEnabled by viewModel.biometricsEnabled.collectAsStateWithLifecycle()
    val autoLockTimeoutMs by viewModel.autoLockTimeoutMs.collectAsStateWithLifecycle()

    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showChangePinDialog by remember { mutableStateOf(false) }
    var showAutoLockPinDialog by remember { mutableStateOf(false) }
    var showAutoLockDialog by remember { mutableStateOf(false) }
    var verifiedAutoLockPin by remember { mutableStateOf<String?>(null) }
    var showBackupExportDialog by remember { mutableStateOf(false) }
    var showBackupRestoreDialog by remember { mutableStateOf(false) }
    var showEmptyTrashConfirm by remember { mutableStateOf(false) }
    var showRetentionPicker by remember { mutableStateOf(false) }
    var localBackups by remember { mutableStateOf(viewModel.listLocalBackups()) }

    val backgroundBrush = editorialBackdropBrush(isDark, ScreenBackdropTone.SETTINGS_EMERALD)
    val headerTextColor = editorialHeaderTextColor(isDark)
    val headerSubColor = editorialHeaderSubColor(isDark)
    val settingsAccent = if (isDark) DarkSettingsAccent else LightSettingsAccent

    ProvideSectionTheme(section = AppSection.SETTINGS, isDark = isDark) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundBrush)
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            modifier = Modifier
                .fillMaxSize()
                .then(if (showChangePinDialog || showAutoLockPinDialog) Modifier.blur(28.dp) else Modifier)
        ) { innerPadding ->
            val navBottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 680.dp)
                        .align(Alignment.TopCenter)
                        .padding(horizontal = 16.dp)
                        .testTag("settings_lazy_column"),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                    contentPadding = PaddingValues(top = 24.dp, bottom = 112.dp + navBottomPadding)
                ) {
                    // Clean Header with Emerald Gear Illustration
                    item {
                        StaggeredSectionEntrance(index = 0) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .statusBarsPadding()
                                .padding(top = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Text(
                                    text = "Settings",
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        fontFamily = DisplayFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 31.sp
                                    ),
                                    color = headerTextColor
                                )
                                Text(
                                    text = "Manage your app, data and experience",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp),
                                    color = headerSubColor
                                )
                            }
                            SettingsHeaderIllustration(isDark = isDark)
                        }
                        }
                    }

                    // Slim Workspace Profile Card
                    item {
                        StaggeredSectionEntrance(index = 1) {
                            SettingsProfileHeroCard(
                                userName = userName,
                                userEmail = userEmail,
                                onEditClick = { showEditProfileDialog = true }
                            )
                        }
                    }

                    // Section 1: Appearance
                    item {
                        StaggeredSectionEntrance(index = 2) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            SettingsSectionHeader("Appearance", icon = Icons.Default.Palette)
                            SettingsGroupCard {
                                Column(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    val resolvedMode = AppThemeMode.fromKey(currentTheme)
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        SettingsRowIcon(
                                            icon = Icons.Default.Palette,
                                            iconColor = settingsAccent,
                                            iconBg = settingsAccent.copy(alpha = 0.14f)
                                        )
                                        Column {
                                            Text(
                                                text = "Theme",
                                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
                                            )
                                            Text(
                                                text = resolvedMode.displayName,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    ThemeSelectorRow(
                                        selectedThemeKey = currentTheme,
                                        onSelectTheme = { viewModel.setThemeMode(it.key) }
                                    )
                                }

                                SettingsDivider()

                                SettingsToggleRow(
                                    title = "Dynamic Color",
                                    subtitle = "Match wallpaper accents (Android 12+)",
                                    icon = Icons.Default.AutoAwesome,
                                    iconColor = settingsAccent,
                                    iconBg = settingsAccent.copy(alpha = 0.14f),
                                    checked = dynamicColor,
                                    onCheckedChange = { viewModel.setDynamicColorEnabled(it) },
                                    switchTestTag = "dynamic_color_switch"
                                )

                                SettingsDivider()

                                SettingsInlineSegmentedRow(
                                    title = "Notes Layout",
                                    subtitle = if (isGridView) "2-column grid" else "Compact list",
                                    icon = if (isGridView) Icons.Default.GridView else Icons.AutoMirrored.Filled.FormatListBulleted,
                                    iconColor = Color(0xFF0EA5E9),
                                    iconBg = Color(0xFF0EA5E9).copy(alpha = 0.13f),
                                    options = listOf(
                                        SegmentedPillOption(
                                            label = "Grid",
                                            selected = isGridView,
                                            testTag = "home_layout_grid_chip",
                                            onClick = { if (!isGridView) viewModel.toggleGridView() }
                                        ),
                                        SegmentedPillOption(
                                            label = "List",
                                            selected = !isGridView,
                                            testTag = "home_layout_list_chip",
                                            onClick = { if (isGridView) viewModel.toggleGridView() }
                                        )
                                    )
                                )
                            }
                        }
                        }
                    }

                    // Section 1b: Editor Customization
                    item {
                        StaggeredSectionEntrance(index = 3) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            SettingsSectionHeader("Editor Customization", icon = Icons.Default.Tune)
                            SettingsGroupCard {
                                val textSizeSubtitle = when {
                                    editorFontSizeSp < 15.5f -> "Small (${editorFontSizeSp.toInt()}sp)"
                                    editorFontSizeSp > 17.5f -> "Large (${editorFontSizeSp.toInt()}sp)"
                                    else -> "Default (${editorFontSizeSp.toInt()}sp)"
                                }
                                SettingsInlineSegmentedRow(
                                    title = "Text Size",
                                    subtitle = textSizeSubtitle,
                                    icon = Icons.Default.FormatSize,
                                    iconColor = Color(0xFF8B5CF6),
                                    iconBg = Color(0xFF8B5CF6).copy(alpha = 0.13f),
                                    options = listOf(
                                        SegmentedPillOption(
                                            label = "S",
                                            selected = editorFontSizeSp < 15.5f,
                                            testTag = "editor_font_size_compact",
                                            onClick = { viewModel.setEditorFontSizeSp(14.0f) }
                                        ),
                                        SegmentedPillOption(
                                            label = "M",
                                            selected = editorFontSizeSp in 15.5f..17.5f,
                                            testTag = "editor_font_size_standard",
                                            onClick = { viewModel.setEditorFontSizeSp(16.5f) }
                                        ),
                                        SegmentedPillOption(
                                            label = "L",
                                            selected = editorFontSizeSp > 17.5f,
                                            testTag = "editor_font_size_large",
                                            onClick = { viewModel.setEditorFontSizeSp(19.5f) }
                                        )
                                    )
                                )
                            }
                        }
                        }
                    }

                    // Section 2: Security
                    item {
                        StaggeredSectionEntrance(index = 4) {
                        val availability = remember(context) { BiometricAuthHelper.checkAvailability(context) }
                        val biometricReady = availability !is BiometricAuthHelper.Availability.Unavailable
                        val biometricSubtitle = when (availability) {
                            is BiometricAuthHelper.Availability.BiometricReady -> "Fingerprint or face unlock"
                            is BiometricAuthHelper.Availability.DeviceCredentialReady -> "Device screen lock fallback"
                            is BiometricAuthHelper.Availability.Unavailable -> availability.reason
                        }
                        val autoLockLabel = SecurityManager.formatAutoLockLabel(autoLockTimeoutMs)

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            SettingsSectionHeader("Security", icon = Icons.Default.Lock)
                            SettingsGroupCard {
                                SettingsNavigationRow(
                                    title = if (viewModel.isVaultInitialized()) "Change Vault PIN" else "Setup Vault PIN",
                                    subtitle = if (viewModel.isVaultInitialized()) "Update your 4-digit passcode" else "Create a secure 4-digit passcode",
                                    icon = Icons.Default.Key,
                                    iconColor = Color(0xFFD97706),
                                    iconBg = Color(0xFFD97706).copy(alpha = 0.13f),
                                    onClick = { showChangePinDialog = true },
                                    modifier = Modifier.testTag("change_pin_settings_row")
                                )

                                SettingsDivider()

                                SettingsToggleRow(
                                    title = "Biometric Unlock",
                                    subtitle = biometricSubtitle,
                                    icon = Icons.Default.Fingerprint,
                                    iconColor = Color(0xFF0EA5E9),
                                    iconBg = Color(0xFF0EA5E9).copy(alpha = 0.13f),
                                    checked = biometricsEnabled && biometricReady,
                                    enabled = biometricReady,
                                    onCheckedChange = { checked -> viewModel.setBiometricsEnabled(checked) },
                                    switchTestTag = "biometrics_toggle_switch"
                                )

                                SettingsDivider()

                                SettingsNavigationRow(
                                    title = "Auto-Lock Vault",
                                    subtitle = if (autoLockTimeoutMs <= 0L) "Locks immediately on exit" else "Locks $autoLockLabel after leaving",
                                    icon = Icons.Default.Timer,
                                    iconColor = Color(0xFF7C3AED),
                                    iconBg = Color(0xFF7C3AED).copy(alpha = 0.13f),
                                    trailingBadge = autoLockLabel,
                                    onClick = { showAutoLockPinDialog = true },
                                    modifier = Modifier.testTag("auto_lock_settings_row")
                                )
                            }
                        }
                        }
                    }

                    // Section 3: Backup and Sync
                    item {
                        StaggeredSectionEntrance(index = 5) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            SettingsSectionHeader("Backup and Sync", icon = Icons.Default.CloudQueue)
                            SettingsGroupCard {
                                SettingsNavigationRow(
                                    title = "Export Backup Archive",
                                    subtitle = "Save encrypted ZIP snapshot of notes & media",
                                    icon = Icons.Default.CloudDownload,
                                    iconColor = Color(0xFF2563EB),
                                    iconBg = Color(0xFF2563EB).copy(alpha = 0.13f),
                                    trailingBadge = "ZIP",
                                    onClick = { showBackupExportDialog = true },
                                    modifier = Modifier.testTag("settings_backup_export_row")
                                )

                                SettingsDivider()

                                SettingsNavigationRow(
                                    title = "Restore Backup Archive",
                                    subtitle = "${localBackups.size} local archive(s) available",
                                    icon = Icons.Default.Restore,
                                    iconColor = Color(0xFF059669),
                                    iconBg = Color(0xFF059669).copy(alpha = 0.13f),
                                    trailingBadge = "${localBackups.size}",
                                    onClick = {
                                        localBackups = viewModel.listLocalBackups()
                                        showBackupRestoreDialog = true
                                    },
                                    modifier = Modifier.testTag("settings_backup_restore_row")
                                )
                            }
                        }
                        }
                    }

                    // Section 4: Notifications & Preferences
                    item {
                        StaggeredSectionEntrance(index = 5) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            SettingsSectionHeader("Notifications", icon = Icons.Default.Notifications)
                            SettingsGroupCard {
                                SettingsToggleRow(
                                    title = "Notifications",
                                    subtitle = "Alerts for reminders & tasks",
                                    icon = Icons.Default.Notifications,
                                    iconColor = settingsAccent,
                                    iconBg = settingsAccent.copy(alpha = 0.14f),
                                    checked = notificationsEnabled,
                                    onCheckedChange = { viewModel.setNotificationsEnabled(it) },
                                    switchTestTag = "notifications_switch"
                                )

                                SettingsDivider()

                                SettingsToggleRow(
                                    title = "PDF Auto-Save",
                                    subtitle = "Save PDF highlights & ink automatically",
                                    icon = Icons.Default.PictureAsPdf,
                                    iconColor = Color(0xFFE11D48),
                                    iconBg = Color(0xFFE11D48).copy(alpha = 0.13f),
                                    checked = pdfAutoSaveEnabled,
                                    onCheckedChange = { viewModel.setPdfAutoSaveEnabled(it) },
                                    switchTestTag = "pdf_autosave_switch"
                                )
                            }
                        }
                        }
                    }

                    // Section 3b: Home Screen Widgets
                    item {
                        StaggeredSectionEntrance(index = 5) {
                        val pinHelper: (Class<out androidx.glance.appwidget.GlanceAppWidgetReceiver>, String) -> Unit = { receiver, label ->
                            val pinned = com.example.widget.WidgetUpdateHelper.requestPinWidget(context, receiver)
                            val msg = if (pinned) {
                                "Adding $label widget to your home screen…"
                            } else {
                                "Long-press your home screen and select Widgets → NoteLand to add $label."
                            }
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            SettingsSectionHeader("Home Screen Widgets", icon = Icons.Default.Widgets)
                            SettingsGroupCard {
                                SettingsNavigationRow(
                                    title = "Daily Tasks",
                                    subtitle = "Interactive checklists & daily progress",
                                    icon = Icons.Default.CheckBox,
                                    iconColor = Color(0xFF6E5AE0),
                                    iconBg = Color(0xFF6E5AE0).copy(alpha = 0.13f),
                                    trailingBadge = "Pin",
                                    onClick = {
                                        pinHelper(com.example.widget.DailyTasksWidgetReceiver::class.java, "Daily Tasks")
                                    },
                                    modifier = Modifier.testTag("pin_daily_tasks_widget_row")
                                )

                                SettingsDivider()

                                SettingsNavigationRow(
                                    title = "Reminders",
                                    subtitle = "Upcoming, overdue & today's alerts",
                                    icon = Icons.Default.Alarm,
                                    iconColor = Color(0xFFE11D48),
                                    iconBg = Color(0xFFE11D48).copy(alpha = 0.13f),
                                    trailingBadge = "Pin",
                                    onClick = {
                                        pinHelper(com.example.widget.RemindersWidgetReceiver::class.java, "Reminders")
                                    },
                                    modifier = Modifier.testTag("pin_reminders_widget_row")
                                )

                                SettingsDivider()

                                SettingsNavigationRow(
                                    title = "Collections Multi-Tile",
                                    subtitle = "Folders, Diary, Tasks, Board & quick capture",
                                    icon = Icons.Default.DashboardCustomize,
                                    iconColor = Color(0xFF059669),
                                    iconBg = Color(0xFF059669).copy(alpha = 0.13f),
                                    trailingBadge = "Pin",
                                    onClick = {
                                        pinHelper(com.example.widget.CollectionsTilesWidgetReceiver::class.java, "Collections")
                                    },
                                    modifier = Modifier.testTag("pin_collections_widget_row")
                                )
                            }
                        }
                        }
                    }

                    // Section 5: Storage
                    item {
                        StaggeredSectionEntrance(index = 5) {
                        val totalAttachmentsBytes = remember(attachments) {
                            attachments.sumOf { it.sizeBytes }
                        }
                        val totalTrashCount = deletedNotes.size + deletedAttachments.size
                        val retentionLabel = when (trashRetentionDays) {
                            7 -> "7 days"
                            14 -> "14 days"
                            30 -> "30 days"
                            90 -> "90 days"
                            else -> "Never"
                        }
                        val storageSummary = "${allNotes.size} notes · ${attachments.size} files (${SmartNoteUtils.formatFileSize(totalAttachmentsBytes)})"
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            SettingsSectionHeader("Storage", icon = Icons.Default.Storage)
                            SettingsGroupCard {
                                SettingsNavigationRow(
                                    title = "Workspace Storage",
                                    subtitle = storageSummary,
                                    icon = Icons.Default.Storage,
                                    iconColor = Color(0xFF0EA5E9),
                                    iconBg = Color(0xFF0EA5E9).copy(alpha = 0.13f),
                                    trailingBadge = "$totalTrashCount trash",
                                    onClick = {
                                        Toast.makeText(context, storageSummary, Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.testTag("settings_storage_summary_row")
                                )

                                SettingsDivider()

                                SettingsNavigationRow(
                                    title = "Recycle Bin Retention",
                                    subtitle = if (trashRetentionDays > 0) "Auto-delete after $retentionLabel" else "Keep deleted items until manually emptied",
                                    icon = Icons.Default.Schedule,
                                    iconColor = Color(0xFFF59E0B),
                                    iconBg = Color(0xFFF59E0B).copy(alpha = 0.14f),
                                    trailingBadge = retentionLabel,
                                    onClick = { showRetentionPicker = true },
                                    modifier = Modifier.testTag("settings_trash_retention_row")
                                )

                                SettingsDivider()

                                SettingsNavigationRow(
                                    title = "Empty Recycle Bin",
                                    subtitle = if (totalTrashCount == 0) "Recycle Bin is empty" else "Permanently remove $totalTrashCount deleted item(s)",
                                    icon = Icons.Default.DeleteSweep,
                                    iconColor = Color(0xFFEF4444),
                                    iconBg = Color(0xFFEF4444).copy(alpha = 0.13f),
                                    trailingBadge = "$totalTrashCount",
                                    onClick = {
                                        if (totalTrashCount == 0) {
                                            Toast.makeText(context, "Recycle Bin is already empty", Toast.LENGTH_SHORT).show()
                                        } else {
                                            showEmptyTrashConfirm = true
                                        }
                                    },
                                    modifier = Modifier.testTag("settings_empty_trash_row")
                                )

                                SettingsDivider()

                                SettingsNavigationRow(
                                    title = "Clear Search History",
                                    subtitle = "Remove recent queries on device",
                                    icon = Icons.Default.CleaningServices,
                                    iconColor = Color(0xFF0D9488),
                                    iconBg = Color(0xFF0D9488).copy(alpha = 0.13f),
                                    trailingBadge = "${searchHistory.size}",
                                    onClick = {
                                        if (searchHistory.isEmpty()) {
                                            Toast.makeText(context, "Search history is already empty", Toast.LENGTH_SHORT).show()
                                        } else {
                                            viewModel.clearSearchHistory()
                                            Toast.makeText(context, "Search history cleared", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier.testTag("clear_search_history_row")
                                )
                            }
                        }
                        }
                    }

                    // Minimal Footer
                    item {
                        Text(
                            text = "NoteLand v${com.example.BuildConfig.VERSION_NAME} · On-Device Vault",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp)
                        )
                    }
                }
            }
        }

        if (showChangePinDialog) {
            ChangePinBlurOverlay(
                onDismiss = { showChangePinDialog = false },
                onVerifyCurrentPin = if (viewModel.isVaultInitialized()) { pin -> viewModel.verifyVaultPin(pin) } else null,
                onUpdatePin = { oldPin, newPin -> viewModel.updateVaultPin(oldPin, newPin) }
            )
        }

        if (showAutoLockPinDialog) {
            PinDialog(
                title = "Verify Vault PIN",
                message = "Enter your 4-digit Vault PIN before changing auto-lock time",
                showDefaultPinHint = false,
                confirmButtonText = "Verify",
                onConfirm = { pin ->
                    val ok = viewModel.verifyVaultPin(pin)
                    if (ok) {
                        verifiedAutoLockPin = pin
                        showAutoLockPinDialog = false
                        showAutoLockDialog = true
                    }
                    ok
                },
                onDismiss = {
                    verifiedAutoLockPin = null
                    showAutoLockPinDialog = false
                }
            )
        }
    }

    if (showEditProfileDialog) {
        EditProfileDialog(
            initialName = userName,
            initialEmail = userEmail,
            onSave = { name, email ->
                viewModel.updateUserProfile(name, email)
                showEditProfileDialog = false
            },
            onDismiss = { showEditProfileDialog = false }
        )
    }

    if (showAutoLockDialog) {
        AutoLockSelectionDialog(
            currentTimeoutMs = autoLockTimeoutMs,
            onSelect = { timeoutMs ->
                val pin = verifiedAutoLockPin
                if (pin != null) {
                    viewModel.setAutoLockTimeoutWithPin(pin, timeoutMs)
                }
            },
            onDismiss = {
                verifiedAutoLockPin = null
                showAutoLockDialog = false
            }
        )
    }

    if (showBackupExportDialog) {
        ZipBackupExportDialog(
            viewModel = viewModel,
            onDismiss = {
                localBackups = viewModel.listLocalBackups()
                showBackupExportDialog = false
            }
        )
    }

    if (showBackupRestoreDialog) {
        ZipBackupRestoreDialog(
            viewModel = viewModel,
            availableBackups = localBackups,
            onDismiss = { showBackupRestoreDialog = false }
        )
    }

    if (showRetentionPicker) {
        val options = listOf(
            7 to "7 days",
            14 to "14 days",
            30 to "30 days (Default)",
            90 to "90 days",
            -1 to "Never auto-delete"
        )
        AlertDialog(
            onDismissRequest = { showRetentionPicker = false },
            title = { Text("Recycle Bin Retention") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    options.forEach { (days, label) ->
                        TextButton(
                            onClick = {
                                viewModel.setTrashRetentionDays(days)
                                showRetentionPicker = false
                                Toast.makeText(context, "Retention updated: $label", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = if (trashRetentionDays == days) "✓ $label" else label,
                                fontWeight = if (trashRetentionDays == days) FontWeight.Bold else FontWeight.Normal,
                                color = if (trashRetentionDays == days) settingsAccent else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showRetentionPicker = false }) {
                    Text("Close")
                }
            }
        )
    }

    if (showEmptyTrashConfirm) {
        val totalTrashCount = deletedNotes.size + deletedAttachments.size
        AlertDialog(
            onDismissRequest = { showEmptyTrashConfirm = false },
            title = { Text("Empty Recycle Bin?") },
            text = {
                Text("All $totalTrashCount deleted item(s) will be permanently removed. This cannot be undone.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.emptyRecycleBin()
                        showEmptyTrashConfirm = false
                        Toast.makeText(context, "Recycle Bin emptied", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Empty")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEmptyTrashConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
    }
}
